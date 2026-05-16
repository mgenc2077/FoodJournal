# Sync Engine

Go server that synchronizes Food Journal data between Android clients over LAN.

## Quick start

### Docker from GHCR (recommended)

Pre-built images are published to GitHub Container Registry on every release:

```sh
docker pull ghcr.io/mgenc2077/FoodJournal/sync-engine:latest

docker run -p 42061:42061 \
  -v sync-data:/data \
  ghcr.io/mgenc2077/FoodJournal/sync-engine:latest
```

Specific versions are also available:

```sh
docker pull ghcr.io/mgenc2077/FoodJournal/sync-engine:1.0.0
```

### Build from source

```sh
docker build -t sync-engine .

# Run with debug logging
docker run -p 42061:42061 \
  -v sync-data:/data \
  -e LOG_LEVEL=debug \
  sync-engine

# Run with info logging (default)
docker run -p 42061:42061 \
  -v sync-data:/data \
  sync-engine
```

### Binary

```sh
go build -o sync-engine .

# Default port 42061, DB in current directory
./sync-engine

# With debug logging
./sync-engine -v

# Custom port and DB path
PORT=8080 DB_PATH=/path/to/db.sqlite ./sync-engine
```

## Configuration

| Variable | Default | Description |
|---|---|---|
| `PORT` | `42061` | HTTP listen port |
| `DB_PATH` | `food_journal.db` | Path to SQLite database file |
| `LOG_LEVEL` | `info` | Log level: `debug` or `info`. Also enabled by `-v` flag |

### Docker volumes

Mount `/data` as a volume to persist the database:

```sh
docker run -p 42061:42061 -v sync-data:/data sync-engine
```

## API

### `POST /sync`

Synchronize changes between client and server.

**Request:**

```json
{
  "last_sync_at": 1700000000000,
  "entries": [
    {
      "id": "019e2dbf-2122-7088-8be5-4d93c35f98c9",
      "epoch_day": 19680,
      "food_name": "Oatmeal",
      "meal_type": "BREAKFAST",
      "notes": "with berries",
      "created_at": 1700000000000,
      "display_order": 0,
      "updated_at": 1700000000000,
      "deleted_at": null
    }
  ],
  "recipes": [
    {
      "id": "019e2dc0-0198-7dbb-b529-34c59b1996fe",
      "name": "Smoothie",
      "notes": "banana, milk",
      "updated_at": 1700000000000,
      "deleted_at": null
    }
  ]
}
```

**Responses:**

| Status | When | Body |
|---|---|---|
| `200 OK` | Sync completed | `{"synced_at": <ms>, "entries": [...], "recipes": [...]}` |
| `303 See Other` | Server DB not initialized (empty) | `{"message": "...", "rebuild_url": "/rebuild"}` + `Location: /rebuild` header |

**Conflict resolution:** When both sides have the same ID, the row with the higher `updated_at` wins. No per-field merging.

### `POST /rebuild`

Replace the server database with a client-provided SQLite file. Used for initial sync when the server is empty.

**Request:**
- Content-Type: `application/octet-stream`
- Body: raw SQLite database file

**Response:**

| Status | When | Body |
|---|---|---|
| `200 OK` | Rebuild succeeded | `{"message": "Database rebuilt", "synced_at": <ms>}` |
| `400 Bad Request` | Empty body or not a SQLite file | `{"message": "..."}` |

**Rebuild process:**
1. Server writes uploaded file to a temp file
2. Ensures all required columns exist (adds `updatedAt`, `deletedAt` if missing)
3. Atomically renames temp file over the real DB file
4. Closes old DB connections and opens new ones
5. Marks server as `initialized` in `sync_meta` table

## Sync flow

```
Client                              Server
  |                                    |
  |  POST /sync (changes since last)   |
  |----------------------------------->|
  |                                    |
  |       303 See Other (if empty)     |
  |<-----------------------------------|
  |                                    |
  |  POST /rebuild (full SQLite DB)    |
  |----------------------------------->|
  |       200 OK (rebuilt)             |
  |<-----------------------------------|
  |                                    |
  |  POST /sync (last_sync_at=0)       |
  |----------------------------------->|
  |       200 OK (server state)        |
  |<-----------------------------------|
```

On subsequent syncs, the server responds with `200 OK` directly — no rebuild needed.

## Data model

The server stores the same schema as the Android client:

```sql
CREATE TABLE food_entries (
    id TEXT PRIMARY KEY,           -- UUIDv7
    epochDay INTEGER NOT NULL,
    foodName TEXT NOT NULL,
    mealType TEXT NOT NULL,        -- BREAKFAST, LUNCH, DINNER, SNACK
    notes TEXT,
    createdAt INTEGER NOT NULL,
    displayOrder INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL,    -- Unix ms, used for conflict resolution
    deletedAt INTEGER              -- Soft delete, NULL = active
);

CREATE TABLE recipes (
    id TEXT PRIMARY KEY,           -- UUIDv7
    name TEXT NOT NULL,
    notes TEXT,
    updatedAt INTEGER NOT NULL,
    deletedAt INTEGER
);

CREATE TABLE sync_meta (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
```

Column names are camelCase to match Room's default naming convention.

### Soft deletes

Deletion sets `deletedAt` to the current timestamp (unix ms) and bumps `updatedAt`. Deleted rows are included in sync responses so the deletion propagates to all clients. The Android app filters deleted rows from UI queries with `WHERE deletedAt IS NULL`.

### IDs

All IDs are UUIDv7 — time-sortable, unique across devices. Generated client-side on insert. No auto-increment.

## File structure

```
sync-engine/
├── main.go        # Entry point, HTTP server, graceful shutdown, config
├── db.go          # SQLite init, schema, queries, rebuild, dbHolder with mutex
├── handlers.go    # POST /sync, POST /rebuild HTTP handlers
├── models.go      # JSON request/response types
├── uuid.go        # UUIDv7 generation
├── Dockerfile     # Multi-stage build (Go builder → Alpine runtime)
├── go.mod
└── go.sum
```

## Dependencies

- [ncruces/go-sqlite3](https://github.com/ncruces/go-sqlite3) — WASM-based SQLite driver, no CGO required
- Standard library only for everything else (`database/sql`, `net/http`, `log/slog`, `encoding/json`)
