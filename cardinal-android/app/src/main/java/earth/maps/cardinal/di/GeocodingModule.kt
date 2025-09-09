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

    @Provides
    @Singleton
    fun provideGeocodingService(@ApplicationContext context: Context): GeocodingService {
        return OfflineGeocodingService(context)
    }

    @Provides
    @Singleton
    fun provideTileProcessor(@ApplicationContext context: Context): TileProcessor {
        return OfflineGeocodingService(context)
    }
}
