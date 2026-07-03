package com.vetstop.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchAreaDao {

    @Insert
    suspend fun insert(area: SearchAreaEntity): Long

    @Update
    suspend fun update(area: SearchAreaEntity)

    @Delete
    suspend fun delete(area: SearchAreaEntity)

    @Query("SELECT * FROM search_areas ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SearchAreaEntity>>

    @Query("SELECT * FROM search_areas WHERE isActive = 1")
    suspend fun getActive(): List<SearchAreaEntity>

    @Query("SELECT * FROM search_areas WHERE id = :id")
    suspend fun getById(id: Long): SearchAreaEntity?
}
