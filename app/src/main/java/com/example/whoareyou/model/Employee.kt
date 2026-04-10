package com.example.whoareyou.model

/**
 * 앱 전반에서 사용하는 임직원 UI 모델
 *
 * 서버에서 받은 DTO([com.example.whoareyou.network.dto.EmpDto],
 * [com.example.whoareyou.network.dto.LoginDataDto]) 를
 * 각 DTO의 toEmployee() 확장 함수를 통해 이 모델로 변환합니다.
 *
 * 필드 출처:
 *   empNo         ← EmpDto.emp_no / LoginDataDto.empNo
 *   name          ← EmpDto.emp_nm / LoginDataDto.empNm
 *   team          ← EmpDto.org_nm / LoginDataDto.orgNm  (소속 팀명)
 *   teamCode      ← EmpDto.org_cd / LoginDataDto.orgCd  (팀 코드, myTeam API에서 활용)
 *   position      ← EmpDto.duty_nm / LoginDataDto.dutyNm  (직책)
 *   nickname      ← EmpDto.eng_duty_nm / LoginDataDto.engDutyNm  (영문 직책)
 *   jobTitle      ← EmpDto.rsp_job_nm  (담당 업무)
 *   internalPhone ← EmpDto.offi_no / LoginDataDto.offiNo  (내선 번호)
 *   mobilePhone   ← EmpDto.cell_tel_no / LoginDataDto.phoneNo  (휴대폰)
 *   fax           ← EmpDto.fax_no
 *   email         ← EmpDto.email / LoginDataDto.email
 *   imgdata       ← EmpDto.imgdata / LoginDataDto.empImgInfo  (Base64 프로필 이미지)
 *   isFavorite    ← EmpDto.fav  (즐겨찾기 여부)
 */
data class Employee(
    /** 직원 사번 (고유 식별자) */
    val empNo: String,

    /** 성명 */
    val name: String,

    /** 소속 팀명 */
    val team: String,

    /** 소속 팀 코드 */
    val teamCode: String = "",

    /** 직책 명칭 (예: 팀장, 파트장) — EmpDto.duty_nm */
    val position: String,

    /** 영문 직책 / 닉네임 — EmpDto.eng_duty_nm */
    val nickname: String,

    /** 담당 업무 — EmpDto.rsp_job_nm */
    val jobTitle: String,

    /** 사무실 내선 번호 */
    val internalPhone: String,

    /** 휴대폰 번호 */
    val mobilePhone: String,

    /** 팩스 번호 */
    val fax: String,

    /** 이메일 주소 */
    val email: String,

    /**
     * 프로필 이미지 데이터 (Base64 인코딩 문자열).
     * null 이면 기본 아바타 아이콘을 표시합니다.
     */
    val imgdata: String? = null,

    /** 즐겨찾기 여부 */
    val isFavorite: Boolean = false
)
