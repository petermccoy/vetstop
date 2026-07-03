package com.vetstop.app.domain.geo

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    const val EARTH_RADIUS_METERS = 6_371_000.0
    const val METERS_PER_MILE = 1_609.344

    /** Great-circle distance between two points, in meters. */
    fun haversineMeters(a: LatLng, b: LatLng): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1 - h))
    }

    /**
     * Ray-casting point-in-polygon test on raw lat/lng coordinates. Accurate
     * enough for regional (city/county scale) polygons away from the poles
     * and the antimeridian.
     */
    fun pointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            val intersects = (pi.latitude > point.latitude) != (pj.latitude > point.latitude) &&
                point.longitude < (pj.longitude - pi.longitude) *
                (point.latitude - pi.latitude) / (pj.latitude - pi.latitude) + pi.longitude
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }

    /** Axis-aligned bounding box of a polygon. */
    fun boundingBox(polygon: List<LatLng>): LatLngBounds {
        require(polygon.isNotEmpty()) { "polygon must not be empty" }
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE
        for (p in polygon) {
            minLat = min(minLat, p.latitude)
            maxLat = max(maxLat, p.latitude)
            minLng = min(minLng, p.longitude)
            maxLng = max(maxLng, p.longitude)
        }
        return LatLngBounds(LatLng(minLat, minLng), LatLng(maxLat, maxLng))
    }

    /**
     * Distance in meters from [point] to the segment [a]-[b], using a local
     * equirectangular projection centered on the point. Good to well under 1%
     * error for segments up to a few tens of kilometers.
     */
    fun distanceToSegmentMeters(point: LatLng, a: LatLng, b: LatLng): Double {
        val cosLat = cos(Math.toRadians(point.latitude))
        // Project to a local flat plane (x = east meters, y = north meters).
        fun project(p: LatLng): Pair<Double, Double> {
            val x = Math.toRadians(p.longitude - point.longitude) * cosLat * EARTH_RADIUS_METERS
            val y = Math.toRadians(p.latitude - point.latitude) * EARTH_RADIUS_METERS
            return x to y
        }

        val (ax, ay) = project(a)
        val (bx, by) = project(b)
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        val t = if (lengthSquared == 0.0) {
            0.0
        } else {
            (((-ax) * dx + (-ay) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        }
        val cx = ax + t * dx
        val cy = ay + t * dy
        return sqrt(cx * cx + cy * cy)
    }

    /**
     * Minimum distance in meters from [point] to a polyline. [sampleStride]
     * lets callers skip segments for long routes as a performance trade-off.
     */
    fun minDistanceToPolylineMeters(
        point: LatLng,
        polyline: List<LatLng>,
        sampleStride: Int = 1,
    ): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        if (polyline.size == 1) return haversineMeters(point, polyline[0])
        var best = Double.MAX_VALUE
        var i = 0
        while (i < polyline.size - 1) {
            val next = min(i + sampleStride, polyline.size - 1)
            val d = distanceToSegmentMeters(point, polyline[i], polyline[next])
            if (d < best) best = d
            i = next
        }
        return best
    }
}
