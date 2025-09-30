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
