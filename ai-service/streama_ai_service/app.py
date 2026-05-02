from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from datetime import datetime
from typing import Any

from fastapi import FastAPI, Request

from .config import Settings, load_settings
from .moderation import ModerationEngine
from .mq import RabbitAuditWorker
from .nacos import NacosRegistration
from .service import AuditWorkflow

LOGGER = logging.getLogger(__name__)


def configure_logging(level: str) -> None:
    logging.basicConfig(
        level=getattr(logging, level.upper(), logging.INFO),
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
        force=True,
    )


class AppContainer:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.started_at = datetime.utcnow()
        self.engine = ModerationEngine(settings.moderation)
        self.workflow = AuditWorkflow(settings, self.engine)
        self.nacos = NacosRegistration(settings.nacos, settings.service.name, settings.service.port)
        self.rabbit_worker = RabbitAuditWorker(settings.rabbitmq, self.workflow)

    def start(self) -> None:
        configure_logging(self.settings.service.log_level)
        LOGGER.info(
            "Starting %s on %s:%s",
            self.settings.service.name,
            self.settings.service.host,
            self.settings.service.port,
        )
        self.engine.preload()
        self.nacos.start()
        self.rabbit_worker.start()

    def stop(self) -> None:
        self.rabbit_worker.stop()
        self.nacos.stop()

    def health_snapshot(self) -> dict[str, Any]:
        return {
            "service": {
                "name": self.settings.service.name,
                "startedAt": self.started_at.isoformat(),
                "port": self.settings.service.port,
            },
            "nacos": self.nacos.health_snapshot(),
            "rabbitmq": self.rabbit_worker.health_snapshot(),
            "moderation": self.engine.health_snapshot(),
        }


def create_app(settings: Settings | None = None) -> FastAPI:
    resolved_settings = settings or load_settings()
    container = AppContainer(resolved_settings)

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        app.state.container = container
        container.start()
        try:
            yield
        finally:
            container.stop()

    app = FastAPI(
        title=resolved_settings.service.name,
        version=resolved_settings.moderation.model_version,
        lifespan=lifespan,
    )

    @app.get("/health")
    def health(request: Request) -> dict[str, Any]:
        return request.app.state.container.health_snapshot()

    @app.get("/actuator/health")
    def actuator_health(request: Request) -> dict[str, Any]:
        snapshot = request.app.state.container.health_snapshot()
        rabbit_ok = not snapshot["rabbitmq"]["enabled"] or snapshot["rabbitmq"]["connected"]
        nacos_ok = not snapshot["nacos"]["enabled"] or snapshot["nacos"]["registered"]
        status = "UP" if rabbit_ok and nacos_ok else "DEGRADED"
        return {"status": status, "components": snapshot}

    @app.get("/actuator/info")
    def actuator_info(request: Request) -> dict[str, Any]:
        settings = request.app.state.container.settings
        return {
            "service": settings.service.name,
            "version": settings.moderation.model_version,
            "port": settings.service.port,
            "rabbitQueue": settings.rabbitmq.request_queue,
            "nacosService": settings.nacos.service_name or settings.service.name,
        }

    return app
