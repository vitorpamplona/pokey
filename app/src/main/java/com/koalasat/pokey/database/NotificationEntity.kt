package com.koalasat.pokey.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification",
    indices = [
        Index(
            value = ["eventId"],
            name = "notification_by_eventId",
        )
    ],
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val eventId: String,
    val kind: Int,
    val time: Long,
)
