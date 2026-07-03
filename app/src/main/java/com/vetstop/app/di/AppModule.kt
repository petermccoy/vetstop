package com.vetstop.app.di

import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.vetstop.app.data.db.LocationDao
import com.vetstop.app.data.db.SearchAreaDao
import com.vetstop.app.data.db.VetStopDatabase
import com.vetstop.app.data.db.VisitDao
import com.vetstop.app.data.remote.DirectionsApi
import com.vetstop.app.data.remote.PlacesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VetStopDatabase =
        Room.databaseBuilder(context, VetStopDatabase::class.java, "vetstop.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSearchAreaDao(db: VetStopDatabase): SearchAreaDao = db.searchAreaDao()

    @Provides
    fun provideLocationDao(db: VetStopDatabase): LocationDao = db.locationDao()

    @Provides
    fun provideVisitDao(db: VetStopDatabase): VisitDao = db.visitDao()

    @Provides
    @Singleton
    fun provideFusedLocationClient(
        @ApplicationContext context: Context,
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun providePlacesApi(client: OkHttpClient): PlacesApi =
        Retrofit.Builder()
            .baseUrl(PlacesApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlacesApi::class.java)

    @Provides
    @Singleton
    fun provideDirectionsApi(client: OkHttpClient): DirectionsApi =
        Retrofit.Builder()
            .baseUrl(DirectionsApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DirectionsApi::class.java)
}
