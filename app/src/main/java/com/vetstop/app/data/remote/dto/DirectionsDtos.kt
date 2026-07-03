package com.vetstop.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// --- Directions API response (subset) ---

data class DirectionsResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("error_message") val errorMessage: String?,
    @SerializedName("routes") val routes: List<RouteDto>?,
)

data class RouteDto(
    @SerializedName("overview_polyline") val overviewPolyline: PolylineDto?,
    @SerializedName("legs") val legs: List<LegDto>?,
)

data class PolylineDto(
    @SerializedName("points") val points: String?,
)

data class LegDto(
    @SerializedName("start_address") val startAddress: String?,
    @SerializedName("end_address") val endAddress: String?,
    @SerializedName("start_location") val startLocation: CoordDto?,
    @SerializedName("end_location") val endLocation: CoordDto?,
    @SerializedName("distance") val distance: TextValueDto?,
    @SerializedName("duration") val duration: TextValueDto?,
)

data class CoordDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
)

data class TextValueDto(
    @SerializedName("text") val text: String?,
    @SerializedName("value") val value: Long?,
)
