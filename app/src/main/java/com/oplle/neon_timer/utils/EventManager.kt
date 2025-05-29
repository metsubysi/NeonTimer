package com.oplle.neon_timer.utils

import com.oplle.neon_timer.model.Event
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EventManager(private val context: Context) {
    private val events = mutableListOf<Event>()
    private val prefs = context.getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        loadEvents()
    }

    fun addEvent(event: Event): Boolean {
        events.add(event)
        saveEvents()
        return true
    }

    fun getEvents(): List<Event> = events

    private fun saveEvents() {
        val json = gson.toJson(events)
        prefs.edit().putString("events_json", json).apply()
    }

    private fun loadEvents() {
        val json = prefs.getString("events_json", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<Event>>() {}.type
            val loadedEvents: List<Event> = gson.fromJson(json, type)
            events.clear()
            events.addAll(loadedEvents)
        }
    }

    fun deleteEvent(event: Event?): Boolean {
        val removed = events.removeIf { it.id == event?.id } // Удаляем по уникальному id
        if (removed) {
            saveEvents() // Сохраняем изменения после удаления

        }
        return removed
    }
}