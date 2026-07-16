package com.empire.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatSession::class,
        Message::class,
        SourceRef::class,
        UserProfile::class,
        UserMemory::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TootDatabase : RoomDatabase() {
    abstract fun tootDao(): TootDao
}
