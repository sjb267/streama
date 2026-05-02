from __future__ import annotations

import uvicorn

from streama_ai_service.app import create_app
from streama_ai_service.config import load_settings

app = create_app()


if __name__ == "__main__":
    settings = load_settings()
    uvicorn.run(
        "main:app",
        host=settings.service.host,
        port=settings.service.port,
        log_level=settings.service.log_level.lower(),
    )
