package com.vetstop.app.data.repo

import com.google.android.gms.maps.model.LatLng
import com.vetstop.app.BuildConfig
import com.vetstop.app.data.db.LocationDao
import com.vetstop.app.data.db.LocationEntity
import com.vetstop.app.data.db.PolygonCodec
import com.vetstop.app.data.db.SearchAreaDao
import com.vetstop.app.data.db.SearchAreaEntity
import com.vetstop.app.data.remote.PlacesApi
import com.vetstop.app.data.remote.dto.LatLngDto
import com.vetstop.app.data.remote.dto.LocationRestriction
import com.vetstop.app.data.remote.dto.Rectangle
import com.vetstop.app.data.remote.dto.SearchTextRequest
import com.vetstop.app.domain.geo.GeoUtils
import com.vetstop.app.domain.model.LocationCategory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

data class SyncResult(
    val areasSynced: Int,
    val added: Int,
    val removed: Int,
    val totalActive: Int,
)

/**
 * Searches every active user-defined area for vets, pet stores, and animal
 * shelters via the Places API, and reconciles the results into the local
 * database (new places inserted, vanished places marked inactive).
 */
@Singleton
class PlaceSyncRepository @Inject constructor(
    private val placesApi: PlacesApi,
    private val searchAreaDao: SearchAreaDao,
    private val locationDao: LocationDao,
) {

    suspend fun syncAllAreas(): SyncResult {
        val areas = searchAreaDao.getActive()
        var added = 0
        var removed = 0
        for (area in areas) {
            val result = syncArea(area)
            added += result.first
            removed += result.second
        }
        return SyncResult(
            areasSynced = areas.size,
            added = added,
            removed = removed,
            totalActive = locationDao.getAllActive().size,
        )
    }

    /** Returns (addedCount, removedCount) for one area. */
    suspend fun syncArea(area: SearchAreaEntity): Pair<Int, Int> {
        val polygon = PolygonCodec.decode(area.polygon)
        if (polygon.size < 3) return 0 to 0

        val bounds = GeoUtils.boundingBox(polygon)
        val restriction = LocationRestriction(
            rectangle = Rectangle(
                low = LatLngDto(bounds.southwest.latitude, bounds.southwest.longitude),
                high = LatLngDto(bounds.northeast.latitude, bounds.northeast.longitude),
            )
        )

        val now = System.currentTimeMillis()
        val before = locationDao.getActivePlaceIds(area.id).toSet()
        val fresh = mutableMapOf<String, LocationEntity>()

        for (category in LocationCategory.entries) {
            var pageToken: String? = null
            var pages = 0
            do {
                val response = placesApi.searchText(
                    apiKey = BuildConfig.MAPS_API_KEY,
                    body = SearchTextRequest(
                        textQuery = category.searchQuery,
                        locationRestriction = restriction,
                        pageToken = pageToken,
                    ),
                )
                for (place in response.places.orEmpty()) {
                    val id = place.id ?: continue
                    val location = place.location ?: continue
                    val point = LatLng(location.latitude, location.longitude)
                    // The API restricts to the bounding box; narrow to the
                    // actual polygon here.
                    if (!GeoUtils.pointInPolygon(point, polygon)) continue
                    if (place.businessStatus == "CLOSED_PERMANENTLY") continue
                    // A place can match several queries (e.g. a shelter with a
                    // vet clinic); first category wins, dedup by place ID.
                    fresh.getOrPut(id) {
                        LocationEntity(
                            placeId = id,
                            name = place.displayName?.text ?: "Unknown",
                            address = place.formattedAddress ?: "",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            category = category.name,
                            areaId = area.id,
                            firstSeenAt = now,
                            lastSeenAt = now,
                        )
                    }
                }
                pageToken = response.nextPageToken
                pages++
                if (pageToken != null) delay(250) // be polite between pages
            } while (pageToken != null && pages < MAX_PAGES_PER_QUERY)
        }

        locationDao.applySync(area.id, fresh.values.toList(), now)

        val addedCount = fresh.keys.count { it !in before }
        val removedCount = before.count { it !in fresh.keys }
        return addedCount to removedCount
    }

    private companion object {
        const val MAX_PAGES_PER_QUERY = 3 // 3 pages x 20 results = API max of 60
    }
}
