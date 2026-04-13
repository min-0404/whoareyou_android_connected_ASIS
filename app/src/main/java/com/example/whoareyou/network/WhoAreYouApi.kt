package com.example.whoareyou.network

import com.example.whoareyou.network.dto.ApiResponse
import com.example.whoareyou.network.dto.CallEmpDto
import com.example.whoareyou.network.dto.LoginDataDto
import okhttp3.ResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * WhoAreYou 앱의 Retrofit API 인터페이스
 *
 * 모든 요청은 Form-URL-Encoded POST 방식으로 전송됩니다.
 * suspend 함수로 선언하여 Kotlin Coroutines 와 함께 사용합니다.
 *
 * 인스턴스 생성은 [ApiClient.api] 를 통해 이루어지며
 * 직접 생성하지 않습니다.
 *
 * 공통 오류 처리:
 *   - 네트워크 오류 → IOException 발생 (try-catch 필요)
 *   - 서버 오류(4xx/5xx) → HttpException 발생
 *   - 비즈니스 오류 → ApiResponse.isSuccess == false 로 확인
 */
interface WhoAreYouApi {

    // =========================================================================
    // ① 로그인
    // POST /app/ubi/member.wru
    // =========================================================================

    /**
     * ASIS 서버 로그인 (현재 운영 중인 방식)
     *
     * ASIS 서버는 isApp=Y 파라미터 포함 시 성공하면 아래 형태의 HTML을 반환합니다:
     * <script>
     *   var json = '{"authKey":"...","empNm":"...","orgNm":"...", ...}';
     *   webkit.messageHandlers.jsonData.postMessage(json);
     * </script>
     *
     * 응답 HTML에서 JSON 을 추출하는 파싱은 [AsisLoginParser] 가 담당합니다.
     *
     * ⚠️ HRMS에 등록된 휴대폰번호와 phoneNo 파라미터가 일치해야 로그인됩니다.
     * ⚠️ phoneNo 는 반드시 하이픈 없이 전송해야 합니다 (예: 01012345678)
     *
     * @param actnKey  항상 "login"
     * @param empNo    직원 사번
     * @param passwd   비밀번호
     * @param isApp    항상 "Y" (JSON 응답 경로 활성화)
     * @param version  앱 버전 코드 (authKey 생성에 사용)
     * @param phoneNo  기기 휴대폰번호 - 하이픈 없이 (HRMS 등록 번호와 일치해야 함)
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_MEMBER)
    suspend fun loginAsis(
        @Field("actnKey") actnKey: String = ApiConstants.ACTN_LOGIN,
        @Field("empNo")   empNo:   String,
        @Field("passwd")  passwd:  String,
        @Field("isApp")   isApp:   String = "Y",
        @Field("version") version: String = "14",
        @Field("phoneNo") phoneNo: String
    ): ResponseBody  // ASIS는 HTML 반환 → AsisLoginParser 로 파싱

    /**
     * Spring Boot 서버 로그인 (신규 서버 배포 후 사용 예정)
     *
     * 순수 JSON 응답을 반환합니다. 현재는 미사용.
     *
     * @param actnKey  항상 "login" (ApiConstants.ACTN_LOGIN)
     * @param empNo    직원 사번
     * @param passwd   비밀번호 (평문. HTTPS 전송 필수)
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_MEMBER)
    suspend fun login(
        @Field("actnKey") actnKey: String = ApiConstants.ACTN_LOGIN,
        @Field("empNo")   empNo: String,
        @Field("passwd")  passwd: String
    ): ApiResponse<LoginDataDto>

    // =========================================================================
    // ① -b 비밀번호 초기화 (MOTP 인증)
    // POST /app/ubi/member.wru
    // =========================================================================

    /**
     * MOTP 값을 이용한 비밀번호 초기화
     *
     * ASIS 서버에 사번 + 신규 비밀번호 + MOTP 값을 전송하여 비밀번호를 변경합니다.
     * 성공 시 webkit.messageHandlers 브릿지 패턴으로 결과 HTML 을 반환합니다.
     *
     * @param actnKey  항상 "chgPwd" (ApiConstants.ACTN_CHANGE_PWD)
     * @param empNo    직원 사번
     * @param newPwd   변경할 신규 비밀번호
     * @param motp     모바일 OTP 값 (MOTP 앱에서 생성)
     * @param isApp    항상 "Y"
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_MEMBER)
    suspend fun changePassword(
        @Field("actnKey") actnKey: String = ApiConstants.ACTN_CHANGE_PWD,
        @Field("empNo")   empNo:   String,
        @Field("passwd")  newPwd:  String,
        @Field("motp")    motp:    String,
        @Field("isApp")   isApp:   String = "Y",
        @Field("version") version: String = "14"
    ): ResponseBody  // ASIS 는 HTML 반환 → AsisLoginParser.parsePasswordReset() 로 파싱

    // =========================================================================
    // ② 로그아웃
    // POST /app/ubi/member.wru
    // =========================================================================

    /**
     * 로그아웃 처리
     *
     * ASIS 서버는 HTML 로 응답합니다. 응답 내용과 무관하게
     * [AuthManager.clearSession] 을 호출하여 로컬 세션을 초기화해야 합니다.
     *
     * @param actnKey  항상 "logout" (ApiConstants.ACTN_LOGOUT)
     * @param authKey  현재 세션의 인증 키 (AuthManager.authKey)
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_MEMBER)
    suspend fun logout(
        @Field("actnKey")  actnKey: String = ApiConstants.ACTN_LOGOUT,
        @Field("authKey")  authKey: String
    ): ResponseBody  // ASIS 는 HTML 반환 → 응답 무시, 세션만 초기화

    // =========================================================================
    // ③ 내 정보 조회
    // POST /app/ubi/member.wru
    // =========================================================================

    /**
     * 로그인한 직원 본인의 상세 정보를 조회합니다.
     *
     * ASIS 서버는 HTML 을 반환합니다.
     * [AsisLoginParser.parse] 로 HTML 에서 정보를 추출합니다.
     *
     * @param actnKey  항상 "myinfo" (ApiConstants.ACTN_MY_INFO)
     * @param authKey  현재 세션의 인증 키
     * @param phoneNo  HRMS 등록 휴대폰번호 (하이픈 없이)
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_MEMBER)
    suspend fun getMyInfo(
        @Field("actnKey") actnKey: String = ApiConstants.ACTN_MY_INFO,
        @Field("authKey") authKey: String,
        @Field("phoneNo") phoneNo: String = "",
        @Field("isApp")   isApp:   String = "Y"
    ): ResponseBody  // ASIS 는 HTML 반환 → AsisLoginParser 로 파싱

    // =========================================================================
    // ④ 직원 검색
    // POST /app/ubi/search.wru
    // =========================================================================

    /**
     * 이름, 사번, 부서명 등 키워드로 직원을 검색합니다.
     *
     * ASIS 서버는 HTML 을 반환합니다.
     * [AsisSearchParser.parseEmployeeList] 로 HTML 에서 직원 목록을 추출합니다.
     *
     * @param actnKey   항상 "search" (ApiConstants.ACTN_SEARCH)
     * @param authKey   현재 세션의 인증 키
     * @param searchStr 검색 키워드 (이름, 사번, 팀명 등)
     * @param phoneNo   HRMS 등록 휴대폰번호 (하이픈 없이)
     * @param isApp     항상 "Y"
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_SEARCH)
    suspend fun searchEmployees(
        @Field("actnKey")    actnKey:   String = ApiConstants.ACTN_SEARCH,
        @Field("authKey")    authKey:   String,
        @Field("keyword")    keyword:   String,
        @Field("phoneNo")    phoneNo:   String = "",
        @Field("isApp")      isApp:     String = "Y"
    ): ResponseBody  // ASIS 는 HTML 반환 → AsisSearchParser.parseEmployeeList() 로 파싱

    // =========================================================================
    // ⑤ 직원 상세 조회
    // POST /app/ubi/search.wru
    // =========================================================================

    /**
     * 특정 직원의 상세 정보를 사번으로 조회합니다.
     *
     * ASIS 서버는 HTML 프로필 페이지를 반환합니다.
     * [AsisSearchParser.parseDetail] 로 HTML 에서 직원 정보를 추출합니다.
     *
     * @param actnKey  항상 "detail" (ApiConstants.ACTN_DETAIL)
     * @param authKey  현재 세션의 인증 키
     * @param empNo    조회할 직원의 사번
     * @param phoneNo  HRMS 등록 휴대폰번호 (하이픈 없이)
     * @param isApp    항상 "Y"
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_SEARCH)
    suspend fun getEmployeeDetail(
        @Field("actnKey") actnKey: String = ApiConstants.ACTN_DETAIL,
        @Field("authKey") authKey: String,
        @Field("empNo")   empNo:   String,
        @Field("phoneNo") phoneNo: String = "",
        @Field("isApp")   isApp:   String = "Y"
    ): ResponseBody  // ASIS 는 HTML 프로필 페이지 반환 → AsisSearchParser.parseDetail() 로 파싱

    // =========================================================================
    // ⑥ 내 즐겨찾기 목록
    // POST /app/ubi/search.wru
    // =========================================================================

    /**
     * 로그인한 직원의 즐겨찾기(Favorites) 목록을 가져옵니다.
     *
     * ASIS 서버는 HTML 을 반환합니다.
     * [AsisSearchParser.parseFavoriteList] 로 HTML 에서 직원 목록을 추출합니다.
     *
     * @param actnKey  항상 "myFav" (ApiConstants.ACTN_MY_FAV)
     * @param authKey  현재 세션의 인증 키
     * @param phoneNo  HRMS 등록 휴대폰번호 (하이픈 없이)
     * @param isApp    항상 "Y"
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_SEARCH)
    suspend fun getMyFavorites(
        @Field("actnKey") actnKey: String = ApiConstants.ACTN_MY_FAV,
        @Field("authKey") authKey: String,
        @Field("phoneNo") phoneNo: String = "",
        @Field("isApp")   isApp:   String = "Y"
    ): ResponseBody  // ASIS 는 HTML 반환 → AsisSearchParser.parseFavoriteList() 로 파싱

    // =========================================================================
    // ⑦ 즐겨찾기 토글 (추가/제거)
    // POST /app/ubi/search.wru
    // =========================================================================

    /**
     * 특정 직원을 즐겨찾기에 추가하거나 제거합니다 (서버에서 현재 상태 반전).
     *
     * ASIS 서버는 HTML 을 반환합니다.
     * [AsisSearchParser.parseToggleFavorite] 로 HTML 에서 새 즐겨찾기 상태를 추출합니다.
     *
     * @param actnKey    항상 "toggleFav" (ApiConstants.ACTN_TOGGLE_FAV)
     * @param authKey    현재 세션의 인증 키
     * @param favrEmpNo  즐겨찾기를 토글할 대상 직원의 사번
     * @param phoneNo    HRMS 등록 휴대폰번호 (하이픈 없이)
     * @param isApp      항상 "Y"
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_SEARCH)
    suspend fun toggleFavorite(
        @Field("actnKey")   actnKey:   String = ApiConstants.ACTN_TOGGLE_FAV,
        @Field("authKey")   authKey:   String,
        @Field("favrEmpNo") favrEmpNo: String,
        @Field("phoneNo")   phoneNo:   String = "",
        @Field("isApp")     isApp:     String = "Y"
    ): ResponseBody  // ASIS 는 HTML 반환 → AsisSearchParser.parseToggleFavorite() 로 파싱

    // =========================================================================
    // ⑧ 내 팀원 목록
    // POST /app/ubi/search.wru
    // =========================================================================

    /**
     * 로그인한 직원과 같은 팀(orgCd)의 팀원 목록을 조회합니다.
     *
     * ASIS 서버는 HTML 을 반환합니다.
     * [AsisSearchParser.parseTeamList] 로 HTML 에서 팀원 목록을 추출합니다.
     * TeamScreen 에서 사용합니다.
     *
     * @param actnKey  항상 "myTeam" (ApiConstants.ACTN_MY_TEAM)
     * @param authKey  현재 세션의 인증 키
     * @param orgCd    팀 조직 코드 (AuthManager.loginOrgCd)
     * @param phoneNo  HRMS 등록 휴대폰번호 (하이픈 없이)
     * @param isApp    항상 "Y"
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_SEARCH)
    suspend fun getMyTeam(
        @Field("actnKey") actnKey: String = ApiConstants.ACTN_MY_TEAM,
        @Field("authKey") authKey: String,
        @Field("orgCd")   orgCd:   String = "",  // ASIS 에서 orgCd 없이도 정상 동작
        @Field("phoneNo") phoneNo: String = "",
        @Field("isApp")   isApp:   String = "Y"
    ): ResponseBody  // ASIS 는 HTML 반환 → AsisSearchParser.parseTeamList() 로 파싱

    // =========================================================================
    // ⑨ 조직도 조회
    // POST /app/ubi/search.wru
    // =========================================================================

    /**
     * 특정 조직 코드의 하위 조직 목록을 조회합니다.
     *
     * ASIS 서버는 HTML 을 반환합니다.
     * [AsisSearchParser.parseOrganization] 로 HTML 에서 조직 목록을 추출합니다.
     * OrgChartScreen 에서 조직도 트리를 렌더링하는 데 사용합니다.
     *
     * ⚠️ actnKey 파라미터 값이 "organizaion" (오타) 임에 주의하세요.
     *    ASIS 서버 호환성을 위해 의도적으로 오타를 유지합니다.
     *
     * @param actnKey  항상 "organizaion" (ApiConstants.ACTN_ORGANIZATION) — 오타 주의
     * @param authKey  현재 세션의 인증 키
     * @param orgCd    조회할 상위 조직 코드
     * @param phoneNo  HRMS 등록 휴대폰번호 (하이픈 없이)
     * @param isApp    항상 "Y"
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_SEARCH)
    suspend fun getOrganization(
        @Field("actnKey") actnKey: String = ApiConstants.ACTN_ORGANIZATION,
        @Field("authKey") authKey: String,
        @Field("orgCd")   orgCd:   String = "",  // 빈값 = 루트 조직 조회. ASIS 에서 없어도 동작
        @Field("phoneNo") phoneNo: String = "",
        @Field("isApp")   isApp:   String = "Y"
    ): ResponseBody  // ASIS 는 HTML 반환 → AsisSearchParser.parseOrganization() 로 파싱

    // =========================================================================
    // ⑩ 수신 전화번호 직원 조회
    // POST /app/ubi/SearchActn.who
    // =========================================================================

    /**
     * 수신 전화번호로 발신자의 직원 정보를 조회합니다.
     *
     * [com.example.whoareyou.call.CallMonitorService] 또는
     * [com.example.whoareyou.call.WhoAreYouScreeningService] 에서 수신 전화 발생 시 호출합니다.
     * 응답의 [CallEmpDto.isAuth] 값으로 내부 직원 여부를 판단합니다.
     *
     * @param authKey         현재 세션의 인증 키
     * @param incomingNumber  수신된 전화번호 (하이픈 포함/미포함 모두 가능, 서버에서 정규화)
     */
    @FormUrlEncoded
    @POST(ApiConstants.ENDPOINT_CALL)
    suspend fun lookupByPhoneNumber(
        @Field("authKey")        authKey: String,
        @Field("incomingNumber") incomingNumber: String
    ): ApiResponse<CallEmpDto>
}
