package com.example.whoareyou.call

import android.os.Build
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Android 13 이하(구형 Samsung One UI)에서 fullScreenIntent 알림을 통해 실행되는 Activity.
 * TYPE_APPLICATION_OVERLAY 가 수신화면 z-order 아래에 가려지는 문제를 Activity 기반으로 우회.
 *
 * - 잠금화면/수신화면 위에 표시 (setShowWhenLocked + setTurnScreenOn)
 * - 투명 배경: Theme.WhoAreYou.Transparent (themes.xml)
 * - 통화 상태(IDLE) 감지 시 자동 종료
 * - 통화 기록 저장은 CallInfoOverlayService 에서 담당 (중복 저장 없음)
 */
class CallInfoActivity : ComponentActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ★ 잠금화면 위에 표시 + 화면 켜기 (API 27+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val name = intent?.getStringExtra(EXTRA_EMPLOYEE_NAME) ?: ""
        val team = intent?.getStringExtra(EXTRA_EMPLOYEE_TEAM) ?: ""
        val job  = intent?.getStringExtra(EXTRA_EMPLOYEE_JOB)  ?: ""

        // 이름 없이 직접 실행된 경우 즉시 종료
        if (name.isEmpty()) {
            finish()
            return
        }

        setContent {
            // 투명 배경 + 상단 중앙에 발신자 카드
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                CallerInfoCard(
                    name = name,
                    team = team,
                    job  = job,
                    modifier = Modifier.padding(top = 280.dp, start = 24.dp, end = 24.dp)
                )
            }
        }

        // 통화 상태 감지 → IDLE 시 자동 종료
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerTelephonyCallback()
        } else {
            registerLegacyListener()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback() {
        val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                if (state == TelephonyManager.CALL_STATE_IDLE) finish()
            }
        }
        telephonyCallback = cb
        telephonyManager.registerTelephonyCallback(mainExecutor, cb)
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyListener() {
        val listener = object : PhoneStateListener() {
            @Suppress("DEPRECATION")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                if (state == TelephonyManager.CALL_STATE_IDLE) finish()
            }
        }
        phoneStateListener = listener
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
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

@Composable
fun CallerInfoCard(name: String, team: String, job: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아바타 원
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFFFEDED), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.toString() ?: "?",
                    color = Color(0xFFFF4545),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1F)
                )
                if (team.isNotBlank()) {
                    Text(text = team, fontSize = 12.sp, color = Color(0xFF8C8C99))
                }
                if (job.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE0EDFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = job,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F47A6)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // BC 뱃지
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFFFF4545), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "BC",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
