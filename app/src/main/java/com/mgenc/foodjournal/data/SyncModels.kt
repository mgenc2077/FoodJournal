package com.mgenc.foodjournal.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncEntry(
    @SerialName("id") val id: String,
    @SerialName("epoch_day") val epochDay: Long,
    @SerialName("food_name") val foodName: String,
    @SerialName("meal_type") val mealType: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("display_order") val displayOrder: Int,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

@Serializable
data class SyncRecipe(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

@Serializable
data class SyncCookingPlan(
    @SerialName("id") val id: String,
    @SerialName("epoch_day") val epochDay: Long,
    @SerialName("name") val name: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

@Serializable
data class SyncRequest(
    @SerialName("last_sync_at") val lastSyncAt: Long,
    @SerialName("entries") val entries: List<SyncEntry>,
    @SerialName("recipes") val recipes: List<SyncRecipe>,
    @SerialName("cooking_plans") val cookingPlans: List<SyncCookingPlan>,
)

@Serializable
data class SyncResponse(
    @SerialName("synced_at") val syncedAt: Long,
    @SerialName("entries") val entries: List<SyncEntry>,
    @SerialName("recipes") val recipes: List<SyncRecipe>,
    @SerialName("cooking_plans") val cookingPlans: List<SyncCookingPlan>,
)

@Serializable
data class RebuildResponse(
    @SerialName("message") val message: String,
    @SerialName("synced_at") val syncedAt: Long,
)

@Serializable
data class ErrorResponse(
    @SerialName("message") val message: String,
    @SerialName("rebuild_url") val rebuildUrl: String? = null,
)

fun FoodEntry.toSync() = SyncEntry(id, epochDay, foodName, mealType, notes, createdAt, displayOrder, updatedAt, deletedAt)

fun Recipe.toSync() = SyncRecipe(id, name, notes, updatedAt, deletedAt)

fun SyncEntry.toEntity() = FoodEntry(id, epochDay, foodName, mealType, notes, createdAt, displayOrder, updatedAt, deletedAt)

fun SyncRecipe.toEntity() = Recipe(id, name, notes, updatedAt, deletedAt)

fun CookingPlan.toSync() = SyncCookingPlan(id, epochDay, name, notes, createdAt, updatedAt, deletedAt)

fun SyncCookingPlan.toEntity() = CookingPlan(id, epochDay, name, notes, createdAt, updatedAt, deletedAt)
