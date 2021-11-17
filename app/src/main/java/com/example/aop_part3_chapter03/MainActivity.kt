package com.example.aop_part3_chapter03

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.example.aop_part3_chapter03.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        // step0 뷰를 초기화해주기
        initOnOffButton()
        initChangeAlarmTimeButton()

        val model = fetchDataFromSharedPreferences()
        renderView(model)

        // step1 데이터 가져오기

        // step2 뷰에 데이터 그려주기

    }

    private fun initOnOffButton() {
        binding.onOffBtn.setOnClickListener {
            // 데이터를 확인을 한다.
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            val newModel = saveAlarmModel(model.hour, model.minute, !model.onOff)
            renderView(newModel)

            // 온오프 에 따라 작업을 처리한다.
            if (newModel.onOff) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)

                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                cancelAlarm()
            }

        }
    }

    private fun initChangeAlarmTimeButton() {
        binding.changeAlarmTimeBtn.setOnClickListener {

            // 현재시간을 가져온다.
            val now = Calendar.getInstance()

            // TimePickerDialog 띄워줘서 시간 설정을 하도록 하게끔 하고, 그 시간을 가져와서
            TimePickerDialog(
                this,
                { picker, hour, minute ->

                    val model = saveAlarmModel(hour, minute, false)
                    renderView(model)

                    // 기존에 있던 알람을 삭제한다.
                    cancelAlarm()

                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false
            )
                .show()


        }
    }

    private fun saveAlarmModel(hour: Int, minute: Int, onOff: Boolean): AlarmDisplayModel {
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        with(sharedPreferences.edit()) {
            putString(ALARM_KEY, model.makeDataForDB())
            putBoolean(ONOFF_KEY, model.onOff)
            commit()
        }

        return model
    }

    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        val timeDBValue = sharedPreferences.getString(ALARM_KEY, "9:30") ?: "9:30"
        val onOffDBValue = sharedPreferences.getBoolean(ONOFF_KEY, false)
        val alarmData = timeDBValue.split(":")

        val model = AlarmDisplayModel(alarmData[0].toInt(), alarmData[1].toInt(), onOffDBValue)
        // 보정 예외처리
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE
        )

        if (pendingIntent == null && model.onOff) {
            // 알람은 꺼져있는데, 데이터는 켜져있는 경우
            model.onOff = false
        } else if (pendingIntent != null && !model.onOff) {
            // 알람은 켜져있는데 ,데이터는 꺼져있는 경우
            // 알람을 취소함
            pendingIntent.cancel()
        }

        return model
    }

    private fun renderView(model: AlarmDisplayModel) {
        binding.ampmTxt.apply {
            text = model.ampmText
        }
        binding.timeTxt.apply {
            text = model.timeText
        }
        binding.onOffBtn.apply {
            text = model.onOffText
            tag = model
        }
    }

    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE
        )
        pendingIntent?.cancel()
    }

    companion object {
        private const val ALARM_REQUEST_CODE = 1000
        private const val SHARED_PREFERENCES_NAME = "time"
        private const val ALARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
    }
}