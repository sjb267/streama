from __future__ import annotations

import logging
import platform
import threading
from pathlib import Path
from typing import Any

import torch

from .config import ModerationSettings
from .video_moderation import VideoModerationService

LOGGER = logging.getLogger(__name__)


class ModerationEngine:
    def __init__(self, settings: ModerationSettings) -> None:
        self._settings = settings
        self._service: VideoModerationService | None = None
        self._service_lock = threading.Lock()
        self._run_lock = threading.Lock()
        self._last_error: str | None = None
        self._qwen_preflight: dict[str, Any] = self._build_preflight_snapshot(
            checked=False,
            passed=False,
            reason="Qwen runtime preflight has not run yet.",
        )

    def preload(self) -> None:
        self._get_service()

    def health_snapshot(self) -> dict[str, Any]:
        return {
            "loaded": self._service is not None,
            "busy": self._run_lock.locked(),
            "lastError": self._last_error,
            "models": self._build_model_snapshot(self._service),
        }

    def _get_service(self) -> VideoModerationService:
        with self._service_lock:
            if self._service is None:
                try:
                    self._qwen_preflight = self._preflight_qwen_runtime()
                    service = VideoModerationService(
                        model_dir=self._settings.model_dir,
                        device="cpu",
                        qwen_device=self._settings.qwen_device,
                        trace_model_inputs=self._settings.trace_model_inputs,
                        trace_dir=self._settings.trace_dir,
                    )
                    service.frame_extraction_rate = self._settings.extraction_rate
                    service.runtime_checks["qwen"] = dict(self._qwen_preflight)
                    service.load_all_models()
                    self._validate_service(service)
                    self._service = service
                    self._last_error = None
                except Exception as exc:
                    self._last_error = str(exc)
                    raise
            return self._service

    def _build_model_snapshot(self, service: VideoModerationService | None) -> dict[str, Any]:
        qwen_preflight = dict(getattr(service, "runtime_checks", {}).get("qwen", self._qwen_preflight))
        model_errors = getattr(service, "model_errors", {}) if service is not None else {}
        if service is None:
            return {
                "cpuDevice": "cpu",
                "qwenTargetDevice": self._settings.qwen_device,
                "qwenPreflight": qwen_preflight,
                "yamnet": {"loaded": False, "assignedDevice": "cpu", "lastError": model_errors.get("yamnet")},
                "whisper": {"loaded": False, "assignedDevice": "cpu", "lastError": model_errors.get("whisper")},
                "qwen": {
                    "loaded": False,
                    "assignedDevice": self._settings.qwen_device,
                    "targetDevice": self._settings.qwen_device,
                    "lastError": model_errors.get("qwen") or self._last_error,
                },
                "vad": {"loaded": False, "assignedDevice": "cpu", "lastError": model_errors.get("vad")},
            }
        return {
            "cpuDevice": str(service.cpu_device),
            "qwenTargetDevice": service.qwen_target_device,
            "qwenPreflight": qwen_preflight,
            "yamnet": {
                "loaded": service.yamnet_model is not None or service.yamnet_interpreter is not None,
                "classes": len(service.yamnet_class_names) if service.yamnet_class_names else 0,
                "assignedDevice": "cpu",
                "lastError": model_errors.get("yamnet"),
            },
            "whisper": {
                "loaded": service.whisper_model is not None,
                "modelType": "small" if service.whisper_model is not None else None,
                "assignedDevice": "cpu",
                "lastError": model_errors.get("whisper"),
            },
            "qwen": {
                "loaded": service.qwen_model is not None and service.qwen_processor is not None,
                "modelType": "Qwen/Qwen3-VL-4B-Instruct" if service.qwen_model is not None else None,
                "assignedDevice": str(service.get_qwen_runtime_device()) if service.qwen_model is not None else None,
                "targetDevice": service.qwen_target_device,
                "lastError": model_errors.get("qwen"),
            },
            "vad": {
                "loaded": service.vad is not None,
                "mode": getattr(service, "vad_mode", None),
                "assignedDevice": "cpu",
                "lastError": model_errors.get("vad"),
            },
        }

    def _validate_service(self, service: VideoModerationService) -> None:
        missing: list[str] = []
        if service.yamnet_model is None and service.yamnet_interpreter is None:
            missing.append("yamnet")
        if service.whisper_model is None:
            missing.append("whisper")
        if service.qwen_model is None or service.qwen_processor is None:
            missing.append("qwen")
        if service.vad is None:
            missing.append("vad")
        if missing:
            details = []
            for model_name in missing:
                last_error = service.model_errors.get(model_name) or "unknown error"
                target_device = service.qwen_target_device if model_name == "qwen" else "cpu"
                details.append(f"{model_name} (target device: {target_device}; last error: {last_error})")
            raise RuntimeError(
                "Core moderation models failed to load: "
                + "; ".join(details)
                + ". Recommended environment: CPU for YAMNet/Whisper/VAD and a CUDA-enabled GPU host for Qwen."
            )

    def _preflight_qwen_runtime(self) -> dict[str, Any]:
        configured_device = (self._settings.qwen_device or "cuda").strip().lower()
        runtime = self._build_preflight_snapshot(checked=True, passed=False, reason=None)
        runtime["configuredDevice"] = configured_device
        runtime["torchCudaBuild"] = torch.version.cuda
        runtime["cudaAvailable"] = torch.cuda.is_available()
        runtime["cudaDeviceCount"] = torch.cuda.device_count() if torch.cuda.is_available() else 0

        if not configured_device.startswith("cuda"):
            runtime["reason"] = (
                f"Unsupported moderation.qwen_device='{configured_device}'. "
                "This service requires Qwen to run on GPU (cuda) and does not support CPU preloading."
            )
            return runtime

        if torch.version.cuda is None:
            runtime["reason"] = (
                "Qwen is configured to run on GPU, but the installed PyTorch build has no CUDA support. "
                "Install a CUDA-enabled PyTorch build on a GPU host before starting the service."
            )
            return runtime

        if not torch.cuda.is_available():
            host_name = platform.system() or "Current"
            runtime["reason"] = (
                "Qwen is configured to run on GPU, but no CUDA device is available on this host. "
                f"{host_name} CPU-only hosts are not supported for full moderation service startup."
            )
            return runtime

        runtime["passed"] = True
        runtime["reason"] = None
        return runtime

    @staticmethod
    def _build_preflight_snapshot(*, checked: bool, passed: bool, reason: str | None) -> dict[str, Any]:
        return {
            "checked": checked,
            "passed": passed,
            "reason": reason,
            "configuredDevice": None,
            "torchCudaBuild": None,
            "cudaAvailable": None,
            "cudaDeviceCount": None,
        }

    def audit_video(
        self,
        video_source: str | Path,
        video_meta: Any | None = None,
        item_meta: Any | None = None,
    ) -> dict[str, Any]:
        with self._run_lock:
            try:
                service = self._get_service()
                source = str(video_source)
                service.begin_model_input_trace_session(source)
                coverage_issues: list[str] = []
                modalities_checked: set[str] = set()
                audio_file: str | None = None
                voice_segments: list[dict[str, Any]] = []
                sound_events: list[dict[str, Any]] = []
                transcriptions: list[dict[str, Any]] = []

                try:
                    has_audio = service.video_has_audio_stream(source)
                except Exception as exc:
                    has_audio = True
                    coverage_issues.append(f"audio_probe_error:{exc}")

                if has_audio:
                    try:
                        audio_file = service.extract_audio_from_video(source)
                        modalities_checked.add("audio")
                    except Exception as exc:
                        coverage_issues.append(f"audio_extract_error:{exc}")

                if audio_file:
                    try:
                        voice_segments = service.detect_voice_segments(audio_file)
                        modalities_checked.add("vad")
                    except Exception as exc:
                        coverage_issues.append(f"vad_error:{exc}")

                    try:
                        sound_events = service.detect_sound_events(
                            audio_file,
                            top_k=self._settings.sound_top_k,
                            confidence_threshold=self._settings.sound_confidence_threshold,
                        )
                        modalities_checked.add("audio_event")
                    except Exception as exc:
                        coverage_issues.append(f"yamnet_error:{exc}")

                    if voice_segments:
                        try:
                            transcriptions = service.transcribe_audio_segments(
                                audio_file,
                                voice_segments,
                                language=self._settings.language,
                            )
                            modalities_checked.add("speech_text")
                        except Exception as exc:
                            coverage_issues.append(f"whisper_error:{exc}")

                risk_sound_times = [
                    float(event.get("time", 0.0))
                    for event in sound_events
                    if event.get("is_risk")
                ]
                try:
                    frames_info = service.extract_keyframes(
                        source,
                        extraction_rate=self._settings.extraction_rate,
                        focus_times=risk_sound_times,
                    )
                except Exception as exc:
                    frames_info = []
                    coverage_issues.append(f"keyframe_extract_error:{exc}")
                if frames_info:
                    modalities_checked.add("visual")
                else:
                    coverage_issues.append("no_keyframes")

                aligned_segments = service.build_audit_segments(
                    transcriptions=transcriptions,
                    frames_info=frames_info,
                    sound_events=sound_events,
                    video_path=source,
                    video_meta=video_meta,
                    item_meta=item_meta,
                    coverage_issues=coverage_issues,
                )
                aligned_segments = service.align_sound_with_segments(aligned_segments, sound_events)
                if any(segment.get("metadata_summary") for segment in aligned_segments):
                    modalities_checked.add("metadata_text")
                service.batch_audit(aligned_segments, max_segments=None)
                report = service.generate_audit_report(
                    aligned_segments,
                    source,
                    coverage_issues=coverage_issues,
                    modalities_checked=sorted(modalities_checked),
                )
                self._last_error = None
                return {"report": report, "segments": aligned_segments}
            except Exception as exc:
                self._last_error = str(exc)
                LOGGER.exception("Video moderation failed for %s", video_source)
                raise
