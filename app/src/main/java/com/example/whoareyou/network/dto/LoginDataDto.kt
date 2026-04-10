package com.example.whoareyou.network.dto

import com.example.whoareyou.model.Employee
import com.google.gson.annotations.SerializedName

/**
 * 로그인 API 성공 응답의 data 필드 DTO
 *
 * POST /app/ubi/member.wru (actnKey=login) 성공 시 반환되는 구조입니다.
 * 서버가 camelCase JSON을 반환하므로 @SerializedName 없이도 동작하지만,
 * 명시성을 위해 주요 필드에 @SerializedName 을 붙여 둡니다.
 *
 * 로그인 성공 후:
 *   1. authKey → AuthManager.saveSession() 에 저장
 *   2. empNo, orgCd, empNm → AuthManager 에 저장 (이후 API 호출에서 활용)
 *   3. toEmployee() 로 변환하여 UI에 내 정보 표시
 */
data class LoginDataDto(

    /**
     * 인증 키 (UUID 형식).
     * 이후 모든 API 호출의 authKey 파라미터로 전송해야 합니다.
     * SharedPreferences 에 저장하여 앱 재시작 후에도 유지합니다.
     */
    @SerializedName("authKey")
    val authKey: String = "",

    /** 사번 */
    @SerializedName("empNo")
    val empNo: String = "",

    /** 성명 */
    @SerializedName("empNm")
    val empNm: String = "",

    /** 소속 조직명 */
    @SerializedName("orgNm")
    val orgNm: String = "",

    /**
     * 소속 조직 코드.
     * myTeam API 호출 시 orgCd 파라미터로 사용됩니다.
     */
    @SerializedName("orgCd")
    val orgCd: String = "",

    /** 직책 명칭 */
    @SerializedName("dutyNm")
    val dutyNm: String = "",

    /** 직책 영문 명칭 */
    @SerializedName("engDutyNm")
    val engDutyNm: String = "",

    /** 사무실 전화번호 (내선) */
    @SerializedName("offiNo")
    val offiNo: String = "",

    /** 휴대폰 번호 */
    @SerializedName("phoneNo")
    val phoneNo: String = "",

    /** 이메일 주소 */
    @SerializedName("email")
    val email: String = "",

    /** 직급 코드 */
    @SerializedName("jfCd")
    val jfCd: String = "",

    /**
     * 프로필 이미지 정보.
     * 서버에서 Base64 인코딩된 이미지 데이터 또는 이미지 경로를 반환합니다.
     * 실제 포맷은 서버 구현에 따라 달라질 수 있습니다.
     */
    @SerializedName("empImgInfo")
    val empImgInfo: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// 확장 함수: LoginDataDto → Employee (UI 모델 변환)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 로그인 응답 DTO를 앱 UI 모델인 [Employee]로 변환합니다.
 * 로그인 후 "내 정보" 화면에 즉시 표시할 수 있도록 변환합니다.
 *
 * 필드 매핑:
 *   - empNo     → empNo
 *   - empNm     → name
 *   - orgNm     → team
 *   - orgCd     → teamCode
 *   - dutyNm    → position
 *   - engDutyNm → nickname
 *   - offiNo    → internalPhone
 *   - phoneNo   → mobilePhone
 *   - email     → email
 *   - empImgInfo→ imgdata
 */
fun LoginDataDto.toEmployee(): Employee = Employee(
    empNo         = empNo,
    name          = empNm,
    team          = orgNm,
    teamCode      = orgCd,
    position      = dutyNm,
    nickname      = engDutyNm,
    jobTitle      = "",          // 로그인 응답에는 담당업무 필드가 없음
    internalPhone = offiNo,
    mobilePhone   = phoneNo,
    fax           = "",          // 로그인 응답에는 팩스 필드가 없음
    email         = email,
    imgdata       = empImgInfo,
    isFavorite    = false        // 본인은 즐겨찾기 대상이 아님
)
