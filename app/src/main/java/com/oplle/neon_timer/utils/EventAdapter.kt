package com.oplle.neon_timer.utils

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.oplle.neon_timer.R
import com.oplle.neon_timer.model.Event
import java.util.*
import java.util.concurrent.TimeUnit

class EventAdapter(
    private val onItemClick: (Event?) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private var events = mutableListOf<Event>()

    fun addEvent(event: Event) {
        events.add(event)
        notifyItemInserted(events.size - 1)
    }

    fun setEvents(events: List<Event>) {
        this.events = events.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    class EventViewHolder(
        itemView: View,
        private val onItemClick: (Event) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val eventTypeTextView: TextView = itemView.findViewById(R.id.event_type)
        private val eventDateTextView: TextView = itemView.findViewById(R.id.event_date)
        private val passedLabelTextView: TextView = itemView.findViewById(R.id.passed_label)
        private val passedTimeTextView: TextView = itemView.findViewById(R.id.passed_time)

        private val handler = Handler(Looper.getMainLooper())
        private var running = true

        fun bind(event: Event?) {
            val context = itemView.context
            val calendar = Calendar.getInstance()

            if (event != null) {
                calendar.set(event.year, event.month, event.day, event.hour, event.minute, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            } else {
                calendar.set(0, 0, 0, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            fun updateTime() {
                if (!running) return

                val now = System.currentTimeMillis()
                val eventTime = calendar.timeInMillis
                val diff = now - eventTime

                val years = TimeUnit.MILLISECONDS.toDays(diff) / 365
                val months = (TimeUnit.MILLISECONDS.toDays(diff) % 365) / 30
                val days = (TimeUnit.MILLISECONDS.toDays(diff) % 365) % 30
                val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60

                val yearLabel = context.getString(R.string.year)
                val monthLabel = context.getString(R.string.month)
                val dayLabel = context.getString(R.string.day)
                val hourLabel = context.getString(R.string.hour)
                val minuteLabel = context.getString(R.string.minute)

                val timeParts = mutableListOf<String>()
                if (years > 0) timeParts.add("$years $yearLabel")
                if (months > 0) timeParts.add("$months $monthLabel")
                if (days > 0) timeParts.add("$days $dayLabel")
                if (hours > 0) timeParts.add("$hours $hourLabel")
                if (minutes > 0) timeParts.add("$minutes $minuteLabel")

                if (event != null) {
                    eventTypeTextView.text = "${event.type}:"
                    eventDateTextView.text = "%02d/%02d/%04d %02d:%02d".format(
                        event.day, event.month + 1, event.year, event.hour, event.minute
                    )
                    passedLabelTextView.text = context.getString(R.string.passed) + ":"
                    passedTimeTextView.text = timeParts.joinToString(", ")
                } else {
                    eventTypeTextView.text = "No event"
                    eventDateTextView.text = ""
                    passedLabelTextView.text = ""
                    passedTimeTextView.text = ""
                }

                handler.postDelayed({ updateTime() }, 60000)
            }

            updateTime()

            itemView.setOnClickListener {
                if (event != null) {
                    onItemClick(event)
                }
            }
        }

        fun stopUpdates() {
            running = false
            handler.removeCallbacksAndMessages(null)
        }
    }

    fun removeEvent(event: Event) {
        val index = events.indexOfFirst { it.id == event.id }
        if (index != -1) {
            events.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}