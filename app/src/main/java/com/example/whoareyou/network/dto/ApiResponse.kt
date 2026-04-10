package com.example.whoareyou.network.dto

import com.example.whoareyou.network.ApiConstants
import com.google.gson.annotations.SerializedName

/**
 * 서버 공통 응답 래퍼
 *
 * 서버는 모든 API에 대해 아래 형식의 JSON을 반환합니다:
 * ```json
 * {
 *   "code":    "0000",
 *   "message": "성공",
 *   "data":    { ... }   // 엔드포인트마다 타입이 다름 → 제네릭 T 로 처리
 * }
 * ```
 *
 * 사용 예:
 * ```kotlin
 * val response: ApiResponse<LoginDataDto> = api.login(...)
 * if (response.isSuccess) {
 *     val loginData: LoginDataDto? = response.data
 * }
 * ```
 *
 * @param T data 필드의 실제 타입 (LoginDataDto, List<EmpDto>, CallEmpDto 등)
 */
data class ApiResponse<T>(

    /**
     * 처리 결과 코드.
     * - "0000" : 정상
     * - "1001"~"1008" : 비즈니스 오류 (message 필드 참조)
     * - "4010" : 인증 토큰 만료 → 재로그인 필요
     * - "9000" : 파라미터 오류
     * - "9999" : 서버 내부 오류
     */
    @SerializedName("code")
    val code: String = "",

    /**
     * 서버가 반환하는 사람이 읽을 수 있는 메시지.
     * 오류 발생 시 사용자에게 직접 노출할 수 있습니다.
     */
    @SerializedName("message")
    val message: String = "",

    /**
     * 실제 응답 데이터.
     * 성공 시에도 null 일 수 있으므로 (예: 로그아웃) nullable 로 선언합니다.
     */
    @SerializedName("data")
    val data: T? = null
) {
    /**
     * 응답 코드가 성공(0000)인지 여부를 반환합니다.
     * Retrofit 콜 이후 가장 먼저 확인해야 하는 프로퍼티입니다.
     */
    val isSuccess: Boolean
        get() = code == ApiConstants.CODE_SUCCESS

    /**
     * 인증이 만료된 상태인지 여부.
     * true 이면 로그인 화면으로 이동 후 재로그인을 요청해야 합니다.
     */
    val isAuthExpired: Boolean
        get() = code == ApiConstants.CODE_AUTH_EXPIRED

    /**
     * 오류 상태 여부 (isSuccess 의 반전).
     * 오류 코드에 따른 분기 처리 시 활용합니다.
     */
    val isError: Boolean
        get() = !isSuccess

    override fun toString(): String =
        "ApiResponse(code=$code, message=$message, hasData=${data != null})"
}
