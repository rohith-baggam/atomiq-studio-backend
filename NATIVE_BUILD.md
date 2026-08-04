# Native Build Guide (GraalVM Native Image)

How to build the AtomIQ backend as a native executable, and — more importantly —
the issues we already debugged so you don't debug them again.

---

## 1. Build commands

### With GraalVM/Mandrel installed locally
`JAVA_HOME`/`GRAALVM_HOME` must point to a GraalVM distribution.

On this machine the default `JAVA_HOME` is Homebrew's plain OpenJDK 21, which has
no `native-image` — the build fails with *"Cannot find the `native-image` in the
GRAALVM_HOME, JAVA_HOME and System PATH."* GraalVM lives outside Homebrew, so
export it first:

```bash
export GRAALVM_HOME="$HOME/graalvm/graalvm-community-openjdk-21.0.2+13.1/Contents/Home"
export JAVA_HOME="$GRAALVM_HOME"
export PATH="$GRAALVM_HOME/bin:$PATH"

./mvnw package -Dnative
```

### Without local GraalVM (container build — needs Docker/Podman running)

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

> ⚠️ A container build produces a **Linux** executable. It cannot be used as the
> macOS sidecar — for a macOS build you need GraalVM running natively on the Mac.

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

### ⚠️ Issue 0: Backend won't start at all in the packaged app (SQLite)
**Symptom:** The desktop app opens but shows *"Cannot reach the AtomIQ backend. Is
it running on :8010?"*. The `atomiq-backend` process exits immediately. Its log
shows:

```
Failed to load native library: sqlite-<ver>-libsqlitejdbc.dylib
java.lang.UnsatisfiedLinkError: Can't load library: /var/folders/.../T/sqlite-....dylib
  ... at org.flywaydb.core.Flyway.migrate
```

**Cause: macOS hardened runtime, not the datasource config.** `sqlite-jdbc`
extracts `libsqlitejdbc.dylib` into a temp dir and `System.load()`s it at runtime.
Tauri signs the sidecar with hardened runtime enabled (`flags=0x10002 adhoc,runtime`),
which turns on **library validation** — macOS then refuses to load any dylib not
signed by the same team, including that freshly-extracted one.

Proven by signing the *same* binary two ways:

| Signature flags | Result |
| --- | --- |
| `0x10002` (adhoc, **runtime**) | `UnsatisfiedLinkError`, backend exits |
| `0x2` (adhoc, no hardened runtime) | starts in ~0.22s |

**Fix:** grant the library-validation exemption via
[`src-tauri/entitlements.plist`](../frontend/src-tauri/entitlements.plist), wired
up in `tauri.conf.json`:

```json
"macOS": { "signingIdentity": "-", "entitlements": "entitlements.plist" }
```

```xml
<key>com.apple.security.cs.disable-library-validation</key><true/>
```

Do **not** "fix" this by turning hardened runtime off — Apple notarization
requires it, so the entitlement is the only path that also works once the app is
notarized.

The datasource is also set to `quarkus.datasource.db-kind=sqlite` (rather than
`other` + an explicit `org.sqlite.JDBC` driver) so the `quarkus-jdbc-sqlite`
extension is actually engaged. That is the correct configuration, but note it did
**not** by itself resolve this failure — the entitlement did.

Like Issue 1, this is invisible in `quarkus dev`: nothing is signed there, so the
dylib loads fine. It only appears in the signed, packaged app.

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

- [ ] Datasource still `db-kind=sqlite`, no explicit jdbc.driver? (Issue 0)
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
