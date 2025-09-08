package earth.maps.cardinal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
}
