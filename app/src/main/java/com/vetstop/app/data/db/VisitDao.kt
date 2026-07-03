package com.vetstop.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Insert
    suspend fun insert(visit: VisitEntity): Long

    @Delete
    suspend fun delete(visit: VisitEntity)

    @Query("SELECT * FROM visits WHERE placeId = :placeId ORDER BY visitedAt DESC")
    fun observeForPlace(placeId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits ORDER BY visitedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<VisitEntity>>
}
