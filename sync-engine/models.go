package main

type FoodEntry struct {
	ID           string  `json:"id"`
	EpochDay     int64   `json:"epoch_day"`
	FoodName     string  `json:"food_name"`
	MealType     string  `json:"meal_type"`
	Notes        *string `json:"notes"`
	CreatedAt    int64   `json:"created_at"`
	DisplayOrder int     `json:"display_order"`
	UpdatedAt    int64   `json:"updated_at"`
	DeletedAt    *int64  `json:"deleted_at"`
}

type Recipe struct {
	ID        string  `json:"id"`
	Name      string  `json:"name"`
	Notes     *string `json:"notes"`
	UpdatedAt int64   `json:"updated_at"`
	DeletedAt *int64  `json:"deleted_at"`
}

type SyncRequest struct {
	LastSyncAt int64       `json:"last_sync_at"`
	Entries    []FoodEntry `json:"entries"`
	Recipes    []Recipe    `json:"recipes"`
}

type SyncResponse struct {
	SyncedAt int64       `json:"synced_at"`
	Entries  []FoodEntry `json:"entries"`
	Recipes  []Recipe    `json:"recipes"`
}

type RebuildResponse struct {
	Message  string `json:"message"`
	SyncedAt int64  `json:"synced_at"`
}

type ErrorResponse struct {
	Message    string `json:"message"`
	RebuildURL string `json:"rebuild_url,omitempty"`
}
