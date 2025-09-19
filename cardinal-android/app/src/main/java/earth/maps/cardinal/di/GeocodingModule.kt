/*
 *    Copyright 2025 The Cardinal Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package earth.maps.cardinal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.geocoding.GeocodingService
import earth.maps.cardinal.geocoding.MultiplexedGeocodingService
import earth.maps.cardinal.geocoding.OfflineGeocodingService
import earth.maps.cardinal.geocoding.PeliasGeocodingService
import earth.maps.cardinal.geocoding.TileProcessor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeocodingModule {

    var geocodingService: OfflineGeocodingService? = null

    @Provides
    @Singleton
    fun provideGeocodingService(
        appPreferenceRepository: AppPreferenceRepository,
        @ApplicationContext context: Context
    ): GeocodingService {
        val onlineService = providePeliasGeocodingService(appPreferenceRepository)
        val offlineService = provideOfflineGeocodingService(context)
        return MultiplexedGeocodingService(
            appPreferenceRepository,
            onlineService,
            offlineService
        )
    }

    @Provides
    @Singleton
    fun providePeliasGeocodingService(appPreferenceRepository: AppPreferenceRepository): PeliasGeocodingService {
        return PeliasGeocodingService(appPreferenceRepository)
    }

    @Provides
    @Singleton
    fun provideOfflineGeocodingService(@ApplicationContext context: Context): OfflineGeocodingService {
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
        return provideOfflineGeocodingService(context)
    }
}
