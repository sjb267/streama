from __future__ import annotations

import json
import logging
import shutil
from datetime import UTC, datetime
from pathlib import Path
from typing import Any
from urllib.parse import urljoin

import requests

from .config import Settings
from .models import (
    AuditItemProgressMessage,
    AuditRequestItem,
    AuditRequestMessage,
    AuditResultItem,
    AuditResultMessage,
)
from .moderation import ModerationEngine

LOGGER = logging.getLogger(__name__)

DECISION_PASS = 1
DECISION_REJECT = 2
DECISION_MANUAL = 3

ITEM_PROCESSING = 1
ITEM_FINISHED = 2
ITEM_FAIL = 3

RISK_LOW = 1
RISK_MEDIUM = 2
RISK_HIGH = 3


class InternalApiClient:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._session = requests.Session()
        self._timeout = settings.integration.http_timeout_seconds
        self._project_root = Path(__file__).resolve().parent.parent

    def download_temp_video(self, request: AuditRequestMessage, item: AuditRequestItem) -> Path:
        source_name = self._build_source_name(item.file_path)
        local_dir = self._project_root / "temp" / "requests" / request.request_id / (item.file_id or "unknown_file")
        local_dir.mkdir(parents=True, exist_ok=True)
        local_path = local_dir / "temp.mp4"
        url = urljoin(self._settings.integration.resource_base_url.rstrip("/") + "/", "getResource")

        response = self._session.get(url, params={"sourceName": source_name}, stream=True, timeout=self._timeout)
        try:
            response.raise_for_status()
            with local_path.open("wb") as output_stream:
                for chunk in response.iter_content(chunk_size=1024 * 1024):
                    if chunk:
                        output_stream.write(chunk)
        finally:
            response.close()
        return local_path

    def update_item_progress(
        self,
        request_id: str,
        file_id: str,
        item_status: int,
        last_error: str | None = None,
    ) -> None:
        if not self._settings.integration.progress_callback_enabled:
            return
        progress = AuditItemProgressMessage(
            request_id=request_id,
            file_id=file_id,
            item_status=item_status,
            last_error=last_error,
            updated_at=datetime.now(UTC).replace(tzinfo=None),
        )
        url = urljoin(self._settings.integration.web_base_url.rstrip("/") + "/", "innerApi/video/aiAuditItemProgress")
        try:
            response = self._session.post(url, json=progress.to_message_dict(), timeout=self._timeout)
            try:
                response.raise_for_status()
            finally:
                response.close()
        except Exception:
            LOGGER.warning(
                "Failed to report audit item progress for requestId=%s fileId=%s",
                request_id,
                file_id,
                exc_info=True,
            )

    @staticmethod
    def _build_source_name(file_path: str | None) -> str:
        if not file_path:
            raise FileNotFoundError("filePath is empty.")
        normalized = file_path.replace("\\", "/").strip()
        if normalized.endswith(".mp4"):
            return normalized.lstrip("/")
        return f"{normalized.rstrip('/')}/temp.mp4".lstrip("/")


class AuditWorkflow:
    def __init__(self, settings: Settings, engine: ModerationEngine) -> None:
        self._settings = settings
        self._engine = engine
        self._internal_api_client = InternalApiClient(settings)

    def process_request(self, request: AuditRequestMessage) -> AuditResultMessage:
        item_results: list[AuditResultItem] = []
        for item in request.items:
            local_source: Path | None = None
            try:
                self._internal_api_client.update_item_progress(request.request_id, item.file_id or "", ITEM_PROCESSING)
                local_source = self._internal_api_client.download_temp_video(request, item)
                audit_payload = self._engine.audit_video(
                    local_source,
                    video_meta=request.video_meta,
                    item_meta=item,
                )
                item_results.append(self._build_item_success(request, item, audit_payload))
            except Exception as exc:
                LOGGER.exception("Audit failed for fileId=%s", item.file_id)
                item_results.append(self._build_item_failure(item, exc))
            finally:
                self._cleanup_local_source(local_source)
        return self._build_result(request, item_results)

    def build_failure_result_from_payload(self, raw_payload: bytes | str, exc: Exception) -> AuditResultMessage:
        try:
            payload = json.loads(raw_payload.decode("utf-8") if isinstance(raw_payload, bytes) else raw_payload)
            request = AuditRequestMessage.model_validate(payload)
        except Exception:
            request = AuditRequestMessage(request_id="", video_id="", items=[])
        item_results = [self._build_item_failure(item, exc) for item in request.items]
        return self._build_result(request, item_results)

    def _build_item_success(
        self,
        request: AuditRequestMessage,
        item: AuditRequestItem,
        audit_payload: dict[str, Any],
    ) -> AuditResultItem:
        report = audit_payload["report"]
        segments = audit_payload["segments"]
        decision_level = str(report.get("decision_level") or "medium").lower()
        item_decision, _ = self._map_level(decision_level)

        max_risk_score = 0.0
        risk_tags: list[str] = []
        audit_segments: list[dict[str, Any]] = []
        risky_segments: list[dict[str, Any]] = []

        for segment in segments:
            audit_result = segment.get("audit_result") or {}
            risk_score = float(audit_result.get("risk_score") or 0.0)
            max_risk_score = max(max_risk_score, risk_score)

            risk_type = str(audit_result.get("risk_type") or "").strip()
            normalized_risk_type = risk_type.lower()
            segment_is_risky = self._to_bool(audit_result.get("is_risky")) or risk_score > 0.3
            if (
                segment_is_risky
                and risk_type
                and normalized_risk_type not in {"none", "normal", "error", "unknown", "无"}
                and risk_type not in risk_tags
            ):
                risk_tags.append(risk_type)

            best_frame_path = ""
            if self._has_exportable_frame_source(segment):
                try:
                    best_frame_path = self._export_best_frame_path(request, item, segment)
                except Exception:
                    LOGGER.warning(
                        "Failed to export best frame for requestId=%s fileId=%s segmentId=%s",
                        request.request_id,
                        item.file_id,
                        segment.get("segment_id"),
                        exc_info=True,
                    )

            display_risk_type = risk_type or "normal"
            if display_risk_type.lower() in {"none", "无"}:
                display_risk_type = "normal"
            segment_payload = {
                "segmentId": segment.get("segment_id"),
                "startSeconds": round(float(segment.get("start", 0.0)), 2),
                "endSeconds": round(float(segment.get("end", 0.0)), 2),
                "textPreview": str(segment.get("text") or "")[:120],
                "riskType": display_risk_type,
                "riskScore": round(risk_score, 2),
                "reason": audit_result.get("reason"),
                "isRisky": segment_is_risky,
                "hasRiskSound": bool(segment.get("has_risk_sound", False)),
                "bestFramePath": best_frame_path,
                "sourceType": segment.get("source_type"),
            }
            audit_segments.append(segment_payload)
            if segment_is_risky:
                risky_segments.append(segment_payload)

        if not report.get("audit_complete", True):
            max_risk_score = max(max_risk_score, 0.5)

        item_reason = self._build_item_reason(report, risky_segments)
        return AuditResultItem(
            file_id=item.file_id,
            item_status=ITEM_FINISHED,
            item_decision=item_decision,
            risk_score=round(max_risk_score, 2),
            risk_tags=risk_tags or None,
            hit_segments=audit_segments or None,
            item_reason=item_reason,
        )

    @staticmethod
    def _has_exportable_frame_source(segment: dict[str, Any]) -> bool:
        if segment.get("best_frame_path"):
            return True
        segment_frames = segment.get("segment_frames") or []
        return bool(segment_frames and segment_frames[0].get("path"))

    @staticmethod
    def _to_bool(value: Any) -> bool:
        if isinstance(value, bool):
            return value
        if isinstance(value, str):
            normalized = value.strip().lower()
            if normalized in {"1", "true", "yes", "on"}:
                return True
            if normalized in {"0", "false", "no", "off", ""}:
                return False
        return bool(value)

    def _build_item_failure(self, item: AuditRequestItem, exc: Exception) -> AuditResultItem:
        message = str(exc) or exc.__class__.__name__
        return AuditResultItem(
            file_id=item.file_id,
            item_status=ITEM_FAIL,
            item_decision=DECISION_MANUAL,
            risk_score=0.5,
            risk_tags=["processing_error"],
            hit_segments=[],
            item_reason=f"AI processing failed, fallback to manual review: {message}",
        )

    def _build_result(self, request: AuditRequestMessage, item_results: list[AuditResultItem]) -> AuditResultMessage:
        video_decision, video_risk_level = self._aggregate_decision(item_results)
        video_summary = self._build_video_summary(item_results, video_decision)
        return AuditResultMessage(
            request_id=request.request_id,
            video_id=request.video_id,
            audit_version=request.audit_version,
            model_name=self._settings.moderation.model_name,
            model_version=self._settings.moderation.model_version,
            completed_at=datetime.utcnow(),
            video_decision=video_decision,
            video_risk_level=video_risk_level,
            video_summary=video_summary,
            items=item_results,
        )

    @staticmethod
    def _map_level(level: str) -> tuple[int, int]:
        if level == "high":
            return DECISION_REJECT, RISK_HIGH
        if level == "medium":
            return DECISION_MANUAL, RISK_MEDIUM
        return DECISION_PASS, RISK_LOW

    @staticmethod
    def _build_item_reason(report: dict[str, Any], hit_segments: list[dict[str, Any]]) -> str:
        decision = str(report.get("decision") or "AI completed")
        modalities = ",".join(str(item) for item in report.get("modalities_checked", []) if item) or "none"
        coverage_issues = [str(item) for item in report.get("coverage_issues", []) if item]
        coverage = "complete" if report.get("audit_complete", True) else "incomplete"
        if coverage_issues:
            coverage = f"{coverage}:{','.join(coverage_issues[:3])}"
        suffix = f"; modalities={modalities}; coverage={coverage}"
        if not hit_segments:
            return f"{decision}; no risky segments detected{suffix}."
        first_hit = hit_segments[0]
        return (
            f"{decision}; riskySegments={len(hit_segments)}; "
            f"topRisk={first_hit.get('riskType')}@{first_hit.get('startSeconds')}-{first_hit.get('endSeconds')}s"
            f"{suffix}"
        )

    @staticmethod
    def _aggregate_decision(items: list[AuditResultItem]) -> tuple[int, int]:
        if not items:
            return DECISION_MANUAL, RISK_MEDIUM
        if any(item.item_status == ITEM_FAIL for item in items):
            return DECISION_MANUAL, RISK_MEDIUM
        if any(item.item_decision == DECISION_REJECT for item in items):
            return DECISION_REJECT, RISK_HIGH
        if any(item.item_decision == DECISION_MANUAL for item in items):
            return DECISION_MANUAL, RISK_MEDIUM
        return DECISION_PASS, RISK_LOW

    @staticmethod
    def _build_video_summary(items: list[AuditResultItem], decision: int) -> str:
        if not items:
            return "No audit items were supplied."

        decision_text = {
            DECISION_PASS: "pass",
            DECISION_REJECT: "reject",
            DECISION_MANUAL: "manual_review",
        }[decision]

        parts = []
        for item in items:
            file_part = item.file_id or "unknown_file"
            parts.append(f"{file_part}: {item.item_reason}")
        return f"videoDecision={decision_text}; " + " | ".join(parts)

    def _export_best_frame_path(
        self,
        request: AuditRequestMessage,
        item: AuditRequestItem,
        segment: dict[str, Any],
    ) -> str:
        source_path = segment.get("best_frame_path")
        if not source_path:
            segment_frames = segment.get("segment_frames") or []
            if segment_frames:
                source_path = segment_frames[0].get("path")
        if not source_path:
            raise FileNotFoundError(f"best_frame_path is empty for fileId={item.file_id}")

        source = Path(source_path)
        if not source.exists():
            raise FileNotFoundError(f"best frame not found: {source}")

        audit_version = str(request.audit_version or 1)
        snapshot_root = Path(self._settings.storage.root_dir) / "audit-snapshot" / request.video_id / audit_version
        snapshot_root.mkdir(parents=True, exist_ok=True)

        file_id = item.file_id or "unknown_file"
        segment_id = segment.get("segment_id") or "unknown_segment"
        target_name = f"{file_id}_{segment_id}{source.suffix or '.jpg'}"
        target = snapshot_root / target_name
        shutil.copy2(source, target)

        relative = Path("audit-snapshot") / request.video_id / audit_version / target_name
        return relative.as_posix()

    @staticmethod
    def _cleanup_local_source(local_source: Path | None) -> None:
        if local_source is None:
            return
        try:
            shutil.rmtree(local_source.parent, ignore_errors=True)
        except Exception:
            LOGGER.warning("Failed to cleanup temp source for %s", local_source, exc_info=True)
