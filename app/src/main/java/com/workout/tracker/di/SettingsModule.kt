package com.workout.tracker.di

import com.workout.tracker.data.settings.SharedPrefsAppSettings
import com.workout.tracker.domain.settings.AppSettings
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindAppSettings(impl: SharedPrefsAppSettings): AppSettings
}
