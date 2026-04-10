package com.example.whoareyou.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whoareyou.model.Employee
import com.example.whoareyou.model.EmployeeRepository
import com.example.whoareyou.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 즐겨찾기 화면
 *
 * 화면 진입 시 API 에서 즐겨찾기 목록을 로드합니다.
 * 별 버튼 클릭 시 즐겨찾기 토글 API 를 호출하고 목록을 새로고침합니다.
 *
 * @param onNavigateToDetail 직원 행 클릭 시 상세 화면으로 이동. empNo 를 전달합니다.
 * @param onBack             뒤로가기 콜백
 */
@Composable
fun FavoritesScreen(
    onNavigateToDetail: (String) -> Unit,  // empNo
    onBack: () -> Unit
) {
    // ── 상태 변수 ────────────────────────────────────────────────────────────
    var favorites by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope     = rememberCoroutineScope()

    // ── 즐겨찾기 목록 로드 ────────────────────────────────────────────────────
    // 화면 진입 시 한 번 API 호출
    LaunchedEffect(Unit) {
        isLoading = true
        favorites = EmployeeRepository.getMyFavorites()
        isLoading = false
    }

    // ── UI 렌더링 ────────────────────────────────────────────────────────────
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
                text       = "즐겨찾기",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
        }

        when {
            // 로딩 중
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            // 즐겨찾기 없음
            favorites.isEmpty() -> {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier         = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "즐겨찾기가 없습니다", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "팀원보기에서 별표를 눌러\n즐겨찾기에 추가하세요", fontSize = 14.sp, color = TextSecondary)
                    // 디버그 정보 (문제 진단용 — 확인 후 제거)
                    val dbgHtml  = EmployeeRepository.debugLastHtml
                    val dbgError = EmployeeRepository.debugLastError
                    if (dbgError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "⚠ $dbgError", fontSize = 11.sp, color = Color.Red)
                    }
                    if (dbgHtml.isNotEmpty()) {
                        Text(text = dbgHtml, fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
            // 즐겨찾기 목록 표시
            else -> {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(favorites) { employee ->
                        FavoriteEmployeeRow(
                            employee         = employee,
                            onToggleFavorite = {
                                // 즐겨찾기 토글: API 호출 후 목록 새로고침
                                scope.launch {
                                    EmployeeRepository.toggleFavorite(employee.empNo)
                                    // 토글 후 목록 다시 로드
                                    favorites = EmployeeRepository.getMyFavorites()
                                }
                            },
                            onClick = { onNavigateToDetail(employee.empNo) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 즐겨찾기 직원 행 컴포넌트
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 즐겨찾기 목록에서 한 직원을 표시하는 카드 컴포넌트.
 *
 * 아바타 + 이름 + 직책 + 팀명 · 닉네임 + 별(즐겨찾기 해제) + 전화 버튼을 표시합니다.
 *
 * @param employee         표시할 직원 정보
 * @param onToggleFavorite 즐겨찾기 토글 콜백
 * @param onClick          카드 클릭(상세 이동) 콜백
 */
@Composable
fun FavoriteEmployeeRow(
    employee: Employee,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 프로필 행
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 아바타: 이름 첫 글자
                ProfileAvatar(name = employee.name, size = 46)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // 이름 + 직책 뱃지
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = employee.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEEEEEE))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(text = employee.position, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    // 팀명 · 닉네임
                    Text(text = "${employee.team} · ${employee.nickname}", fontSize = 12.sp, color = TextSecondary)
                }
                // 별 버튼 (현재 즐겨찾기 = 채워진 별 → 해제 가능)
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "즐겨찾기 해제", tint = AccentOrange, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 전화 버튼 (사내전화 초록 / 휴대전화 레드)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${employee.internalPhone}"))
                        context.startActivity(intent)
                    },
                    modifier       = Modifier.weight(1f).height(28.dp),
                    shape          = RoundedCornerShape(8.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = SoftCallGreen),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "사내전화", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${employee.mobilePhone}"))
                        context.startActivity(intent)
                    },
                    modifier       = Modifier.weight(1f).height(28.dp),
                    shape          = RoundedCornerShape(8.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "휴대전화", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
