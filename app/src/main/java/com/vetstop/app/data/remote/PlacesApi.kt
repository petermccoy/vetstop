package com.vetstop.app.data.remote

import com.vetstop.app.data.remote.dto.SearchTextRequest
import com.vetstop.app.data.remote.dto.SearchTextResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Places API (New) — https://places.googleapis.com
 * Text Search: https://developers.google.com/maps/documentation/places/web-service/text-search
 */
interface PlacesApi {

    @Headers("Content-Type: application/json")
    @POST("v1/places:searchText")
    suspend fun searchText(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String = FIELD_MASK,
        @Body body: SearchTextRequest,
    ): SearchTextResponse

    companion object {
        const val BASE_URL = "https://places.googleapis.com/"
        const val FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location,places.businessStatus,nextPageToken"
    }
}
