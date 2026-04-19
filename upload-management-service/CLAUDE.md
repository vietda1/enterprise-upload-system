# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Compile
mvn clean compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=AppTest

# Package (skip tests)
mvn clean package -DskipTests

# Run locally
mvn spring-boot:run
```

## Architecture Overview

This is a **Spring Boot 3.3.4 / Java 21** microservice for managing enterprise file uploads. It sits behind a Kong API Gateway and communicates with downstream services via Kafka.

### Request Flow

```
Frontend → Kong Gateway → JwtAuthenticationFilter → Controller → Service → Repository / S3 / Kafka
```

**Authentication** (`security/JwtAuthenticationFilter.java`): Parses `Authorization: Bearer <JWT>` and populates `UserPrincipal` (userId, departmentId, roles). Falls back to `X-User-Id` / `X-Department-Id` headers injected by Kong when no JWT is present. JWT claims: `sub`=userId, `roles` (List), `departmentId`, `username`.

**Authorization**: `@PreAuthorize` annotations on controller methods. Approve/reject endpoints require `ROLE_DEPARTMENT_MANAGER` or `ROLE_ADMIN`.

### Upload Lifecycle (Status Machine)

```
PENDING → UPLOADED → VALIDATED → PENDING_APPROVAL → APPROVED → INGESTING → COMPLETED
                  ↘ VALIDATION_FAILED
                                              ↘ REJECTED
PENDING → EXPIRED  (presigned URL expired)
any     → DELETED  (soft delete)
any     → FAILED
```

Kafka drives status transitions after `UPLOADED`:
- `upload.completed` (published by this service) → triggers Validation Service
- `validation.completed` (consumed by `UploadEventConsumer`) → updates to `VALIDATED` or `VALIDATION_FAILED`
- `ingestion.completed` (consumed by `UploadEventConsumer`) → updates to `COMPLETED` or `FAILED`

### Key Entity Design

**`Upload` entity** (`model/Upload.java`) has two identifiers:
- `id` (UUID): JPA primary key, never exposed in APIs
- `uploadId` (String, e.g. `UPL-2026-0001`): business key, unique, immutable — **always use `uploadRepository.findByUploadId(String)` in business logic, never `findById(UUID)`**

The `metadata` JSONB field (`Map<String, Object>`) stores arbitrary data. Multipart uploads store `s3UploadId` and `totalParts` here to avoid schema changes (DDL-auto is `validate`).

**DB triggers** (do not replicate in Java):
- `trg_uploads_updated_at`: auto-updates `updated_at` on every row change
- `trg_log_upload_status_change`: writes status transitions to `upload_history`

Because of these triggers, there is no `@PreUpdate` on Upload. `createdAt`/`updatedAt` are set manually in `@PrePersist`.

### Storage Layer

`MinioService` interface abstracts object storage. The implementation (`MinioServiceImpl`) uses **AWS SDK v2** (`S3Client` + `S3Presigner`). Config is in `MinioConfig.java` (bean names kept for backward compatibility).

Object key format: `uploads/YYYY/MM/<departmentId>/<userId>/<uploadId>/<sanitized-fileName>`

File names are sanitized to `[a-zA-Z0-9._\-]` (others → `_`).

### Response Conventions

- All API responses: `ApiResponse<T>` wrapper with `success`, `message`, `data`, `timestamp`
- All errors: RFC 9457 `ProblemDetail` format (`GlobalExceptionHandler`)
- `UploadMapper` (MapStruct): handles `Upload → UploadResponse` including lazy-loaded `validationResults` and `ingestionJobs` (index 0 = latest due to `@OrderBy`)

### Configuration Properties

```yaml
aws.s3.access-key / secret-key / region / bucket-name / presigned-url-expiry-minutes
jwt.secret / jwt.expiration
kafka.topics.upload-completed / validation-completed / ingestion-completed
upload.max-file-size-mb / allowed-extensions
```

Database schema: `upload_db` (PostgreSQL). `spring.jpa.hibernate.ddl-auto: validate` — the schema must exist before startup; do not use `update` or `create`.

### Scheduled Tasks

`UploadCleanupTask` runs every hour (`0 0 * * * *`): finds PENDING uploads whose `presignedUrlExpiresAt` is in the past, deletes the S3 object if it exists, and hard-deletes the DB record.

### Enum Location

`UploadStatus` is exclusively in `model/enums/UploadStatus.java`. There is no `model/UploadStatus.java` — it was removed as a duplicate.

### Entry Point

The real main class is `upload/UploadApplication.java`. `App.java` at the root package is unused legacy code.