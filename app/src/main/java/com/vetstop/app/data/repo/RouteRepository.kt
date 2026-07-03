package com.vetstop.app.data.repo

import com.google.android.gms.maps.model.LatLng
import com.vetstop.app.BuildConfig
import com.vetstop.app.data.db.LocationWithLastVisit
import com.vetstop.app.data.remote.DirectionsApi
import com.vetstop.app.domain.geo.GeoUtils
import com.vetstop.app.domain.geo.PolylineDecoder
import javax.inject.Inject
import javax.inject.Singleton

data class PlannedRoute(
    val originLabel: String,
    val destinationLabel: String,
    val originLatLng: LatLng?,
    val destinationLatLng: LatLng?,
    val polyline: List<LatLng>,
    val distanceText: String,
    val durationText: String,
)

/** A saved location plus its distance (meters) from the planned route. */
data class LocationAlongRoute(
    val location: LocationWithLastVisit,
    val distanceFromRouteMeters: Double,
)

@Singleton
class RouteRepository @Inject constructor(
    private val directionsApi: DirectionsApi,
) {

    /**
     * Computes a driving route between two free-form origin/destination
     * strings (addresses, place names, or "lat,lng").
     */
    suspend fun getRoute(origin: String, destination: String): Result<PlannedRoute> {
        return try {
            val response = directionsApi.getDirections(
                origin = origin,
                destination = destination,
                apiKey = BuildConfig.MAPS_API_KEY,
            )
            val route = response.routes?.firstOrNull()
            if (response.status != "OK" || route == null) {
                return Result.failure(
                    IllegalStateException(
                        response.errorMessage ?: "Directions failed: ${response.status}"
                    )
                )
            }
            val encoded = route.overviewPolyline?.points
                ?: return Result.failure(IllegalStateException("Route has no polyline"))
            val firstLeg = route.legs?.firstOrNull()
            val lastLeg = route.legs?.lastOrNull()
            val distanceMeters = route.legs.orEmpty().sumOf { it.distance?.value ?: 0L }
            val durationSeconds = route.legs.orEmpty().sumOf { it.duration?.value ?: 0L }
            Result.success(
                PlannedRoute(
                    originLabel = firstLeg?.startAddress ?: origin,
                    destinationLabel = lastLeg?.endAddress ?: destination,
                    originLatLng = firstLeg?.startLocation?.let { LatLng(it.lat, it.lng) },
                    destinationLatLng = lastLeg?.endLocation?.let { LatLng(it.lat, it.lng) },
                    polyline = PolylineDecoder.decode(encoded),
                    distanceText = formatDistance(distanceMeters),
                    durationText = formatDuration(durationSeconds),
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Filters [locations] to those within [maxDistanceMeters] of the route
     * polyline, sorted by their position along the route so stops appear in
     * driving order.
     */
    fun locationsAlongRoute(
        route: PlannedRoute,
        locations: List<LocationWithLastVisit>,
        maxDistanceMeters: Double,
    ): List<LocationAlongRoute> {
        if (route.polyline.isEmpty()) return emptyList()
        return locations.mapNotNull { location ->
            val point = LatLng(location.latitude, location.longitude)
            val distance = GeoUtils.minDistanceToPolylineMeters(point, route.polyline)
            if (distance <= maxDistanceMeters) {
                LocationAlongRoute(location, distance)
            } else {
                null
            }
        }.sortedBy { candidate ->
            nearestPolylineIndex(
                LatLng(candidate.location.latitude, candidate.location.longitude),
                route.polyline,
            )
        }
    }

    private fun nearestPolylineIndex(point: LatLng, polyline: List<LatLng>): Int {
        var bestIndex = 0
        var bestDistance = Double.MAX_VALUE
        for (i in polyline.indices) {
            val d = GeoUtils.haversineMeters(point, polyline[i])
            if (d < bestDistance) {
                bestDistance = d
                bestIndex = i
            }
        }
        return bestIndex
    }

    private fun formatDistance(meters: Long): String {
        val miles = meters / GeoUtils.METERS_PER_MILE
        return String.format(java.util.Locale.US, "%.1f mi", miles)
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
