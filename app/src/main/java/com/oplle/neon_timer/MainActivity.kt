package com.oplle.neon_timer

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.oplle.neon_timer.model.Event
import com.oplle.neon_timer.utils.EventAdapter
import com.oplle.neon_timer.utils.EventDetailDialogFragment
import com.oplle.neon_timer.utils.EventManager
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() , EventDetailDialogFragment.EventDialogListener{
    private lateinit var eventManager: EventManager
    private lateinit var noEventsText: TextView
    private lateinit var eventList: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private var mInterstitialAd: InterstitialAd? = null
    private var eventCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MobileAds.initialize(this) {}
        loadInterstitialAd()
        // Отключаем автоматическую установку отступов для системных окон
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Получаем контроллер для управления системными панелями
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                // Скрываем статус-бар и навигационную панель
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())

                // Устанавливаем поведение: панели появляются при свайпе
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        // Делаем статус-бар прозрачным
        window.statusBarColor = Color.TRANSPARENT

        // Убираем ActionBar, если он есть
        supportActionBar?.hide()

        eventManager = EventManager(this)
        noEventsText = findViewById(R.id.no_events_text)
        eventList = findViewById(R.id.event_list)
        val addEventButton: Button = findViewById(R.id.add_event_button)

        eventAdapter = EventAdapter { event ->
            val dialog = EventDetailDialogFragment.newInstance(event)
            dialog.setListener(this) // 👈 передаём слушателя
            dialog.show(supportFragmentManager, "EventDetailDialog")
        }
        eventList.layoutManager = LinearLayoutManager(this)
        eventList.adapter = eventAdapter

        addEventButton.setOnClickListener {
            showDateTimePicker()
        }


        updateUI()
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this, R.style.NeonDatePickerDialogTheme,
            { _, year, month, dayOfMonth ->
                showCustomNeonTimePicker(this , year, month, dayOfMonth) { hour, minute ->
                    // Используй выбранное время
                   // Toast.makeText(this, "Вы выбрали $hour:$minute", Toast.LENGTH_SHORT).show()
                    showEventTypeDialog(year, month, dayOfMonth, hour, minute)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }



    private fun showEventTypeDialog(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_event_type, null)
        val input = dialogView.findViewById<AutoCompleteTextView>(R.id.event_type_input)
        val okButton = dialogView.findViewById<Button>(R.id.ok_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)

        val eventTypes = getString(R.string.default_event_types)
            .split(",")
            .map { it.trim() }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, eventTypes)
        input.setAdapter(adapter)
        input.threshold = 1

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        okButton.setOnClickListener {
            val selectedEvent = input.text.toString().trim()
            if (selectedEvent.isNotEmpty()) {
                addEvent(selectedEvent, year, month, day, hour, minute)
            }
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }



    private fun addEvent(type: String, year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        val event = Event.create(type, year, month, day, hour, minute)
        if (eventManager.addEvent(event)) {
            eventAdapter.setEvents(eventManager.getEvents()) // ✅ Полная перезагрузка списка
            updateUI()
            eventCounter++
            if (eventCounter % 2 == 0 && mInterstitialAd != null) {
                mInterstitialAd?.show(this)
            }
        }
    }

    private fun updateUI() {
        val updatedEvents = eventManager.getEvents()
        if (updatedEvents.isEmpty()) {
            noEventsText.visibility = TextView.VISIBLE
            eventList.visibility = RecyclerView.GONE
        } else {
            noEventsText.visibility = TextView.GONE
            eventList.visibility = RecyclerView.VISIBLE

            eventAdapter = EventAdapter { event ->
                val dialog = EventDetailDialogFragment.newInstance(event)
                dialog.setListener(this)
                dialog.show(supportFragmentManager, "EventDetailDialog")
            }
            eventAdapter.setEvents(updatedEvents)
            eventList.adapter = eventAdapter
        }
    }
    fun showCustomNeonTimePicker(
        context: Context,
        year: Int,
        month: Int,
        day: Int,
        onTimeSelected: (hour: Int, minute: Int) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.neon_time_picker, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val hourPicker = dialogView.findViewById<NumberPicker>(R.id.hourPicker)
        val minutePicker = dialogView.findViewById<NumberPicker>(R.id.minutePicker)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        // Настройка границ
        hourPicker.minValue = 0
        hourPicker.maxValue = 23
        minutePicker.minValue = 0
        minutePicker.maxValue = 59

        // Установка текущего времени
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        hourPicker.value = currentHour
        minutePicker.value = currentMinute

        btnOk.setOnClickListener {
            onTimeSelected(hourPicker.value, minutePicker.value)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    fun deleteEvent(event: Event?) {
        val deleted = eventManager.deleteEvent(event)
        if (deleted) {
            // Обнови список в адаптере,
            eventAdapter.setEvents(eventManager.getEvents())
        }
    }

    override fun onEventDeleted(event: Event?) {
        deleteEvent(event)
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,
            "ca-app-pub-3940256099942544/1033173712", // ✅ Тестовый ID, замени на свой в продакшене!
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    mInterstitialAd = ad
                    mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            // Загружаем новую после закрытия
                            loadInterstitialAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            mInterstitialAd = null
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                }
            })
    }
}