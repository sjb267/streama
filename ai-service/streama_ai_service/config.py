from __future__ import annotations

import copy
import logging
import os
from pathlib import Path
from typing import Any

import requests
import yaml
from pydantic import BaseModel, Field

LOGGER = logging.getLogger(__name__)
PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_CONFIG_FILE = PROJECT_ROOT / "config" / "application.yaml"


class ServiceSettings(BaseModel):
    name: str = "streama-cloud-ai"
    host: str = "0.0.0.0"
    port: int = 7075
    log_level: str = "INFO"


class NacosSettings(BaseModel):
    enabled: bool = True
    server_addr: str = "127.0.0.1:8848"
    namespace: str = ""
    group: str = "DEFAULT_GROUP"
    username: str = "nacos"
    password: str = "nacos123"
    service_name: str = "streama-cloud-ai"
    cluster_name: str = "DEFAULT"
    ephemeral: bool = True
    heartbeat_interval_seconds: int = 5
    register_ip: str = ""
    metadata: dict[str, Any] = Field(default_factory=dict)
    config_enabled: bool = False
    config_data_id: str = "streama-cloud-ai-dev.yml"
    config_group: str = "DEFAULT_GROUP"


class RabbitMQSettings(BaseModel):
    enabled: bool = True
    host: str = "127.0.0.1"
    port: int = 5672
    username: str = "guest"
    password: str = "guest"
    virtual_host: str = "/"
    heartbeat: int = 60
    blocked_connection_timeout: int = 30
    prefetch_count: int = 1
    reconnect_interval_seconds: int = 5
    request_dedupe_ttl_seconds: int = 1800
    request_dedupe_max_entries: int = 256
    auto_declare: bool = True
    exchange: str = "streama.audit.exchange"
    exchange_type: str = "topic"
    request_queue: str = "streama.audit.request.queue"
    request_routing_key: str = "audit.video.request"
    request_dlx: str = "streama.audit.request.dlx"
    request_dlq: str = "streama.audit.request.dlq"
    result_queue: str = "streama.audit.result.queue"
    result_routing_key: str = "audit.video.result"
    result_dlx: str = "streama.audit.result.dlx"
    result_dlq: str = "streama.audit.result.dlq"


class StorageSettings(BaseModel):
    root_dir: str = "../streama-cloud/file"


class IntegrationSettings(BaseModel):
    web_base_url: str = "http://127.0.0.1:7071/web"
    resource_base_url: str = "http://127.0.0.1:7071/file"
    http_timeout_seconds: int = 120
    progress_callback_enabled: bool = False


class ModerationSettings(BaseModel):
    model_dir: str = "./models"
    device: str = "cpu"
    qwen_device: str = "cuda"
    preload_models: bool = False
    extraction_rate: int = 1
    sound_top_k: int = 5
    sound_confidence_threshold: float = 0.3
    language: str | None = "zh"
    model_name: str = "qwen-video-moderation"
    model_version: str = "1.0.0"
    trace_model_inputs: bool = True
    trace_dir: str = "./audit_results/model_input_traces"


class Settings(BaseModel):
    service: ServiceSettings = Field(default_factory=ServiceSettings)
    nacos: NacosSettings = Field(default_factory=NacosSettings)
    rabbitmq: RabbitMQSettings = Field(default_factory=RabbitMQSettings)
    storage: StorageSettings = Field(default_factory=StorageSettings)
    integration: IntegrationSettings = Field(default_factory=IntegrationSettings)
    moderation: ModerationSettings = Field(default_factory=ModerationSettings)


ENV_MAPPING: dict[tuple[str, ...], tuple[str, Any]] = {
    ("service", "host"): ("STREAMA_AI_SERVICE_HOST", str),
    ("service", "port"): ("STREAMA_AI_SERVICE_PORT", int),
    ("service", "log_level"): ("STREAMA_AI_LOG_LEVEL", str),
    ("nacos", "enabled"): ("STREAMA_AI_NACOS_ENABLED", bool),
    ("nacos", "server_addr"): ("STREAMA_AI_NACOS_SERVER_ADDR", str),
    ("nacos", "namespace"): ("STREAMA_AI_NACOS_NAMESPACE", str),
    ("nacos", "group"): ("STREAMA_AI_NACOS_GROUP", str),
    ("nacos", "username"): ("STREAMA_AI_NACOS_USERNAME", str),
    ("nacos", "password"): ("STREAMA_AI_NACOS_PASSWORD", str),
    ("nacos", "service_name"): ("STREAMA_AI_NACOS_SERVICE_NAME", str),
    ("nacos", "register_ip"): ("STREAMA_AI_NACOS_REGISTER_IP", str),
    ("nacos", "config_enabled"): ("STREAMA_AI_NACOS_CONFIG_ENABLED", bool),
    ("nacos", "config_data_id"): ("STREAMA_AI_NACOS_CONFIG_DATA_ID", str),
    ("rabbitmq", "enabled"): ("STREAMA_AI_RABBITMQ_ENABLED", bool),
    ("rabbitmq", "host"): ("STREAMA_AI_RABBITMQ_HOST", str),
    ("rabbitmq", "port"): ("STREAMA_AI_RABBITMQ_PORT", int),
    ("rabbitmq", "username"): ("STREAMA_AI_RABBITMQ_USERNAME", str),
    ("rabbitmq", "password"): ("STREAMA_AI_RABBITMQ_PASSWORD", str),
    ("rabbitmq", "virtual_host"): ("STREAMA_AI_RABBITMQ_VHOST", str),
    ("rabbitmq", "request_dedupe_ttl_seconds"): ("STREAMA_AI_RABBITMQ_REQUEST_DEDUPE_TTL_SECONDS", int),
    ("rabbitmq", "request_dedupe_max_entries"): ("STREAMA_AI_RABBITMQ_REQUEST_DEDUPE_MAX_ENTRIES", int),
    ("storage", "root_dir"): ("STREAMA_AI_STORAGE_ROOT_DIR", str),
    ("integration", "web_base_url"): ("STREAMA_AI_WEB_BASE_URL", str),
    ("integration", "resource_base_url"): ("STREAMA_AI_RESOURCE_BASE_URL", str),
    ("integration", "http_timeout_seconds"): ("STREAMA_AI_HTTP_TIMEOUT_SECONDS", int),
    ("integration", "progress_callback_enabled"): ("STREAMA_AI_PROGRESS_CALLBACK_ENABLED", bool),
    ("moderation", "model_dir"): ("STREAMA_AI_MODEL_DIR", str),
    ("moderation", "device"): ("STREAMA_AI_DEVICE", str),
    ("moderation", "qwen_device"): ("STREAMA_AI_QWEN_DEVICE", str),
    ("moderation", "preload_models"): ("STREAMA_AI_PRELOAD_MODELS", bool),
    ("moderation", "trace_model_inputs"): ("STREAMA_AI_TRACE_MODEL_INPUTS", bool),
    ("moderation", "trace_dir"): ("STREAMA_AI_TRACE_DIR", str),
}


def _to_bool(value: str) -> bool:
    return value.strip().lower() in {"1", "true", "yes", "on"}


def _apply_env_overrides(config: dict[str, Any]) -> dict[str, Any]:
    updated = copy.deepcopy(config)
    for key_path, (env_name, caster) in ENV_MAPPING.items():
        raw_value = os.getenv(env_name)
        if raw_value is None:
            continue
        target = updated
        for key in key_path[:-1]:
            target = target.setdefault(key, {})
        target[key_path[-1]] = _to_bool(raw_value) if caster is bool else caster(raw_value)
    return updated


def _deep_merge(base: dict[str, Any], override: dict[str, Any]) -> dict[str, Any]:
    merged = copy.deepcopy(base)
    for key, value in override.items():
        if isinstance(value, dict) and isinstance(merged.get(key), dict):
            merged[key] = _deep_merge(merged[key], value)
        else:
            merged[key] = value
    return merged


def _normalize_device_name(value: str | None, default: str) -> str:
    raw = (value or "").strip().lower()
    if not raw:
        return default
    if raw == "gpu":
        return "cuda"
    if raw.startswith("cuda"):
        return raw
    return "cpu"


def _apply_moderation_compatibility(config: dict[str, Any]) -> dict[str, Any]:
    updated = copy.deepcopy(config)
    moderation = updated.setdefault("moderation", {})
    if not isinstance(moderation, dict):
        return updated

    if "qwen_device" not in moderation and moderation.get("device") is not None:
        moderation["qwen_device"] = moderation["device"]
        LOGGER.warning("`moderation.device` is deprecated; use `moderation.qwen_device` instead.")

    moderation["device"] = _normalize_device_name(moderation.get("device"), "cpu")
    moderation["qwen_device"] = _normalize_device_name(moderation.get("qwen_device"), "cuda")
    return updated


def _read_yaml(config_path: Path) -> dict[str, Any]:
    if not config_path.exists():
        return {}
    content = yaml.safe_load(config_path.read_text(encoding="utf-8"))
    if content is None:
        return {}
    if not isinstance(content, dict):
        raise ValueError(f"Config file must contain a mapping: {config_path}")
    return content


def _build_nacos_base_url(server_addr: str) -> str:
    return server_addr if server_addr.startswith("http://") or server_addr.startswith("https://") else f"http://{server_addr}"


def _fetch_nacos_access_token(session: requests.Session, settings: NacosSettings) -> str | None:
    if not settings.username or not settings.password:
        return None
    payload = {"username": settings.username, "password": settings.password}
    for path in ("/nacos/v1/auth/login", "/nacos/v3/auth/user/login"):
        url = f"{_build_nacos_base_url(settings.server_addr)}{path}"
        try:
            response = session.post(url, data=payload, timeout=10)
            if response.status_code == 404:
                continue
            response.raise_for_status()
            body = response.json()
            token = body.get("accessToken")
            if token:
                return token
        except requests.RequestException:
            LOGGER.debug("Nacos login failed via %s", path, exc_info=True)
    return None


def _fetch_nacos_config(settings: NacosSettings) -> dict[str, Any]:
    if not settings.config_enabled or not settings.config_data_id:
        return {}
    session = requests.Session()
    params = {
        "dataId": settings.config_data_id,
        "group": settings.config_group or settings.group,
    }
    if settings.namespace:
        params["tenant"] = settings.namespace
    token = _fetch_nacos_access_token(session, settings)
    if token:
        params["accessToken"] = token
    url = f"{_build_nacos_base_url(settings.server_addr)}/nacos/v1/cs/configs"
    try:
        response = session.get(url, params=params, timeout=10)
        if response.status_code == 404:
            return {}
        response.raise_for_status()
        text = response.text.strip()
        if not text:
            return {}
        content = yaml.safe_load(text)
        if content is None:
            return {}
        if not isinstance(content, dict):
            raise ValueError("Nacos config content must be a mapping.")
        LOGGER.info("Loaded remote config from Nacos dataId=%s", settings.config_data_id)
        return content
    except requests.RequestException:
        LOGGER.warning("Failed to load remote config from Nacos.", exc_info=True)
        return {}


def _resolve_path(raw_path: str) -> str:
    candidate = Path(raw_path)
    if not candidate.is_absolute():
        candidate = (PROJECT_ROOT / candidate).resolve()
    return str(candidate)


def load_settings(config_file: str | Path | None = None) -> Settings:
    config_path = Path(config_file) if config_file else DEFAULT_CONFIG_FILE
    if not config_path.is_absolute():
        config_path = (PROJECT_ROOT / config_path).resolve()

    raw_config = _apply_moderation_compatibility(_apply_env_overrides(_read_yaml(config_path)))
    bootstrap = Settings.model_validate(raw_config)

    remote_config = _fetch_nacos_config(bootstrap.nacos) if bootstrap.nacos.enabled else {}
    if remote_config:
        raw_config = _apply_moderation_compatibility(_apply_env_overrides(_deep_merge(raw_config, remote_config)))

    settings = Settings.model_validate(raw_config)
    settings.storage.root_dir = _resolve_path(settings.storage.root_dir)
    settings.moderation.model_dir = _resolve_path(settings.moderation.model_dir)
    settings.moderation.trace_dir = _resolve_path(settings.moderation.trace_dir)
    settings.moderation.device = _normalize_device_name(settings.moderation.device, "cpu")
    settings.moderation.qwen_device = _normalize_device_name(settings.moderation.qwen_device, "cuda")
    return settings
