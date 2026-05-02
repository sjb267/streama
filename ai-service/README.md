# streama-cloud-ai

Python AI moderation microservice for `streama-cloud`.

## What It Does

- Registers itself to Nacos as `streama-cloud-ai`
- Consumes audit requests from RabbitMQ queue `streama.audit.request.queue`
- Reuses the moderation pipeline migrated from `test2.py` into `streama_ai_service`
- Publishes audit results back to `streama.audit.exchange` with routing key `audit.video.result`
- Exposes health endpoints:
  - `GET /health`
  - `GET /actuator/health`
  - `GET /actuator/info`

## Install

```powershell
.\ai-service-env\python.exe -m pip install -r requirements.txt
```

## Start

```powershell
.\ai-service-env\python.exe main.py
```

## Default Ports And Names

- HTTP port: `7075`
- Nacos service name: `streama-cloud-ai`
- Rabbit exchange: `streama.audit.exchange`

## Config File

Main config file: `config/application.yaml`

Important defaults:

- Nacos: `127.0.0.1:8848`
- RabbitMQ: `127.0.0.1:5672`
- Gateway web base: `http://127.0.0.1:7071/web`
- Gateway file base: `http://127.0.0.1:7071/file`
- Video storage root: `../streama-cloud/file`
- Mixed device policy:
  - `YAMNet` on CPU
  - `Whisper` on CPU
  - `VAD` on CPU
  - `Qwen/Qwen3-VL-4B-Instruct` on GPU (`cuda`)

## Deployment Notes

- The service preloads all core moderation models during startup.
- Startup fails fast if Qwen cannot run on a CUDA device.
- CPU-only Windows hosts are acceptable for development or static checks, but not for full AI moderation service startup.
- Raw video download now goes through the gateway public file route by default.
- Audit item progress callback is disabled by default because the current Java gateway does not expose a public route for `aiAuditItemProgress`.
- RabbitMQ consumption now keeps the broker connection responsive during long AI runs and reuses recent completed request results to avoid duplicate re-audits after redelivery.

## Environment Overrides

You can override the main bootstrap values with env vars:

- `STREAMA_AI_SERVICE_HOST`
- `STREAMA_AI_SERVICE_PORT`
- `STREAMA_AI_NACOS_SERVER_ADDR`
- `STREAMA_AI_NACOS_USERNAME`
- `STREAMA_AI_NACOS_PASSWORD`
- `STREAMA_AI_RABBITMQ_HOST`
- `STREAMA_AI_RABBITMQ_PORT`
- `STREAMA_AI_RABBITMQ_USERNAME`
- `STREAMA_AI_RABBITMQ_PASSWORD`
- `STREAMA_AI_RABBITMQ_REQUEST_DEDUPE_TTL_SECONDS`
- `STREAMA_AI_RABBITMQ_REQUEST_DEDUPE_MAX_ENTRIES`
- `STREAMA_AI_STORAGE_ROOT_DIR`
- `STREAMA_AI_WEB_BASE_URL`
- `STREAMA_AI_RESOURCE_BASE_URL`
- `STREAMA_AI_PROGRESS_CALLBACK_ENABLED`
- `STREAMA_AI_HTTP_TIMEOUT_SECONDS`
- `STREAMA_AI_MODEL_DIR`
- `STREAMA_AI_QWEN_DEVICE`
- `STREAMA_AI_PRELOAD_MODELS`

`STREAMA_AI_DEVICE` is kept only as a compatibility fallback. Use `moderation.qwen_device` or `STREAMA_AI_QWEN_DEVICE` for the formal Qwen deployment target.
