package com.oplle.neon_timer.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Calendar
import java.util.UUID

@Parcelize
data class Event(
    val id: String,
    val type: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val timestamp: Long // Время в миллисекундах — остаётся, удобно для сортировки/фильтрации
) : Parcelable {
    companion object {
        fun create(type: String, year: Int, month: Int, day: Int, hour: Int, minute: Int): Event {
            val now = Calendar.getInstance()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1) // Месяцы в Calendar начинаются с 0
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, now.get(Calendar.SECOND))
                set(Calendar.MILLISECOND, 0) // Можно обнулить, раз миллисекунды не нужны
            }

            return Event(
                id = UUID.randomUUID().toString(),
                type = type,
                year = year,
                month = month,
                day = day,
                hour = hour,
                minute = minute,
                second = now.get(Calendar.SECOND),
                timestamp = calendar.timeInMillis
            )
        }
    }
}