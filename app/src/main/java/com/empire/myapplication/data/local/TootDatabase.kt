package com.empire.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatSession::class,
        Message::class,
        SourceRef::class,
        UserProfile::class,
        MemoryProfile::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TootDatabase : RoomDatabase() {
    abstract fun tootDao(): TootDao
}
