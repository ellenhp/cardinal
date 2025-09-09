package earth.maps.cardinal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import earth.maps.cardinal.geocoding.TileProcessor
import earth.maps.cardinal.tileserver.TileDownloadService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TileDownloadModule {

    @Provides
    @Singleton
    fun provideTileDownloadService(
        @ApplicationContext context: Context,
        tileProcessor: TileProcessor
    ): TileDownloadService {
        return TileDownloadService(context, tileProcessor)
    }
}
