package com.example.whoareyou.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.whoareyou.BuildConfig
import com.example.whoareyou.R
import com.example.whoareyou.model.Employee
import com.example.whoareyou.model.EmployeeRepository
import com.example.whoareyou.ui.theme.*

// ─── 메뉴 카드 색상 ──────────────────────────────────────────────────────────
private val ColorTeal = Color(0xFF00B8D9)   // 전화번호 추가
private val ColorRose = Color(0xFFE91E8C)   // 통화내역

// ─── Glass Card 디자인 토큰 (iOS 26 Liquid Glass 스타일) ────────────────────
private val GlassCardShape   = RoundedCornerShape(20.dp)
private val GlassCardBg      = Color(0xFFF5F5F7)   // 거의 흰색, 중성 그레이 (블루 틴트 없음)
private val GlassShadowAmb   = Color(0xFFAAAAAA)   // 중성 앰비언트 그림자
private val GlassShadowSpot  = Color(0xFF888888)   // 중성 스팟 그림자
private val GlassHighlight   = Color(0xFFE8E8E8)    // 카드 테두리 (옅은 회색)

@Composable
fun HomeScreen(
    loggedInEmployee: Employee,
    onLogout: () -> Unit,
    onNavigateToDetail: (Employee) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToTeam: () -> Unit,
    onNavigateToOrgChart: () -> Unit,
    onNavigateToAddPhone: () -> Unit,
    onNavigateToCallHistory: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    // 내 프로필 상태: 로그인 정보로 초기화 후 상세 API로 이미지 로드
    var currentEmployee by remember { mutableStateOf(loggedInEmployee) }

    // 내 프로필 이미지 비동기 로드
    // 로그인 응답(AsisLoginParser)에는 이미지 정보가 없으므로 별도로 detail API 조회
    LaunchedEffect(loggedInEmployee.empNo) {
        val detail = EmployeeRepository.getDetail(loggedInEmployee.empNo)
        if (detail != null) currentEmployee = detail
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── 상단 헤더 ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter            = painterResource(id = R.drawable.top_logo),
                    contentDescription = "BC카드 로고",
                    modifier           = Modifier.height(50.dp),
                    contentScale       = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text       = "후아유 임직원 서비스",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick        = { showLogoutDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, "로그아웃", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("로그아웃", fontSize = 12.sp, color = TextSecondary)
                }
            }
            HorizontalDivider(color = Color(0xFFEEF0F4), thickness = 1.dp)
        }

        // ── 콘텐츠 영역 ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MyProfileCard(
                employee = currentEmployee,
                modifier = Modifier.fillMaxWidth(),
                onClick  = { onNavigateToDetail(currentEmployee) }
            )
            Text(
                text       = "바로가기",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = TextSecondary,
                modifier   = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeMenuCard(icon = Icons.Default.Star,   title = "즐겨찾기",     color = AccentOrange, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onNavigateToFavorites)
                HomeMenuCard(icon = Icons.Default.Person, title = "팀원보기",     color = AccentBlue,   modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onNavigateToTeam)
            }
            Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeMenuCard(icon = Icons.Default.Search, title = "검색",         color = AccentGreen,  modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onNavigateToSearch)
                HomeMenuCard(icon = Icons.Default.List,   title = "조직도",       color = AccentPurple, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onNavigateToOrgChart)
            }
            Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeMenuCard(icon = Icons.Default.Add,   title = "전화번호 추가", color = ColorTeal,    modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onNavigateToAddPhone)
                HomeMenuCard(icon = Icons.Default.Phone, title = "통화내역",      color = ColorRose,    modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onNavigateToCallHistory)
            }
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
    // Glass Card: 그림자 → 흰색 하이라이트 테두리 → 클립 → 연쿨그레이 배경
    Box(
        modifier = modifier
            .clickable { onClick() }
            .shadow(
                elevation    = 14.dp,
                shape        = GlassCardShape,
                ambientColor = GlassShadowAmb.copy(alpha = 0.45f),
                spotColor    = GlassShadowSpot.copy(alpha = 0.38f)
            )
            .border(2.dp, GlassHighlight, GlassCardShape)
            .clip(GlassCardShape)
            .background(GlassCardBg)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 아바타
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                ProfileAvatar(
                    name    = employee.name,
                    size    = 56,
                    imgdata = employee.imgdata
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                // 이름 + 닉네임
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = employee.name,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = TextPrimary
                    )
                    if (employee.nickname.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Primary.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text       = employee.nickname,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 팀 + 직책
                Text(
                    text       = if (employee.position.isNotBlank())
                                     "${employee.team}  ·  ${employee.position}"
                                 else employee.team,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF666677)
                )
                if (employee.jobTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(7.dp))
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
                            tint = BadgeText,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text       = employee.jobTitle,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = BadgeText
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint     = Color(0xFFB0B8C8),
                modifier = Modifier.size(20.dp)
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
    // Glass Card: 그림자 → 흰색 하이라이트 테두리 → 클립 → 연쿨그레이 배경
    Box(
        modifier = modifier
            .clickable { onClick() }
            .shadow(
                elevation    = 10.dp,
                shape        = GlassCardShape,
                ambientColor = GlassShadowAmb.copy(alpha = 0.42f),
                spotColor    = GlassShadowSpot.copy(alpha = 0.35f)
            )
            .border(2.dp, GlassHighlight, GlassCardShape)
            .clip(GlassCardShape)
            .background(GlassCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 아이콘 배경 박스
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(
                        elevation    = 6.dp,
                        shape        = RoundedCornerShape(15.dp),
                        ambientColor = color.copy(alpha = 0.30f),
                        spotColor    = color.copy(alpha = 0.22f)
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.80f), RoundedCornerShape(15.dp))
                    .clip(RoundedCornerShape(15.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = title,
                    tint               = color,
                    modifier           = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text       = title,
                fontSize   = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color(0xFF1A1A2E),
                textAlign  = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 공통 프로필 아바타 컴포넌트
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 직원 프로필 아바타를 표시합니다.
 *
 * [imgdata] 가 제공된 경우 실제 프로필 사진을 보여줍니다.
 * 지원 포맷:
 *   - 상대 URL: /app/ubi/photo.wru?empNo=... → BASE_URL 을 앞에 붙여 절대 URL 로 변환
 *   - 절대 URL: https://...
 *   - data URI: data:image/jpeg;base64,... → Base64 디코딩 후 표시
 *   - 순수 Base64 문자열 → 디코딩 시도
 *
 * 이미지 로드 실패 또는 [imgdata] 가 null 이면 이름 첫 글자 아바타로 폴백합니다.
 * Coil 이미지 로더를 사용하며, Coil 은 MainActivity 에서 ApiClient.okHttpClient 로
 * 초기화되어 동일한 SSL / 쿠키 설정을 공유합니다.
 *
 * @param name     직원 이름 (폴백 아바타에 표시)
 * @param size     아바타 크기 (dp)
 * @param imgdata  프로필 이미지 데이터 (URL / data URI / Base64). null 이면 폴백.
 * @param modifier Modifier
 */
@Composable
fun ProfileAvatar(
    name: String,
    size: Int,
    imgdata: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // imgdata → Coil 이 처리할 수 있는 모델로 변환
    // remember(imgdata) 로 캐싱하여 리컴포지션마다 재계산 방지
    val imageModel: Any? = remember(imgdata) {
        when {
            imgdata.isNullOrBlank() -> null

            // data URI: "data:image/jpeg;base64,/9j/..." → ByteArray
            imgdata.startsWith("data:image") -> runCatching {
                val base64Part = imgdata.substringAfter("base64,", "")
                android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                    .takeIf { it.isNotEmpty() }
            }.getOrNull()

            // 절대 URL
            imgdata.startsWith("http://") || imgdata.startsWith("https://") -> imgdata

            // 상대 URL: BASE_URL 을 앞에 붙여 절대 URL 로 변환
            imgdata.startsWith("/") ->
                "${BuildConfig.BASE_URL.trimEnd('/')}$imgdata"

            // 순수 Base64 문자열 시도
            else -> runCatching {
                android.util.Base64.decode(imgdata, android.util.Base64.DEFAULT)
                    .takeIf { it.isNotEmpty() }
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageModel != null) {
            // 이미지 로드 시도: 성공하면 실제 사진, 실패하면 문자 아바타로 폴백
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageModel)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                modifier     = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    else -> ProfileLetterAvatar(name = name, size = size)
                }
            }
        } else {
            ProfileLetterAvatar(name = name, size = size)
        }
    }
}

/**
 * 이름 첫 글자를 원형 배경에 표시하는 폴백 아바타.
 * 이미지 없거나 로드 실패 시 사용됩니다.
 */
@Composable
private fun ProfileLetterAvatar(name: String, size: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
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

// PhotoUrl 파라미터를 받는 레거시 시그니처 (하위 호환용)
@Composable
fun ProfileAvatar(
    size: Int,
    photoUrl: String? = null,
    name: String = "?",
    modifier: Modifier = Modifier
) {
    ProfileAvatar(name = name, size = size, imgdata = photoUrl, modifier = modifier)
}
