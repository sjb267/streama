from __future__ import annotations

import json
import logging
import socket
import threading
import time
from typing import Any

import requests

from .config import NacosSettings

LOGGER = logging.getLogger(__name__)


def _build_base_url(server_addr: str) -> str:
    return server_addr if server_addr.startswith("http://") or server_addr.startswith("https://") else f"http://{server_addr}"


def _resolve_local_ip(server_addr: str) -> str:
    host = server_addr.replace("http://", "").replace("https://", "").split(":")[0]
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.connect((host, 1))
        return sock.getsockname()[0]
    except OSError:
        return "127.0.0.1"
    finally:
        sock.close()


class NacosOpenApiClient:
    def __init__(self, settings: NacosSettings) -> None:
        self._settings = settings
        self._session = requests.Session()
        self._base_url = _build_base_url(settings.server_addr)
        self._access_token: str | None = None

    def _ensure_token(self) -> str | None:
        if self._access_token is not None:
            return self._access_token
        if not self._settings.username or not self._settings.password:
            return None
        payload = {"username": self._settings.username, "password": self._settings.password}
        for path in ("/nacos/v1/auth/login", "/nacos/v3/auth/user/login"):
            url = f"{self._base_url}{path}"
            try:
                response = self._session.post(url, data=payload, timeout=10)
                if response.status_code == 404:
                    continue
                response.raise_for_status()
                body = response.json()
                token = body.get("accessToken")
                if token:
                    self._access_token = token
                    return token
            except requests.RequestException:
                LOGGER.debug("Nacos login attempt failed via %s", path, exc_info=True)
        return None

    def _request(self, method: str, path: str, *, params: dict[str, Any] | None = None) -> requests.Response:
        request_params = dict(params or {})
        token = self._ensure_token()
        if token:
            request_params["accessToken"] = token
        url = f"{self._base_url}{path}"
        response = self._session.request(method, url, params=request_params, timeout=10)
        response.raise_for_status()
        return response

    def register_instance(self, *, service_name: str, ip: str, port: int, metadata: dict[str, Any] | None = None) -> None:
        params = {
            "serviceName": service_name,
            "ip": ip,
            "port": port,
            "groupName": self._settings.group,
            "clusterName": self._settings.cluster_name,
            "ephemeral": str(self._settings.ephemeral).lower(),
            "healthy": "true",
            "enabled": "true",
            "weight": 1.0,
            "metadata": json.dumps(metadata or {}, ensure_ascii=False),
        }
        if self._settings.namespace:
            params["namespaceId"] = self._settings.namespace
        self._request("POST", "/nacos/v1/ns/instance", params=params)

    def send_beat(self, *, service_name: str, ip: str, port: int, metadata: dict[str, Any] | None = None) -> None:
        beat = {
            "serviceName": service_name,
            "ip": ip,
            "port": port,
            "cluster": self._settings.cluster_name,
            "metadata": metadata or {},
            "scheduled": True,
            "weight": 1.0,
        }
        params = {
            "serviceName": service_name,
            "ip": ip,
            "port": port,
            "groupName": self._settings.group,
            "beat": json.dumps(beat, ensure_ascii=False),
        }
        if self._settings.namespace:
            params["namespaceId"] = self._settings.namespace
        self._request("PUT", "/nacos/v1/ns/instance/beat", params=params)

    def deregister_instance(self, *, service_name: str, ip: str, port: int) -> None:
        params = {
            "serviceName": service_name,
            "ip": ip,
            "port": port,
            "groupName": self._settings.group,
            "clusterName": self._settings.cluster_name,
            "ephemeral": str(self._settings.ephemeral).lower(),
        }
        if self._settings.namespace:
            params["namespaceId"] = self._settings.namespace
        self._request("DELETE", "/nacos/v1/ns/instance", params=params)


class NacosRegistration:
    def __init__(self, settings: NacosSettings, service_name: str, port: int) -> None:
        self._settings = settings
        self._service_name = settings.service_name or service_name
        self._port = port
        self._client = NacosOpenApiClient(settings)
        self._ip = settings.register_ip or _resolve_local_ip(settings.server_addr)
        self._stop_event = threading.Event()
        self._thread: threading.Thread | None = None
        self._registered = False
        self._last_error: str | None = None

    def start(self) -> None:
        if not self._settings.enabled:
            return
        if self._thread and self._thread.is_alive():
            return
        self._stop_event.clear()
        self._thread = threading.Thread(target=self._run, name="nacos-heartbeat", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        if not self._settings.enabled:
            return
        self._stop_event.set()
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=5)
        if self._registered:
            try:
                self._client.deregister_instance(
                    service_name=self._service_name,
                    ip=self._ip,
                    port=self._port,
                )
            except requests.RequestException:
                LOGGER.warning("Failed to deregister Nacos instance.", exc_info=True)
        self._registered = False

    def _run(self) -> None:
        interval = max(1, self._settings.heartbeat_interval_seconds)
        while not self._stop_event.is_set():
            try:
                if not self._registered:
                    self._client.register_instance(
                        service_name=self._service_name,
                        ip=self._ip,
                        port=self._port,
                        metadata=self._settings.metadata,
                    )
                    self._registered = True
                    self._last_error = None
                    LOGGER.info("Registered %s with Nacos at %s:%s", self._service_name, self._ip, self._port)
                elif self._settings.ephemeral:
                    self._client.send_beat(
                        service_name=self._service_name,
                        ip=self._ip,
                        port=self._port,
                        metadata=self._settings.metadata,
                    )
            except requests.RequestException as exc:
                self._registered = False
                self._last_error = str(exc)
                LOGGER.warning("Nacos heartbeat failed.", exc_info=True)
            if self._stop_event.wait(interval):
                break

    def health_snapshot(self) -> dict[str, Any]:
        return {
            "enabled": self._settings.enabled,
            "registered": self._registered,
            "serviceName": self._service_name,
            "ip": self._ip,
            "port": self._port,
            "lastError": self._last_error,
        }
