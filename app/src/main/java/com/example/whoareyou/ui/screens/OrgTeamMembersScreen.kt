package com.example.whoareyou.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
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
import kotlinx.coroutines.launch

/**
 * 조직도에서 특정 팀을 선택했을 때 표시되는 팀원 목록 화면.
 *
 * organizaion API (actnKey=organizaion) 에 [orgCd] 를 전달하여
 * 해당 팀의 goEmpDetail 블록에서 팀원 목록을 파싱합니다.
 *
 * @param orgCd    조회할 팀의 조직 코드
 * @param deptName 화면 상단에 표시할 팀명
 * @param onNavigateToDetail 팀원 클릭 시 상세 화면 이동 콜백 (empNo 전달)
 * @param onBack   뒤로가기 콜백
 */
@Composable
fun OrgTeamMembersScreen(
    orgCd: String,
    deptName: String,
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(orgCd) {
        isLoading = true
        employees = EmployeeRepository.getTeamByOrgCd(orgCd)
        isLoading = false
    }

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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = deptName,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.size(38.dp))
        }

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            employees.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "소속 직원이 없습니다", fontSize = 15.sp, color = TextSecondary)
                    val dbgError = EmployeeRepository.debugLastError
                    if (dbgError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = dbgError, fontSize = 11.sp, color = Color.Red)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        scope.launch {
                            isLoading = true
                            employees = EmployeeRepository.getTeamByOrgCd(orgCd)
                            isLoading = false
                        }
                    }) {
                        Text("다시 시도", color = Primary)
                    }
                }
            }
            else -> {
                val groupedByTeam = employees.groupBy { it.team }
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    groupedByTeam.forEach { (teamName, teamMembers) ->
                        item {
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
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                        .background(Primary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = teamName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${teamMembers.size}명",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        items(teamMembers) { employee ->
                            var isFav by remember(employee.empNo) { mutableStateOf(employee.isFavorite) }
                            TeamMemberRow(
                                employee = employee.copy(isFavorite = isFav),
                                onToggleFavorite = {
                                    scope.launch {
                                        isFav = EmployeeRepository.toggleFavorite(employee.empNo, isFav)
                                    }
                                },
                                onClick = { onNavigateToDetail(employee.empNo) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}
