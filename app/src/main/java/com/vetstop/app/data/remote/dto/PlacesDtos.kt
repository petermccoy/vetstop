package com.vetstop.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// --- Places API (New) places:searchText request ---

data class SearchTextRequest(
    @SerializedName("textQuery") val textQuery: String,
    @SerializedName("locationRestriction") val locationRestriction: LocationRestriction,
    @SerializedName("pageSize") val pageSize: Int = 20,
    @SerializedName("pageToken") val pageToken: String? = null,
)

data class LocationRestriction(
    @SerializedName("rectangle") val rectangle: Rectangle,
)

data class Rectangle(
    @SerializedName("low") val low: LatLngDto,
    @SerializedName("high") val high: LatLngDto,
)

data class LatLngDto(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
)

// --- Places API (New) places:searchText response ---

data class SearchTextResponse(
    @SerializedName("places") val places: List<PlaceDto>?,
    @SerializedName("nextPageToken") val nextPageToken: String?,
)

data class PlaceDto(
    @SerializedName("id") val id: String?,
    @SerializedName("displayName") val displayName: LocalizedTextDto?,
    @SerializedName("formattedAddress") val formattedAddress: String?,
    @SerializedName("location") val location: LatLngDto?,
    @SerializedName("businessStatus") val businessStatus: String?,
)

data class LocalizedTextDto(
    @SerializedName("text") val text: String?,
)
