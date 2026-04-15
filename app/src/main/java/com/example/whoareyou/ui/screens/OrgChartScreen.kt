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
import com.example.whoareyou.network.dto.Dept
import com.example.whoareyou.network.dto.OrgSection
import com.example.whoareyou.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 조직도 화면
 *
 * organizaion API 응답의 accordion 구조를 파싱하여 섹션별로 표시합니다.
 *  - 각 섹션(CEO / 감사 / 노동조합 등)은 탭하면 확장/축소됩니다.
 *  - 확장 시: 책임자 직원 프로필(있으면) + 하위 부서 목록을 표시합니다.
 *  - 하위 부서 행 탭 → 해당 부서 팀원 목록을 로드하여 인라인으로 표시합니다.
 *  - 팀원 행 탭 → [onNavigateToDetail] 으로 직원 상세 화면으로 이동합니다.
 *
 * @param onNavigateToDetail 직원 행 클릭 시 empNo 를 전달하며 상세 화면으로 이동.
 * @param onBack             상단 뒤로가기 버튼 콜백.
 */
@Composable
fun OrgChartScreen(
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    // ── 상태 변수 ────────────────────────────────────────────────────────────
    var orgSections  by remember { mutableStateOf<List<OrgSection>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(true) }
    // 섹션 확장 상태 (섹션명 → 확장 여부)
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }
    // 하위 부서 팀원 캐시 (deptCode → 직원 목록)
    val deptMembers  = remember { mutableStateMapOf<String, List<Employee>>() }
    val loadingDepts = remember { mutableStateMapOf<String, Boolean>() }
    val scope        = rememberCoroutineScope()

    // ── 조직 섹션 로드 ─────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        isLoading   = true
        orgSections = EmployeeRepository.getOrgSections("")
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Background)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Text(
                text       = "조직도",
                modifier   = Modifier.weight(1f),
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                textAlign  = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.size(38.dp))
        }

        when {
            // 로딩 중
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            // 데이터 없음
            orgSections.isEmpty() -> {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "조직 정보를 불러올 수 없습니다", fontSize = 15.sp, color = TextSecondary)
                    val dbgError = EmployeeRepository.debugLastError
                    if (dbgError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = dbgError, fontSize = 11.sp, color = Color.Red)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        scope.launch {
                            isLoading   = true
                            orgSections = EmployeeRepository.getOrgSections("")
                            isLoading   = false
                        }
                    }) {
                        Text("다시 시도", color = Primary)
                    }
                }
            }
            // 조직도 표시
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    item {
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
                    }

                    items(orgSections, key = { it.name }) { section ->
                        val isExpanded = expandedSections[section.name] == true

                        OrgSectionCard(
                            section    = section,
                            isExpanded = isExpanded,
                            onToggle   = {
                                val expanding = !isExpanded
                                expandedSections[section.name] = expanding
                                // 직접 코드가 있고 하위 부서가 없으면 탭 시 팀원 로드
                                if (expanding && section.deptCode.isNotBlank() && section.subDepts.isEmpty()
                                    && deptMembers[section.deptCode] == null
                                    && loadingDepts[section.deptCode] != true
                                ) {
                                    scope.launch {
                                        loadingDepts[section.deptCode] = true
                                        deptMembers[section.deptCode]  = EmployeeRepository.getTeamByOrgCd(section.deptCode)
                                        loadingDepts.remove(section.deptCode)
                                    }
                                }
                            },
                            deptMembers   = deptMembers,
                            loadingDepts  = loadingDepts,
                            onDeptToggle  = { dept ->
                                val alreadyLoaded = deptMembers.containsKey(dept.deptCode)
                                if (alreadyLoaded) {
                                    deptMembers.remove(dept.deptCode)
                                } else if (loadingDepts[dept.deptCode] != true) {
                                    scope.launch {
                                        loadingDepts[dept.deptCode] = true
                                        deptMembers[dept.deptCode]  = EmployeeRepository.getTeamByOrgCd(dept.deptCode)
                                        loadingDepts.remove(dept.deptCode)
                                    }
                                }
                            },
                            onEmployeeClick = onNavigateToDetail
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OrgSectionCard — 섹션 헤더 + 확장 내용 (책임자 카드 + 하위 부서 목록)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OrgSectionCard(
    section: OrgSection,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    deptMembers: Map<String, List<Employee>>,
    loadingDepts: Map<String, Boolean>,
    onDeptToggle: (Dept) -> Unit,
    onEmployeeClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // 섹션 헤더
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onToggle() },
            shape     = RoundedCornerShape(if (isExpanded) 12.dp else 12.dp),
            colors    = CardDefaults.cardColors(
                containerColor = if (isExpanded) Primary.copy(alpha = 0.10f) else CardBackground
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isExpanded) Primary else Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint               = if (isExpanded) Color.White else Primary,
                        modifier           = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = section.name,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isExpanded) Primary else TextPrimary
                    )
                    if (!isExpanded && section.subDepts.isNotEmpty()) {
                        Text(
                            text     = "하위 조직 ${section.subDepts.size}개",
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                }
                Icon(
                    imageVector        = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint               = TextSecondary
                )
            }
        }

        // 확장 내용
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)
            ) {
                // 책임자 직원 프로필 카드 (있으면)
                section.headEmployee?.let { emp ->
                    HeadEmployeeCard(
                        employee  = emp,
                        onClick   = { onEmployeeClick(emp.empNo) }
                    )
                }

                // 하위 부서 목록
                if (section.subDepts.isNotEmpty()) {
                    section.subDepts.forEach { dept ->
                        val members   = deptMembers[dept.deptCode]
                        val isLoading = loadingDepts[dept.deptCode] == true
                        val isOpen    = members != null

                        SubDeptRow(
                            dept     = dept,
                            isOpen   = isOpen,
                            isLoading = isLoading,
                            onToggle = { onDeptToggle(dept) }
                        )

                        // 팀원 목록
                        if (isOpen) {
                            if (members!!.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, bottom = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(text = "소속 직원이 없습니다", fontSize = 13.sp, color = TextSecondary)
                                }
                            } else {
                                members.forEach { employee ->
                                    EmployeeRow(
                                        employee = employee,
                                        onClick  = { onEmployeeClick(employee.empNo) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 직접 코드가 있고 하위 부서 없음 → 섹션 자체의 팀원 표시 (예: 노동조합)
                if (section.deptCode.isNotBlank() && section.subDepts.isEmpty()) {
                    val members   = deptMembers[section.deptCode]
                    val isLoading = loadingDepts[section.deptCode] == true
                    when {
                        isLoading -> {
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Primary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            }
                        }
                        members != null && members.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp)) {
                                Text(text = "소속 직원이 없습니다", fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                        members != null -> {
                            members.forEach { employee ->
                                EmployeeRow(
                                    employee = employee,
                                    onClick  = { onEmployeeClick(employee.empNo) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HeadEmployeeCard — 섹션 책임자 직원 카드
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeadEmployeeCard(
    employee: Employee,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(name = employee.name, size = 60, imgdata = employee.imgdata)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = employee.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                val subtitle = buildString {
                    if (employee.position.isNotBlank()) append(employee.position)
                    if (employee.team.isNotBlank()) {
                        if (isNotEmpty()) append("  /  ")
                        append(employee.team)
                    }
                }
                if (subtitle.isNotBlank()) {
                    Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
                }
                if (employee.internalPhone.isNotBlank() || employee.mobilePhone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (employee.internalPhone.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${employee.internalPhone}"))
                                    context.startActivity(intent)
                                },
                                modifier      = Modifier.height(32.dp),
                                shape         = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors        = ButtonDefaults.outlinedButtonColors(contentColor = SoftCallGreen)
                            ) {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "사내", fontSize = 11.sp)
                            }
                        }
                        if (employee.mobilePhone.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${employee.mobilePhone}"))
                                    context.startActivity(intent)
                                },
                                modifier      = Modifier.height(32.dp),
                                shape         = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors        = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                            ) {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "휴대", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SubDeptRow — 하위 부서 행 (팀원 확장/축소)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubDeptRow(
    dept: Dept,
    isOpen: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isOpen) AccentBlue.copy(alpha = 0.08f) else Background)
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Default.SubdirectoryArrowRight,
            contentDescription = null,
            tint               = AccentBlue.copy(alpha = 0.6f),
            modifier           = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text       = dept.deptName,
            fontSize   = 14.sp,
            fontWeight = if (isOpen) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (isOpen) AccentBlue else TextPrimary,
            modifier   = Modifier.weight(1f)
        )
        if (isLoading) {
            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector        = if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint               = TextSecondary.copy(alpha = 0.7f),
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}
