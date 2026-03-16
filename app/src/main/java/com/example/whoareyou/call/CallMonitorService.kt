package com.example.whoareyou.call

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.example.whoareyou.model.CallRecord
import com.example.whoareyou.model.ContactStorage
import java.util.UUID

/**
 * 발신 통화 기록용 백그라운드 서비스.
 * WhoAreYouScreeningService에서 등록된 번호로의 발신 전화가 감지되면 시작되며,
 * 통화 상태(OFFHOOK → IDLE)를 감지해 통화 기록을 저장한 후 스스로 종료합니다.
 */
class CallMonitorService : Service() {

    companion object {
        const val EXTRA_EMPLOYEE_NAME = "employee_name"
        const val EXTRA_EMPLOYEE_TEAM = "employee_team"
        const val EXTRA_EMPLOYEE_JOB  = "employee_job"
        const val EXTRA_PHONE_NUMBER  = "phone_number"
    }

    private lateinit var telephonyManager: TelephonyManager

    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    private var callerName = ""
    private var callerTeam = ""
    private var callerJob  = ""
    private var callerPhone = ""

    private var callStartTimeMs      = 0L
    private var callConnectedTimeMs  = 0L
    private var callConnected        = false
    private var recordSaved          = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callerName  = intent?.getStringExtra(EXTRA_EMPLOYEE_NAME) ?: ""
        callerTeam  = intent?.getStringExtra(EXTRA_EMPLOYEE_TEAM) ?: ""
        callerJob   = intent?.getStringExtra(EXTRA_EMPLOYEE_JOB)  ?: ""
        callerPhone = intent?.getStringExtra(EXTRA_PHONE_NUMBER)  ?: ""
        callStartTimeMs = System.currentTimeMillis()

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // 서비스 시작 시점에 이미 OFFHOOK 상태일 수 있으므로 즉시 확인
        @Suppress("DEPRECATION")
        if (telephonyManager.callState == TelephonyManager.CALL_STATE_OFFHOOK) {
            callConnected       = true
            callConnectedTimeMs = System.currentTimeMillis()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerTelephonyCallback()
        } else {
            registerLegacyListener()
        }

        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback() {
        val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) = handleCallState(state)
        }
        telephonyCallback = cb
        telephonyManager.registerTelephonyCallback(mainExecutor, cb)
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyListener() {
        val listener = object : PhoneStateListener() {
            @Suppress("DEPRECATION")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) = handleCallState(state)
        }
        phoneStateListener = listener
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!callConnected) {
                    callConnected       = true
                    callConnectedTimeMs = System.currentTimeMillis()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (!recordSaved) {
                    recordSaved = true
                    saveCallRecord()
                    stopSelf()
                }
            }
        }
    }

    private fun saveCallRecord() {
        val durationSeconds = if (callConnected)
            (System.currentTimeMillis() - callConnectedTimeMs) / 1000
        else 0L

        val record = CallRecord(
            id             = UUID.randomUUID().toString(),
            callerName     = callerName,
            callerTeam     = callerTeam,
            callerJob      = callerJob,
            phone          = callerPhone,
            timestampMs    = callStartTimeMs,
            durationSeconds = durationSeconds,
            callType       = "outgoing"
        )
        ContactStorage.addCallRecord(this, record)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
        }
    }
}
