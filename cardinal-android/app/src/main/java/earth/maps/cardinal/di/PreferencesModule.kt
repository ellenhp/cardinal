package earth.maps.cardinal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import earth.maps.cardinal.data.AppPreferences
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.ViewportPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideViewportPreferences(@ApplicationContext context: Context): ViewportPreferences {
        return ViewportPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideAppPreferenceRepository(@ApplicationContext context: Context): AppPreferenceRepository {
        return AppPreferenceRepository(context)
    }
}
