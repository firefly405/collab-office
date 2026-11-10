# Collab Office Docker deployment

The Compose project runs the complete server-side platform and Web client. The
Android application remains a client artifact and is built with Android Studio.

## Start

Requirements: Docker Desktop with at least 8 GB memory available.

```powershell
cd collab-office
./docker-start.ps1
```

Equivalent command:

```powershell
docker compose up --build -d --remove-orphans
```

The first build downloads the Java, Node, LibreOffice and middleware images and
can take several minutes. Subsequent builds reuse BuildKit and Maven/npm caches.

## Entry points

| Component | URL |
| --- | --- |
| Web application | http://localhost:3030 |
| Gateway health/API debugging | http://localhost:18090/api/health |
| MinIO Console | http://localhost:19001 |
| Mailpit | http://localhost:18025 |
| Nacos Console | http://localhost:18848/nacos |
| RabbitMQ management | http://localhost:15673 |
| Elasticsearch | http://localhost:19200 |
| MySQL | localhost:13306 |

The browser uses a single origin. Nginx forwards `/api` to the Gateway and
upgrades `/ws` to the Netty WebSocket service.

## Operations

```powershell
docker compose ps
docker compose logs -f gateway
docker compose logs --tail=200
./docker-stop.ps1
```

`docker-stop.ps1` preserves MySQL, Redis, RabbitMQ, MinIO, Elasticsearch and
Nacos volumes. To intentionally reset all development data, stop the stack and
run `docker compose down -v`; this operation is destructive.

Database migrations are executed by the one-shot `db-migrate` Flyway service.
MinIO bucket creation and Elasticsearch index creation are also handled by
one-shot initialization containers.

## One-click offline package

The application is a multi-container system, so it should not be collapsed
into one runtime container. For offline delivery, all required images are
stored in one `collab-office-images.tar` archive and installed by one BAT file.

On the prepared development machine, double-click:

```text
docker-offline-export.bat
```

The exporter builds the business images, verifies every middleware image and
creates a timestamped directory under `offline-packages`. Copy that entire
directory to the target Windows computer and double-click:

```text
docker-offline-import.bat
```

The importer starts Docker Desktop when necessary, verifies the SHA256 file,
loads all images, creates `.env` with random secrets on first use, starts
Compose with `--no-build --pull never`, waits for Web health and opens the
application. The target computer needs Docker Desktop but does not need Java,
Maven, Node.js or network access.

Use `docker-offline-stop.bat` to stop the application while retaining all
named volumes and application data.

Published ports can be changed in `.env` with `WEB_PORT`, `GATEWAY_PORT`,
`MYSQL_PORT`, `REDIS_PORT`, `RABBITMQ_AMQP_PORT`,
`RABBITMQ_MANAGEMENT_PORT`, `MINIO_API_PORT`, `MINIO_CONSOLE_PORT`,
`ELASTICSEARCH_PORT`, `NACOS_PORT`, and `MAILPIT_PORT`.
