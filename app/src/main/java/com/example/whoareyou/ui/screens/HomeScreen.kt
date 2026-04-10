package com.example.whoareyou.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whoareyou.R
import com.example.whoareyou.model.Employee
import com.example.whoareyou.ui.theme.*

// 트렌디한 신규 색상
private val ColorTeal = Color(0xFF00B4D9)   // 전화번호 추가
private val ColorRose = Color(0xFFE91E8C)   // 통화내역

@Composable
fun HomeScreen(
    loggedInEmployee: Employee,
    onLogout: () -> Unit,
    onNavigateToDetail: (Employee) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToTeam: () -> Unit,
    onNavigateToOrgChart: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAddPhone: () -> Unit,
    onNavigateToCallHistory: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 상단 헤더 (고정 높이)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.top_logo),
                contentDescription = "BC카드 로고",
                modifier = Modifier
                    .height(40.dp)
                    .background(Color.White),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "후아유 임직원 서비스",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { showLogoutDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "로그아웃",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = "로그아웃", fontSize = 13.sp, color = TextSecondary)
            }
        }

        // 콘텐츠 영역 (스크롤 가능)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 내 프로필 카드 (고정 높이)
            MyProfileCard(
                employee = loggedInEmployee,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigateToDetail(loggedInEmployee) }
            )

            // 메뉴 카드 행 1 (고정 높이 → 하단 여백 자동 확보)
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeMenuCard(
                    icon = Icons.Default.Star,
                    title = "즐겨찾기",
                    color = AccentOrange,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onNavigateToFavorites
                )
                HomeMenuCard(
                    icon = Icons.Default.Person,
                    title = "팀원보기",
                    color = AccentBlue,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onNavigateToTeam
                )
            }

            // 메뉴 카드 행 2
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeMenuCard(
                    icon = Icons.Default.Search,
                    title = "검색",
                    color = AccentGreen,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onNavigateToSearch
                )
                HomeMenuCard(
                    icon = Icons.Default.List,
                    title = "조직도",
                    color = AccentPurple,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onNavigateToOrgChart
                )
            }

            // 메뉴 카드 행 3
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeMenuCard(
                    icon = Icons.Default.Add,
                    title = "전화번호 추가",
                    color = ColorTeal,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onNavigateToAddPhone
                )
                HomeMenuCard(
                    icon = Icons.Default.Phone,
                    title = "통화내역",
                    color = ColorRose,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onNavigateToCallHistory
                )
            }

        }

        // 하단 탭바 (고정 높이)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomTabItem(
                icon = Icons.Default.Info,
                label = "가이드",
                isSelected = false,
                onClick = onNavigateToInfo
            )
            BottomTabItem(
                icon = Icons.Default.Home,
                label = "홈",
                isSelected = true,
                onClick = {}
            )
            BottomTabItem(
                icon = Icons.Default.Settings,
                label = "설정",
                isSelected = false,
                onClick = onNavigateToSettings
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃") },
            text = { Text("로그아웃 하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("로그아웃", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", color = Primary)
                }
            }
        )
    }
}

@Composable
fun MyProfileCard(
    employee: Employee,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(size = 54, name = employee.name)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = employee.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = employee.nickname,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = employee.team,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BadgeBackground)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = Color(0xFF263DA0),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = employee.jobTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BadgeText
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun HomeMenuCard(
    icon: ImageVector,
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BottomTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Primary else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Primary else TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 공통 프로필 아바타 컴포넌트
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 직원 프로필 아바타를 표시합니다.
 *
 * Base64 이미지 로딩 대신 이름의 첫 글자를 원형 배경 위에 표시합니다.
 * 모든 화면에서 일관된 아바타 스타일을 유지합니다.
 *
 * @param name     직원 이름 (첫 글자를 아바타에 표시)
 * @param size     아바타 크기 (dp)
 * @param modifier Modifier
 */
@Composable
fun ProfileAvatar(
    name: String,
    size: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(PrimaryLight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = name.firstOrNull()?.toString() ?: "?",
            color      = Primary,
            fontWeight = FontWeight.Bold,
            fontSize   = (size * 0.4f).sp
        )
    }
}

// PhotoUrl 파라미터를 받는 레거시 시그니처 (하위 호환용 - 이름으로 위임)
@Composable
fun ProfileAvatar(
    size: Int,
    photoUrl: String? = null,
    name: String = "?",
    modifier: Modifier = Modifier
) {
    ProfileAvatar(name = name, size = size, modifier = modifier)
}
