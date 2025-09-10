package earth.maps.cardinal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import earth.maps.cardinal.geocoding.GeocodingService
import earth.maps.cardinal.geocoding.OfflineGeocodingService
import earth.maps.cardinal.geocoding.TileProcessor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeocodingModule {

    var geocodingService: OfflineGeocodingService? = null

    @Provides
    @Singleton
    fun provideGeocodingService(@ApplicationContext context: Context): GeocodingService {
        val globalGeocodingService = geocodingService
        if (globalGeocodingService != null) {
            return globalGeocodingService
        } else {
            val newGeocoder = OfflineGeocodingService(context)
            geocodingService = newGeocoder
            return newGeocoder
        }
    }

    @Provides
    @Singleton
    fun provideTileProcessor(@ApplicationContext context: Context): TileProcessor {
        val globalGeocodingService = geocodingService
        if (globalGeocodingService != null) {
            return globalGeocodingService
        } else {
            val newGeocoder = OfflineGeocodingService(context)
            geocodingService = newGeocoder
            return newGeocoder
        }
    }
}
