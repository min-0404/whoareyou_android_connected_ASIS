package com.example.whoareyou.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whoareyou.ui.theme.*
import com.example.whoareyou.network.ApiClient
import com.example.whoareyou.network.ApiConstants
import com.example.whoareyou.network.AuthManager
import kotlinx.coroutines.launch

/**
 * 설정 화면
 *
 * 전화번호 설정, 가이드, 로그아웃을 제공합니다.
 * 임직원 데이터는 로컬에 저장되지 않으므로 (보안팀 지침)
 * 데이터베이스 업데이트/초기화 기능은 포함되지 않습니다.
 *
 * @param onBack                    뒤로가기 콜백
 * @param onLogout                  로그아웃 완료 후 로그인 화면으로 이동 콜백
 * @param onNavigateToInfo          가이드 화면으로 이동 콜백
 * @param onNavigateToPhoneSettings 전화번호 설정 화면으로 이동 콜백
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToPhoneSettings: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoggingOut     by remember { mutableStateOf(false) }
    val scope            = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = TextPrimary)
            }
            Text(
                text       = "설정",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 비씨후아유 섹션
            SettingsSectionHeader(title = "비씨후아유")

            Card(
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        icon     = Icons.Default.Phone,
                        iconColor = AccentGreen,
                        title    = "전화번호 설정",
                        subtitle = "발신자 표시 설정",
                        trailingContent = {
                            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                        },
                        onClick = onNavigateToPhoneSettings
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Background)
                    SettingsRow(
                        icon      = Icons.Default.Info,
                        iconColor = AccentBlue,
                        title     = "가이드",
                        subtitle  = "앱 사용 방법 안내",
                        trailingContent = {
                            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                        },
                        onClick = onNavigateToInfo
                    )
                }
            }

            // 계정 섹션
            SettingsSectionHeader(title = "계정")

            Card(
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingsRow(
                    icon       = Icons.Default.ExitToApp,
                    iconColor  = Color.Red,
                    title      = "로그아웃",
                    subtitle   = "현재 계정에서 로그아웃합니다",
                    titleColor = Color.Red,
                    trailingContent = {
                        if (isLoggingOut) {
                            CircularProgressIndicator(color = Color.Red, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Red.copy(alpha = 0.5f))
                        }
                    },
                    onClick = { if (!isLoggingOut) showLogoutDialog = true }
                )
            }
        }
    }

    // 로그아웃 확인 다이얼로그
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃") },
            text  = { Text("로그아웃 하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        isLoggingOut = true
                        scope.launch {
                            try {
                                // 서버에 로그아웃 API 호출 (ASIS 는 HTML 반환 → 응답 무시)
                                AuthManager.authKey?.let { key ->
                                    ApiClient.api.logout(
                                        actnKey = ApiConstants.ACTN_LOGOUT,
                                        authKey = key
                                    ).close()  // ResponseBody 메모리 누수 방지
                                }
                            } catch (_: Exception) {
                                // 네트워크 오류가 있어도 로컬 세션은 초기화
                            } finally {
                                AuthManager.clearSession()
                                isLoggingOut = false
                                onLogout()
                            }
                        }
                    }
                ) {
                    Text("로그아웃", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    titleColor: Color = TextPrimary,
    trailingContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = titleColor)
            Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        trailingContent()
    }
}
