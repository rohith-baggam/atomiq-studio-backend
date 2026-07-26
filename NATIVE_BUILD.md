# Native Build Guide (GraalVM Native Image)

How to build the AtomIQ backend as a native executable, and — more importantly —
the issues we already debugged so you don't debug them again.

---

## 1. Build commands

### With GraalVM/Mandrel installed locally
`JAVA_HOME`/`GRAALVM_HOME` must point to a GraalVM distribution.

```bash
./mvnw package -Dnative
```

### Without local GraalVM (container build — needs Docker/Podman running)

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

### Faster iteration (skip tests)

```bash
./mvnw package -Dnative -DskipTests
```

### Output

```
target/atomic-<version>-runner        # ~168MB binary, no JVM needed
```

Run it directly:

```bash
./target/atomic-*-runner
```

> Native builds take 5–10 min and several GB of RAM. That's normal.

---

## 2. Known issues & fixes (DO NOT re-debug these)

### ⚠️ Issue 1: Endpoints return 500 in native, but work in `quarkus dev`
**Symptom:** `Jackson: No serializer found for class ... no properties discovered`
only in the packaged/native app. JVM dev mode works fine.

**Cause:** REST methods return raw `jakarta.ws.rs.core.Response` (via
`ApiResponse.success/error`), so Quarkus can't infer entity types at build time
and GraalVM strips their reflection metadata.

**Fix (already in place):**
[ReflectionConfig.java](src/main/java/com/shared/config/ReflectionConfig.java)
registers every DTO/entity/enum that crosses the JSON boundary with
`@RegisterForReflection`.

**👉 Rule: every NEW request/response DTO, entity, or enum that gets serialized
to JSON MUST be added to `ReflectionConfig`.** If you forget, it will work in
dev and 500 in the native build.

### ⚠️ Issue 2: SQLite DB path is read-only in the installed app
The install dir is read-only on end-user machines. The JDBC URL resolves the DB
path from an env var:

```properties
quarkus.datasource.jdbc.url=jdbc:sqlite:${atomiq.db.path:./atomic.db}?journal_mode=WAL&...
```

- Packaged app: the Tauri shell sets `ATOMIQ_DB_PATH=<app-data-dir>/atomic.db`
- Dev: falls back to `./atomic.db`

Don't hardcode DB paths anywhere else.

### ⚠️ Issue 3: Port is dynamic in the packaged app
The Tauri shell spawns the binary on a free loopback port via
`QUARKUS_HTTP_PORT`. Never assume :8010/:8080 in packaged mode — dev uses the
Vite proxy on :8010.

### ⚠️ Issue 4: Webview can't call the backend directly (frontend side)
Mixed content (`tauri://` → `http://`) + CORS blocks direct fetches from the
webview. API calls must go through `@tauri-apps/plugin-http` (wired in the
frontend's `src/api/bootstrap.ts` via `setHttpFetch`). If a new frontend call
bypasses that, it fails only in the packaged app.

---

## 3. Where the binary goes (desktop packaging)

The binary is bundled as a Tauri **sidecar** in the frontend repo:

```
frontend/src-tauri/binaries/atomiq-backend-<target-triple>   # gitignored
```

- The Rust shell (`src-tauri/src/lib.rs`) spawns it in RELEASE builds only.
- Multi-platform installers build only in CI (frontend repo
  `.github/workflows/release.yml`, triggered by a `v*` tag) — `tauri build`
  only targets its host OS.
- Keep the version in backend `pom.xml` aligned with `tauri.conf.json`.

See `frontend/docs/PACKAGING.md` for the full desktop pipeline.

---

## 4. Pre-build checklist

- [ ] New DTOs/entities/enums added to `ReflectionConfig`? (Issue 1)
- [ ] No hardcoded DB paths or ports? (Issues 2–3)
- [ ] Flyway migrations in `src/main/resources/db/migration` compile-clean?
      (`migrate-at-start=true` — a bad migration crashes the packaged app on boot)
- [ ] Version bumped in `pom.xml` **and** frontend `tauri.conf.json`?
- [ ] Smoke-test the actual binary, not just `quarkus dev`:
      `./target/atomic-*-runner` then hit a few JSON endpoints.

## 5. If the native build itself fails

- The pom bundles JDBC drivers for MySQL, PostgreSQL, MSSQL, **Oracle**, and
  SQLite. Oracle's driver is the most likely to need extra native-image config —
  check the failing class in the build log and add
  `-Dquarkus.native.additional-build-args=...` or reflection config as needed.
- Out-of-memory during `native-image`: close other apps or add
  `-Dquarkus.native.native-image-xmx=8g`.
