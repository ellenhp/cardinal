package earth.maps.cardinal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import earth.maps.cardinal.data.OfflineAreaDao
import earth.maps.cardinal.data.OfflineAreaRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideOfflineAreaRepository(offlineAreaDao: OfflineAreaDao): OfflineAreaRepository {
        return OfflineAreaRepository(offlineAreaDao)
    }
}
