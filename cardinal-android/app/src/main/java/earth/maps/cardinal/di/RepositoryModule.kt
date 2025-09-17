package earth.maps.cardinal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import earth.maps.cardinal.data.DownloadedTileDao
import earth.maps.cardinal.data.OfflineAreaDao
import earth.maps.cardinal.data.OfflineAreaRepository
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
