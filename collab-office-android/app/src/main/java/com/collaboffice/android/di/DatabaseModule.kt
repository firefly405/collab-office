package com.collaboffice.android.di

import android.content.Context
import androidx.room.Room
import com.collaboffice.core.database.CollabDatabase
import com.collaboffice.core.database.dao.*
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
    fun provideDatabase(@ApplicationContext context: Context): CollabDatabase =
        Room.databaseBuilder(
            context,
            CollabDatabase::class.java,
            "collab_office.db"
        ).fallbackToDestructiveMigration()
         .build()

    @Provides
    fun provideChatSessionDao(db: CollabDatabase) = db.chatSessionDao()

    @Provides
    fun provideChatMessageDao(db: CollabDatabase) = db.chatMessageDao()

    @Provides
    fun provideOfflineOpDao(db: CollabDatabase) = db.offlineOpDao()
}
