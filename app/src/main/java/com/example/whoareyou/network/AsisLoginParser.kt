package com.example.whoareyou.network

import android.util.Log
import com.example.whoareyou.network.dto.LoginDataDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ASIS 서버 로그인 HTML 응답 파서
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * ASIS 서버는 isApp=Y 로그인 성공 시 순수 JSON 이 아닌 HTML을 반환합니다.
 * JSON 데이터는 HTML 안의 JavaScript 변수에 임베딩되어 있으며,
 * webkit.messageHandlers 브릿지를 통해 WebView 앱에 전달하는 구조입니다.
 *
 * 성공 응답 HTML 구조:
 * ──────────────────────────────────────────────────────────────────────────────
 * <script>
 *   $(window).load(function(){
 *     var json = '{
 *       "authKey": "-2ddf9dc29...00014",
 *       "empNm":   "홍길동",
 *       "orgNm":   "플랫폼DX팀",
 *       "dutyNm":  "과장",
 *       "offiNo":  "02-XXX-9999",
 *       "phoneNo": "010-XXXX-XXXX",
 *       "email":   "hong@bccard.com"
 *     }';
 *     webkit.messageHandlers.jsonData.postMessage(json);
 *   });
 * </script>
 * ──────────────────────────────────────────────────────────────────────────────
 *
 * ⚠️ ASIS 응답에 포함되지 않는 필드:
 *   - empNo  → 로그인 입력값을 그대로 사용
 *   - orgCd  → 빈 문자열 (팀원보기 API 호출 시 제한될 수 있음)
 *   - engDutyNm, jfCd, empImgInfo → 빈 문자열/null
 */
object AsisLoginParser {

    private const val TAG = "AsisLoginParser"

    // 성공 응답 판별 키워드: ASIS 가 로그인 성공 시에만 포함하는 문자열
    private const val SUCCESS_MARKER  = "webkit.messageHandlers"

    // JSON 추출 정규식: var json = '{...}' 패턴 (멀티라인 지원)
    private val JSON_REGEX = Regex("""var json = '(\{[\s\S]*?\})'""")

    // ─────────────────────────────────────────────────────────────────────────
    // 로그인 성공 응답 파싱
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ASIS 로그인 HTML 응답에서 [LoginDataDto] 를 추출합니다.
     *
     * 처리 순서:
     *   1. 성공 응답 여부 확인 (webkit.messageHandlers 포함 여부)
     *   2. 정규식으로 var json = '...' 패턴에서 JSON 문자열 추출
     *   3. Gson 으로 Map 파싱
     *   4. [LoginDataDto] 로 매핑 (누락 필드는 기본값 처리)
     *
     * @param html       ASIS 서버 응답 HTML 전체 문자열
     * @param inputEmpNo 로그인 폼에서 사용자가 입력한 사번 (ASIS 응답에 미포함)
     * @return 파싱 성공 시 [LoginDataDto], 실패 시 null
     */
    fun parse(html: String, inputEmpNo: String): LoginDataDto? {
        // 1. 성공 응답 여부 확인
        if (!html.contains(SUCCESS_MARKER)) {
            Log.w(TAG, "ASIS 로그인 실패 응답 (webkit.messageHandlers 없음)")
            return null
        }

        return try {
            // 2. var json = '...' 에서 JSON 문자열 추출
            val jsonStr = JSON_REGEX.find(html)?.groupValues?.get(1) ?: run {
                Log.e(TAG, "JSON 추출 실패: var json 패턴을 찾을 수 없음")
                return null
            }

            Log.d(TAG, "추출된 JSON: $jsonStr")

            // 3. Map 으로 파싱
            val type = object : TypeToken<Map<String, String>>() {}.type
            val map: Map<String, String> = Gson().fromJson(jsonStr, type)

            // 4. LoginDataDto 매핑
            val dto = LoginDataDto(
                authKey   = map["authKey"]  ?: "",
                empNo     = inputEmpNo,             // ASIS 응답에 없음 → 입력값 사용
                empNm     = map["empNm"]    ?: "",
                orgNm     = map["orgNm"]    ?: "",
                orgCd     = "",                     // ASIS 응답에 없음 (팀원보기 제한)
                dutyNm    = map["dutyNm"]   ?: "",
                engDutyNm = "",                     // ASIS 응답에 없음
                offiNo    = map["offiNo"]   ?: "",
                phoneNo   = map["phoneNo"]  ?: "",
                email     = map["email"]    ?: "",
                jfCd      = "",
                empImgInfo = null
            )

            if (dto.authKey.isBlank()) {
                Log.e(TAG, "authKey 가 비어있음. 파싱 결과: $map")
                return null
            }

            Log.d(TAG, "로그인 파싱 성공: empNo=${dto.empNo}, empNm=${dto.empNm}, authKey=${dto.authKey.take(8)}...")
            dto

        } catch (e: Exception) {
            Log.e(TAG, "ASIS 로그인 응답 파싱 오류", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 로그인 실패 응답 파싱
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ASIS 로그인 실패 HTML 에서 사용자에게 보여줄 에러 메시지를 추출합니다.
     *
     * ASIS 에러 HTML 구조:
     * <div class="form-group"><label>죄송합니다.</label></div>
     * <div class="form-group">HRMS에 등록한 휴대폰번호와...<br>Device: ...<br>Hrms: ...</div>
     *
     * @param html ASIS 서버 응답 HTML 전체 문자열
     * @return 추출된 에러 메시지. 추출 실패 시 기본 메시지 반환.
     */
    fun parseErrorMessage(html: String): String {
        return try {
            // form-group div 에서 텍스트 추출
            val divRegex = Regex("""<div class="form-group">([\s\S]*?)</div>""")
            val matches  = divRegex.findAll(html).toList()

            // 두 번째 form-group 에 실제 에러 메시지 있음 (첫 번째는 "죄송합니다." label)
            val rawMsg = matches.getOrNull(1)?.groupValues?.get(1)
                ?: return resolveKnownError(html)

            rawMsg
                .replace(Regex("<br\\s*/?>"), "\n")  // <br> → 줄바꿈
                .replace(Regex("<[^>]+>"), "")        // 나머지 HTML 태그 제거
                .trim()
                .ifBlank { resolveKnownError(html) }

        } catch (e: Exception) {
            Log.e(TAG, "에러 메시지 파싱 실패", e)
            "로그인에 실패했습니다."
        }
    }

    /**
     * HTML 내 ASIS 에러 키워드를 분석해 사용자 친화적 메시지로 변환합니다.
     *
     * @param html ASIS 에러 HTML
     * @return 사용자 친화적 에러 메시지
     */
    private fun resolveKnownError(html: String): String = when {
        html.contains("휴대폰번호")       -> "HRMS에 등록된 휴대폰번호와 일치하지 않습니다.\n입력한 번호를 확인해주세요."
        html.contains("비밀번호")         -> "사번 또는 비밀번호가 올바르지 않습니다."
        html.contains("5회")             -> "비밀번호 오류 횟수를 초과했습니다.\n관리자에게 문의하세요."
        html.contains("재직")            -> "재직 중인 임직원만 이용 가능합니다."
        html.contains("WRU_0010")        -> "자동로그인 정보가 초기화되었습니다.\n다시 로그인해주세요."
        else                             -> "로그인에 실패했습니다.\n입력 정보를 확인해주세요."
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 비밀번호 초기화 응답 파싱
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ASIS 비밀번호 초기화(chgPwd) HTML 응답에서 성공/실패 여부를 판단합니다.
     *
     * 성공 패턴 (webkit.messageHandlers 포함 OR 서버 성공 메시지):
     *   - webkit.messageHandlers.jsonData.postMessage(...)
     *   - "success" / "비밀번호가 변경" / "완료"
     *
     * 실패 패턴:
     *   - form-group div 안의 에러 메시지
     *   - MOTP 오류, 사번 불일치 등
     *
     * @param html ASIS 서버 응답 HTML
     * @return 성공 시 null, 실패 시 사용자 친화적 에러 메시지 문자열
     */
    fun parsePasswordReset(html: String): String? {
        Log.d(TAG, "parsePasswordReset: html=${html.take(400)}")

        // 성공 판정: webkit 브릿지 메시지 또는 서버 성공 키워드
        val isSuccess = html.contains(SUCCESS_MARKER)
            || html.contains("비밀번호가 변경")
            || html.contains("변경되었습니다")
            || html.contains("변경 완료")
            || html.contains("\"result\":\"success\"", ignoreCase = true)
            || html.contains("\"result\":\"ok\"",      ignoreCase = true)

        if (isSuccess) {
            Log.d(TAG, "parsePasswordReset: 성공")
            return null  // null = 성공
        }

        // 실패: form-group 에서 에러 메시지 추출
        return try {
            val divRegex = Regex("""<div class="form-group">([\s\S]*?)</div>""")
            val matches  = divRegex.findAll(html).toList()
            val rawMsg   = matches.getOrNull(1)?.groupValues?.get(1)
                ?: matches.getOrNull(0)?.groupValues?.get(1)

            val extracted = rawMsg
                ?.replace(Regex("<br\\s*/?>"), "\n")
                ?.replace(Regex("<[^>]+>"), "")
                ?.trim()
                .orEmpty()

            extracted.ifBlank { resolvePwdResetError(html) }
        } catch (e: Exception) {
            Log.e(TAG, "parsePasswordReset 파싱 오류", e)
            resolvePwdResetError(html)
        }
    }

    /** 비밀번호 초기화 실패 시 에러 메시지 매핑 */
    private fun resolvePwdResetError(html: String): String = when {
        html.contains("motp", ignoreCase = true) ||
        html.contains("OTP",  ignoreCase = true)  -> "MOTP 값이 올바르지 않습니다.\nMOTP 앱에서 생성된 값을 다시 확인해주세요."
        html.contains("사번")                      -> "사번이 올바르지 않습니다."
        html.contains("비밀번호")                   -> "비밀번호 형식이 올바르지 않습니다.\n(영문+숫자+특수문자 조합 8자 이상)"
        else                                       -> "비밀번호 초기화에 실패했습니다.\n입력 정보를 다시 확인해주세요."
    }
}
