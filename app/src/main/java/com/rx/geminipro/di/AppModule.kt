package com.rx.geminipro.di

import android.content.Context
import com.rx.geminipro.data.UserPreferencesRepository
import com.rx.geminipro.utils.services.GoogleServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideGoogleServices(): GoogleServices {
        return GoogleServices()
    }

}