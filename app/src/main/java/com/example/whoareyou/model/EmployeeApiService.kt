package com.example.whoareyou.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

// ─────────────────────────────────────────────
// API 설정
// ⚠️ 서버 구현 후 BASE_URL을 실제 서버 주소로 변경하세요
// ─────────────────────────────────────────────
object ApiConfig {
    const val BASE_URL = "https://api.whoareyou.bccard.com"  // 가상 URL (서버 완성 후 교체)

    object Endpoints {
        const val EMPLOYEES = "/api/v1/employees"        // GET  : 임직원 전체 목록
        const val LOGIN     = "/api/v1/auth/login"       // POST : 로그인
        fun employeePhoto(id: Int) = "/api/v1/employees/$id/photo"  // GET : 프로필 사진
    }
}

// ─────────────────────────────────────────────
// API 서비스
// ─────────────────────────────────────────────
object EmployeeApiService {
    private const val TAG = "EmployeeApiService"

    /**
     * 임직원 목록을 서버에서 가져옵니다.
     * - API 호출 성공 시 서버 데이터를 반환합니다.
     * - 네트워크 오류·서버 오류·파싱 실패 시 MockData를 반환합니다.
     */
    suspend fun fetchEmployees(): List<Employee> = withContext(Dispatchers.IO) {
        val urlString = ApiConfig.BASE_URL + ApiConfig.Endpoints.EMPLOYEES

        try {
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout    = 10_000
                setRequestProperty("Accept", "application/json")
                // 인증 토큰이 필요할 경우 아래 주석을 해제하세요:
                // setRequestProperty("Authorization", "Bearer $authToken")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.w(TAG, "서버 오류 $responseCode → MockData 사용")
                connection.disconnect()
                return@withContext MockData.employees
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val result = parseEmployees(body)
            Log.d(TAG, "API 성공: ${result.size}명 로드")
            result

        } catch (e: Exception) {
            Log.w(TAG, "API 실패 (${e.message}) → MockData 사용")
            MockData.employees
        }
    }

    // ─────────────────────────────────────────────
    // JSON 파싱
    // ⚠️ 서버 JSON 키 이름에 맞춰 getString() 인자를 조정하세요
    // ─────────────────────────────────────────────
    private fun parseEmployees(json: String): List<Employee> {
        val array  = JSONArray(json)
        val result = mutableListOf<Employee>()

        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result += Employee(
                id            = o.getInt("id"),
                name          = o.getString("name"),
                team          = o.getString("team"),
                position      = o.getString("position"),
                nickname      = o.getString("nickname"),
                jobTitle      = o.getString("job_title"),
                internalPhone = o.getString("internal_phone"),
                mobilePhone   = o.getString("mobile_phone"),
                fax           = o.getString("fax"),
                email         = o.getString("email"),
                photoUrl      = o.optString("photo_url").takeIf { it.isNotBlank() }
            )
        }
        return result
    }
}
