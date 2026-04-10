package com.example.whoareyou.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 수신 전화번호 조회 API 응답의 data 필드 DTO
 *
 * POST /app/ubi/SearchActn.who (authKey + incomingNumber) 성공 시 반환됩니다.
 * 수신 전화 발생 시 오버레이 화면에 발신자 정보를 표시하는 데 사용됩니다.
 *
 * isAuth 값에 따른 처리:
 *   "1" → BC카드 내부 직원 (name, teamNm, position 등 사용 가능)
 *   "2" → 미등록 번호 / 조회 불가 (기본값)
 *   "0" → 비인가 요청 (authKey 오류 등)
 */
data class CallEmpDto(

    /**
     * 인증/조회 결과 코드.
     *   "1" = 내부 직원 확인됨
     *   "2" = 미확인 번호 (기본값)
     *   "0" = 비인가 / 오류
     *
     * 기본값을 "2"로 설정하여 Gson 파싱 실패 시에도 안전하게 미확인 상태로 처리합니다.
     */
    @SerializedName("isAuth")
    val isAuth: String? = "2",

    /** 발신자 이름 */
    @SerializedName("name")
    val name: String? = null,

    /** 발신자 소속 팀명 */
    @SerializedName("teamNm")
    val teamNm: String? = null,

    /** 발신자 직위/직급 */
    @SerializedName("position")
    val position: String? = null,

    /**
     * 추가 상세 정보.
     * 서버 구현에 따라 담당 업무, 메모 등 부가 정보가 포함될 수 있습니다.
     */
    @SerializedName("detail")
    val detail: String? = null,

    /** 발신자 사무실 전화번호 */
    @SerializedName("officeNo")
    val officeNo: String? = null,

    /** 발신자 휴대폰 번호 */
    @SerializedName("mobileNo")
    val mobileNo: String? = null,

    /**
     * 발신자 프로필 이미지 데이터 (Base64 인코딩).
     * null 또는 빈 문자열이면 기본 아바타를 표시합니다.
     */
    @SerializedName("imgData")
    val imgData: String? = null
) {
    /**
     * BC카드 내부 직원인지 여부.
     * 오버레이 UI에서 직원 정보 표시 여부를 결정할 때 사용합니다.
     */
    val isInternalEmployee: Boolean
        get() = isAuth == "1"

    /**
     * 오버레이에 표시할 발신자 이름.
     * 내부 직원이면 이름 + 팀명을 반환하고, 미확인이면 null 을 반환합니다.
     */
    val displayName: String?
        get() = if (isInternalEmployee) name else null
}
