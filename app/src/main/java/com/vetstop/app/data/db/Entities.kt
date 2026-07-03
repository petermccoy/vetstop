package com.vetstop.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user-drawn polygon that the weekly sync searches for pet-service places. */
@Entity(tableName = "search_areas")
data class SearchAreaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Polygon vertices encoded as "lat,lng;lat,lng;...". */
    val polygon: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A vet / pet store / animal shelter discovered by the place sync. */
@Entity(
    tableName = "locations",
    indices = [Index("category"), Index("isActive")],
)
data class LocationEntity(
    /** Google Places place ID. */
    @PrimaryKey val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    /** Name of a [com.vetstop.app.domain.model.LocationCategory]. */
    val category: String,
    /** ID of the search area whose sync most recently saw this place. */
    val areaId: Long,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    /** False once the place stops appearing in sync results (closed/moved). */
    val isActive: Boolean = true,
)

/** A logged brochure-drop visit to a location. */
@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["placeId"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("placeId")],
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeId: String,
    val visitorName: String,
    val visitedAt: Long,
    val brochuresRemaining: Int,
    val brochuresLeft: Int,
    val notes: String,
)
