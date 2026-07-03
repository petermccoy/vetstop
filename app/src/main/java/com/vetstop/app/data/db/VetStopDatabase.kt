package com.vetstop.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SearchAreaEntity::class, LocationEntity::class, VisitEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class VetStopDatabase : RoomDatabase() {
    abstract fun searchAreaDao(): SearchAreaDao
    abstract fun locationDao(): LocationDao
    abstract fun visitDao(): VisitDao
}
