from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator
from pydantic.alias_generators import to_camel


def _parse_java_datetime(value: Any) -> datetime | None:
    if value in (None, ""):
        return None
    if isinstance(value, datetime):
        return value.replace(tzinfo=None)
    if isinstance(value, (int, float)):
        timestamp = float(value)
        if timestamp > 1_000_000_000_000:
            timestamp /= 1000
        return datetime.fromtimestamp(timestamp, tz=timezone.utc).replace(tzinfo=None)
    if isinstance(value, str):
        candidate = value.strip()
        if not candidate:
            return None
        if candidate.isdigit():
            return _parse_java_datetime(int(candidate))
        candidate = candidate.replace("Z", "+00:00")
        for parser in (datetime.fromisoformat,):
            try:
                parsed = parser(candidate)
                return parsed.replace(tzinfo=None)
            except ValueError:
                continue
        for pattern in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
            try:
                return datetime.strptime(candidate, pattern)
            except ValueError:
                continue
    raise ValueError(f"Unsupported datetime value: {value!r}")


class CamelModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True, alias_generator=to_camel, extra="ignore")


class AuditRequestVideoMeta(CamelModel):
    user_id: str | None = None
    video_name: str | None = None
    video_cover: str | None = None
    tags: str | None = None
    introduction: str | None = None
    p_category_id: int | None = None
    category_id: int | None = None
    post_type: int | None = None


class AuditRequestItem(CamelModel):
    file_id: str | None = None
    file_index: int | None = None
    upload_id: str | None = None
    file_name: str | None = None
    file_path: str | None = None
    duration: int | None = None
    update_type: int | None = None


class AuditRequestMessage(CamelModel):
    request_id: str
    video_id: str
    audit_version: int | None = None
    source_type: int | None = None
    trigger_time: datetime | None = None
    video_meta: AuditRequestVideoMeta | None = None
    items: list[AuditRequestItem] = Field(default_factory=list)

    @field_validator("trigger_time", mode="before")
    @classmethod
    def validate_trigger_time(cls, value: Any) -> datetime | None:
        return _parse_java_datetime(value)


class AuditResultItem(CamelModel):
    file_id: str | None = None
    item_status: int
    item_decision: int
    risk_score: float = 0.0
    risk_tags: Any = None
    hit_segments: Any = None
    item_reason: str | None = None


class AuditResultMessage(CamelModel):
    request_id: str
    video_id: str
    audit_version: int | None = None
    model_name: str
    model_version: str
    completed_at: datetime
    video_decision: int
    video_risk_level: int
    video_summary: str
    items: list[AuditResultItem] = Field(default_factory=list)

    @field_validator("completed_at", mode="before")
    @classmethod
    def validate_completed_at(cls, value: Any) -> datetime | None:
        return _parse_java_datetime(value)

    def to_message_dict(self) -> dict[str, Any]:
        payload = self.model_dump(by_alias=True, exclude_none=True)
        payload["completedAt"] = int(self.completed_at.timestamp() * 1000)
        return payload


class AuditItemProgressMessage(CamelModel):
    request_id: str
    file_id: str
    item_status: int
    last_error: str | None = None
    updated_at: datetime

    @field_validator("updated_at", mode="before")
    @classmethod
    def validate_updated_at(cls, value: Any) -> datetime | None:
        return _parse_java_datetime(value)

    def to_message_dict(self) -> dict[str, Any]:
        payload = self.model_dump(by_alias=True, exclude_none=True)
        payload["updatedAt"] = int(self.updated_at.timestamp() * 1000)
        return payload
