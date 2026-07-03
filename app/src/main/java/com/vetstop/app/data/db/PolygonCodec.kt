package com.vetstop.app.data.db

import com.google.android.gms.maps.model.LatLng

/** Encodes polygon vertices to/from the "lat,lng;lat,lng" string stored in Room. */
object PolygonCodec {

    fun encode(points: List<LatLng>): String =
        points.joinToString(";") { "${it.latitude},${it.longitude}" }

    fun decode(encoded: String): List<LatLng> =
        encoded.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { pair ->
                val parts = pair.split(",")
                if (parts.size != 2) return@mapNotNull null
                val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                val lng = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                LatLng(lat, lng)
            }
}
