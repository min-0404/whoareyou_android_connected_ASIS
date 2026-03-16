package com.example.whoareyou.call

import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.whoareyou.model.ContactStorage

class WhoAreYouScreeningService : CallScreeningService() {

    companion object {
        const val CHANNEL_ID      = "whoareyou_call_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onScreenCall(callDetails: Call.Details) {
        // 발신 전화: 등록된 번호인 경우 통화 기록을 위해 모니터링 서비스 시작
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            val outgoingNumber = callDetails.handle?.schemeSpecificPart
            if (outgoingNumber != null) {
                val outgoingInfo = ContactStorage.findCallerInfo(this, outgoingNumber)
                if (outgoingInfo != null) {
                    val (name, team, job) = outgoingInfo
                    startService(Intent(this, CallMonitorService::class.java).apply {
                        putExtra(CallMonitorService.EXTRA_EMPLOYEE_NAME, name)
                        putExtra(CallMonitorService.EXTRA_EMPLOYEE_TEAM, team)
                        putExtra(CallMonitorService.EXTRA_EMPLOYEE_JOB,  job)
                        putExtra(CallMonitorService.EXTRA_PHONE_NUMBER,  outgoingNumber)
                    })
                }
            }
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val rawNumber = callDetails.handle?.schemeSpecificPart ?: run {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // 임직원 + 커스텀 연락처 통합 조회
        val callerInfo = ContactStorage.findCallerInfo(this, rawNumber)

        if (callerInfo != null) {
            val (name, team, job) = callerInfo

            // ★ startForegroundService: 화면 켜짐/꺼짐 무관하게 포그라운드 서비스로 직접 시작
            //   TYPE_APPLICATION_OVERLAY + FLAG_SHOW_WHEN_LOCKED 으로 잠금화면/수신화면 위에 표시
            try {
                startForegroundService(Intent(this, CallInfoOverlayService::class.java).apply {
                    putExtra(CallInfoOverlayService.EXTRA_EMPLOYEE_NAME, name)
                    putExtra(CallInfoOverlayService.EXTRA_EMPLOYEE_TEAM, team)
                    putExtra(CallInfoOverlayService.EXTRA_EMPLOYEE_JOB,  job)
                    putExtra(CallInfoOverlayService.EXTRA_PHONE_NUMBER,  rawNumber)
                })
            } catch (_: Exception) {
                // 서비스 시작 실패 시 전화 수신은 정상 처리 유지
            }
        }

        // 전화 자체는 정상 처리 (차단하지 않음)
        respondToCall(callDetails, CallResponse.Builder().build())
    }

}
