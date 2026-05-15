package main

import (
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"strings"
	"time"
)

type app struct {
	holder *dbHolder
}

func (a *app) handleSync(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		slog.Debug("sync rejected: method not allowed", "method", r.Method, "remote", r.RemoteAddr)
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	slog.Info("sync request received", "remote", r.RemoteAddr)

	db := a.holder.get()

	empty, err := isEmpty(db)
	if err != nil {
		slog.Error("failed to check if db is empty", "error", err)
		writeJSON(w, http.StatusInternalServerError, ErrorResponse{Message: "database error"})
		return
	}

	if empty {
		slog.Info("database is empty, requesting rebuild", "remote", r.RemoteAddr)
		w.Header().Set("Location", "/rebuild")
		writeJSON(w, http.StatusSeeOther, ErrorResponse{
			Message:    "Server database is empty, rebuild required",
			RebuildURL: "/rebuild",
		})
		return
	}

	var req SyncRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		slog.Debug("sync rejected: invalid request body", "error", err)
		writeJSON(w, http.StatusBadRequest, ErrorResponse{Message: "invalid request body"})
		return
	}

	slog.Debug("sync payload", "last_sync_at", req.LastSyncAt, "entries", len(req.Entries), "recipes", len(req.Recipes))

	for _, e := range req.Entries {
		if err := upsertEntry(db, e); err != nil {
			slog.Error("upsert entry failed", "id", e.ID, "error", err)
		}
	}
	for _, rc := range req.Recipes {
		if err := upsertRecipe(db, rc); err != nil {
			slog.Error("upsert recipe failed", "id", rc.ID, "error", err)
		}
	}

	entries, err := getEntriesSince(db, req.LastSyncAt)
	if err != nil {
		slog.Error("get entries since failed", "since", req.LastSyncAt, "error", err)
		writeJSON(w, http.StatusInternalServerError, ErrorResponse{Message: "database error"})
		return
	}
	recipes, err := getRecipesSince(db, req.LastSyncAt)
	if err != nil {
		slog.Error("get recipes since failed", "since", req.LastSyncAt, "error", err)
		writeJSON(w, http.StatusInternalServerError, ErrorResponse{Message: "database error"})
		return
	}

	syncedAt := nowMs()
	markInitialized(db)
	slog.Info("sync completed", "synced_at", syncedAt, "entries_returned", len(entries), "recipes_returned", len(recipes))

	writeJSON(w, http.StatusOK, SyncResponse{
		SyncedAt: syncedAt,
		Entries:  entries,
		Recipes:  recipes,
	})
}

func (a *app) handleRebuild(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		slog.Debug("rebuild rejected: method not allowed", "method", r.Method, "remote", r.RemoteAddr)
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	slog.Info("rebuild request received", "remote", r.RemoteAddr, "content_length", r.ContentLength)

	data, err := io.ReadAll(r.Body)
	if err != nil {
		slog.Error("rebuild failed to read body", "error", err)
		writeJSON(w, http.StatusBadRequest, ErrorResponse{Message: "failed to read body"})
		return
	}

	if len(data) == 0 {
		slog.Debug("rebuild rejected: empty body")
		writeJSON(w, http.StatusBadRequest, ErrorResponse{Message: "empty body"})
		return
	}

	if !isSQLite(data) {
		slog.Debug("rebuild rejected: not a valid SQLite database", "size", len(data))
		writeJSON(w, http.StatusBadRequest, ErrorResponse{Message: "not a valid SQLite database"})
		return
	}

	slog.Debug("rebuild: valid SQLite file received", "size", len(data))

	if err := a.holder.rebuildAndReload(data); err != nil {
		slog.Error("rebuild failed", "error", err)
		writeJSON(w, http.StatusInternalServerError, ErrorResponse{Message: "rebuild failed"})
		return
	}

	syncedAt := nowMs()
	slog.Info("rebuild completed", "synced_at", syncedAt)

	writeJSON(w, http.StatusOK, RebuildResponse{
		Message:  "Database rebuilt",
		SyncedAt: syncedAt,
	})
}

func isSQLite(data []byte) bool {
	if len(data) < 16 {
		return false
	}
	header := string(data[:16])
	return strings.HasPrefix(header, "SQLite format 3")
}

func nowMs() int64 {
	return time.Now().UnixMilli()
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}
