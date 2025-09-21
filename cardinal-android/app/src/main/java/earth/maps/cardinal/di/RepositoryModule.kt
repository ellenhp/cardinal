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
import earth.maps.cardinal.data.room.DownloadedTileDao
import earth.maps.cardinal.data.room.OfflineAreaDao
import earth.maps.cardinal.data.room.OfflineAreaRepository
import earth.maps.cardinal.geocoding.TileProcessor
import earth.maps.cardinal.tileserver.TileDownloadManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideOfflineAreaRepository(offlineAreaDao: OfflineAreaDao): OfflineAreaRepository {
        return OfflineAreaRepository(offlineAreaDao)
    }

    @Provides
    @Singleton
    fun provideTileDownloadManager(
        @ApplicationContext context: Context,
        downloadedTileDao: DownloadedTileDao,
        offlineAreaDao: OfflineAreaDao,
        tileProcessor: TileProcessor
    ): TileDownloadManager {
        return TileDownloadManager(context, downloadedTileDao, offlineAreaDao, tileProcessor)
    }
}
