package com.example.whoareyou.model

object EmployeeRepository {

    // API 로드 후 갱신되는 캐시. 초기값은 MockData (오프라인·로드 전 폴백)
    private var _employees: List<Employee> = MockData.employees

    /** 전체 임직원 목록 (읽기 전용) */
    val employees: List<Employee> get() = _employees

    /**
     * API에서 임직원 목록을 새로고침합니다.
     * - 성공 시 내부 캐시(_employees)를 갱신하고 결과를 반환합니다.
     * - 실패 시 MockData 캐시를 유지합니다.
     */
    suspend fun loadEmployees(): List<Employee> {
        _employees = EmployeeApiService.fetchEmployees()
        return _employees
    }

    /**
     * 전화번호로 임직원을 찾습니다.
     * 하이픈 제거 후 비교하여 형식 차이를 무시합니다.
     */
    fun findByPhoneNumber(rawNumber: String): Employee? {
        val normalized = normalizePhone(rawNumber)
        return _employees.firstOrNull { emp ->
            normalizePhone(emp.mobilePhone) == normalized ||
            normalizePhone(emp.internalPhone) == normalized
        }
    }

    private fun normalizePhone(phone: String): String {
        // 하이픈, 공백 제거 후 국가코드(+82, 0082) → 0 으로 변환
        var num = phone.replace(Regex("[^0-9]"), "")
        if (num.startsWith("82") && num.length > 10) {
            num = "0" + num.substring(2)
        }
        return num
    }
}
