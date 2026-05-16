package main

import (
	"database/sql"
	"fmt"
	"log/slog"
	"os"
	"sync"

	_ "github.com/ncruces/go-sqlite3/driver"
)

const schema = `
CREATE TABLE IF NOT EXISTS food_entries (
    id TEXT PRIMARY KEY,
    epochDay INTEGER NOT NULL,
    foodName TEXT NOT NULL,
    mealType TEXT NOT NULL,
    notes TEXT,
    createdAt INTEGER NOT NULL,
    displayOrder INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL,
    deletedAt INTEGER
);

CREATE TABLE IF NOT EXISTS recipes (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    notes TEXT,
    updatedAt INTEGER NOT NULL,
    deletedAt INTEGER
);

CREATE TABLE IF NOT EXISTS cooking_plans (
    id TEXT PRIMARY KEY,
    epochDay INTEGER NOT NULL,
    name TEXT NOT NULL,
    notes TEXT,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    deletedAt INTEGER
);

CREATE TABLE IF NOT EXISTS sync_meta (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
`

type dbHolder struct {
	mu     sync.Mutex
	db     *sql.DB
	dbPath string
}

func newDBHolder(dbPath string) (*dbHolder, error) {
	db, err := openDB(dbPath)
	if err != nil {
		return nil, err
	}
	return &dbHolder{db: db, dbPath: dbPath}, nil
}

func (h *dbHolder) get() *sql.DB {
	h.mu.Lock()
	defer h.mu.Unlock()
	return h.db
}

func (h *dbHolder) rebuildAndReload(data []byte) error {
	h.mu.Lock()
	defer h.mu.Unlock()

	slog.Debug("rebuilding database", "path", h.dbPath, "size", len(data))

	if err := h.db.Close(); err != nil {
		slog.Warn("closing old db connection", "error", err)
	}

	if err := rebuildToFile(h.dbPath, data); err != nil {
		newDb, openErr := openDB(h.dbPath)
		if openErr != nil {
			slog.Error("failed to reopen db after rebuild failure", "error", openErr)
		} else {
			h.db = newDb
		}
		return err
	}

	newDb, err := openDB(h.dbPath)
	if err != nil {
		slog.Error("failed to reopen db after rebuild", "error", err)
		return fmt.Errorf("reopen after rebuild: %w", err)
	}
	h.db = newDb
	markInitialized(h.db)

	slog.Info("database rebuilt and reloaded")
	return nil
}

func (h *dbHolder) close() {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.db.Close()
}

func openDB(path string) (*sql.DB, error) {
	db, err := sql.Open("sqlite3", "file:"+path+"?_journal_mode=WAL")
	if err != nil {
		return nil, fmt.Errorf("open db: %w", err)
	}
	if _, err := db.Exec(schema); err != nil {
		db.Close()
		return nil, fmt.Errorf("init schema: %w", err)
	}
	return db, nil
}

func isEmpty(db *sql.DB) (bool, error) {
	var val string
	err := db.QueryRow("SELECT value FROM sync_meta WHERE key = 'initialized'").Scan(&val)
	if err == nil && val == "true" {
		return false, nil
	}
	return true, nil
}

func markInitialized(db *sql.DB) {
	db.Exec("INSERT OR REPLACE INTO sync_meta (key, value) VALUES ('initialized', 'true')")
	slog.Debug("marked database as initialized")
}

func upsertEntry(db *sql.DB, e FoodEntry) error {
	existing, err := getEntry(db, e.ID)
	if err != nil {
		return err
	}
	if existing != nil && existing.UpdatedAt >= e.UpdatedAt {
		slog.Debug("skipping entry, server is newer or equal", "id", e.ID, "server_updated", existing.UpdatedAt, "client_updated", e.UpdatedAt)
		return nil
	}
	_, err = db.Exec(`INSERT OR REPLACE INTO food_entries
		(id, epochDay, foodName, mealType, notes, createdAt, displayOrder, updatedAt, deletedAt)
		VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
		e.ID, e.EpochDay, e.FoodName, e.MealType, e.Notes, e.CreatedAt, e.DisplayOrder, e.UpdatedAt, e.DeletedAt)
	if err != nil {
		return err
	}
	slog.Debug("upserted entry", "id", e.ID, "foodName", e.FoodName, "updatedAt", e.UpdatedAt, "deleted", e.DeletedAt != nil)
	return nil
}

func upsertRecipe(db *sql.DB, r Recipe) error {
	existing, err := getRecipe(db, r.ID)
	if err != nil {
		return err
	}
	if existing != nil && existing.UpdatedAt >= r.UpdatedAt {
		slog.Debug("skipping recipe, server is newer or equal", "id", r.ID, "server_updated", existing.UpdatedAt, "client_updated", r.UpdatedAt)
		return nil
	}
	_, err = db.Exec(`INSERT OR REPLACE INTO recipes
		(id, name, notes, updatedAt, deletedAt) VALUES (?, ?, ?, ?, ?)`,
		r.ID, r.Name, r.Notes, r.UpdatedAt, r.DeletedAt)
	if err != nil {
		return err
	}
	slog.Debug("upserted recipe", "id", r.ID, "name", r.Name, "updatedAt", r.UpdatedAt, "deleted", r.DeletedAt != nil)
	return nil
}

func getEntry(db *sql.DB, id string) (*FoodEntry, error) {
	row := db.QueryRow(`SELECT id, epochDay, foodName, mealType, notes, createdAt, displayOrder, updatedAt, deletedAt
		FROM food_entries WHERE id = ?`, id)
	var e FoodEntry
	if err := row.Scan(&e.ID, &e.EpochDay, &e.FoodName, &e.MealType, &e.Notes, &e.CreatedAt, &e.DisplayOrder, &e.UpdatedAt, &e.DeletedAt); err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &e, nil
}

func getRecipe(db *sql.DB, id string) (*Recipe, error) {
	row := db.QueryRow(`SELECT id, name, notes, updatedAt, deletedAt FROM recipes WHERE id = ?`, id)
	var r Recipe
	if err := row.Scan(&r.ID, &r.Name, &r.Notes, &r.UpdatedAt, &r.DeletedAt); err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &r, nil
}

func getEntriesSince(db *sql.DB, since int64) ([]FoodEntry, error) {
	result := make([]FoodEntry, 0)
	rows, err := db.Query(`SELECT id, epochDay, foodName, mealType, notes, createdAt, displayOrder, updatedAt, deletedAt
		FROM food_entries WHERE updatedAt > ? ORDER BY updatedAt`, since)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	for rows.Next() {
		var e FoodEntry
		if err := rows.Scan(&e.ID, &e.EpochDay, &e.FoodName, &e.MealType, &e.Notes, &e.CreatedAt, &e.DisplayOrder, &e.UpdatedAt, &e.DeletedAt); err != nil {
			return nil, err
		}
		result = append(result, e)
	}
	return result, rows.Err()
}

func getRecipesSince(db *sql.DB, since int64) ([]Recipe, error) {
	result := make([]Recipe, 0)
	rows, err := db.Query(`SELECT id, name, notes, updatedAt, deletedAt FROM recipes WHERE updatedAt > ? ORDER BY updatedAt`, since)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	for rows.Next() {
		var r Recipe
		if err := rows.Scan(&r.ID, &r.Name, &r.Notes, &r.UpdatedAt, &r.DeletedAt); err != nil {
			return nil, err
		}
		result = append(result, r)
	}
	return result, rows.Err()
}

func upsertCookingPlan(db *sql.DB, p CookingPlan) error {
	existing, err := getCookingPlan(db, p.ID)
	if err != nil {
		return err
	}
	if existing != nil && existing.UpdatedAt >= p.UpdatedAt {
		slog.Debug("skipping cooking plan, server is newer or equal", "id", p.ID, "server_updated", existing.UpdatedAt, "client_updated", p.UpdatedAt)
		return nil
	}
	_, err = db.Exec(`INSERT OR REPLACE INTO cooking_plans
		(id, epochDay, name, notes, createdAt, updatedAt, deletedAt)
		VALUES (?, ?, ?, ?, ?, ?, ?)`,
		p.ID, p.EpochDay, p.Name, p.Notes, p.CreatedAt, p.UpdatedAt, p.DeletedAt)
	if err != nil {
		return err
	}
	slog.Debug("upserted cooking plan", "id", p.ID, "name", p.Name, "updatedAt", p.UpdatedAt, "deleted", p.DeletedAt != nil)
	return nil
}

func getCookingPlan(db *sql.DB, id string) (*CookingPlan, error) {
	row := db.QueryRow(`SELECT id, epochDay, name, notes, createdAt, updatedAt, deletedAt FROM cooking_plans WHERE id = ?`, id)
	var p CookingPlan
	if err := row.Scan(&p.ID, &p.EpochDay, &p.Name, &p.Notes, &p.CreatedAt, &p.UpdatedAt, &p.DeletedAt); err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &p, nil
}

func getCookingPlansSince(db *sql.DB, since int64) ([]CookingPlan, error) {
	result := make([]CookingPlan, 0)
	rows, err := db.Query(`SELECT id, epochDay, name, notes, createdAt, updatedAt, deletedAt FROM cooking_plans WHERE updatedAt > ? ORDER BY updatedAt`, since)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	for rows.Next() {
		var p CookingPlan
		if err := rows.Scan(&p.ID, &p.EpochDay, &p.Name, &p.Notes, &p.CreatedAt, &p.UpdatedAt, &p.DeletedAt); err != nil {
			return nil, err
		}
		result = append(result, p)
	}
	return result, rows.Err()
}

func rebuildToFile(dbPath string, data []byte) error {
	tmp := dbPath + ".tmp"
	if err := os.WriteFile(tmp, data, 0644); err != nil {
		return fmt.Errorf("write temp db: %w", err)
	}

	tmpDB, err := sql.Open("sqlite3", "file:"+tmp+"?mode=rw")
	if err != nil {
		os.Remove(tmp)
		return fmt.Errorf("open temp db: %w", err)
	}

	if _, err := tmpDB.Exec(schema); err != nil {
		tmpDB.Close()
		os.Remove(tmp)
		return fmt.Errorf("ensure schema: %w", err)
	}

	migrateColumn(tmpDB, "food_entries", "updatedAt", "ALTER TABLE food_entries ADD COLUMN updatedAt INTEGER", "UPDATE food_entries SET updatedAt = createdAt WHERE updatedAt IS NULL")
	migrateColumn(tmpDB, "food_entries", "deletedAt", "ALTER TABLE food_entries ADD COLUMN deletedAt INTEGER", "")
	migrateColumn(tmpDB, "recipes", "updatedAt", "ALTER TABLE recipes ADD COLUMN updatedAt INTEGER", "UPDATE recipes SET updatedAt = strftime('%s','now')*1000 WHERE updatedAt IS NULL")
	migrateColumn(tmpDB, "recipes", "deletedAt", "ALTER TABLE recipes ADD COLUMN deletedAt INTEGER", "")

	tmpDB.Close()

	if err := os.Rename(tmp, dbPath); err != nil {
		os.Remove(tmp)
		return fmt.Errorf("rename temp db: %w", err)
	}

	slog.Debug("database file replaced atomically", "path", dbPath)
	return nil
}

func migrateColumn(db *sql.DB, table, column, alterSQL, updateSQL string) {
	if !columnExists(db, table, column) {
		db.Exec(alterSQL)
		if updateSQL != "" {
			db.Exec(updateSQL)
		}
		slog.Debug("migrated column", "table", table, "column", column)
	}
}

func columnExists(db *sql.DB, table, column string) bool {
	rows, err := db.Query("PRAGMA table_info(" + table + ")")
	if err != nil {
		return false
	}
	defer rows.Close()
	for rows.Next() {
		var cid int
		var name, typ string
		var notNull int
		var dflt sql.NullString
		var pk int
		if err := rows.Scan(&cid, &name, &typ, &notNull, &dflt, &pk); err != nil {
			continue
		}
		if name == column {
			return true
		}
	}
	return false
}
