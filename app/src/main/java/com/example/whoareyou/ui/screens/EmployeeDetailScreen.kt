package com.example.whoareyou.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whoareyou.model.Employee
import com.example.whoareyou.model.EmployeeRepository
import com.example.whoareyou.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 직원 상세 화면
 *
 * 화면 진입 시 [empNo] 로 API 에서 최신 직원 정보를 로드합니다.
 * 즐겨찾기 버튼 클릭 시 API 를 호출해 상태를 토글하고 UI 를 즉시 반영합니다.
 *
 * @param empNo  조회할 직원의 사번 (네비게이션 인자)
 * @param onBack 뒤로가기 콜백
 */
@Composable
fun EmployeeDetailScreen(
    empNo: String,
    onBack: () -> Unit
) {
    // ── 상태 변수 ────────────────────────────────────────────────────────────
    var employee  by remember { mutableStateOf<Employee?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope     = rememberCoroutineScope()
    val context   = LocalContext.current

    // ── 직원 상세 로드 ─────────────────────────────────────────────────────
    // 화면 진입 시 한 번 API 호출
    LaunchedEffect(empNo) {
        isLoading = true
        employee  = EmployeeRepository.getDetail(empNo)
        isLoading = false
    }

    // ── UI 렌더링 ────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 상단 바: 뒤로가기 + 이름 + 즐겨찾기
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 뒤로가기 원형 버튼
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
            // 이름 타이틀 (가운데 정렬)
            Text(
                text       = employee?.name ?: "",
                modifier   = Modifier.weight(1f),
                textAlign  = TextAlign.Center,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
            // 즐겨찾기 원형 버튼
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Background)
                    .clickable(enabled = employee != null) {
                        employee?.let { emp ->
                            scope.launch {
                                val newFav = EmployeeRepository.toggleFavorite(emp.empNo, emp.isFavorite)
                                employee   = emp.copy(isFavorite = newFav)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = if (employee?.isFavorite == true) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "즐겨찾기",
                    tint               = if (employee?.isFavorite == true) AccentOrange else TextSecondary.copy(alpha = 0.5f),
                    modifier           = Modifier.size(20.dp)
                )
            }
        }

        when {
            // 로딩 중
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            // 데이터 없음 (오류)
            employee == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "직원 정보를 불러올 수 없습니다", fontSize = 15.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            scope.launch {
                                isLoading = true
                                employee  = EmployeeRepository.getDetail(empNo)
                                isLoading = false
                            }
                        }) {
                            Text("다시 시도", color = Primary)
                        }
                    }
                }
            }
            // 정상 데이터 표시
            else -> {
                val emp = employee!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 프로필 카드
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ProfileAvatar(name = emp.name, size = 100, imgdata = emp.imgdata)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = emp.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text      = "${emp.team} ${emp.position} / ${emp.nickname}",
                                fontSize  = 14.sp,
                                color     = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            // 직무 뱃지
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(BadgeBackground)
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = BadgeText, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = emp.jobTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BadgeText)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            // 전화 버튼 행
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${emp.internalPhone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = SoftCallGreen)
                                ) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "사내전화", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${emp.mobilePhone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "휴대전화", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 연락처 정보 카드
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            ContactInfoRow(
                                icon     = Icons.Default.Phone,
                                label    = "사내전화",
                                value    = emp.internalPhone,
                                isLast   = false,
                                onArrowClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${emp.internalPhone}"))
                                    context.startActivity(intent)
                                }
                            )
                            ContactInfoRow(
                                icon     = Icons.Default.Smartphone,
                                label    = "휴대전화",
                                value    = emp.mobilePhone,
                                isLast   = false,
                                onArrowClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${emp.mobilePhone}"))
                                    context.startActivity(intent)
                                }
                            )
                            ContactInfoRow(
                                icon     = Icons.Default.Print,
                                label    = "팩스",
                                value    = emp.fax,
                                isLast   = false,
                                onArrowClick = null
                            )
                            ContactInfoRow(
                                icon     = Icons.Default.Email,
                                label    = "이메일",
                                value    = emp.email,
                                isLast   = true,
                                onArrowClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${emp.email}"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 연락처 정보 행 컴포넌트
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 아이콘 + 레이블 + 값 + 실행 버튼으로 구성된 연락처 정보 행.
 *
 * @param icon         왼쪽 아이콘
 * @param label        필드명 (예: "사내전화")
 * @param value        표시할 값
 * @param isLast       마지막 항목 여부 (Divider 표시 여부)
 * @param onArrowClick 오른쪽 버튼 클릭 콜백. null 이면 버튼을 표시하지 않습니다.
 */
@Composable
fun ContactInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isLast: Boolean = false,
    onArrowClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 원형 아이콘 배경
        Box(
            modifier         = Modifier.size(40.dp).clip(CircleShape).background(Primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
            Text(text = value.ifBlank { "—" }, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        // 실행 버튼 (전화, 이메일 등)
        if (onArrowClick != null && value.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Background)
                    .clickable { onArrowClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Launch, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
    if (!isLast) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Background)
    }
}
