package com.koalasat.pokey.database

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vitorpamplona.ammolite.relays.Relay
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {
    @Query("SELECT MAX(time) FROM notification")
    fun getLatestNotification(): Long?

    @Query("SELECT EXISTS (SELECT 1 FROM notification WHERE eventId = :eventId)")
    fun existsNotification(eventId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotification(notificationEntity: NotificationEntity): Long?

    @Query("SELECT * FROM relay")
    fun getRelays(): List<RelayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRelay(notificationEntity: RelayEntity): Long?

    @Query("DELETE FROM relay where kind = :kind")
    fun deleteRelaysByKind(kind: Int): Int

    @Query("SELECT MAX(createdAt) FROM relay WHERE kind = :kind")
    fun getLatestRelaysByKind(kind: Int): Long?
}
