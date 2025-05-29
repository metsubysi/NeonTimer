package com.oplle.neon_timer.utils

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Looper.*
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.oplle.neon_timer.R
import com.oplle.neon_timer.model.Event
import java.util.Calendar
import java.util.concurrent.TimeUnit
import android.os.Handler
import android.os.Looper
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import kotlin.time.Duration


class EventDetailDialogFragment : DialogFragment() {

    interface EventDialogListener {
        fun onEventDeleted(event: Event?)
    }

    private var event: Event? = null
    private lateinit var handler: Handler
    private var running = true
    private lateinit var timeTextView: TextView
    private lateinit var titleTextView: TextView
    private var listener: EventDialogListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        event = arguments?.getParcelable("event")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_event_detail, null)
        timeTextView = view.findViewById(R.id.timeTextView)
        titleTextView = view.findViewById(R.id.titleTextView)
        val backButton = view.findViewById<Button>(R.id.backButton)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)

        event?.let { e ->
            titleTextView.text = "${e.type}: ${String.format("%02d/%02d/%04d %02d:%02d", e.day, e.month+1, e.year, e.hour, e.minute)}"
            startTimer(e)
        }

        backButton.setOnClickListener { dismiss() }

        deleteButton.setOnClickListener {
            showConfirmDeleteDialog()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // прозрачный фон

        return dialog
    }

    private fun startTimer(e: Event) {
        handler = android.os.Handler(getMainLooper())
        val calendar = Calendar.getInstance().apply {
            set(e.year, e.month, e.day, e.hour, e.minute, e.second)
        }
        val startTime = calendar.timeInMillis

        fun update() {
            if (!running) return

            val diff = System.currentTimeMillis() - startTime

            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
            val millis = diff % 1000

            timeTextView.text = getString(R.string.time_passed_format, hours, minutes, seconds, millis)
            handler.postDelayed({ update() }, 50)
        }

        update()
    }

    override fun onDestroyView() {
        running = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    companion object {
        fun newInstance(event: Event?): EventDetailDialogFragment {
            val fragment = EventDetailDialogFragment()
            val args = Bundle().apply {
                putParcelable("event", event)
            }
            fragment.arguments = args
            return fragment
        }
    }

    fun setListener(listener: EventDialogListener) {
        this.listener = listener
    }

    private fun showConfirmDeleteDialog() {
        val confirmView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(confirmView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // прозрачный фон

        val cancelBtn = confirmView.findViewById<Button>(R.id.confirmCancelButton)
        val deleteBtn = confirmView.findViewById<Button>(R.id.confirmDeleteButton)

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        deleteBtn.setOnClickListener {
            listener?.onEventDeleted(event)
            dialog.dismiss()
            dismiss()
        }

        dialog.show()
    }

}
