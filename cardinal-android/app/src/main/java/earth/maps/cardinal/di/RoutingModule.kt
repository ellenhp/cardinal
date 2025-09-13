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
