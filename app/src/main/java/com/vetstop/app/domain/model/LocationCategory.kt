package com.vetstop.app.domain.model

/**
 * The kinds of places VetStop tracks. Each category maps to a Places API
 * text-search query used by the weekly sync.
 */
enum class LocationCategory(
    val label: String,
    val searchQuery: String,
) {
    VET(label = "Veterinarian", searchQuery = "veterinarian"),
    PET_STORE(label = "Pet store", searchQuery = "pet store"),
    ANIMAL_SHELTER(label = "Animal shelter", searchQuery = "animal shelter");

    companion object {
        fun fromName(name: String): LocationCategory =
            entries.firstOrNull { it.name == name } ?: VET
    }
}
