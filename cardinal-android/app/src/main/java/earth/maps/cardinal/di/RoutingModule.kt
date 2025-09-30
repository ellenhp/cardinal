/*
 *     Cardinal Maps
 *     Copyright (C) 2025 Cardinal Maps Authors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package earth.maps.cardinal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.routing.MultiplexedRoutingService
import earth.maps.cardinal.routing.OfflineRoutingService
import earth.maps.cardinal.routing.RoutingService
import earth.maps.cardinal.routing.ValhallaRoutingService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoutingModule {

    @Provides
    @Singleton
    fun provideRoutingService(
        appPreferenceRepository: AppPreferenceRepository,
        valhallaRoutingService: ValhallaRoutingService,
        offlineRoutingService: OfflineRoutingService
    ): RoutingService {
        return MultiplexedRoutingService(
            appPreferenceRepository,
            valhallaRoutingService,
            offlineRoutingService
        )
    }

    @Provides
    @Singleton
    fun provideValhallaRoutingService(appPreferenceRepository: AppPreferenceRepository): ValhallaRoutingService {
        return ValhallaRoutingService(appPreferenceRepository)
    }

    @Provides
    @Singleton
    fun provideOfflineRoutingService(@ApplicationContext context: Context): OfflineRoutingService {
        return OfflineRoutingService(context)
    }
}
