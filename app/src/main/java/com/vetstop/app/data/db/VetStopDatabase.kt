package com.vetstop.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SearchAreaEntity::class,
        LocationEntity::class,
        VisitEntity::class,
        PendingVisitEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class VetStopDatabase : RoomDatabase() {
    abstract fun searchAreaDao(): SearchAreaDao
    abstract fun locationDao(): LocationDao
    abstract fun visitDao(): VisitDao
    abstract fun pendingVisitDao(): PendingVisitDao

    companion object {
        /** v2 adds the pending_visits table (stops queued from launched trips). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_visits` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`placeId` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`placeId`) REFERENCES `locations`(`placeId`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_visits_placeId` " +
                        "ON `pending_visits` (`placeId`)"
                )
            }
        }
    }
}
