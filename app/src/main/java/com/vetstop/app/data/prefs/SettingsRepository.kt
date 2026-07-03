package com.vetstop.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val visitorName: String,
    val corridorMiles: Float,
    val lastSyncAt: Long?,
    val lastSyncSummary: String?,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val VISITOR_NAME = stringPreferencesKey("visitor_name")
        val CORRIDOR_MILES = floatPreferencesKey("corridor_miles")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LAST_SYNC_SUMMARY = stringPreferencesKey("last_sync_summary")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            visitorName = prefs[Keys.VISITOR_NAME] ?: "",
            corridorMiles = prefs[Keys.CORRIDOR_MILES] ?: DEFAULT_CORRIDOR_MILES,
            lastSyncAt = prefs[Keys.LAST_SYNC_AT],
            lastSyncSummary = prefs[Keys.LAST_SYNC_SUMMARY],
        )
    }

    suspend fun setVisitorName(name: String) {
        context.dataStore.edit { it[Keys.VISITOR_NAME] = name }
    }

    suspend fun setCorridorMiles(miles: Float) {
        context.dataStore.edit { it[Keys.CORRIDOR_MILES] = miles }
    }

    suspend fun recordSyncResult(timestamp: Long, summary: String) {
        context.dataStore.edit {
            it[Keys.LAST_SYNC_AT] = timestamp
            it[Keys.LAST_SYNC_SUMMARY] = summary
        }
    }

    companion object {
        const val DEFAULT_CORRIDOR_MILES = 2f
    }
}
