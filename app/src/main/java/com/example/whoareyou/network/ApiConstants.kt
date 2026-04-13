package com.example.whoareyou.network

import com.example.whoareyou.BuildConfig

/**
 * 네트워크 레이어 전반에서 사용하는 상수 모음
 *
 * BASE_URL 은 빌드 플레이버에 따라 자동 결정됩니다:
 *   dev  → https://isrnd.bccard.com:64443/  (개발서버, Trust-All SSL)
 *   prod → https://u2.bccard.com/           (운영서버, 정식 SSL)
 */
object ApiConstants {

    /**
     * 현재 빌드 플레이버의 서버 URL.
     * build.gradle.kts 의 buildConfigField 로 주입됩니다.
     */
    val BASE_URL: String get() = BuildConfig.BASE_URL

    // ─────────────────────────────────────────────────────────────────────
    // 엔드포인트 경로 (BASE_URL 기준 상대 경로)
    // ─────────────────────────────────────────────────────────────────────

    /** 회원(로그인/로그아웃/내정보) 엔드포인트 */
    const val ENDPOINT_MEMBER  = "app/ubi/member.wru"

    /** 검색/즐겨찾기/팀/조직 엔드포인트 */
    const val ENDPOINT_SEARCH  = "app/ubi/search.wru"

    /** 수신 전화번호 조회 엔드포인트 */
    const val ENDPOINT_CALL    = "app/ubi/SearchActn.who"

    // ─────────────────────────────────────────────────────────────────────
    // actnKey 값 목록
    // ─────────────────────────────────────────────────────────────────────

    // member.wru 액션 키
    const val ACTN_LOGIN      = "login"
    const val ACTN_LOGOUT     = "logout"
    const val ACTN_MY_INFO    = "myinfo"
    const val ACTN_CHANGE_PWD = "chgPwd"   // 비밀번호 초기화 (MOTP 인증)

    // search.wru 액션 키
    const val ACTN_SEARCH       = "search"
    const val ACTN_DETAIL       = "detail"
    const val ACTN_MY_FAV       = "myFav"
    const val ACTN_TOGGLE_FAV   = "toggleFav"
    const val ACTN_MY_TEAM      = "myTeam"

    /**
     * 서버 오타 그대로 유지 (ASIS 호환성).
     * 실제 파라미터 값은 "organizaion" (i 하나 빠짐) — 서버 수정 전까지 변경 금지.
     */
    const val ACTN_ORGANIZATION = "organizaion"

    // ─────────────────────────────────────────────────────────────────────
    // 서버 응답 코드
    // ─────────────────────────────────────────────────────────────────────

    const val CODE_SUCCESS          = "0000"  // 정상 처리
    const val CODE_AUTH_EXPIRED     = "4010"  // 인증 만료 → 재로그인 필요
    const val CODE_BAD_PARAMS       = "9000"  // 잘못된 파라미터
    const val CODE_SERVER_ERROR     = "9999"  // 서버 내부 오류

    // 1001~1008: 비즈니스 오류 (서버 message 필드 참조)

    // ─────────────────────────────────────────────────────────────────────
    // SharedPreferences 키 (AuthManager 와 공유)
    // ─────────────────────────────────────────────────────────────────────

    const val PREF_FILE_NAME    = "whoareyou_prefs"
    const val PREF_AUTH_KEY      = "auth_key"
    const val PREF_LOGIN_EMP_NO  = "login_emp_no"
    const val PREF_LOGIN_ORG_CD  = "login_org_cd"
    const val PREF_LOGIN_EMP_NM  = "login_emp_nm"
    const val PREF_LOGIN_PHONE   = "login_phone_no"  // ASIS API 호출 시 phoneNo 파라미터로 사용

    // ─────────────────────────────────────────────────────────────────────
    // OkHttp 타임아웃 (초)
    // ─────────────────────────────────────────────────────────────────────

    const val TIMEOUT_CONNECT_SEC = 30L
    const val TIMEOUT_READ_SEC    = 30L
    const val TIMEOUT_WRITE_SEC   = 30L
}
