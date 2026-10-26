package com.example.moneymanager.data.local

import androidx.room.TypeConverter
import com.google.firebase.Timestamp

/**
 * Room TypeConverters for non-primitive types.
 * Converts Firebase Timestamp <-> Long (epoch milliseconds).
 *
 * Security note: All values are stored as plain Long to avoid serialization
 * vulnerabilities from storing complex objects as blobs.
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Timestamp? {
        return value?.let {
            // Firebase Timestamp uses seconds + nanoseconds; store millis and reconstruct cleanly
            Timestamp(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }
    }

    @TypeConverter
    fun timestampToLong(timestamp: Timestamp?): Long? {
        return timestamp?.let {
            // Combine seconds and nanos into a single millisecond Long
            it.seconds * 1000 + it.nanoseconds / 1_000_000
        }
    }
}
