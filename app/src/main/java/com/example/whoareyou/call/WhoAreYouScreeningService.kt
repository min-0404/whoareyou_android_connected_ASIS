package com.example.whoareyou.call

import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.whoareyou.model.ContactStorage
import com.example.whoareyou.network.ApiClient
import com.example.whoareyou.network.AuthManager
import kotlinx.coroutines.runBlocking

/**
 * 시스템 전화 스크리닝 서비스 – 수신/발신 전화를 가로채 발신자 정보를 실시간으로 조회한다.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * 보안 정책 변경 (2024-04):
 *   이전 방식: 앱 실행 시 임직원 전화번호 목록을 서버에서 벌크 다운로드 → 기기 로컬에 저장
 *              → 수신 전화 시 로컬 DB 에서 검색
 *
 *   현재 방식: 기기에 임직원 번호를 저장 금지 (보안팀 강제)
 *              → 수신 전화 발생 시 POST /app/ubi/SearchActn.who 를 실시간 호출
 *              → 응답 결과로 오버레이 표시 여부 결정
 *
 * 실시간 API 조회를 사용하면:
 *   - 기기 분실/도난 시 임직원 번호 유출 위험 없음
 *   - 퇴사/인사발령 즉시 반영 (로컬 캐시 무효화 문제 없음)
 *   - 앱 용량 및 초기 로딩 시간 감소
 *
 * 타임아웃 고려사항:
 *   Android 는 CallScreeningService.onScreenCall() 호출 후 수 초 안에 respondToCall() 을
 *   호출하지 않으면 시스템이 강제로 전화를 허용한다. API 호출은 일반적으로 200~500ms 이내
 *   완료되므로 충분하지만, 네트워크 장애 시 즉시 null 처리하여 전화 차단이 발생하지 않도록
 *   try-catch 로 보호한다.
 * ──────────────────────────────────────────────────────────────────────────────
 */
class WhoAreYouScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "WhoAreYouScreening"

        const val CHANNEL_ID      = "whoareyou_call_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onScreenCall(callDetails: Call.Details) {
        // 발신 전화 처리: 커스텀 연락처에 등록된 번호면 통화 기록을 위해 모니터링 서비스 시작
        // ※ 임직원 발신 모니터링은 보안 정책상 불필요 (임직원 번호 미저장)
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            val outgoingNumber = callDetails.handle?.schemeSpecificPart
            if (outgoingNumber != null) {
                // 커스텀 연락처에서만 조회 (임직원 번호는 저장하지 않음)
                val customInfo = ContactStorage.findCustomContact(this, outgoingNumber)
                if (customInfo != null) {
                    val (name, note, _) = customInfo
                    startService(Intent(this, CallMonitorService::class.java).apply {
                        putExtra(CallMonitorService.EXTRA_EMPLOYEE_NAME, name)
                        putExtra(CallMonitorService.EXTRA_EMPLOYEE_TEAM, note) // note를 team 위치에 표시
                        putExtra(CallMonitorService.EXTRA_EMPLOYEE_JOB,  "")
                        putExtra(CallMonitorService.EXTRA_PHONE_NUMBER,  outgoingNumber)
                    })
                }
            }
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // ── 수신 전화 처리 ──────────────────────────────────────────────────────

        val rawNumber = callDetails.handle?.schemeSpecificPart ?: run {
            // 전화번호를 가져올 수 없는 경우 → 정상 수신 처리 (차단 없음)
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // ── STEP 1: 인증 키 확인 ──────────────────────────────────────────────
        val authKey = AuthManager.authKey
        if (authKey == null) {
            // 로그인 전 상태: 임직원 조회 불가 → 오버레이 없이 정상 수신
            Log.d(TAG, "authKey 없음: 로그인 필요. 오버레이 생략.")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // ── STEP 2: 실시간 임직원 API 조회 ────────────────────────────────────
        //
        // onScreenCall() 은 이미 백그라운드 스레드에서 호출되므로 runBlocking 사용이 안전하다.
        // 메인 스레드에서 runBlocking 을 사용하면 ANR 위험이 있지만,
        // CallScreeningService 의 콜백은 시스템이 별도 스레드에서 호출한다.
        //
        // 네트워크 오류(서버 다운, 타임아웃 등) 발생 시 → catch 에서 null 반환
        // → 오버레이 없이 전화 정상 수신 (전화 차단은 절대 없음)
        val caller = runBlocking {
            try {
                val response = ApiClient.api.lookupByPhoneNumber(
                    authKey        = authKey,
                    incomingNumber = rawNumber
                )
                // isAuth == "1" : 임직원 확인됨
                // isAuth == "2" : 비임직원 (일반인)
                if (response.isSuccess && response.data?.isAuth == "1") {
                    Log.d(TAG, "임직원 확인: ${response.data?.name} / ${response.data?.teamNm}")
                    response.data
                } else {
                    Log.d(TAG, "임직원 아님 또는 조회 실패: code=${response.code}")
                    null
                }
            } catch (e: Exception) {
                // 네트워크 오류 발생 시: 오버레이 없이 정상 수신 처리
                // 전화 차단이 발생하지 않도록 반드시 null 반환
                Log.w(TAG, "실시간 임직원 조회 실패 (네트워크 오류): ${e.message}")
                null
            }
        }

        // ── STEP 3: 임직원 확인 시 오버레이 표시 ──────────────────────────────
        if (caller != null) {
            // API 응답에서 받아온 임직원 정보를 Intent 에 담아 오버레이 서비스 시작
            // ★ startForegroundService: 화면 켜짐/꺼짐 무관하게 포그라운드 서비스로 직접 시작
            //   TYPE_APPLICATION_OVERLAY + FLAG_SHOW_WHEN_LOCKED 으로 잠금화면/수신화면 위에 표시
            try {
                startForegroundService(Intent(this, CallInfoOverlayService::class.java).apply {
                    putExtra(CallInfoOverlayService.EXTRA_EMPLOYEE_NAME, caller.name)
                    putExtra(CallInfoOverlayService.EXTRA_EMPLOYEE_TEAM, caller.teamNm)
                    putExtra(CallInfoOverlayService.EXTRA_EMPLOYEE_JOB,  caller.position)
                    putExtra(CallInfoOverlayService.EXTRA_PHONE_NUMBER,  rawNumber)
                    // 통화 기록용 추가 정보 (officeNo/mobileNo)
                    putExtra("office_no",  caller.officeNo)
                    putExtra("mobile_no",  caller.mobileNo)
                })
                Log.d(TAG, "오버레이 서비스 시작: ${caller.name} (${caller.teamNm})")
            } catch (e: Exception) {
                // 서비스 시작 실패 시에도 전화 수신은 정상 처리
                Log.e(TAG, "오버레이 서비스 시작 실패: ${e.message}")
            }
        } else {
            // ── STEP 4: 임직원 아닌 경우 커스텀 연락처 폴백 조회 ──────────────
            // 임직원이 아니더라도 사용자가 직접 등록한 연락처라면 오버레이 표시
            val customInfo = ContactStorage.findCustomContact(this, rawNumber)
            if (customInfo != null) {
                val (name, note, _) = customInfo
                try {
                    startForegroundService(Intent(this, CallInfoOverlayService::class.java).apply {
                        putExtra(CallInfoOverlayService.EXTRA_EMPLOYEE_NAME, name)
                        putExtra(CallInfoOverlayService.EXTRA_EMPLOYEE_TEAM, note)
                        putExtra(CallInfoOverlayService.EXTRA_EMPLOYEE_JOB,  "")
                        putExtra(CallInfoOverlayService.EXTRA_PHONE_NUMBER,  rawNumber)
                    })
                    Log.d(TAG, "커스텀 연락처 오버레이 표시: $name")
                } catch (e: Exception) {
                    Log.e(TAG, "커스텀 연락처 오버레이 서비스 시작 실패: ${e.message}")
                }
            }
        }

        // ── 전화 자체는 항상 정상 처리 (차단 금지) ────────────────────────────
        // CallResponse.Builder().build() = 기본값: 차단 없음, 거절 없음, 무음 없음
        respondToCall(callDetails, CallResponse.Builder().build())
    }
}
