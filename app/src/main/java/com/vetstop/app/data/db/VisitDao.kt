package com.vetstop.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A visit joined with the name of the location it happened at. */
data class RecentVisit(
    @Embedded val visit: VisitEntity,
    val locationName: String,
)

@Dao
interface VisitDao {

    @Insert
    suspend fun insert(visit: VisitEntity): Long

    @Update
    suspend fun update(visit: VisitEntity)

    @Delete
    suspend fun delete(visit: VisitEntity)

    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun getById(id: Long): VisitEntity?

    @Query("SELECT * FROM visits WHERE placeId = :placeId ORDER BY visitedAt DESC")
    fun observeForPlace(placeId: String): Flow<List<VisitEntity>>

    @Query(
        """SELECT v.*, l.name AS locationName
           FROM visits v JOIN locations l ON l.placeId = v.placeId
           ORDER BY v.visitedAt DESC
           LIMIT :limit"""
    )
    fun observeRecentWithLocation(limit: Int): Flow<List<RecentVisit>>
}
