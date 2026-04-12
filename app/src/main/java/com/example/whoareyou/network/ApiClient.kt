package com.example.whoareyou.network

import android.util.Log
import com.example.whoareyou.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Retrofit 인스턴스를 생성하고 관리하는 싱글턴 객체
 *
 * 앱 전체에서 [api] 프로퍼티 하나로 모든 API 호출을 처리합니다.
 * Retrofit 인스턴스는 최초 접근 시 지연 초기화(lazy)됩니다.
 *
 * 사용 예:
 * ```kotlin
 * val response = ApiClient.api.login(empNo = "12345", passwd = "password")
 * ```
 */
object ApiClient {

    private const val TAG = "ApiClient"

    // ─────────────────────────────────────────────────────────────────────────
    // SSL Trust-All 설정 (개발 전용)
    //
    // ⚠️ 보안 경고:
    //   아래 X509TrustManager 는 모든 SSL 인증서를 신뢰합니다.
    //   ASIS 개발서버(isrnd.bccard.com:64443)가 자체 서명 인증서를 사용하므로
    //   개발 단계에서만 임시로 사용합니다.
    //
    //   운영(Production) 서버 배포 시 반드시 아래 중 하나로 교체해야 합니다:
    //     1. 정식 CA 인증서 사용 → 기본 OkHttpClient 로 교체
    //     2. 서버 인증서를 앱 assets 에 번들링 → Certificate Pinning 적용
    //
    //   Trust-All 설정이 운영 환경에 포함되면 MITM(중간자 공격) 취약점이 발생합니다.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 모든 인증서를 신뢰하는 TrustManager (개발 전용).
     * 운영 배포 전 반드시 제거하거나 적절한 인증서 검증으로 교체하세요.
     */
    private val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    /**
     * Trust-All SSL 소켓 팩토리를 생성합니다.
     * 실패 시 null 을 반환하고 기본 SSL 설정으로 폴백합니다.
     */
    private fun createTrustAllSslSocketFactory(): Pair<javax.net.ssl.SSLSocketFactory, X509TrustManager>? {
        return try {
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(
                    null,                          // KeyManager: 기본값 사용
                    arrayOf<TrustManager>(trustAllManager),
                    SecureRandom()
                )
            }
            Pair(sslContext.socketFactory, trustAllManager)
        } catch (e: Exception) {
            // SSL 설정 실패 시 경고 로그 출력 후 null 반환
            // → OkHttpClient 기본 SSL 설정으로 폴백됩니다.
            Log.w(TAG, "Trust-All SSL 설정 실패, 기본 SSL 사용: ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HttpLoggingInterceptor 설정
    // BuildConfig.DEBUG 가 true 인 경우에만 BODY 레벨 로그를 출력합니다.
    // 릴리즈 빌드에서는 NONE 으로 자동 전환되어 민감한 요청/응답이 노출되지 않습니다.
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    // CookieJar 설정
    //
    // ASIS 서버는 로그인 시 JSESSIONID 세션 쿠키를 발급합니다.
    // 이후 search.wru API 호출 시 이 쿠키가 없으면 서버가 세션을 인식 못하여
    // 로그인 페이지를 반환합니다.
    // JavaNetCookieJar 로 쿠키를 앱 세션 동안 메모리에 유지합니다.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 메모리 기반 쿠키 저장소 (앱 세션 동안 유지).
     * 서버가 발급하는 모든 쿠키(JSESSIONID 등)를 호스트별로 저장합니다.
     * okhttp-urlconnection 없이 순수 OkHttp CookieJar 인터페이스로 구현합니다.
     */
    private val cookieJar: CookieJar = object : CookieJar {
        private val store = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            store.getOrPut(host) { mutableListOf() }.apply {
                // 같은 이름의 쿠키는 교체
                cookies.forEach { newCookie ->
                    removeAll { it.name == newCookie.name }
                    add(newCookie)
                }
            }
            Log.d(TAG, "쿠키 저장 [$host]: ${cookies.map { "${it.name}=${it.value}" }}")
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val host = url.host
            val cookies = store[host] ?: emptyList()
            Log.d(TAG, "쿠키 전송 [$host]: ${cookies.map { it.name }}")
            return cookies
        }
    }

    /**
     * HTTP 요청/응답 전체를 Logcat에 출력하는 인터셉터.
     * 디버그 빌드에서만 BODY 레벨 로그를 활성화합니다.
     */
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OkHttpClient 빌드
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 설정이 완료된 OkHttpClient 인스턴스.
     * - 타임아웃: 연결/읽기/쓰기 모두 30초
     * - Trust-All SSL: 개발 서버 자체 서명 인증서 대응
     * - 로깅 인터셉터: 디버그 환경에서 요청/응답 전체 출력
     */
    /** Coil 이미지 로더 등 앱 내부에서 동일한 SSL/쿠키 설정을 재사용하기 위해 internal 공개 */
    internal val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(ApiConstants.TIMEOUT_CONNECT_SEC, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.TIMEOUT_READ_SEC, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.TIMEOUT_WRITE_SEC, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(loggingInterceptor)

        // Trust-All SSL: dev 플레이버에서만 적용 (개발서버 자체 서명 인증서 대응)
        // prod 플레이버에서는 기본 SSL 검증을 사용합니다.
        if (BuildConfig.TRUST_ALL_SSL) {
            try {
                val sslPair = createTrustAllSslSocketFactory()
                if (sslPair != null) {
                    builder
                        .sslSocketFactory(sslPair.first, sslPair.second)
                        .hostnameVerifier { _, _ -> true }
                    Log.d(TAG, "Trust-All SSL 설정 완료 (dev 전용)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "OkHttpClient SSL 설정 오류: ${e.message}")
            }
        } else {
            Log.d(TAG, "기본 SSL 검증 사용 (prod)")
        }

        builder.build()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Retrofit 인스턴스
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrofit 인스턴스.
     * - 기본 URL: [ApiConstants.BASE_URL]
     * - JSON 변환: Gson (snake_case ↔ camelCase 는 @SerializedName 으로 처리)
     * - HTTP 클라이언트: [okHttpClient]
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .also { Log.d(TAG, "Retrofit 초기화 완료: ${ApiConstants.BASE_URL}") }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API 인터페이스 인스턴스 (외부 접근 진입점)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * [WhoAreYouApi] 의 구현체.
     * Retrofit 이 내부적으로 프록시 객체를 생성합니다.
     *
     * 모든 API 호출의 진입점입니다:
     * ```kotlin
     * val result = ApiClient.api.login(empNo = "12345", passwd = "pass")
     * ```
     */
    val api: WhoAreYouApi by lazy {
        retrofit.create(WhoAreYouApi::class.java)
    }
}
