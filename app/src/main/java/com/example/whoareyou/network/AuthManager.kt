package com.example.whoareyou.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * 로그인 세션 정보를 관리하는 싱글턴 객체
 *
 * 인증 키(authKey)와 로그인한 직원의 기본 정보를 SharedPreferences 에 저장하여
 * 앱 재시작 후에도 세션이 유지되도록 합니다.
 *
 * 초기화:
 *   Application 클래스 또는 MainActivity.onCreate() 에서 반드시 한 번 호출하세요:
 *   ```kotlin
 *   AuthManager.init(applicationContext)
 *   ```
 *
 * 사용 예:
 *   ```kotlin
 *   // 로그인 성공 후 세션 저장
 *   AuthManager.saveSession(
 *       authKey = loginData.authKey,
 *       empNo   = loginData.empNo,
 *       orgCd   = loginData.orgCd,
 *       empNm   = loginData.empNm
 *   )
 *
 *   // API 호출 시 인증 키 사용
 *   val key = AuthManager.authKey ?: return  // 미로그인 처리
 *   val response = ApiClient.api.getMyFavorites(authKey = key)
 *
 *   // 로그아웃 시 세션 초기화
 *   AuthManager.clearSession()
 *   ```
 */
object AuthManager {

    private const val TAG = "AuthManager"

    // ─────────────────────────────────────────────────────────────────────────
    // SharedPreferences 인스턴스
    // init() 호출 전 접근 시 IllegalStateException 을 발생시킵니다.
    // ─────────────────────────────────────────────────────────────────────────

    private var prefs: SharedPreferences? = null

    /**
     * AuthManager 를 초기화합니다.
     *
     * Application 클래스나 MainActivity.onCreate() 에서 가장 먼저 호출해야 합니다.
     * 이미 초기화된 경우 재호출은 무시됩니다.
     *
     * @param context 애플리케이션 Context (메모리 누수 방지를 위해 applicationContext 권장)
     */
    fun init(context: Context) {
        if (prefs != null) {
            Log.d(TAG, "이미 초기화되어 있습니다. 재초기화를 건너뜁니다.")
            return
        }
        prefs = context.applicationContext.getSharedPreferences(
            ApiConstants.PREF_FILE_NAME,
            Context.MODE_PRIVATE
        )
        Log.d(TAG, "AuthManager 초기화 완료. 로그인 상태: ${isLoggedIn()}")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼: prefs 접근 전 초기화 여부 확인
    // ─────────────────────────────────────────────────────────────────────────

    private fun requirePrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException(
            "AuthManager가 초기화되지 않았습니다. " +
            "Application 클래스나 MainActivity.onCreate()에서 AuthManager.init(context)를 먼저 호출하세요."
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 세션 정보 프로퍼티 (get/set)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 현재 세션의 인증 키 (UUID 형식).
     *
     * getter: SharedPreferences 에서 읽어옵니다. 로그인 전이면 null 을 반환합니다.
     * setter: SharedPreferences 에 즉시 저장합니다. null 설정 시 키를 삭제합니다.
     *
     * 모든 API 호출 시 @Field("authKey") 파라미터로 전달해야 합니다.
     */
    var authKey: String?
        get() = requirePrefs().getString(ApiConstants.PREF_AUTH_KEY, null)
        set(value) {
            requirePrefs().edit().apply {
                if (value != null) putString(ApiConstants.PREF_AUTH_KEY, value)
                else remove(ApiConstants.PREF_AUTH_KEY)
            }.apply()
        }

    /**
     * 로그인한 직원의 사번.
     *
     * getter: SharedPreferences 에서 읽어옵니다.
     * setter: SharedPreferences 에 즉시 저장합니다.
     */
    var loginEmpNo: String?
        get() = requirePrefs().getString(ApiConstants.PREF_LOGIN_EMP_NO, null)
        set(value) {
            requirePrefs().edit().apply {
                if (value != null) putString(ApiConstants.PREF_LOGIN_EMP_NO, value)
                else remove(ApiConstants.PREF_LOGIN_EMP_NO)
            }.apply()
        }

    /**
     * 로그인한 직원의 소속 조직 코드.
     *
     * myTeam API (actnKey=myTeam) 호출 시 orgCd 파라미터로 사용됩니다.
     * getter: SharedPreferences 에서 읽어옵니다.
     * setter: SharedPreferences 에 즉시 저장합니다.
     */
    var loginOrgCd: String?
        get() = requirePrefs().getString(ApiConstants.PREF_LOGIN_ORG_CD, null)
        set(value) {
            requirePrefs().edit().apply {
                if (value != null) putString(ApiConstants.PREF_LOGIN_ORG_CD, value)
                else remove(ApiConstants.PREF_LOGIN_ORG_CD)
            }.apply()
        }

    /**
     * 로그인한 직원의 성명.
     *
     * 앱 상단 헤더, 드로어 등 UI에서 로그인한 사용자 이름을 표시할 때 사용합니다.
     * getter: SharedPreferences 에서 읽어옵니다.
     * setter: SharedPreferences 에 즉시 저장합니다.
     */
    var loginEmpNm: String?
        get() = requirePrefs().getString(ApiConstants.PREF_LOGIN_EMP_NM, null)
        set(value) {
            requirePrefs().edit().apply {
                if (value != null) putString(ApiConstants.PREF_LOGIN_EMP_NM, value)
                else remove(ApiConstants.PREF_LOGIN_EMP_NM)
            }.apply()
        }

    /**
     * 로그인 시 입력한 휴대폰번호 (정규화된 숫자 형식, 예: 01012345678).
     *
     * ASIS 서버의 모든 search.wru / member.wru 호출에 phoneNo 파라미터로 전달합니다.
     * getter: SharedPreferences 에서 읽어옵니다.
     * setter: SharedPreferences 에 즉시 저장합니다.
     */
    var loginPhoneNo: String?
        get() = requirePrefs().getString(ApiConstants.PREF_LOGIN_PHONE, null)
        set(value) {
            requirePrefs().edit().apply {
                if (value != null) putString(ApiConstants.PREF_LOGIN_PHONE, value)
                else remove(ApiConstants.PREF_LOGIN_PHONE)
            }.apply()
        }

    // ─────────────────────────────────────────────────────────────────────────
    // 세션 상태 확인
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 현재 로그인 상태인지 여부를 반환합니다.
     *
     * authKey 가 존재하고 비어 있지 않으면 로그인 상태로 간주합니다.
     * 실제 서버 세션 유효성은 확인하지 않습니다.
     * 서버로부터 4010(인증 만료) 응답을 받으면 [clearSession] 을 호출하여 로그아웃 처리하세요.
     *
     * @return 로그인 상태이면 true
     */
    fun isLoggedIn(): Boolean {
        return !authKey.isNullOrBlank()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 세션 저장 / 초기화
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 로그인 성공 후 세션 정보를 한 번에 저장합니다.
     *
     * 원자적(atomic)으로 처리하기 위해 단일 commit 으로 모든 값을 저장합니다.
     *
     * 로그인 응답([com.example.whoareyou.network.dto.LoginDataDto]) 수신 후 즉시 호출하세요:
     * ```kotlin
     * AuthManager.saveSession(
     *     authKey = response.data.authKey,
     *     empNo   = response.data.empNo,
     *     orgCd   = response.data.orgCd,
     *     empNm   = response.data.empNm
     * )
     * ```
     *
     * @param authKey 서버에서 발급한 인증 키 (UUID)
     * @param empNo   로그인한 직원의 사번
     * @param orgCd   로그인한 직원의 소속 조직 코드
     * @param empNm   로그인한 직원의 성명
     */
    fun saveSession(
        authKey: String,
        empNo: String,
        orgCd: String,
        empNm: String,
        phoneNo: String = ""
    ) {
        requirePrefs().edit().apply {
            putString(ApiConstants.PREF_AUTH_KEY,     authKey)
            putString(ApiConstants.PREF_LOGIN_EMP_NO, empNo)
            putString(ApiConstants.PREF_LOGIN_ORG_CD, orgCd)
            putString(ApiConstants.PREF_LOGIN_EMP_NM, empNm)
            putString(ApiConstants.PREF_LOGIN_PHONE,  phoneNo)
        }.apply()  // apply() 는 비동기 저장 (commit() 은 동기 저장)

        Log.d(TAG, "세션 저장 완료: empNo=$empNo, orgCd=$orgCd, empNm=$empNm, phoneNo=${phoneNo.take(4)}****")
    }

    /**
     * 세션 정보를 모두 삭제합니다.
     *
     * 아래 상황에서 호출해야 합니다:
     *   - 사용자가 로그아웃 버튼을 누른 경우
     *   - 서버 응답 코드가 4010 (인증 만료) 인 경우
     *
     * 이 메서드 호출 후 로그인 화면으로 내비게이션해야 합니다.
     */
    fun clearSession() {
        requirePrefs().edit().apply {
            remove(ApiConstants.PREF_AUTH_KEY)
            remove(ApiConstants.PREF_LOGIN_EMP_NO)
            remove(ApiConstants.PREF_LOGIN_ORG_CD)
            remove(ApiConstants.PREF_LOGIN_EMP_NM)
            remove(ApiConstants.PREF_LOGIN_PHONE)
        }.apply()

        Log.d(TAG, "세션 초기화 완료 (로그아웃)")
    }

    /**
     * 현재 세션 상태를 문자열로 반환합니다 (디버깅용).
     */
    override fun toString(): String {
        val hasKey = !authKey.isNullOrBlank()
        return "AuthManager(loggedIn=$hasKey, empNo=$loginEmpNo, empNm=$loginEmpNm, orgCd=$loginOrgCd)"
    }
}
