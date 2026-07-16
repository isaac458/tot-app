package com.empire.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val ownerId: String = "guest",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user" or "model"
    val content: String,
    val imageUri: String? = null,
    val hasSources: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity
data class SourceRef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val title: String,
    val ownerId: String = "guest",
    val url: String
)

@Entity
data class UserProfile(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val age: Int,
    val provider: String, // "google", "facebook", "guest"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity
data class UserMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String
)
