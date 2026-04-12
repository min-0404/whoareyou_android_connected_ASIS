package com.example.whoareyou.network

import android.util.Log
import com.example.whoareyou.model.Employee
import com.example.whoareyou.network.dto.Dept

/**
 * ASIS search.wru HTML 응답 파서
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * ASIS 서버는 search.wru 의 모든 actnKey 에 대해 순수 HTML 페이지를 반환합니다.
 * API 종류별 HTML 구조:
 *
 *   actnKey=search / myFav  →  carousel-info 구조
 *     <div class="active item">
 *       <div class="carousel-info" ...>
 *         <span onclick="setStyle(this);empDetail('EMPNO');">
 *         <span class="testimonials-name">성명</span>
 *         <span class="testimonials-post">팀이름\n\t\t#직책 (닉네임)</span>
 *         <a onclick="callTo('내선번호');"> / <a onclick="callTo('휴대번호');">
 *       </div>
 *     </div>
 *
 *   actnKey=myTeam  →  accordion-inner 구조 (goEmpDetail 사용)
 *     <div class="accordion-inner">
 *       <span onclick="goEmpDetail('EMPNO')">
 *       <span class="testimonials-name">성명</span>
 *       <span class="testimonials-post">팀이름\n\t\t#직책 (닉네임)</span>
 *       <a onclick="callTo('내선번호');"> / <a onclick="callTo('휴대번호');">
 *     </div>
 *
 *   actnKey=organizaion  →  accordion 부서 트리 구조 (goDeptList 사용)
 *     최상위 섹션: <a class="accordion-toggle">섹션명</a>
 *     하위 부서:   <li><a onclick="goDeptList('CODE');">부서명</a></li>
 *     일부 섹션은 accordion-toggle 자체에 onclick="goDeptList('CODE');" 포함
 *
 *   actnKey=detail  →  프로필 카드 구조 (parseDetail 에서 처리)
 * ──────────────────────────────────────────────────────────────────────────────
 */
object AsisSearchParser {

    private const val TAG = "AsisSearchParser"

    // ─────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼: HTML 에서 프로필 이미지 URL 추출
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * HTML 에서 프로필 이미지 URL(또는 data URI)을 추출합니다.
     *
     * ASIS 서버가 반환하는 이미지는 보통 다음 경로 중 하나입니다:
     *   - 상대 URL: /app/ubi/photo.wru?empNo=12345&authKey=...
     *   - 상대 URL: /ubi/upload/empPhoto/12345.jpg
     *   - data URI:  data:image/jpeg;base64,...
     *
     * 추출 우선순위:
     *   1. class="profile-userpic" 컨테이너 내부 img.src
     *   2. class="testimonials-photo" 컨테이너 내부 img.src (carousel)
     *   3. photo 키워드를 포함하는 img.src
     *   4. data:image/ 로 시작하는 인라인 Base64 img.src
     *
     * @param html 파싱 대상 HTML 문자열
     * @return 추출된 이미지 URL(상대/절대/data URI) 또는 null
     */
    private fun extractProfileImageUrl(html: String): String? {
        // 패턴 1: profile-userpic div 내 img src (상세 페이지 프로필 영역)
        Regex("""class="profile-userpic"[\s\S]*?<img[^>]+src="([^"]+)"""")
            .find(html)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }

        // 패턴 2: testimonials-photo div 내 img src (carousel 목록 썸네일)
        Regex("""class="testimonials-photo"[\s\S]*?<img[^>]+src="([^"]+)"""")
            .find(html)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }

        // 패턴 3: "photo" 키워드를 포함하는 img src (photo.wru / empPhoto 등)
        Regex("""<img[^>]+src="(/[^"]*photo[^"]*)"[^>]*/?>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }

        // 패턴 4: data:image/ 인라인 Base64 (ASIS 일부 버전)
        Regex("""<img[^>]+src="(data:image/[^"]+)"[^>]*/?>""")
            .find(html)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }

        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼: 직원 HTML 블록 → Employee
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * search / myFav / myTeam 에서 공통으로 사용되는 직원 블록을 파싱합니다.
     *
     * empNo 추출 : onclick 속성의 empDetail('N') 또는 goEmpDetail('N')
     * 성명       : <span class="testimonials-name">
     * 팀/직책    : <span class="testimonials-post"> → "팀이름 #직책 (닉네임)"
     * 연락처     : callTo('번호') → 첫 번째=사내전화, 두 번째=휴대전화
     */
    private fun parseEmpBlock(block: String): Employee? {
        // ── empNo ────────────────────────────────────────────────────────────
        val empNo = Regex("""(?:empDetail|goEmpDetail)\('(\d+)'\)""")
            .find(block)?.groupValues?.get(1) ?: return null

        // ── 성명 ─────────────────────────────────────────────────────────────
        val name = Regex(
            """<span\s+class="testimonials-name"[^>]*>\s*(.*?)\s*</span>""",
            RegexOption.DOT_MATCHES_ALL
        ).find(block)?.groupValues?.get(1)
            ?.replace(Regex("<[^>]+>"), "")
            ?.trim() ?: return null

        if (name.isBlank()) return null

        // ── 팀 / 직책 / 닉네임 ──────────────────────────────────────────────
        // 원문 예: "플랫폼DX팀\n\t\t\t\t\t\t#팀원 (퍼스트펭귄) "
        val postRaw = Regex(
            """<span\s+class="testimonials-post"[^>]*>\s*(.*?)\s*</span>""",
            RegexOption.DOT_MATCHES_ALL
        ).find(block)?.groupValues?.get(1)
            ?.replace(Regex("<[^>]+>"), "")
            ?.replace(Regex("""\s+"""), " ")
            ?.trim() ?: ""

        // "#" 기준으로 팀이름과 직책+닉네임 분리
        val hashIdx   = postRaw.indexOf('#')
        val team      = if (hashIdx > 0) postRaw.substring(0, hashIdx).trim() else postRaw
        val afterHash = if (hashIdx >= 0) postRaw.substring(hashIdx + 1).trim() else ""

        // "직책 (닉네임)" → 괄호 기준 분리
        val parenStart = afterHash.indexOf('(')
        val parenEnd   = afterHash.lastIndexOf(')')
        val position   = if (parenStart > 0) afterHash.substring(0, parenStart).trim() else afterHash.trim()
        val nickname   = if (parenStart >= 0 && parenEnd > parenStart)
            afterHash.substring(parenStart + 1, parenEnd).trim()
        else ""

        // ── 연락처 ───────────────────────────────────────────────────────────
        val phones = Regex("""callTo\('([^']+)'\)""")
            .findAll(block).map { it.groupValues[1].trim() }.toList()
        val internalPhone = phones.getOrNull(0) ?: ""
        val mobilePhone   = phones.getOrNull(1) ?: ""

        // ── 프로필 이미지 ─────────────────────────────────────────────────────
        // carousel-info / accordion-inner 블록에 img 태그가 포함된 경우 URL 추출
        val imgSrc = extractProfileImageUrl(block)

        return Employee(
            empNo         = empNo,
            name          = name,
            team          = team,
            teamCode      = "",
            position      = position,
            nickname      = nickname,
            jobTitle      = "",          // 목록 응답에는 담당업무 미포함
            internalPhone = internalPhone,
            mobilePhone   = mobilePhone,
            fax           = "",
            email         = "",
            imgdata       = imgSrc,
            isFavorite    = false
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 직원 상세 파서 — actnKey=detail
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * actnKey=detail HTML 응답에서 [Employee] 를 추출합니다.
     *
     * 파싱 규칙 (실제 응답 기반):
     *   - 성명    : <div class="profile-usertitle-name"><p>
     *   - 팀/직책 : <div class="profile-usertitle-job"><p style="...font-size: 23px...">팀 직책 / 닉네임</p>
     *   - 담당업무 : 두 번째 profile-usertitle-job div
     *   - 내선번호 : glyphicon-phone-alt 태그 텍스트
     *   - 휴대전화 : glyphicon-phone" 태그 텍스트 (phone-alt 제외)
     *   - 팩스     : glyphicon-print 태그 텍스트
     *   - 이메일   : glyphicon-ok 태그 텍스트
     *   - 즐겨찾기 : id="isFav" value="true|false"
     *   - 사번     : toggleFav('EMPNO') — 없으면 파라미터 값 사용
     */
    fun parseDetail(html: String, empNo: String): Employee? {
        return try {
            // 성명
            val name = Regex("""<div class="profile-usertitle-name">\s*<p>(.*?)</p>""")
                .find(html)?.groupValues?.get(1)
                ?.replace(Regex("<[^>]+>"), "")?.trim() ?: ""
            if (name.isBlank()) {
                Log.e(TAG, "parseDetail: 성명 추출 실패\n${html.take(500)}")
                return null
            }

            // 팀 + 직책 + 닉네임: "플랫폼DX팀 팀원 / 퍼스트펭귄"
            val teamPosRaw = Regex(
                """<div class="profile-usertitle-job">\s*<p[^>]*font-size\s*:\s*23px[^>]*>(.*?)</p>""",
                RegexOption.DOT_MATCHES_ALL
            ).find(html)?.groupValues?.get(1)
                ?.replace(Regex("<[^>]+>"), "")?.trim() ?: ""

            val slashIdx    = teamPosRaw.indexOf(" / ")
            val teamPosPart = if (slashIdx >= 0) teamPosRaw.substring(0, slashIdx).trim() else teamPosRaw
            val nickname    = if (slashIdx >= 0) teamPosRaw.substring(slashIdx + 3).trim() else ""

            // "플랫폼DX팀 팀원" → 마지막 공백 기준 분리
            val lastSpace = teamPosPart.lastIndexOf(' ')
            val team      = if (lastSpace > 0) teamPosPart.substring(0, lastSpace).trim() else teamPosPart
            val position  = if (lastSpace > 0) teamPosPart.substring(lastSpace + 1).trim() else ""

            // 담당업무: 두 번째 profile-usertitle-job
            val jobTitle = Regex(
                """<div class="profile-usertitle-job">\s*<p[^>]*>(.*?)</p>""",
                RegexOption.DOT_MATCHES_ALL
            ).findAll(html).toList().getOrNull(1)
                ?.groupValues?.get(1)
                ?.replace(Regex("<[^>]+>"), "")?.trim() ?: ""

            // 연락처
            val internalPhone = Regex("""glyphicon-phone-alt[^>]*>\s*(.*?)\s*</i>""", RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.get(1)?.trim() ?: ""
            val mobilePhone   = Regex("""glyphicon-phone"\s*>\s*(.*?)\s*</i>""", RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.get(1)?.trim() ?: ""
            val fax           = Regex("""glyphicon-print[^>]*>\s*(.*?)\s*</i>""", RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.get(1)?.trim() ?: ""
            val email         = Regex("""glyphicon-ok[^>]*>\s*(.*?)\s*</i>""", RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.get(1)?.trim() ?: ""

            // 즐겨찾기 / 사번
            val isFav       = Regex("""id="isFav"\s+value="(true|false)"""").find(html)?.groupValues?.get(1) == "true"
            val finalEmpNo  = Regex("""toggleFav\('(\d+)'\)""").find(html)?.groupValues?.get(1) ?: empNo

            // 프로필 이미지 URL 추출
            val imgSrc = extractProfileImageUrl(html)

            Log.d(TAG, "parseDetail 성공: empNo=$finalEmpNo, name=$name, team=$team, position=$position, hasImg=${imgSrc != null}")

            Employee(
                empNo         = finalEmpNo,
                name          = name,
                team          = team,
                teamCode      = "",
                position      = position,
                nickname      = nickname,
                jobTitle      = jobTitle,
                internalPhone = internalPhone,
                mobilePhone   = mobilePhone,
                fax           = fax,
                email         = email,
                imgdata       = imgSrc,
                isFavorite    = isFav
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseDetail 예외", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 검색 결과 목록 파서 — actnKey=search
    // carousel-info 구조, empDetail('EMPNO') 사용
    // ─────────────────────────────────────────────────────────────────────────

    fun parseEmployeeList(html: String): List<Employee> {
        return parseCarouselList(html, "parseEmployeeList")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 즐겨찾기 목록 파서 — actnKey=myFav
    // search 와 동일한 carousel-info 구조
    // ─────────────────────────────────────────────────────────────────────────

    fun parseFavoriteList(html: String): List<Employee> {
        // myFav 목록에 있는 직원은 모두 즐겨찾기 상태이므로 isFavorite = true 로 세팅
        return parseCarouselList(html, "parseFavoriteList")
            .map { it.copy(isFavorite = true) }
    }

    /**
     * carousel-info 구조 공통 파서 (search / myFav 공용)
     *
     * 분리 기준: `<div class="carousel-info"` → 각 블록 = 직원 1명
     */
    private fun parseCarouselList(html: String, tag: String): List<Employee> {
        val blocks = html.split("""<div class="carousel-info"""").drop(1)
        if (blocks.isEmpty()) {
            Log.w(TAG, "$tag: carousel-info 블록 없음 (결과 0건 또는 구조 변경)")
            return emptyList()
        }

        val result = blocks.mapNotNull { block ->
            try { parseEmpBlock(block) }
            catch (e: Exception) { Log.e(TAG, "$tag: 블록 파싱 오류", e); null }
        }
        Log.d(TAG, "$tag 완료: ${result.size}명")
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 팀원 목록 파서 — actnKey=myTeam
    // accordion-inner 구조, goEmpDetail('EMPNO') 사용
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * myTeam HTML 에서 팀원 목록을 추출합니다.
     *
     * 분리 기준: `<div class="accordion-inner">` → goEmpDetail 포함 블록만 사용
     * (goDeptList 만 포함된 조직 목록 블록은 자동 제외)
     */
    fun parseTeamList(html: String): List<Employee> {
        val blocks = html.split("""<div class="accordion-inner">""").drop(1)

        val result = blocks.mapNotNull { block ->
            try {
                if (!block.contains("goEmpDetail")) return@mapNotNull null
                parseEmpBlock(block)
            } catch (e: Exception) {
                Log.e(TAG, "parseTeamList: 블록 파싱 오류", e)
                null
            }
        }
        Log.d(TAG, "parseTeamList 완료: ${result.size}명")
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 즐겨찾기 토글 파서 — actnKey=toggleFav
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * toggleFav HTML 응답에서 토글 후 새로운 즐겨찾기 상태를 추출합니다.
     *
     * ASIS 서버 응답 형식은 버전에 따라 다를 수 있으므로 여러 패턴을 순서대로 시도합니다:
     *   1. `id="isFav" value="true|false"` — detail 페이지 내장 형식
     *   2. JSON `"isFav":"Y"|"N"` — JSON embedded in HTML
     *   3. 단순 Y/N/true/false 응답
     *
     * 모든 패턴 실패 시 null 을 반환합니다.
     * 호출자(EmployeeRepository)에서 null 일 때 현재 상태를 반전하여 사용합니다.
     *
     * @return 파싱된 즐겨찾기 상태 (null = 파싱 불가)
     */
    fun parseToggleFavorite(html: String, empNo: String): Boolean? {
        // 패턴 1: id="isFav" value="true|false"
        Regex("""id="isFav"\s+value="(true|false)"""")
            .find(html)?.groupValues?.get(1)?.let { v ->
                Log.d(TAG, "parseToggleFavorite 패턴1 성공: empNo=$empNo, isFav=$v")
                return v == "true"
            }

        // 패턴 2: JSON {"isFav":"Y"} 또는 {"isFav":"N"}
        Regex(""""isFav"\s*:\s*"([YN])"""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.let { v ->
                Log.d(TAG, "parseToggleFavorite 패턴2 성공: empNo=$empNo, isFav=$v")
                return v.equals("Y", ignoreCase = true)
            }

        // 패턴 3: 단순 문자열 응답 (Y / N / true / false / 1 / 0)
        val trimmed = html.trim()
        when (trimmed.lowercase()) {
            "y", "true",  "1" -> { Log.d(TAG, "parseToggleFavorite 패턴3 성공: empNo=$empNo, Y"); return true  }
            "n", "false", "0" -> { Log.d(TAG, "parseToggleFavorite 패턴3 성공: empNo=$empNo, N"); return false }
        }

        Log.w(TAG, "parseToggleFavorite: 모든 패턴 실패 (empNo=$empNo) → 호출자가 반전값 사용\n${html.take(300)}")
        return null  // 파싱 불가 → EmployeeRepository 에서 현재 상태 반전으로 폴백
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 조직도 파서 — actnKey=organizaion (서버 오타 그대로)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * organizaion HTML 응답에서 [Dept] 목록을 추출합니다.
     *
     * 두 가지 패턴으로 부서 정보를 추출합니다:
     *   1. accordion-toggle 자체에 goDeptList 있는 경우 (예: 노동조합) → level=0
     *   2. <li><a onclick="goDeptList('CODE');">부서명</a></li> 패턴 → level=1
     *
     * 예시 결과:
     *   - Dept(deptCode="50008324", deptName="노동조합",   level=0)
     *   - Dept(deptCode="50040218", deptName="경영기획총괄", level=1)
     *   - Dept(deptCode="50040540", deptName="매입사업본부", level=1)
     *   - ...
     */
    fun parseOrganization(html: String): List<Dept> {
        val result = mutableListOf<Dept>()

        try {
            // 패턴 1: accordion-toggle 에 goDeptList 직접 포함 (예: 노동조합)
            Regex(
                """class="accordion-toggle"[^>]*onclick="[^"]*goDeptList\('([^']+)'\)[^"]*"[^>]*>\s*(.*?)\s*</a>""",
                RegexOption.DOT_MATCHES_ALL
            ).findAll(html).forEach { m ->
                val code = m.groupValues[1].trim()
                val name = m.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                if (code.isNotBlank() && name.isNotBlank()) {
                    result.add(Dept(deptCode = code, deptName = name, level = 0))
                }
            }

            // 패턴 2: <li> 안의 goDeptList 링크 (하위 부서 목록)
            // <li><a href="..." onclick="goDeptList('50040218');" style="..."><i ...></i>경영기획총괄</a></li>
            Regex(
                """<li>\s*<a\b[^>]*onclick="[^"]*goDeptList\('([^']+)'\)[^"]*"[^>]*>(.*?)</a>\s*</li>""",
                RegexOption.DOT_MATCHES_ALL
            ).findAll(html).forEach { m ->
                val code = m.groupValues[1].trim()
                val name = m.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                if (code.isNotBlank() && name.isNotBlank()) {
                    result.add(Dept(deptCode = code, deptName = name, level = 1))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "parseOrganization 예외", e)
        }

        Log.d(TAG, "parseOrganization 완료: ${result.size}개 조직")
        return result
    }
}
