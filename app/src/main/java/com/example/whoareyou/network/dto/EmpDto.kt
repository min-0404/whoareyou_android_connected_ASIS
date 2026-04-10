package com.example.whoareyou.network.dto

import com.example.whoareyou.model.Employee
import com.google.gson.annotations.SerializedName

/**
 * 임직원 검색/상세/팀/조직 API 응답에서 사용되는 DTO (Data Transfer Object)
 *
 * 서버 EmpVO의 snake_case JSON 키를 그대로 매핑합니다.
 * Gson의 @SerializedName 을 사용하여 Kotlin camelCase 프로퍼티에 연결합니다.
 *
 * 사용 API:
 *   - search.wru (actnKey=search)   → List<EmpDto>
 *   - search.wru (actnKey=detail)   → EmpDto
 *   - search.wru (actnKey=myFav)    → List<EmpDto>
 *   - search.wru (actnKey=myTeam)   → List<EmpDto>
 *   - search.wru (actnKey=organizaion) → List<EmpDto>
 */
data class EmpDto(

    /** 사번 (직원 고유 식별자) */
    @SerializedName("emp_no")
    val empNo: String = "",

    /** 성명 */
    @SerializedName("emp_nm")
    val empNm: String = "",

    /**
     * 직급 코드
     * (예: "10" = 사원, "20" = 대리, ... 실제 코드표는 서버 참조)
     */
    @SerializedName("jf_cd")
    val jfCd: String = "",

    /** 직급 명칭 (예: 대리, 과장) */
    @SerializedName("jf_nm")
    val jfNm: String = "",

    /** 직책 명칭 (예: 팀장, 파트장) */
    @SerializedName("duty_nm")
    val dutyNm: String = "",

    /** 직책 영문 명칭 (예: Team Leader) */
    @SerializedName("eng_duty_nm")
    val engDutyNm: String = "",

    /** 직위 명칭 */
    @SerializedName("pos_nm")
    val posNm: String = "",

    /** 소속 조직명 (팀명) */
    @SerializedName("org_nm")
    val orgNm: String = "",

    /** 소속 조직 코드 */
    @SerializedName("org_cd")
    val orgCd: String = "",

    /** 사무실 전화번호 (내선) */
    @SerializedName("offi_no")
    val offiNo: String = "",

    /** 휴대폰 번호 */
    @SerializedName("cell_tel_no")
    val cellTelNo: String = "",

    /** 팩스 번호 */
    @SerializedName("fax_no")
    val faxNo: String = "",

    /** 이메일 주소 */
    @SerializedName("email")
    val email: String = "",

    /** 담당 업무 (직무) 명칭 */
    @SerializedName("rsp_job_nm")
    val rspJobNm: String = "",

    /** 근무지 코드 */
    @SerializedName("in_offi_cd")
    val inOffiCd: String = "",

    /**
     * 프로필 이미지 데이터 (Base64 인코딩 문자열).
     * null 이거나 빈 문자열이면 기본 아바타를 표시합니다.
     */
    @SerializedName("imgdata")
    val imgdata: String? = null,

    /**
     * 즐겨찾기 여부.
     * 서버 EmpVO의 isFav() getter가 JSON 직렬화 시 "fav" 키로 반환됩니다.
     * Gson은 Boolean 필드를 자동으로 처리합니다.
     */
    @SerializedName("fav")
    val fav: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// 확장 함수: EmpDto → Employee (UI 모델 변환)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 네트워크 DTO를 앱 UI 모델인 [Employee]로 변환합니다.
 *
 * 필드 매핑 규칙:
 *   - empNo        → empNo (직원 고유 ID로 사용)
 *   - empNm        → name
 *   - orgNm        → team (소속 팀명)
 *   - orgCd        → teamCode (팀 코드)
 *   - dutyNm       → position (직책)
 *   - engDutyNm    → nickname (영문 직책 / 닉네임 역할)
 *   - rspJobNm     → jobTitle (담당 업무)
 *   - offiNo       → internalPhone (내선 번호)
 *   - cellTelNo    → mobilePhone (휴대폰)
 *   - faxNo        → fax
 *   - email        → email
 *   - imgdata      → imgdata (Base64)
 *   - fav          → isFavorite
 */
fun EmpDto.toEmployee(): Employee = Employee(
    empNo         = empNo,
    name          = empNm,
    team          = orgNm,
    teamCode      = orgCd,
    position      = dutyNm,
    nickname      = engDutyNm,
    jobTitle      = rspJobNm,
    internalPhone = offiNo,
    mobilePhone   = cellTelNo,
    fax           = faxNo,
    email         = email,
    imgdata       = imgdata,
    isFavorite    = fav
)
