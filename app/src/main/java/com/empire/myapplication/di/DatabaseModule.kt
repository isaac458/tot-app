package com.empire.myapplication.di

import android.content.Context
import androidx.room.Room
import com.empire.myapplication.data.local.TootDao
import com.empire.myapplication.data.local.TootDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TootDatabase {
        return Room.databaseBuilder(
            context,
            TootDatabase::class.java,
            "toot_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTootDao(database: TootDatabase): TootDao {
        return database.tootDao()
    }
}
