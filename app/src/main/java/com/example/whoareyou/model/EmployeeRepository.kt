package com.example.whoareyou.model

import android.util.Log
import com.example.whoareyou.network.ApiClient
import com.example.whoareyou.network.ApiConstants
import com.example.whoareyou.network.AsisSearchParser
import com.example.whoareyou.network.AuthManager
import com.example.whoareyou.network.dto.Dept
import com.example.whoareyou.network.dto.OrgSection
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.Charset

/**
 * 임직원 데이터 접근 계층 (Repository)
 *
 * 로컬 캐시 없이 모든 데이터를 ASIS API 에서 즉시 조회합니다.
 * ASIS 서버는 모든 search.wru 엔드포인트에서 HTML 을 반환하므로
 * [AsisSearchParser] 를 이용하여 HTML 에서 데이터를 추출합니다.
 *
 * 공통 처리 규칙:
 *   1. AuthManager.authKey 가 null → 미로그인 상태로 간주, 빈 목록/null 반환
 *   2. phoneNo 는 AuthManager.loginPhoneNo 에서 가져옴 (로그인 시 저장됨)
 *   3. AsisSearchParser 로 HTML 파싱 → Employee 또는 Dept 반환
 *   4. IOException / HttpException → try-catch 로 처리, 빈 목록/null 반환
 */
object EmployeeRepository {

    private const val TAG = "EmployeeRepository"

    // ─── 디버그용 전역 상태 ─────────────────────────────────────────────────
    // 마지막으로 수신한 HTML 앞부분 저장 (화면 empty state 에서 표시)
    var debugLastHtml: String = ""
        private set
    var debugLastError: String = ""
        private set

    /** ASIS 서버는 EUC-KR 인코딩으로 HTML을 반환합니다.
     *  OkHttp ResponseBody.string() 은 Content-Type charset 이 명시되지 않으면
     *  UTF-8 로 디코딩하므로 한글이 깨집니다.
     *  → bytes() 로 읽은 뒤 EUC-KR 로 직접 디코딩합니다. */
    private fun ResponseBody.eucKrString(): String {
        val bytes = bytes()
        // EUC-KR 로 디코딩 시도, 실패하면 UTF-8 폴백
        return try {
            String(bytes, Charset.forName("EUC-KR"))
        } catch (e: Exception) {
            Log.w(TAG, "EUC-KR 디코딩 실패, UTF-8 폴백: ${e.message}")
            String(bytes, Charsets.UTF_8)
        }
    }

    private fun storeDebug(html: String, tag: String) {
        // 파서가 찾는 핵심 패턴 존재 여부 체크
        val hasCarousel   = html.contains("""class="carousel-info"""")
        val hasAccordion  = html.contains("""class="accordion-inner"""")
        val hasEmpDetail  = html.contains("empDetail(")
        val hasGoEmpDetail= html.contains("goEmpDetail(")
        val hasGoDeptList = html.contains("goDeptList(")
        val hasWebkit     = html.contains("webkit.messageHandlers")
        val hasLoginPasswd= html.contains("actnKey") && html.contains("passwd")
        // 실제 내용 앞부분: DOCTYPE 이후 유의미한 텍스트 200자
        val bodySnippet   = html.substringAfter("<body", "").take(150).replace("\n", " ").replace("\t", "")

        debugLastHtml = buildString {
            append("[$tag] ${html.length}자\n")
            append("carousel:$hasCarousel  accordion:$hasAccordion\n")
            append("empDetail:$hasEmpDetail  goEmp:$hasGoEmpDetail  goDept:$hasGoDeptList\n")
            append("webkit:$hasWebkit  loginPage:$hasLoginPasswd\n")
            append("body: ${bodySnippet.take(120)}")
        }

        Log.d(TAG, "=== HTML 수신 ($tag) ${html.length}자 ===")
        Log.d(TAG, "carousel=$hasCarousel accordion=$hasAccordion empDetail=$hasEmpDetail goEmpDetail=$hasGoEmpDetail goDeptList=$hasGoDeptList")
        Log.d(TAG, "webkit=$hasWebkit loginPage=$hasLoginPasswd")
        Log.d(TAG, html.take(800))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 임직원 검색
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 키워드로 임직원을 검색합니다.
     *
     * ASIS: POST search.wru (actnKey=search) → HTML 응답
     *       → [AsisSearchParser.parseEmployeeList] 로 파싱
     *
     * @param keyword 검색 키워드 (최소 1자 이상 권장)
     * @return 검색된 직원 목록. 오류 또는 미로그인 시 빈 리스트.
     */
    suspend fun search(keyword: String): List<Employee> {
        val authKey = AuthManager.authKey ?: run {
            Log.w(TAG, "search() 호출 실패: authKey 없음 (미로그인)")
            return emptyList()
        }
        val phoneNo = AuthManager.loginPhoneNo ?: ""

        // 이전 오류 메시지 초기화 (이전 화면의 오류가 표시되지 않도록)
        debugLastError = ""

        // ASIS 서버는 EUC-KR 인코딩을 기대하므로 한글 키워드를 EUC-KR URL 인코딩으로 변환합니다.
        val encodedKeyword = try {
            URLEncoder.encode(keyword, "EUC-KR")
        } catch (e: Exception) {
            keyword  // 인코딩 실패 시 원본 키워드 사용
        }

        return try {
            val body = ApiClient.api.searchEmployees(
                actnKey = ApiConstants.ACTN_SEARCH,
                authKey = authKey,
                keyword = encodedKeyword,
                phoneNo = phoneNo
            )
            val html = body.eucKrString()
            storeDebug(html, "search")
            AsisSearchParser.parseEmployeeList(html)
        } catch (e: CancellationException) {
            throw e  // 코루틴 취소는 정상 흐름 — 잡지 않고 전파
        } catch (e: IOException) {
            debugLastError = "search() 네트워크 오류: ${e.message}"
            Log.e(TAG, "search() 네트워크 오류", e)
            emptyList()
        } catch (e: HttpException) {
            debugLastError = "search() HTTP ${e.code()}: ${e.message()}"
            Log.e(TAG, "search() HTTP 오류: ${e.code()}", e)
            emptyList()
        } catch (e: Exception) {
            debugLastError = "search() 오류: ${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "search() 예상치 못한 오류", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내 즐겨찾기 목록 조회
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 로그인한 직원의 즐겨찾기 목록을 가져옵니다.
     *
     * ASIS: POST search.wru (actnKey=myFav) → HTML 응답
     *       → [AsisSearchParser.parseFavoriteList] 로 파싱
     *
     * @return 즐겨찾기 직원 목록. 오류 또는 미로그인 시 빈 리스트.
     */
    suspend fun getMyFavorites(): List<Employee> {
        val authKey = AuthManager.authKey ?: run {
            Log.w(TAG, "getMyFavorites() 호출 실패: authKey 없음")
            return emptyList()
        }
        val phoneNo = AuthManager.loginPhoneNo ?: ""

        return try {
            val body = ApiClient.api.getMyFavorites(
                actnKey = ApiConstants.ACTN_MY_FAV,
                authKey = authKey,
                phoneNo = phoneNo
            )
            val html = body.eucKrString()
            storeDebug(html, "myFav")
            AsisSearchParser.parseFavoriteList(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            debugLastError = "myFav 네트워크 오류: ${e.message}"
            Log.e(TAG, "getMyFavorites() 네트워크 오류", e)
            emptyList()
        } catch (e: HttpException) {
            debugLastError = "myFav HTTP ${e.code()}: ${e.message()}"
            Log.e(TAG, "getMyFavorites() HTTP 오류: ${e.code()}", e)
            emptyList()
        } catch (e: Exception) {
            debugLastError = "myFav 오류: ${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "getMyFavorites() 예상치 못한 오류", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내 팀원 목록 조회
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 로그인한 직원과 같은 팀의 팀원 목록을 가져옵니다.
     *
     * ASIS: POST search.wru (actnKey=myTeam) → HTML 응답
     *       → [AsisSearchParser.parseTeamList] 로 파싱
     *
     * @return 팀원 목록. 오류 또는 미로그인 시 빈 리스트.
     */
    suspend fun getMyTeam(): List<Employee> {
        val authKey = AuthManager.authKey ?: run {
            Log.w(TAG, "getMyTeam() 호출 실패: authKey 없음")
            return emptyList()
        }
        val orgCd   = AuthManager.loginOrgCd  ?: ""
        val phoneNo = AuthManager.loginPhoneNo ?: ""

        return try {
            val body = ApiClient.api.getMyTeam(
                actnKey = ApiConstants.ACTN_MY_TEAM,
                authKey = authKey,
                orgCd   = orgCd,
                phoneNo = phoneNo
            )
            val html = body.eucKrString()
            storeDebug(html, "myTeam")
            AsisSearchParser.parseTeamList(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            debugLastError = "myTeam 네트워크 오류: ${e.message}"
            Log.e(TAG, "getMyTeam() 네트워크 오류", e)
            emptyList()
        } catch (e: HttpException) {
            debugLastError = "myTeam HTTP ${e.code()}: ${e.message()}"
            Log.e(TAG, "getMyTeam() HTTP 오류: ${e.code()}", e)
            emptyList()
        } catch (e: Exception) {
            debugLastError = "myTeam 오류: ${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "getMyTeam() 예상치 못한 오류", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 특정 팀원 목록 조회 (조직코드 지정)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 특정 조직 코드로 팀원 목록을 가져옵니다.
     *
     * OrgChartScreen 에서 부서 확장 시 해당 부서의 구성원을 로드할 때 사용합니다.
     *
     * @param orgCd 조회할 조직 코드
     * @return 해당 팀 구성원 목록. 오류 시 빈 리스트.
     */
    suspend fun getTeamByOrgCd(orgCd: String): List<Employee> {
        val authKey = AuthManager.authKey ?: run {
            Log.w(TAG, "getTeamByOrgCd() 호출 실패: authKey 없음")
            return emptyList()
        }
        val phoneNo = AuthManager.loginPhoneNo ?: ""

        // organizaion API 에 팀 단위 orgCd 를 전달하면 accordion-inner 에 goEmpDetail 블록이 포함됩니다.
        // parseTeamList 는 이 goEmpDetail 블록에서 팀원을 추출합니다.
        // (본부 단위 orgCd 를 전달하면 goDeptList 만 반환되어 parseTeamList 결과가 비어있습니다 — 정상)
        return try {
            val body = ApiClient.api.getOrganization(
                actnKey = ApiConstants.ACTN_ORGANIZATION,
                authKey = authKey,
                orgCd   = orgCd,
                phoneNo = phoneNo
            )
            val html = body.eucKrString()
            storeDebug(html, "teamByOrgCd($orgCd)")
            AsisSearchParser.parseTeamList(html)
        } catch (e: IOException) {
            debugLastError = "teamByOrgCd 네트워크 오류: ${e.message}"
            Log.e(TAG, "getTeamByOrgCd() 네트워크 오류", e)
            emptyList()
        } catch (e: HttpException) {
            debugLastError = "teamByOrgCd HTTP ${e.code()}: ${e.message()}"
            Log.e(TAG, "getTeamByOrgCd() HTTP 오류: ${e.code()}", e)
            emptyList()
        } catch (e: Exception) {
            debugLastError = "teamByOrgCd 오류: ${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "getTeamByOrgCd() 예상치 못한 오류", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 직원 상세 조회
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 사번으로 특정 직원의 상세 정보를 조회합니다.
     *
     * ASIS: POST search.wru (actnKey=detail) → HTML 프로필 페이지
     *       → [AsisSearchParser.parseDetail] 로 파싱
     *
     * @param empNo 조회할 직원의 사번
     * @return 직원 상세 정보. 조회 실패 또는 미로그인 시 null.
     */
    suspend fun getDetail(empNo: String): Employee? {
        val authKey = AuthManager.authKey ?: run {
            Log.w(TAG, "getDetail() 호출 실패: authKey 없음")
            return null
        }
        val phoneNo = AuthManager.loginPhoneNo ?: ""

        return try {
            val body = ApiClient.api.getEmployeeDetail(
                actnKey = ApiConstants.ACTN_DETAIL,
                authKey = authKey,
                empNo   = empNo,
                phoneNo = phoneNo
            )
            val html = body.eucKrString()
            storeDebug(html, "detail($empNo)")
            AsisSearchParser.parseDetail(html, empNo)
        } catch (e: IOException) {
            Log.e(TAG, "getDetail() 네트워크 오류", e)
            null
        } catch (e: HttpException) {
            Log.e(TAG, "getDetail() HTTP 오류: ${e.code()}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "getDetail() 예상치 못한 오류", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 즐겨찾기 토글
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 특정 직원의 즐겨찾기 상태를 토글합니다 (추가 ↔ 제거).
     *
     * ASIS: POST search.wru (actnKey=toggleFav) → HTML 응답
     *       → [AsisSearchParser.parseToggleFavorite] 로 새 상태 추출
     *
     * 파싱 결과(null)로 상태를 알 수 없을 때는 [currentIsFavorite] 의 반전값을 반환합니다.
     * (서버 API 호출 자체는 성공했으므로 상태가 반전되었다고 가정)
     *
     * @param empNo            즐겨찾기를 토글할 직원의 사번
     * @param currentIsFavorite 토글 이전의 현재 즐겨찾기 상태
     * @return 토글 후 새로운 즐겨찾기 상태 (오류 시 currentIsFavorite 유지)
     */
    suspend fun toggleFavorite(empNo: String, currentIsFavorite: Boolean): Boolean {
        val authKey = AuthManager.authKey ?: run {
            Log.w(TAG, "toggleFavorite() 호출 실패: authKey 없음")
            return currentIsFavorite   // 인증 없으면 현재 상태 유지
        }
        val phoneNo = AuthManager.loginPhoneNo ?: ""

        return try {
            val body = ApiClient.api.toggleFavorite(
                actnKey  = ApiConstants.ACTN_TOGGLE_FAV,
                authKey  = authKey,
                empNo    = empNo,
                phoneNo  = phoneNo
            )
            val html = body.eucKrString()
            Log.d(TAG, "toggleFavorite() HTML 수신 (${html.length}자): ${html.take(300)}")

            // null = 파싱 불가 → 현재 상태의 반전값으로 폴백
            AsisSearchParser.parseToggleFavorite(html, empNo) ?: run {
                Log.d(TAG, "toggleFavorite() 파싱 실패 → 폴백: ${!currentIsFavorite}")
                !currentIsFavorite
            }
        } catch (e: IOException) {
            Log.e(TAG, "toggleFavorite() 네트워크 오류", e)
            currentIsFavorite   // 오류 시 상태 유지
        } catch (e: HttpException) {
            Log.e(TAG, "toggleFavorite() HTTP 오류: ${e.code()}", e)
            currentIsFavorite
        } catch (e: Exception) {
            Log.e(TAG, "toggleFavorite() 예상치 못한 오류", e)
            currentIsFavorite
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 조직도 조회
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 특정 조직 코드 하위의 조직 목록(부서 트리)을 가져옵니다.
     *
     * ASIS: POST search.wru (actnKey=organizaion) → HTML 응답
     *       → [AsisSearchParser.parseOrganization] 로 파싱
     *
     * ⚠️ actnKey 파라미터 값은 "organizaion" (서버 오타) — ApiConstants.ACTN_ORGANIZATION 사용
     *
     * @param orgCd 조회할 상위 조직 코드 (빈 문자열 = 루트 조직)
     * @return 조직 목록. 오류 또는 미로그인 시 빈 리스트.
     */
    suspend fun getOrganization(orgCd: String): List<Dept> {
        val authKey = AuthManager.authKey ?: run {
            Log.w(TAG, "getOrganization() 호출 실패: authKey 없음")
            return emptyList()
        }
        val phoneNo = AuthManager.loginPhoneNo ?: ""

        return try {
            val body = ApiClient.api.getOrganization(
                actnKey = ApiConstants.ACTN_ORGANIZATION,
                authKey = authKey,
                orgCd   = orgCd,
                phoneNo = phoneNo
            )
            val html = body.eucKrString()
            storeDebug(html, "organization($orgCd)")
            AsisSearchParser.parseOrganization(html)
        } catch (e: IOException) {
            Log.e(TAG, "getOrganization() 네트워크 오류", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e(TAG, "getOrganization() HTTP 오류: ${e.code()}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getOrganization() 예상치 못한 오류", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 조직도 섹션 조회 (accordion 구조 파싱)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * organizaion API 응답에서 accordion 섹션 목록(CEO/감사/노동조합 등)을 가져옵니다.
     *
     * [getOrganization] 과 같은 API 를 호출하지만 [AsisSearchParser.parseOrgSections] 로
     * 파싱하여 책임자 직원 + 하위 부서 목록이 담긴 [OrgSection] 을 반환합니다.
     *
     * @param orgCd 조회할 상위 조직 코드 (빈 문자열 = 루트 조직)
     * @return 조직 섹션 목록. 오류 또는 미로그인 시 빈 리스트.
     */
    suspend fun getOrgSections(orgCd: String): List<OrgSection> {
        val authKey = AuthManager.authKey ?: run {
            Log.w(TAG, "getOrgSections() 호출 실패: authKey 없음")
            return emptyList()
        }
        val phoneNo = AuthManager.loginPhoneNo ?: ""

        return try {
            val body = ApiClient.api.getOrganization(
                actnKey = ApiConstants.ACTN_ORGANIZATION,
                authKey = authKey,
                orgCd   = orgCd,
                phoneNo = phoneNo
            )
            val html = body.eucKrString()
            storeDebug(html, "orgSections($orgCd)")
            AsisSearchParser.parseOrgSections(html)
        } catch (e: IOException) {
            debugLastError = "orgSections 네트워크 오류: ${e.message}"
            Log.e(TAG, "getOrgSections() 네트워크 오류", e)
            emptyList()
        } catch (e: HttpException) {
            debugLastError = "orgSections HTTP ${e.code()}: ${e.message()}"
            Log.e(TAG, "getOrgSections() HTTP 오류: ${e.code()}", e)
            emptyList()
        } catch (e: Exception) {
            debugLastError = "orgSections 오류: ${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "getOrgSections() 예상치 못한 오류", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 전화번호 조회 (통화 서비스용, 기존 기능 유지)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 전화번호로 임직원을 찾습니다 (수신 전화 서비스용).
     *
     * CallMonitorService / WhoAreYouScreeningService 에서 사용됩니다.
     * 하이픈 제거 후 비교하여 형식 차이를 무시합니다.
     *
     * @param rawNumber 수신 전화번호 (형식 무관)
     * @param employees 비교 대상 직원 목록
     * @return 일치하는 직원. 없으면 null.
     */
    fun findByPhoneNumber(rawNumber: String, employees: List<Employee>): Employee? {
        val normalized = normalizePhone(rawNumber)
        return employees.firstOrNull { emp ->
            normalizePhone(emp.mobilePhone)   == normalized ||
            normalizePhone(emp.internalPhone) == normalized
        }
    }

    /**
     * 전화번호를 정규화합니다 (하이픈·공백 제거, 국가코드 변환).
     */
    private fun normalizePhone(phone: String): String {
        var num = phone.replace(Regex("[^0-9]"), "")
        if (num.startsWith("82") && num.length > 10) {
            num = "0" + num.substring(2)
        }
        return num
    }
}
