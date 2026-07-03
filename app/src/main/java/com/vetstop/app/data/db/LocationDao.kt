package com.vetstop.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** A location joined with the timestamp of its most recent visit (null if never visited). */
data class LocationWithLastVisit(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val areaId: Long,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val isActive: Boolean,
    val lastVisitAt: Long?,
)

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(location: LocationEntity): Long

    @Query(
        """UPDATE locations SET
               name = :name,
               address = :address,
               latitude = :latitude,
               longitude = :longitude,
               category = :category,
               areaId = :areaId,
               lastSeenAt = :lastSeenAt,
               isActive = 1
           WHERE placeId = :placeId"""
    )
    suspend fun refresh(
        placeId: String,
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        category: String,
        areaId: Long,
        lastSeenAt: Long,
    )

    @Query("UPDATE locations SET isActive = 0 WHERE areaId = :areaId AND lastSeenAt < :syncStartedAt")
    suspend fun deactivateStale(areaId: Long, syncStartedAt: Long)

    /**
     * Applies one sync run for an area: places in [fresh] are inserted or
     * refreshed with lastSeenAt >= [now]; places previously attributed to the
     * area but absent from [fresh] are marked inactive.
     */
    @Transaction
    suspend fun applySync(areaId: Long, fresh: List<LocationEntity>, now: Long) {
        for (location in fresh) {
            val inserted = insertIgnore(location)
            if (inserted == -1L) {
                refresh(
                    placeId = location.placeId,
                    name = location.name,
                    address = location.address,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    category = location.category,
                    areaId = areaId,
                    lastSeenAt = now,
                )
            }
        }
        deactivateStale(areaId, now)
    }

    @Query(
        """SELECT l.*,
                  (SELECT MAX(v.visitedAt) FROM visits v WHERE v.placeId = l.placeId) AS lastVisitAt
           FROM locations l
           WHERE l.isActive = 1
           ORDER BY l.name"""
    )
    fun observeActiveWithLastVisit(): Flow<List<LocationWithLastVisit>>

    @Query(
        """SELECT l.*,
                  (SELECT MAX(v.visitedAt) FROM visits v WHERE v.placeId = l.placeId) AS lastVisitAt
           FROM locations l
           WHERE l.placeId = :placeId"""
    )
    fun observeByIdWithLastVisit(placeId: String): Flow<LocationWithLastVisit?>

    @Query("SELECT * FROM locations WHERE placeId = :placeId")
    suspend fun getById(placeId: String): LocationEntity?

    @Query("SELECT * FROM locations WHERE isActive = 1")
    suspend fun getAllActive(): List<LocationEntity>

    @Query("SELECT placeId FROM locations WHERE isActive = 1 AND areaId = :areaId")
    suspend fun getActivePlaceIds(areaId: Long): List<String>

    @Query("SELECT COUNT(*) FROM locations WHERE isActive = 1")
    fun observeActiveCount(): Flow<Int>
}
