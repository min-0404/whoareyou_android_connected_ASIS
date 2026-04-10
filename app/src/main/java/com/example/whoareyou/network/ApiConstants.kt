package com.example.whoareyou.network

/**
 * 네트워크 레이어 전반에서 사용하는 상수 모음
 *
 * ⚠️ 서버 마이그레이션 안내:
 *   현재 ASIS 개발서버는 `/app/ubi/member.wru` (로그인)가 HTML을 반환합니다.
 *   `/app/ubi/search.wru` 및 `/app/ubi/SearchActn.who` 는 JSON을 반환합니다.
 *   로그인 API 정상화는 신규 Spring Boot 서버 배포 후 BASE_URL 교체만으로 완료됩니다.
 */
object ApiConstants {

    /**
     * ASIS 개발서버 URL.
     * 신규 Spring Boot 서버 배포 후 교체 필요.
     *
     * 주의: 해당 서버는 자체 서명(Self-Signed) 인증서를 사용하므로
     *       ApiClient 에서 SSL 신뢰 처리가 별도로 필요합니다.
     */
    const val BASE_URL = "https://isrnd.bccard.com:64443/"

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
    const val ACTN_LOGIN    = "login"
    const val ACTN_LOGOUT   = "logout"
    const val ACTN_MY_INFO  = "myinfo"

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
