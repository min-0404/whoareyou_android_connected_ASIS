package com.example.whoareyou.network.dto

import com.example.whoareyou.model.Employee
import com.google.gson.annotations.SerializedName

/**
 * 조직 API 응답의 부서/조직 단위 DTO
 *
 * 서버 DeptVO의 snake_case JSON 키를 매핑합니다.
 * organization API (actnKey=organizaion) 응답에서 사용됩니다.
 *
 * 주의: actnKey 파라미터 값의 오타 "organizaion" 은 ASIS 서버 호환성을 위해 유지합니다.
 */
data class DeptDto(

    /** 조직 코드 (고유 식별자) */
    @SerializedName("org_cd")
    val orgCd: String = "",

    /** 조직명 */
    @SerializedName("org_nm")
    val orgNm: String = "",

    /**
     * 상위 조직 코드.
     * 최상위 조직이면 빈 문자열이거나 null 일 수 있습니다.
     */
    @SerializedName("super_org_cd")
    val superOrgCd: String = "",

    /** 상위 조직명 */
    @SerializedName("super_org_nm")
    val superOrgNm: String = "",

    /**
     * 사용 여부.
     * "Y" = 활성, "N" = 비활성.
     * UI에서 "N" 인 조직은 필터링하는 것을 권장합니다.
     */
    @SerializedName("use_yn")
    val useYn: String = "Y",

    /** 조직장(팀장) 사번 */
    @SerializedName("chief_emp_no")
    val chiefEmpNo: String = "",

    /**
     * 조직 레벨 (계층 깊이).
     * 숫자가 클수록 하위 조직입니다.
     * 조직도 트리 구성 시 들여쓰기 등에 활용합니다.
     */
    @SerializedName("org_level")
    val orgLevel: Int = 0,

    /**
     * 정렬 순서 (sq2).
     * 같은 레벨의 조직을 정렬할 때 사용합니다.
     */
    @SerializedName("sq2")
    val sq2: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// Dept: 조직도 화면용 표시 모델 (Display Model)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 조직도 UI 화면에서 사용하는 표시용 모델.
 *
 * [DeptDto] 는 서버 응답을 그대로 담는 DTO이고,
 * [Dept] 는 화면 렌더링에 필요한 정보만 추린 간결한 모델입니다.
 *
 * @param deptCode    조직 코드 (DeptDto.orgCd)
 * @param deptName    조직명 (DeptDto.orgNm)
 * @param level       계층 깊이 (DeptDto.orgLevel)
 * @param memberCount 소속 인원 수. API 응답에 포함되지 않으므로 별도 집계 또는 기본값 0 사용.
 */
data class Dept(
    val deptCode: String,
    val deptName: String,
    val level: Int,
    val memberCount: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// 확장 함수: DeptDto → Dept
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 서버 DTO를 조직도 화면용 표시 모델로 변환합니다.
 *
 * @param memberCount 소속 인원 수 (기본값 0, 필요 시 외부에서 주입)
 */
fun DeptDto.toDept(memberCount: Int = 0): Dept = Dept(
    deptCode    = orgCd,
    deptName    = orgNm,
    level       = orgLevel,
    memberCount = memberCount
)

// ─────────────────────────────────────────────────────────────────────────────
// OrgSection: 조직도 최상위 그룹 (예: CEO, 감사, 노동조합)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * organizaion API 응답의 accordion 섹션 하나를 표현합니다.
 *
 * @param name         그룹명 (e.g. "CEO", "감사", "노동조합")
 * @param deptCode     그룹 자체의 부서 코드. accordion-toggle 에 goDeptList 가 직접 있는 경우에만 존재.
 * @param headEmployee 해당 그룹의 책임자 직원 (없으면 null)
 * @param subDepts     하위 부서 목록 (없으면 빈 리스트)
 */
data class OrgSection(
    val name: String,
    val deptCode: String = "",
    val headEmployee: Employee? = null,
    val subDepts: List<Dept> = emptyList()
)
