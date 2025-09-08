package earth.maps.cardinal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import earth.maps.cardinal.geocoding.GeocodingService
import earth.maps.cardinal.geocoding.GeocodingServiceFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeocodingModule {

    @Provides
    @Singleton
    fun provideGeocodingService(): GeocodingService {
        return GeocodingServiceFactory.createDefaultService()
    }
}
