package com.example.whoareyou.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.whoareyou.network.dto.Dept
import com.example.whoareyou.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 조직도 화면
 *
 * 화면 진입 시 API 에서 최상위 조직 목록을 로드합니다.
 * 조직 행을 클릭하면 해당 조직의 팀원 목록을 API 에서 로드하여 확장 표시합니다.
 *
 * @param onNavigateToDetail 직원 행 클릭 시 상세 화면으로 이동. empNo 를 전달합니다.
 * @param onBack             뒤로가기 콜백
 */
@Composable
fun OrgChartScreen(
    onNavigateToDetail: (String) -> Unit,  // empNo (String)
    onBack: () -> Unit
) {
    // ── 상태 변수 ────────────────────────────────────────────────────────────
    var departments by remember { mutableStateOf<List<Dept>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    // 확장된 팀의 팀원 목록 캐시: deptCode → List<Employee>
    val expandedTeamMembers = remember { mutableStateMapOf<String, List<Employee>>() }
    val loadingDepts        = remember { mutableStateMapOf<String, Boolean>() }
    val scope               = rememberCoroutineScope()

    // ── 조직 목록 로드 ─────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        isLoading   = true
        departments = EmployeeRepository.getOrganization("")
        isLoading   = false
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
            Text(text = "조직도", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        when {
            // 로딩 중
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            // 데이터 없음
            departments.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "조직 정보를 불러올 수 없습니다", fontSize = 15.sp, color = TextSecondary)
                    // 디버그 정보 (문제 진단용 — 확인 후 제거)
                    val dbgHtml  = EmployeeRepository.debugLastHtml
                    val dbgError = EmployeeRepository.debugLastError
                    if (dbgError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "⚠ $dbgError", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Red)
                    }
                    if (dbgHtml.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = dbgHtml, fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
            // 조직도 트리 표시
            else -> {
                // 회사 헤더 카드
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "비씨카드", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "BC Card Co., Ltd.", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }

                LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(departments, key = { it.deptCode }) { dept ->
                        val isExpanded = expandedTeamMembers.containsKey(dept.deptCode)
                        val isLoadingDept = loadingDepts[dept.deptCode] == true
                        val members = expandedTeamMembers[dept.deptCode] ?: emptyList()

                        // 조직 행 (접기/펼치기)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .clickable {
                                    if (isExpanded) {
                                        // 이미 열린 경우 접기
                                        expandedTeamMembers.remove(dept.deptCode)
                                    } else {
                                        // 처음 열 때 팀원 목록 로드
                                        if (!isLoadingDept) {
                                            scope.launch {
                                                loadingDepts[dept.deptCode] = true
                                                val empList = EmployeeRepository.getTeamByOrgCd(dept.deptCode)
                                                expandedTeamMembers[dept.deptCode] = empList
                                                loadingDepts.remove(dept.deptCode)
                                            }
                                        }
                                    }
                                },
                            shape  = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExpanded) Primary.copy(alpha = 0.08f) else CardBackground
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.List, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = dept.deptName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    if (isExpanded && members.isNotEmpty()) {
                                        Text(text = "팀원 ${members.size}명", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                                if (isLoadingDept) {
                                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }

                        // 팀원 목록 (확장 시 표시)
                        if (isExpanded) {
                            if (members.isEmpty() && !isLoadingDept) {
                                // 팀원 없음
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(text = "팀원 정보가 없습니다", fontSize = 13.sp, color = TextSecondary)
                                }
                            } else {
                                members.forEach { employee ->
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
        }
    }
}
