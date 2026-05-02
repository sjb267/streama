from __future__ import annotations

import functools
import hashlib
import json
import logging
import queue
import threading
import time
from collections import OrderedDict
from dataclasses import dataclass, field
from typing import Any

from .config import RabbitMQSettings
from .models import AuditRequestMessage, AuditResultMessage
from .service import AuditWorkflow

LOGGER = logging.getLogger(__name__)


@dataclass(frozen=True)
class _RequestIdentity:
    raw_request_id: str
    dedupe_request_id: str
    video_id: str
    audit_version: int | None
    items_signature: str
    file_ids: tuple[str, ...]
    item_count: int

    @property
    def dedupe_key(self) -> tuple[str, str, int | None, str]:
        return (self.dedupe_request_id, self.video_id, self.audit_version, self.items_signature)


@dataclass
class _DeliveryContext:
    channel: Any
    delivery_tag: int
    redelivered: bool


@dataclass
class _QueuedRequest:
    identity: _RequestIdentity
    request: AuditRequestMessage
    raw_body: bytes


@dataclass
class _InFlightRequest:
    identity: _RequestIdentity
    request: AuditRequestMessage
    raw_body: bytes
    deliveries: list[_DeliveryContext] = field(default_factory=list)


@dataclass
class _CompletedRequest:
    identity: _RequestIdentity
    result_message: AuditResultMessage
    cached_at_monotonic: float


class RabbitAuditWorker:
    def __init__(self, settings: RabbitMQSettings, workflow: AuditWorkflow) -> None:
        self._settings = settings
        self._workflow = workflow
        self._thread: threading.Thread | None = None
        self._worker_thread: threading.Thread | None = None
        self._stop_event = threading.Event()
        self._connection = None
        self._channel = None
        self._pika = None
        self._connected = False
        self._last_error: str | None = None
        self._state_lock = threading.Lock()
        self._work_queue: queue.Queue[object] = queue.Queue()
        self._worker_stop = object()
        self._inflight_requests: dict[tuple[str, str, int | None, str], _InFlightRequest] = {}
        self._completed_requests: OrderedDict[tuple[str, str, int | None, str], _CompletedRequest] = OrderedDict()

    def start(self) -> None:
        if not self._settings.enabled:
            return
        if self._thread and self._thread.is_alive():
            return
        self._pika = self._import_pika()
        self._stop_event.clear()
        self._worker_thread = threading.Thread(target=self._worker_loop, name="rabbitmq-audit-worker", daemon=True)
        self._thread = threading.Thread(target=self._run, name="rabbitmq-consumer", daemon=True)
        self._worker_thread.start()
        self._thread.start()

    def stop(self) -> None:
        if not self._settings.enabled:
            return
        self._stop_event.set()
        self._work_queue.put(self._worker_stop)
        if self._connection is not None and getattr(self._connection, "is_open", False):
            try:
                self._connection.add_callback_threadsafe(self._connection.close)
            except Exception:
                LOGGER.debug("Failed to close RabbitMQ connection via callback.", exc_info=True)
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=5)
        if self._worker_thread and self._worker_thread.is_alive():
            self._worker_thread.join(timeout=5)

    def health_snapshot(self) -> dict[str, Any]:
        return {
            "enabled": self._settings.enabled,
            "connected": self._connected,
            "queue": self._settings.request_queue,
            "exchange": self._settings.exchange,
            "lastError": self._last_error,
        }

    def _import_pika(self):
        try:
            import pika
        except ModuleNotFoundError as exc:
            raise RuntimeError("Missing dependency `pika`. Install it from requirements.txt before starting the service.") from exc
        return pika

    def _run(self) -> None:
        while not self._stop_event.is_set():
            try:
                self._open_connection()
                self._connected = True
                self._last_error = None
                LOGGER.info("RabbitMQ consumer connected to %s:%s", self._settings.host, self._settings.port)
                while not self._stop_event.is_set():
                    self._connection.process_data_events(time_limit=1)
                break
            except Exception as exc:
                self._connected = False
                self._last_error = str(exc)
                LOGGER.warning("RabbitMQ worker loop failed.", exc_info=True)
                if self._stop_event.wait(max(1, self._settings.reconnect_interval_seconds)):
                    break
            finally:
                self._close_connection()

    def _open_connection(self) -> None:
        credentials = self._pika.PlainCredentials(self._settings.username, self._settings.password)
        parameters = self._pika.ConnectionParameters(
            host=self._settings.host,
            port=self._settings.port,
            virtual_host=self._settings.virtual_host,
            credentials=credentials,
            heartbeat=self._settings.heartbeat,
            blocked_connection_timeout=self._settings.blocked_connection_timeout,
        )
        self._connection = self._pika.BlockingConnection(parameters)
        self._channel = self._connection.channel()
        if self._settings.auto_declare:
            self._declare_topology()
        self._channel.basic_qos(prefetch_count=self._settings.prefetch_count)
        self._channel.basic_consume(
            queue=self._settings.request_queue,
            on_message_callback=self._on_message,
            auto_ack=False,
        )

    def _close_connection(self) -> None:
        channel = self._channel
        connection = self._connection
        self._channel = None
        self._connection = None
        self._connected = False
        self._clear_pending_deliveries()
        if channel is not None and getattr(channel, "is_open", False):
            try:
                channel.close()
            except Exception:
                LOGGER.debug("Closing RabbitMQ channel failed.", exc_info=True)
        if connection is not None and getattr(connection, "is_open", False):
            try:
                connection.close()
            except Exception:
                LOGGER.debug("Closing RabbitMQ connection failed.", exc_info=True)

    def _worker_loop(self) -> None:
        while True:
            try:
                work_item = self._work_queue.get(timeout=0.5)
            except queue.Empty:
                if self._stop_event.is_set():
                    break
                continue

            try:
                if work_item is self._worker_stop:
                    break
                if not isinstance(work_item, _QueuedRequest):
                    continue

                try:
                    result_message = self._workflow.process_request(work_item.request)
                except Exception as exc:
                    LOGGER.exception(
                        "Failed to process audit request in worker thread, fallback to manual review. %s",
                        self._identity_log_context(work_item.identity),
                    )
                    result_message = self._workflow.build_failure_result_from_payload(work_item.raw_body, exc)

                with self._state_lock:
                    state = self._inflight_requests.pop(work_item.identity.dedupe_key, None)
                    deliveries = list(state.deliveries) if state is not None else []
                    self._store_completed_locked(work_item.identity, result_message)

                if not deliveries:
                    LOGGER.info(
                        "Completed audit request without active RabbitMQ deliveries; cached result for future redelivery. %s",
                        self._identity_log_context(work_item.identity),
                    )
                    continue

                if not self._schedule_delivery_dispatch(deliveries, result_message, work_item.identity):
                    LOGGER.warning(
                        "Failed to schedule RabbitMQ completion callback; cached result for future redelivery. %s",
                        self._identity_log_context(work_item.identity),
                    )
            finally:
                self._work_queue.task_done()

    def _declare_topology(self) -> None:
        self._channel.exchange_declare(
            exchange=self._settings.exchange,
            exchange_type=self._settings.exchange_type,
            durable=True,
        )
        self._channel.exchange_declare(exchange=self._settings.request_dlx, exchange_type="direct", durable=True)
        self._channel.exchange_declare(exchange=self._settings.result_dlx, exchange_type="direct", durable=True)

        self._channel.queue_declare(
            queue=self._settings.request_queue,
            durable=True,
            arguments={
                "x-dead-letter-exchange": self._settings.request_dlx,
                "x-dead-letter-routing-key": self._settings.request_queue,
            },
        )
        self._channel.queue_declare(queue=self._settings.request_dlq, durable=True)
        self._channel.queue_bind(
            queue=self._settings.request_queue,
            exchange=self._settings.exchange,
            routing_key=self._settings.request_routing_key,
        )
        self._channel.queue_bind(
            queue=self._settings.request_dlq,
            exchange=self._settings.request_dlx,
            routing_key=self._settings.request_queue,
        )

        self._channel.queue_declare(
            queue=self._settings.result_queue,
            durable=True,
            arguments={
                "x-dead-letter-exchange": self._settings.result_dlx,
                "x-dead-letter-routing-key": self._settings.result_queue,
            },
        )
        self._channel.queue_declare(queue=self._settings.result_dlq, durable=True)
        self._channel.queue_bind(
            queue=self._settings.result_queue,
            exchange=self._settings.exchange,
            routing_key=self._settings.result_routing_key,
        )
        self._channel.queue_bind(
            queue=self._settings.result_dlq,
            exchange=self._settings.result_dlx,
            routing_key=self._settings.result_queue,
        )

    def _on_message(self, channel, method, properties, body: bytes) -> None:
        del properties
        delivery = _DeliveryContext(
            channel=channel,
            delivery_tag=method.delivery_tag,
            redelivered=bool(getattr(method, "redelivered", False)),
        )
        try:
            request_message = AuditRequestMessage.model_validate_json(body)
        except Exception as exc:
            LOGGER.exception("Failed to process audit request, fallback to manual review.")
            result_message = self._workflow.build_failure_result_from_payload(body, exc)
            self._dispatch_delivery_results([delivery], result_message, None)
            return

        identity = self._build_request_identity(request_message)
        LOGGER.info(
            "Received audit request: %s redelivered=%s",
            self._identity_log_context(identity),
            delivery.redelivered,
        )

        completed_result: AuditResultMessage | None = None
        queue_item: _QueuedRequest | None = None

        with self._state_lock:
            self._log_request_collisions_locked(identity)
            completed = self._get_completed_locked(identity.dedupe_key)
            if completed is not None:
                completed_result = completed.result_message
            else:
                state = self._inflight_requests.get(identity.dedupe_key)
                if state is not None:
                    state.deliveries.append(delivery)
                    LOGGER.info(
                        "Merged duplicate RabbitMQ delivery into in-flight request: %s waitingDeliveries=%s",
                        self._identity_log_context(identity),
                        len(state.deliveries),
                    )
                    return

                state = _InFlightRequest(identity=identity, request=request_message, raw_body=body)
                state.deliveries.append(delivery)
                self._inflight_requests[identity.dedupe_key] = state
                queue_item = _QueuedRequest(identity=identity, request=request_message, raw_body=body)

        if completed_result is not None:
            LOGGER.info("Reusing cached audit result for duplicate request: %s", self._identity_log_context(identity))
            self._dispatch_delivery_results([delivery], completed_result, identity)
            return

        LOGGER.info("Queued audit request for worker thread: %s", self._identity_log_context(identity))
        self._work_queue.put(queue_item)

    def _build_request_identity(self, request: AuditRequestMessage) -> _RequestIdentity:
        raw_request_id = (request.request_id or "").strip()
        items_payload = [item.model_dump(by_alias=True, exclude_none=False) for item in request.items]
        items_json = json.dumps(items_payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        items_signature = hashlib.sha256(items_json.encode("utf-8")).hexdigest()[:16]
        file_ids = tuple(
            item.file_id
            or f"index:{item.file_index if item.file_index is not None else item_index}"
            for item_index, item in enumerate(request.items)
        )
        if raw_request_id:
            dedupe_request_id = raw_request_id
        else:
            dedupe_request_id = f"missing:{request.video_id}:{request.audit_version or 0}:{items_signature}"
            LOGGER.error(
                "Received audit request without requestId; using fallback dedupe key. videoId=%s auditVersion=%s fileIds=%s",
                request.video_id,
                request.audit_version,
                list(file_ids),
            )

        return _RequestIdentity(
            raw_request_id=raw_request_id,
            dedupe_request_id=dedupe_request_id,
            video_id=request.video_id,
            audit_version=request.audit_version,
            items_signature=items_signature,
            file_ids=file_ids,
            item_count=len(request.items),
        )

    def _identity_log_context(self, identity: _RequestIdentity) -> str:
        request_id = identity.raw_request_id or "<missing>"
        return (
            f"requestId={request_id} videoId={identity.video_id} auditVersion={identity.audit_version} "
            f"itemCount={identity.item_count} fileIds={list(identity.file_ids)}"
        )

    def _log_request_collisions_locked(self, identity: _RequestIdentity) -> None:
        if not identity.raw_request_id:
            return

        for state in self._inflight_requests.values():
            other = state.identity
            if other.raw_request_id == identity.raw_request_id and other.dedupe_key != identity.dedupe_key:
                LOGGER.error(
                    "Request identity collision detected for in-flight requestId=%s. "
                    "Incoming videoId=%s auditVersion=%s fileIds=%s signature=%s; "
                    "existing videoId=%s auditVersion=%s fileIds=%s signature=%s. Processing separately without dedupe merge.",
                    identity.raw_request_id,
                    identity.video_id,
                    identity.audit_version,
                    list(identity.file_ids),
                    identity.items_signature,
                    other.video_id,
                    other.audit_version,
                    list(other.file_ids),
                    other.items_signature,
                )

        for completed in self._completed_requests.values():
            other = completed.identity
            if other.raw_request_id == identity.raw_request_id and other.dedupe_key != identity.dedupe_key:
                LOGGER.error(
                    "Request identity collision detected for completed requestId=%s. "
                    "Incoming videoId=%s auditVersion=%s fileIds=%s signature=%s; "
                    "cached videoId=%s auditVersion=%s fileIds=%s signature=%s. Processing separately without dedupe merge.",
                    identity.raw_request_id,
                    identity.video_id,
                    identity.audit_version,
                    list(identity.file_ids),
                    identity.items_signature,
                    other.video_id,
                    other.audit_version,
                    list(other.file_ids),
                    other.items_signature,
                )

    def _get_completed_locked(self, dedupe_key: tuple[str, str, int | None, str]) -> _CompletedRequest | None:
        self._prune_completed_locked()
        completed = self._completed_requests.get(dedupe_key)
        if completed is None:
            return None
        self._completed_requests.move_to_end(dedupe_key)
        return completed

    def _store_completed_locked(self, identity: _RequestIdentity, result_message: AuditResultMessage) -> None:
        dedupe_key = identity.dedupe_key
        self._completed_requests.pop(dedupe_key, None)
        self._completed_requests[dedupe_key] = _CompletedRequest(
            identity=identity,
            result_message=result_message,
            cached_at_monotonic=time.monotonic(),
        )
        self._prune_completed_locked()

    def _prune_completed_locked(self) -> None:
        ttl_seconds = max(0, self._settings.request_dedupe_ttl_seconds)
        max_entries = max(0, self._settings.request_dedupe_max_entries)
        if ttl_seconds == 0 or max_entries == 0:
            self._completed_requests.clear()
            return

        now = time.monotonic()
        while self._completed_requests:
            oldest_key, oldest_entry = next(iter(self._completed_requests.items()))
            if now - oldest_entry.cached_at_monotonic <= ttl_seconds:
                break
            self._completed_requests.pop(oldest_key, None)

        while len(self._completed_requests) > max_entries:
            self._completed_requests.popitem(last=False)

    def _clear_pending_deliveries(self) -> None:
        with self._state_lock:
            pending_delivery_count = 0
            for state in self._inflight_requests.values():
                pending_delivery_count += len(state.deliveries)
                state.deliveries.clear()
        if pending_delivery_count:
            LOGGER.warning(
                "Cleared %s pending RabbitMQ deliveries after connection close; awaiting redelivery for unfinished requests.",
                pending_delivery_count,
            )

    def _schedule_delivery_dispatch(
        self,
        deliveries: list[_DeliveryContext],
        result_message: AuditResultMessage,
        identity: _RequestIdentity,
    ) -> bool:
        connection = self._connection
        if connection is None or not getattr(connection, "is_open", False):
            return False

        try:
            connection.add_callback_threadsafe(
                functools.partial(self._dispatch_delivery_results, list(deliveries), result_message, identity)
            )
            return True
        except Exception:
            LOGGER.warning(
                "Failed to schedule RabbitMQ completion callback. %s",
                self._identity_log_context(identity),
                exc_info=True,
            )
            return False

    def _dispatch_delivery_results(
        self,
        deliveries: list[_DeliveryContext],
        result_message: AuditResultMessage,
        identity: _RequestIdentity | None,
    ) -> None:
        request_context = self._identity_log_context(identity) if identity is not None else "requestId=<unknown>"
        for delivery in deliveries:
            channel = delivery.channel
            if channel is None or not getattr(channel, "is_open", False):
                LOGGER.warning(
                    "Skipping stale RabbitMQ delivery completion because channel is not open. %s deliveryTag=%s",
                    request_context,
                    delivery.delivery_tag,
                )
                continue

            try:
                self._publish_result(result_message, channel=channel)
                channel.basic_ack(delivery_tag=delivery.delivery_tag)
            except Exception:
                LOGGER.exception(
                    "Publishing audit result failed, message moved to DLQ. %s deliveryTag=%s",
                    request_context,
                    delivery.delivery_tag,
                )
                try:
                    if getattr(channel, "is_open", False):
                        channel.basic_nack(delivery_tag=delivery.delivery_tag, requeue=False)
                except Exception:
                    LOGGER.warning(
                        "Failed to nack RabbitMQ delivery after publish failure. %s deliveryTag=%s",
                        request_context,
                        delivery.delivery_tag,
                        exc_info=True,
                    )

    def _publish_result(self, result_message: AuditResultMessage, channel=None) -> None:
        body = json.dumps(result_message.to_message_dict(), ensure_ascii=False).encode("utf-8")
        properties = self._pika.BasicProperties(content_type="application/json", delivery_mode=2)
        publish_channel = channel or self._channel
        if publish_channel is None:
            raise RuntimeError("RabbitMQ channel is not available.")
        publish_channel.basic_publish(
            exchange=self._settings.exchange,
            routing_key=self._settings.result_routing_key,
            body=body,
            properties=properties,
        )
