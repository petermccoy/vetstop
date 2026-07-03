package com.vetstop.app.data.remote

import com.vetstop.app.data.remote.dto.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Directions API — https://maps.googleapis.com/maps/api/directions
 * https://developers.google.com/maps/documentation/directions/get-directions
 */
interface DirectionsApi {

    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String,
    ): DirectionsResponse

    companion object {
        const val BASE_URL = "https://maps.googleapis.com/"
    }
}
