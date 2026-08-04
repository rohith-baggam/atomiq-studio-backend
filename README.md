# AtomIQ

**A universal database workbench.** One desktop app to connect to, query, explore, and manage PostgreSQL, MySQL, SQL Server, and Oracle — without switching tools per engine.

AtomIQ is a native desktop application (built with Tauri, not Electron), so it starts fast and stays small. It runs entirely on your machine: no cloud account, no telemetry service, no server to deploy.

---

## Download

**[Latest release →](https://github.com/rohith-baggam/atomiq-studio-backend/releases/latest)**

| Platform | Download | Size |
| --- | --- | --- |
| **macOS** (Apple Silicon) | [AtomIQ_aarch64.dmg](https://github.com/rohith-baggam/atomiq-studio-backend/releases/latest/download/AtomIQ_aarch64.dmg) | 62 MB |
| **Windows** 10/11 | [AtomIQ_x64-setup.exe](https://github.com/rohith-baggam/atomiq-studio-backend/releases/latest/download/AtomIQ_x64-setup.exe) | 43 MB |
| **Windows** (MSI, for IT deployment) | [AtomIQ_x64_en-US.msi](https://github.com/rohith-baggam/atomiq-studio-backend/releases/latest/download/AtomIQ_x64_en-US.msi) | 62 MB |
| **Linux** (any distro) | [AtomIQ_amd64.AppImage](https://github.com/rohith-baggam/atomiq-studio-backend/releases/latest/download/AtomIQ_amd64.AppImage) | 142 MB |
| **Debian / Ubuntu** | [AtomIQ_amd64.deb](https://github.com/rohith-baggam/atomiq-studio-backend/releases/latest/download/AtomIQ_amd64.deb) | 79 MB |
| **Fedora / RHEL** | [AtomIQ_x86_64.rpm](https://github.com/rohith-baggam/atomiq-studio-backend/releases/latest/download/AtomIQ_x86_64.rpm) | 79 MB |

Nothing else to install. Everything the app needs ships inside the installer.

**Not yet available:** Intel Macs, ARM Linux, and ARM Windows. On ARM Windows the x64 build runs under emulation.

### After downloading

The app isn't signed by Apple or Microsoft yet, so your OS will warn you on first launch. This is expected:

- **macOS** — Right-click the app in Applications and choose **Open**, then **Open** again in the dialog. Double-clicking the first time will just show "unidentified developer" and refuse.
- **Windows** — SmartScreen shows a blue warning. Click **More info**, then **Run anyway**.
- **Linux (AppImage)** — Mark it executable first: `chmod +x AtomIQ_amd64.AppImage`, then run it.

---

## What you can do with it

**Connect to anything.** Save named connection profiles for PostgreSQL, MySQL, SQL Server, and Oracle. Tag each one by environment (Local / Dev / Stage / Prod), pin your favorites, and reconnect in one click. Press `⌘K` / `Ctrl+K` to jump straight to a connection.

**Write and run SQL.** A full editor with syntax highlighting, autocomplete, and formatting. Open several queries in tabs, run the whole script, just the statement under your cursor, or just what you've selected. Query history is kept locally.

**Explore the schema without writing SQL.** Browse schemas, tables, columns, foreign keys, and indexes in a side panel. Click to insert identifiers into the editor, or right-click any table for ready-made `SELECT`/`INSERT`/`UPDATE`/`DELETE`/JOIN snippets generated from its real columns — in the correct dialect for that engine.

**See how tables relate.** An interactive ER diagram you can drag to rearrange, scoped to the whole database or one table's neighborhood. Plus a dependency view showing what a table references and what references it.

**Browse data directly.** A fast paginated grid for reading table contents, with configurable page size, NULL display, date format, and timezone.

**Inspect structure.** Per-table columns with types and key markers, indexes, generated DDL, and a live data sample.

**Work in a SQL notebook.** A Jupyter-style document of independently runnable cells, each with its own results. Saves as a plain `.sql` file using `-- %%` cell markers, so it opens in any text editor — no proprietary format.

**Compare environments.** Diff two tables' columns, indexes, keys, and DDL to spot drift between Stage and Prod.

**Import and export.** Export a portable SQL dump (schema-only or schema + data), or run a SQL script and get a per-statement pass/fail report.

**Generate realistic test data.** Describe what you want in plain language — *"realistic Indian e-commerce customers, unique emails, phone in +91 format, usernames like CUST-000001"* — and AtomIQ generates matching synthetic rows for one or more tables. Preview a sample before committing, choose your row count, or export it as a SQL script instead of writing to the database.

---

## Built so you don't break production

Safety is a deliberate design pillar, not a checkbox:

- **Read-only profiles** — mark a connection read-only and write statements are blocked before they're ever sent.
- **PROD guard** — connections tagged `PROD` get a hard block on destructive statements, with no per-query bypass.
- **Confirm before writing** — `UPDATE`, `DELETE`, and `DROP`-class statements prompt first.
- **Sensitive columns masked** — columns named like `password`, `secret`, `token`, or `api_key` render as `••••••••` and can't be copied.
- **Typed confirmation for destructive actions** — dropping a database means typing its name, not clicking through a dialog.

---

## Who it's for

Developers and data engineers who already know what a foreign key is. AtomIQ is dense, keyboard-first, and skips the onboarding wizard — it assumes SQL fluency. It's especially aimed at people working across more than one database engine in the same job, and at anyone who has ever run the wrong statement against production.

---

## Good to know

- **This is a 1.0 release from a small team.** It's new software; expect rough edges and please [report them](https://github.com/rohith-baggam/atomiq-studio-backend/issues).
- **Your session persists locally** — closing and reopening the app restores your open tabs, notebook cells, and selected table.
- **Connection credentials are stored locally** in app storage. Hardening this to use the OS secure store is on the roadmap; treat it as you would any local developer tool's saved credentials.
- The app updates itself — new releases are delivered through the built-in updater.

---

## About this repository

This repository holds the **AtomIQ backend** — the API that runs locally inside the desktop app. The desktop shell and user interface live in the separate `atomiq-frontend` repository.

Building from source is documented in [NATIVE_BUILD.md](NATIVE_BUILD.md). [downloads.json](downloads.json) is the machine-readable download manifest used by the landing page.
