package com.example.whoareyou.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
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
 * 팀원 보기 화면
 *
 * 화면 진입 시 로그인한 직원의 팀원 목록을 API 에서 로드합니다.
 * 즐겨찾기 버튼 클릭 시 즉시 API 를 호출해 상태를 토글합니다.
 *
 * @param onNavigateToDetail 팀원 행 클릭 시 상세 화면으로 이동. empNo 를 전달합니다.
 * @param onBack             뒤로가기 콜백
 */
@Composable
fun TeamScreen(
    onNavigateToDetail: (String) -> Unit,  // empNo (String)
    onBack: () -> Unit
) {
    // ── 상태 변수 ────────────────────────────────────────────────────────────
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope     = rememberCoroutineScope()

    // ── 팀원 목록 로드 ─────────────────────────────────────────────────────
    // 화면 진입 시 한 번 API 호출
    LaunchedEffect(Unit) {
        isLoading = true
        employees = EmployeeRepository.getMyTeam()
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
                text       = "팀원보기",
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
            // 팀원 없음
            employees.isEmpty() -> {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "팀원 정보가 없습니다", fontSize = 16.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    // 디버그 정보 (문제 진단용 — 데이터 수신 확인 후 제거)
                    val dbgHtml  = EmployeeRepository.debugLastHtml
                    val dbgError = EmployeeRepository.debugLastError
                    if (dbgError.isNotEmpty()) {
                        Text(text = "⚠ $dbgError", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Red)
                    }
                    if (dbgHtml.isNotEmpty()) {
                        Text(text = dbgHtml, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            // 팀원 목록 (팀명별 그룹)
            else -> {
                val groupedByTeam = employees.groupBy { it.team }
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    groupedByTeam.forEach { (teamName, teamMembers) ->
                        item {
                            // 팀 헤더: 좌측 Primary 수직 바
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Primary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = teamName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${teamMembers.size}명", fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                        items(teamMembers) { employee ->
                            // 즐겨찾기 토글을 위한 로컬 상태 (isFavorite)
                            var isFav by remember(employee.empNo) { mutableStateOf(employee.isFavorite) }
                            TeamMemberRow(
                                employee         = employee.copy(isFavorite = isFav),
                                onToggleFavorite = {
                                    scope.launch {
                                        val newFav = EmployeeRepository.toggleFavorite(employee.empNo, isFav)
                                        isFav = newFav
                                    }
                                },
                                onClick = { onNavigateToDetail(employee.empNo) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                modifier  = Modifier.padding(horizontal = 20.dp),
                                color     = Background
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 팀원 행 카드 컴포넌트
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 팀원 목록에서 한 직원을 표시하는 카드 컴포넌트.
 *
 * 아바타(이름 첫 글자) + 이름 + 직책 + 팀명·닉네임 + 즐겨찾기 버튼 + 전화 버튼을 표시합니다.
 *
 * @param employee         표시할 직원 정보
 * @param onToggleFavorite 즐겨찾기 토글 콜백
 * @param onClick          카드 클릭(상세 이동) 콜백
 */
@Composable
fun TeamMemberRow(
    employee: Employee,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
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
                ProfileAvatar(name = employee.name, size = 46, imgdata = employee.imgdata)
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
                // 즐겨찾기 별 버튼
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector        = if (employee.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = "즐겨찾기",
                        tint               = if (employee.isFavorite) AccentOrange else TextSecondary.copy(alpha = 0.4f),
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 전화 버튼 행
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
