package earth.maps.cardinal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import earth.maps.cardinal.data.ContrastPreferences
import earth.maps.cardinal.data.ContrastRepository
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
    fun provideContrastPreferences(@ApplicationContext context: Context): ContrastPreferences {
        return ContrastPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideContrastRepository(@ApplicationContext context: Context): ContrastRepository {
        return ContrastRepository(context)
    }
}
