package com.example.whoareyou

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import coil.Coil
import coil.ImageLoader
import com.example.whoareyou.navigation.AppNavigation
import com.example.whoareyou.network.ApiClient
import com.example.whoareyou.network.AuthManager
import com.example.whoareyou.ui.theme.WhoAreYouTheme

class MainActivity : ComponentActivity() {

    // CALL_SCREENING 역할 요청 런처 (Android 10+)
    // 이 역할이 허용되어야 설정 > 기본앱 > 발신번호 표시 및 스팸 앱 목록에 앱이 나타납니다.
    private val callScreeningRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* 역할 허용/거부와 무관하게 앱 계속 실행 */ }

    // READ_PHONE_STATE 런타임 권한 요청 런처
    private val phoneStatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 결과와 무관하게 앱 계속 실행 */ }

    // POST_NOTIFICATIONS 런타임 권한 요청 런처 (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 결과와 무관하게 앱 계속 실행 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // AuthManager 초기화: SharedPreferences 에서 저장된 세션(authKey, loginEmpNo 등)을 로드한다.
        // setContent() 보다 먼저 호출해야 Composable 에서 로그인 상태를 올바르게 읽을 수 있다.
        AuthManager.init(this)

        // Coil 전역 이미지 로더 설정:
        // ApiClient.okHttpClient 를 재사용하여 Retrofit 과 동일한 SSL(Trust-All dev) + 쿠키(JSESSIONID) 설정을 공유합니다.
        // 이렇게 해야 ASIS 서버의 인증이 필요한 프로필 이미지 URL도 정상적으로 로드됩니다.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient { ApiClient.okHttpClient }
                .build()
        )

        // CALL_SCREENING 역할 요청 (Android 10+)
        // 허용 시 설정 > 기본앱 > 발신번호 표시 및 스팸 앱에 후아유가 표시됩니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                callScreeningRoleLauncher.launch(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                )
            }
        }

        // 오버레이 권한이 없으면 최초 1회 요청
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        // READ_PHONE_STATE 권한 요청 (통화 상태 감지에 필요)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }

        // POST_NOTIFICATIONS 권한 요청 (Android 13+, 화면 꺼진 상태 알림에 필요)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            WhoAreYouTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
