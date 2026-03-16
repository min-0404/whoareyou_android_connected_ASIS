package com.example.whoareyou.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.whoareyou.R
import com.example.whoareyou.model.CallRecord
import com.example.whoareyou.model.ContactStorage
import java.util.UUID

/**
 * 수신 전화 시 시스템 통화 화면 위에 발신자 정보를 표시하는 포그라운드 서비스.
 *
 * Android 14+ (Fold 6 등): shortService + TYPE_APPLICATION_OVERLAY 직접 오버레이
 * Android 13 이하 (Note20 등): 동일 오버레이 시도 + fullScreenIntent 알림으로 CallInfoActivity 보조 실행
 *   → 구형 Samsung One UI 는 TYPE_APPLICATION_OVERLAY 가 수신화면보다 z-order 낮아
 *     Activity 기반 fullScreenIntent 방식으로 수신화면 위에 안정적으로 표시
 */
class CallInfoOverlayService : Service() {

    companion object {
        const val EXTRA_EMPLOYEE_NAME = "employee_name"
        const val EXTRA_EMPLOYEE_TEAM = "employee_team"
        const val EXTRA_EMPLOYEE_JOB  = "employee_job"
        const val EXTRA_PHONE_NUMBER  = "phone_number"

        const val OVERLAY_NOTIFICATION_ID    = 2001
        const val OVERLAY_CHANNEL_ID         = "whoareyou_overlay_channel"

        // Android 13 이하 fullScreenIntent 알림용
        const val FULLSCREEN_NOTIFICATION_ID = 2002
        const val FULLSCREEN_CHANNEL_ID      = "whoareyou_fullscreen_channel"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private lateinit var telephonyManager: TelephonyManager

    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    private var callerName  = ""
    private var callerTeam  = ""
    private var callerJob   = ""
    private var callerPhone = ""

    private var callStartTimeMs     = 0L
    private var callConnectedTimeMs = 0L
    private var callAnswered        = false
    private var recordSaved         = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callerName  = intent?.getStringExtra(EXTRA_EMPLOYEE_NAME) ?: ""
        callerTeam  = intent?.getStringExtra(EXTRA_EMPLOYEE_TEAM) ?: ""
        callerJob   = intent?.getStringExtra(EXTRA_EMPLOYEE_JOB)  ?: ""
        callerPhone = intent?.getStringExtra(EXTRA_PHONE_NUMBER)  ?: ""
        callStartTimeMs = System.currentTimeMillis()

        // ★ 포그라운드 서비스로 즉시 승격 (Android 8+ 백그라운드 제한 우회)
        createOverlayNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+: shortService 타입 명시 (역할 불필요, 최대 3분)
                startForeground(
                    OVERLAY_NOTIFICATION_ID,
                    buildForegroundNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                )
            } else {
                startForeground(OVERLAY_NOTIFICATION_ID, buildForegroundNotification())
            }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        // 화면-꺼짐 경로의 Full-Screen 알림이 있으면 제거
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(WhoAreYouScreeningService.NOTIFICATION_ID)

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        @Suppress("DEPRECATION")
        if (telephonyManager.callState == TelephonyManager.CALL_STATE_OFFHOOK) {
            callAnswered        = true
            callConnectedTimeMs = System.currentTimeMillis()
        }

        // 모든 버전: TYPE_APPLICATION_OVERLAY 오버레이 시도
        showOverlay()

        // Android 13 이하: 구형 Samsung One UI z-order 문제 대응
        // → fullScreenIntent 알림으로 CallInfoActivity 를 수신화면 위에 직접 실행
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            createFullScreenNotificationChannel()
            showFullScreenNotification()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerTelephonyCallback()
        } else {
            registerLegacyListener()
        }

        return START_NOT_STICKY
    }

    // ── 포그라운드 서비스 알림 ────────────────────────────────────

    private fun createOverlayNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                "후아유 수신 전화",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification =
        Notification.Builder(this, OVERLAY_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("후아유: $callerName")
            .setContentText(callerTeam.ifBlank { "수신 전화" })
            .setOngoing(true)
            .build()

    // ── Android 13 이하: fullScreenIntent 알림 (Samsung One UI 5.x 호환) ──

    private fun createFullScreenNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FULLSCREEN_CHANNEL_ID,
                "후아유 수신 전화 정보",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
                setBypassDnd(true)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun showFullScreenNotification() {
        val activityIntent = Intent(this, CallInfoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CallInfoActivity.EXTRA_EMPLOYEE_NAME, callerName)
            putExtra(CallInfoActivity.EXTRA_EMPLOYEE_TEAM, callerTeam)
            putExtra(CallInfoActivity.EXTRA_EMPLOYEE_JOB,  callerJob)
            putExtra(CallInfoActivity.EXTRA_PHONE_NUMBER,  callerPhone)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, FULLSCREEN_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("후아유: $callerName")
            .setContentText(callerTeam.ifBlank { "수신 전화" })
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setCategory(Notification.CATEGORY_CALL)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(FULLSCREEN_NOTIFICATION_ID, notification)
    }

    private fun cancelFullScreenNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(FULLSCREEN_NOTIFICATION_ID)
    }

    // ── 오버레이 뷰 생성 및 표시 ─────────────────────────────────

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            // SYSTEM_ALERT_WINDOW 권한 없음 → 오버레이 불가 (fullScreenIntent 가 보조)
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val dm  = resources.displayMetrics
        val dp  = dm.density
        val screenWidth = dm.widthPixels

        val card      = buildCard(dp)
        val cardWidth = screenWidth - (48 * dp).toInt()

        @Suppress("DEPRECATION")
        val wlp = WindowManager.LayoutParams(
            cardWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                // ★ 화면 꺼짐 상태에서 수신 시 화면을 즉시 켜줌 (Note20 등 대응)
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (280 * dp).toInt()
        }

        overlayView = card
        try {
            windowManager?.addView(card, wlp)
        } catch (e: Exception) {
            // 오버레이 추가 실패 → fullScreenIntent(Activity) 가 보조 역할
        }
    }

    private fun buildCard(dp: Float): FrameLayout {
        val card = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape        = GradientDrawable.RECTANGLE
                cornerRadius = 20 * dp
                setColor(Color.WHITE)
            }
            elevation = 16 * dp
        }

        val pad = (16 * dp).toInt()
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(pad, pad, pad, pad)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        inner.addView(buildAvatar(dp))
        inner.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams((14 * dp).toInt(), 1)
        })
        inner.addView(buildTextColumn(dp))
        inner.addView(buildBcBadge(dp))

        card.addView(inner)
        return card
    }

    private fun buildAvatar(dp: Float): FrameLayout {
        val size = (52 * dp).toInt()
        return FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FFEDED"))
            }
            layoutParams = LinearLayout.LayoutParams(size, size)
            addView(TextView(this@CallInfoOverlayService).apply {
                text      = callerName.firstOrNull()?.toString() ?: "?"
                textSize  = 20f
                typeface  = Typeface.DEFAULT_BOLD
                gravity   = Gravity.CENTER
                setTextColor(Color.parseColor("#FF4545"))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            })
        }
    }

    private fun buildTextColumn(dp: Float): LinearLayout {
        return LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            addView(TextView(this@CallInfoOverlayService).apply {
                text     = callerName
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1A1A1F"))
            })

            if (callerTeam.isNotBlank()) {
                addView(TextView(this@CallInfoOverlayService).apply {
                    text     = callerTeam
                    textSize = 12f
                    setTextColor(Color.parseColor("#8C8C99"))
                })
            }

            if (callerJob.isNotBlank()) {
                val bPad = (8 * dp).toInt()
                val vPad = (3 * dp).toInt()
                addView(TextView(this@CallInfoOverlayService).apply {
                    text     = callerJob
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    setTextColor(Color.parseColor("#1F47A6"))
                    setPadding(bPad, vPad, bPad, vPad)
                    background = GradientDrawable().apply {
                        shape        = GradientDrawable.RECTANGLE
                        cornerRadius = 20 * dp
                        setColor(Color.parseColor("#E0EDFF"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (4 * dp).toInt() }
                })
            }
        }
    }

    private fun buildBcBadge(dp: Float): FrameLayout {
        val size = (34 * dp).toInt()
        return FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF4545"))
            }
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = (8 * dp).toInt()
                gravity     = Gravity.CENTER_VERTICAL
            }
            addView(TextView(this@CallInfoOverlayService).apply {
                text     = "BC"
                textSize = 9f
                typeface = Typeface.DEFAULT_BOLD
                gravity  = Gravity.CENTER
                setTextColor(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            })
        }
    }

    // ── 통화 상태 모니터링 ────────────────────────────────────────

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
            override fun onCallStateChanged(state: Int, phoneNumber: String?) =
                handleCallState(state)
        }
        phoneStateListener = listener
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!callAnswered) {
                    callAnswered        = true
                    callConnectedTimeMs = System.currentTimeMillis()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (!recordSaved) {
                    recordSaved = true
                    cancelFullScreenNotification()
                    saveCallRecord()
                    removeOverlay()
                    stopSelf()
                }
            }
        }
    }

    private fun saveCallRecord() {
        val durationSeconds = if (callAnswered)
            (System.currentTimeMillis() - callConnectedTimeMs) / 1000
        else 0L
        val callType = if (callAnswered) "incoming" else "missed"

        ContactStorage.addCallRecord(
            this, CallRecord(
                id              = UUID.randomUUID().toString(),
                callerName      = callerName,
                callerTeam      = callerTeam,
                callerJob       = callerJob,
                phone           = callerPhone,
                timestampMs     = callStartTimeMs,
                durationSeconds = durationSeconds,
                callType        = callType
            )
        )
    }

    // ── 오버레이 제거 ─────────────────────────────────────────────

    private fun removeOverlay() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
            overlayView = null
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelFullScreenNotification()
        removeOverlay()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
        }
    }
}
