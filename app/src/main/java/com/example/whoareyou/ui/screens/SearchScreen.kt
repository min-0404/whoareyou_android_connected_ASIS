package com.example.whoareyou.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whoareyou.model.Employee
import com.example.whoareyou.model.EmployeeRepository
import com.example.whoareyou.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 임직원 검색 화면
 *
 * 검색어 입력 시 300ms 디바운스 후 API 를 호출하여 결과를 표시합니다.
 * 검색어가 비어 있으면 안내 메시지를, 결과가 없으면 빈 결과 메시지를 표시합니다.
 *
 * @param onNavigateToDetail 직원 행 클릭 시 상세 화면으로 이동. empNo 를 전달합니다.
 * @param onBack             뒤로가기 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToDetail: (String) -> Unit,  // empNo (String)
    onBack: () -> Unit
) {
    // ── 상태 변수 ────────────────────────────────────────────────────────────
    var query     by remember { mutableStateOf("") }               // 현재 입력된 검색어
    var results   by remember { mutableStateOf<List<Employee>>(emptyList()) } // 검색 결과
    var isLoading by remember { mutableStateOf(false) }            // 로딩 중 여부

    // ── 검색 디바운스 ─────────────────────────────────────────────────────────
    // query 가 변경될 때마다 LaunchedEffect 가 재시작됩니다.
    // 300ms 대기 후 API 호출 → 빠른 타이핑 시 불필요한 API 요청을 줄입니다.
    LaunchedEffect(query) {
        if (query.isBlank()) {
            // 검색어가 없으면 결과 초기화
            results = emptyList()
            return@LaunchedEffect
        }
        // 300ms 디바운스 (마지막 입력 후 300ms 대기)
        delay(300L)
        isLoading = true
        results = EmployeeRepository.search(query)
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
                text       = "검색",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                modifier   = Modifier.weight(1f)
            )
        }

        // 검색 입력 필드
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            TextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("이름, 팀, 직급, 업무 검색", color = TextSecondary) },
                singleLine    = true,
                colors        = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                // 검색어 지우기 버튼
                IconButton(onClick = { query = "" }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "지우기", tint = TextSecondary)
                }
            }
        }

        // 상태에 따른 본문 영역 렌더링
        when {
            // 검색어 없음 → 초기 안내
            query.isBlank() -> {
                SearchEmptyPrompt()
            }
            // API 호출 중 → 로딩 인디케이터
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            // 결과 없음
            results.isEmpty() -> {
                SearchNoResults(
                    debugHtml  = EmployeeRepository.debugLastHtml,
                    debugError = EmployeeRepository.debugLastError
                )
            }
            // 검색 결과 목록
            else -> {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(results) { employee ->
                        EmployeeRow(
                            employee = employee,
                            onClick  = { onNavigateToDetail(employee.empNo) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 검색 초기 안내 상태
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchEmptyPrompt() {
    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier            = Modifier.size(72.dp).clip(CircleShape).background(PrimaryLight),
            contentAlignment    = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Primary, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "임직원을 검색해보세요", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "이름, 팀, 직급, 업무로 검색할 수 있어요", fontSize = 14.sp, color = TextSecondary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 검색 결과 없음 상태
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchNoResults(debugHtml: String = "", debugError: String = "") {
    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier         = Modifier.size(72.dp).clip(CircleShape).background(PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Primary, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "검색 결과가 없습니다", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "다른 검색어를 입력해보세요", fontSize = 14.sp, color = TextSecondary)
        // 디버그 정보 (문제 진단용 — 확인 후 제거)
        if (debugError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "⚠ $debugError", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Red)
        }
        if (debugHtml.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = debugHtml, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 직원 행 카드 컴포넌트 (검색, 조직도 등 공통 사용)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 직원 목록의 한 행을 카드 형태로 표시합니다.
 *
 * 아바타(이름 첫 글자) + 이름 + 직책 + 팀명을 표시합니다.
 *
 * @param employee 표시할 직원 정보
 * @param onClick  행 클릭 콜백
 */
@Composable
fun EmployeeRow(employee: Employee, onClick: () -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 원형 아바타: 이름 첫 글자
            ProfileAvatar(name = employee.name, size = 48)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 이름
                    Text(text = employee.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    // 직책 (파란색 텍스트)
                    Text(text = employee.position, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(2.dp))
                // 팀명
                Text(text = employee.team, fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                // 직무 뱃지
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BadgeBackground)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = employee.jobTitle, fontSize = 11.sp, color = BadgeText, fontWeight = FontWeight.Medium)
                }
            }

            Icon(
                imageVector        = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint               = TextSecondary.copy(alpha = 0.4f),
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}
