package com.vetstop.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A pending (not-yet-logged) trip stop joined with its location's name. */
data class PendingVisitWithLocation(
    @Embedded val pending: PendingVisitEntity,
    val locationName: String,
)

@Dao
interface PendingVisitDao {

    /** One pending entry per place: relaunching a trip refreshes the timestamp. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pending: PendingVisitEntity)

    @Query("DELETE FROM pending_visits WHERE placeId = :placeId")
    suspend fun deleteByPlaceId(placeId: String)

    @Query(
        """SELECT p.*, l.name AS locationName
           FROM pending_visits p JOIN locations l ON l.placeId = p.placeId
           ORDER BY p.createdAt DESC"""
    )
    fun observeAllWithLocation(): Flow<List<PendingVisitWithLocation>>
}
