package com.example.whoareyou.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whoareyou.R
import com.example.whoareyou.model.Employee
import com.example.whoareyou.network.ApiClient
import com.example.whoareyou.network.ApiConstants
import com.example.whoareyou.network.AsisLoginParser
import com.example.whoareyou.network.AuthManager
import com.example.whoareyou.network.dto.toEmployee
import com.example.whoareyou.ui.theme.*
import kotlinx.coroutines.launch
import java.nio.charset.Charset

// ─────────────────────────────────────────────────────────────────────────────
// 색상 상수
// ─────────────────────────────────────────────────────────────────────────────
private val BcRed        = Color(0xFFE53935)
private val FieldBg      = Color(0xFFEEEEF0)   // 입력 필드 중성 라이트그레이 (iOS26)
private val FieldBorder  = Color.White           // 입력 필드 하이라이트 테두리

// Glass 카드 토큰 (iOS 26 Liquid Glass 스타일: 중성 화이트, 블루 틴트 없음)
private val GlassCardBg    = Color(0xFFF5F5F7)   // 거의 흰색에 가까운 중성 그레이
private val GlassShadowAmb = Color(0xFFAAAAAA)   // 중성 그림자
private val GlassShadowSpt = Color(0xFF888888)   // 중성 스팟 그림자

// ─────────────────────────────────────────────────────────────────────────────
// LoginScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 로그인 화면 (슬라이딩 탭: 로그인 ↔ 비밀번호 초기화)
 *
 * @param onLoginSuccess 로그인 성공 시 콜백
 */
@Composable
fun LoginScreen(onLoginSuccess: (Employee) -> Unit) {

    // ── 탭 상태 (0 = 로그인, 1 = 비밀번호 초기화) ────────────────────────
    var selectedTab by remember { mutableIntStateOf(0) }

    // ── 에러 다이얼로그 ───────────────────────────────────────────────────
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var dialogTitle   by remember { mutableStateOf("알림") }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── 로고 ──────────────────────────────────────────────────────
            Image(
                painter            = painterResource(id = R.drawable.login_logo),
                contentDescription = "BC카드 로고",
                modifier           = Modifier.size(150.dp),
                contentScale       = ContentScale.Fit
            )

            // ── 앱 타이틀 ─────────────────────────────────────────────────
            val bcBrush = Brush.horizontalGradient(listOf(Color(0xFFE53935), Color(0xFFFF6B6B)))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(brush = bcBrush, fontWeight = FontWeight.ExtraBold)) { append("BC") }
                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.ExtraBold)) { append("후아유") }
                },
                fontSize = 26.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text          = "임직원 정보 검색 서비스",
                fontSize      = 12.sp,
                color         = TextSecondary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 카드 섹션 (Glass Card) ────────────────────────────────────
            // 흰색 배경 위에서 뚜렷이 보이도록:
            // 그림자(깊이감) → 흰색 2dp 하이라이트 테두리 → 클립 → 연쿨그레이 배경
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation    = 18.dp,
                        shape        = RoundedCornerShape(28.dp),
                        ambientColor = GlassShadowAmb.copy(alpha = 0.45f),
                        spotColor    = GlassShadowSpt.copy(alpha = 0.38f)
                    )
                    .border(2.dp, Color.White, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(GlassCardBg)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // ── 슬라이딩 탭 선택기 ────────────────────────────────
                    SlidingTabSelector(
                        tabs       = listOf("로그인", "비밀번호 초기화"),
                        selectedIdx = selectedTab,
                        onTabSelect = { selectedTab = it }
                    )

                    // ── 슬라이딩 콘텐츠 ───────────────────────────────────
                    AnimatedContent(
                        targetState   = selectedTab,
                        transitionSpec = {
                            val toRight = targetState > initialState
                            (slideInHorizontally(
                                initialOffsetX  = { if (toRight) it else -it },
                                animationSpec   = tween(300, easing = EaseOutCubic)
                            ) + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(
                                targetOffsetX   = { if (toRight) -it else it },
                                animationSpec   = tween(300, easing = EaseOutCubic)
                            ) + fadeOut(tween(150)))
                        },
                        label = "login_tab_content"
                    ) { tab ->
                        when (tab) {
                            0 -> LoginContent(
                                onSuccess = onLoginSuccess,
                                onError   = { title, msg ->
                                    dialogTitle   = title
                                    dialogMessage = msg
                                }
                            )
                            else -> PasswordResetContent(
                                onSuccess = {
                                    dialogTitle   = "완료"
                                    dialogMessage = "비밀번호가 성공적으로 변경되었습니다.\n새 비밀번호로 로그인해주세요."
                                    selectedTab   = 0
                                },
                                onError = { title, msg ->
                                    dialogTitle   = title
                                    dialogMessage = msg
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    // ── 알림 다이얼로그 ───────────────────────────────────────────────────────
    if (dialogMessage != null) {
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = Color.White,
            title            = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
            text             = { Text(dialogMessage ?: "", fontSize = 14.sp, color = TextSecondary) },
            confirmButton    = {
                TextButton(onClick = { dialogMessage = null }) {
                    Text("확인", color = BcRed, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 슬라이딩 글래스 탭 선택기 (로그인 화면용)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SlidingTabSelector(
    tabs:       List<String>,
    selectedIdx: Int,
    onTabSelect: (Int) -> Unit
) {
    // 카드와 동일한 Glass 스타일 탭 바 (iOS26: 중성 그레이)
    val barBg     = Color(0xFFE8E8EA)   // 카드보다 살짝 어두운 중성 그레이
    val barBorder = Color.White
    val pillColor = Color(0xFFF5F5F7)   // 선택된 탭 pill – 카드와 동일한 밝은 흰색

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp),
                ambientColor = GlassShadowAmb.copy(alpha = 0.40f),
                spotColor    = GlassShadowSpt.copy(alpha = 0.32f))
            .border(2.dp, barBorder, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(barBg)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / tabs.size

        // 슬라이딩 pill
        val offsetX by animateDpAsState(
            targetValue   = tabWidth * selectedIdx,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow
            ),
            label = "login_tab_pill"
        )
        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(pillColor)
        )

        // 레이블
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { idx, label ->
                val isSelected = idx == selectedIdx
                val textColor by animateColorAsState(
                    targetValue   = if (isSelected) Color(0xFF1A1A2E) else Color(0xFF888888),
                    animationSpec = tween(200),
                    label         = "login_tab_text_$idx"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onTabSelect(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label,
                        color      = textColor,
                        fontSize   = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 로그인 탭 콘텐츠
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginContent(
    onSuccess: (Employee) -> Unit,
    onError:   (String, String) -> Unit
) {
    var empNo        by remember { mutableStateOf("") }
    var passwd       by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var phone1       by remember { mutableStateOf("") }
    var phone2       by remember { mutableStateOf("") }
    var phone3       by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }

    val scope        = rememberCoroutineScope()
    val focusPhone2  = remember { FocusRequester() }
    val focusPhone3  = remember { FocusRequester() }

    LaunchedEffect(phone1.length) {
        if (phone1.length >= 3) try { focusPhone2.requestFocus() } catch (_: Exception) {}
    }
    LaunchedEffect(phone2.length) {
        if (phone2.length >= 4) try { focusPhone3.requestFocus() } catch (_: Exception) {}
    }

    fun doLogin() {
        val normalizedPhone = "${phone1}${phone2}${phone3}"
            .replace(Regex("[^0-9]"), "")
            .let { if (it.startsWith("82")) "0${it.substring(2)}" else it }

        if (empNo.isBlank() || passwd.isBlank()) {
            onError("알림", "사번과 비밀번호를 입력해주세요.")
            return
        }
        if (normalizedPhone.length < 10) {
            onError("알림", "올바른 휴대폰번호를 입력해주세요.\n(예: 010 · 1234 · 5678)")
            return
        }
        scope.launch {
            isLoading = true
            try {
                val responseBody = ApiClient.api.loginAsis(
                    empNo   = empNo.trim(),
                    passwd  = passwd,
                    phoneNo = normalizedPhone
                )
                val html = responseBody.string()
                val loginData = AsisLoginParser.parse(html = html, inputEmpNo = empNo.trim())

                if (loginData != null && loginData.authKey.isNotBlank()) {
                    AuthManager.saveSession(
                        authKey = loginData.authKey,
                        empNo   = loginData.empNo,
                        orgCd   = loginData.orgCd,
                        empNm   = loginData.empNm,
                        phoneNo = normalizedPhone
                    )
                    onSuccess(loginData.toEmployee())
                } else {
                    onError("로그인 실패", AsisLoginParser.parseErrorMessage(html))
                }
            } catch (e: Exception) {
                onError("연결 오류", "서버 연결에 실패했습니다.\n네트워크 상태를 확인해주세요.")
            } finally {
                isLoading = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 사번
        LoginField(
            icon          = Icons.Default.Person,
            placeholder   = "사번",
            value         = empNo,
            onValueChange = { empNo = it.filter { c -> c.isDigit() } },
            keyboardType  = KeyboardType.Number,
            imeAction     = ImeAction.Next
        )

        // 비밀번호
        LoginField(
            icon          = Icons.Default.Lock,
            placeholder   = "비밀번호",
            value         = passwd,
            onValueChange = { passwd = it },
            isSecure      = !showPassword,
            imeAction     = ImeAction.Next,
            trailingIcon  = {
                IconButton(
                    onClick  = { showPassword = !showPassword },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector        = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint               = TextSecondary,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
        )

        HorizontalDivider(color = FieldBorder)

        // 전화번호 레이블
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Phone, null, tint = BcRed, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text("HRMS 등록 휴대폰번호", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        }

        // 전화번호 3칸
        Row(
            modifier              = Modifier.fillMaxWidth().height(44.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PhoneField(
                value         = phone1,
                onValueChange = { if (it.length <= 3) phone1 = it.filter { c -> c.isDigit() } },
                placeholder   = "010",
                modifier      = Modifier.weight(2.5f).fillMaxHeight(),
                imeAction     = ImeAction.Next,
                onImeAction   = { try { focusPhone2.requestFocus() } catch (_: Exception) {} }
            )
            Text("—", fontSize = 15.sp, color = TextSecondary)
            PhoneField(
                value         = phone2,
                onValueChange = { if (it.length <= 4) phone2 = it.filter { c -> c.isDigit() } },
                placeholder   = "0000",
                modifier      = Modifier.weight(3f).fillMaxHeight().focusRequester(focusPhone2),
                imeAction     = ImeAction.Next,
                onImeAction   = { try { focusPhone3.requestFocus() } catch (_: Exception) {} }
            )
            Text("—", fontSize = 15.sp, color = TextSecondary)
            PhoneField(
                value         = phone3,
                onValueChange = { if (it.length <= 4) phone3 = it.filter { c -> c.isDigit() } },
                placeholder   = "0000",
                modifier      = Modifier.weight(3f).fillMaxHeight().focusRequester(focusPhone3),
                imeAction     = ImeAction.Done,
                onImeAction   = { if (!isLoading) doLogin() }
            )
        }

        Text(
            text     = "HRMS에 등록된 번호와 일치해야 로그인됩니다",
            fontSize = 11.sp,
            color    = TextSecondary.copy(alpha = 0.6f)
        )

        // 로그인 버튼
        ActionButton(
            text      = "로그인",
            isLoading = isLoading,
            enabled   = !isLoading,
            onClick   = { doLogin() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 비밀번호 초기화 탭 콘텐츠
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PasswordResetContent(
    onSuccess: () -> Unit,
    onError:   (String, String) -> Unit
) {
    var empNo       by remember { mutableStateOf("") }
    var phoneNo     by remember { mutableStateOf("") }
    var newPwd      by remember { mutableStateOf("") }
    var confirmPwd  by remember { mutableStateOf("") }
    var motpValue   by remember { mutableStateOf("") }
    var showNewPwd  by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var isLoading   by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // 휴대폰번호 정규화: 하이픈·공백 제거 후 숫자만
    fun normalizePhone(raw: String) = raw.replace(Regex("[^0-9]"), "")

    fun doReset() {
        val normalizedPhone = normalizePhone(phoneNo)
        when {
            empNo.isBlank()            -> { onError("알림", "사번을 입력해주세요."); return }
            normalizedPhone.length < 10 -> { onError("알림", "HRMS에 등록된 휴대폰번호를 입력해주세요."); return }
            newPwd.isBlank()           -> { onError("알림", "새 비밀번호를 입력해주세요."); return }
            newPwd.length < 8          -> { onError("알림", "비밀번호는 8자 이상 입력해주세요."); return }
            newPwd != confirmPwd        -> { onError("알림", "비밀번호가 일치하지 않습니다."); return }
            motpValue.isBlank()        -> { onError("알림", "MOTP 값을 입력해주세요."); return }
        }
        scope.launch {
            isLoading = true
            try {
                val responseBody = ApiClient.api.changePassword(
                    actnKey = ApiConstants.ACTN_CHANGE_PWD,
                    empNo   = empNo.trim(),
                    phoneNo = normalizePhone(phoneNo),
                    newPwd  = newPwd,
                    motp    = motpValue.trim()
                )
                val bytes = responseBody.bytes()
                val html  = try { String(bytes, Charset.forName("EUC-KR")) }
                            catch (_: Exception) { String(bytes, Charsets.UTF_8) }

                val errorMsg = AsisLoginParser.parsePasswordReset(html)
                if (errorMsg == null) {
                    onSuccess()
                } else {
                    onError("초기화 실패", errorMsg)
                }
            } catch (e: Exception) {
                onError("연결 오류", "서버 연결에 실패했습니다.\n네트워크 상태를 확인해주세요.")
            } finally {
                isLoading = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 안내 문구
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BcRed.copy(alpha = 0.06f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Info, null, tint = BcRed, modifier = Modifier.size(15.dp).padding(top = 1.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                text       = "HRMS에 등록된 휴대폰번호와 MOTP 앱의 6~8자리 OTP 값을 입력하세요.\n비밀번호는 영문+숫자+특수문자 조합 8자 이상입니다.",
                fontSize   = 11.sp,
                color      = BcRed.copy(alpha = 0.8f),
                lineHeight = 17.sp
            )
        }

        // 사번
        LoginField(
            icon          = Icons.Default.Person,
            placeholder   = "사번",
            value         = empNo,
            onValueChange = { empNo = it.filter { c -> c.isDigit() } },
            keyboardType  = KeyboardType.Number,
            imeAction     = ImeAction.Next
        )

        // 휴대폰번호 (HRMS 등록 번호)
        LoginField(
            icon          = Icons.Default.Phone,
            placeholder   = "휴대폰번호 (010-0000-0000)",
            value         = phoneNo,
            onValueChange = { input ->
                // 숫자만 받고 최대 11자리로 제한
                val digits = input.filter { c -> c.isDigit() }.take(11)
                // 자동 하이픈 삽입: 010-XXXX-XXXX
                phoneNo = when {
                    digits.length <= 3  -> digits
                    digits.length <= 7  -> "${digits.substring(0,3)}-${digits.substring(3)}"
                    else                -> "${digits.substring(0,3)}-${digits.substring(3,7)}-${digits.substring(7)}"
                }
            },
            keyboardType  = KeyboardType.Phone,
            imeAction     = ImeAction.Next
        )

        // 새 비밀번호
        LoginField(
            icon          = Icons.Default.Lock,
            placeholder   = "새 비밀번호",
            value         = newPwd,
            onValueChange = { newPwd = it },
            isSecure      = !showNewPwd,
            imeAction     = ImeAction.Next,
            trailingIcon  = {
                IconButton(onClick = { showNewPwd = !showNewPwd }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (showNewPwd) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null, tint = TextSecondary, modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        // 비밀번호 확인
        LoginField(
            icon          = Icons.Default.Lock,
            placeholder   = "비밀번호 확인",
            value         = confirmPwd,
            onValueChange = { confirmPwd = it },
            isSecure      = !showConfirm,
            imeAction     = ImeAction.Next,
            trailingIcon  = {
                IconButton(onClick = { showConfirm = !showConfirm }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (showConfirm) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null, tint = if (confirmPwd.isNotEmpty() && confirmPwd == newPwd)
                            Color(0xFF4CAF50) else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        // MOTP 입력
        LoginField(
            icon          = Icons.Default.Security,
            placeholder   = "MOTP (모바일 OTP)",
            value         = motpValue,
            onValueChange = { motpValue = it.filter { c -> c.isDigit() } },
            keyboardType  = KeyboardType.Number,
            imeAction     = ImeAction.Done,
            onImeAction   = { if (!isLoading) doReset() }
        )

        // 비밀번호 일치 상태 표시
        if (confirmPwd.isNotEmpty()) {
            val match = newPwd == confirmPwd
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (match) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint     = if (match) Color(0xFF4CAF50) else Color(0xFFE53935),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text     = if (match) "비밀번호가 일치합니다" else "비밀번호가 일치하지 않습니다",
                    fontSize = 11.sp,
                    color    = if (match) Color(0xFF4CAF50) else Color(0xFFE53935)
                )
            }
        }

        // 초기화 버튼
        ActionButton(
            text      = "비밀번호 초기화",
            isLoading = isLoading,
            enabled   = !isLoading,
            onClick   = { doReset() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 공통 입력 필드 (BasicTextField 기반, 54dp 고정)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginField(
    icon:          ImageVector,
    placeholder:   String,
    value:         String,
    onValueChange: (String) -> Unit,
    keyboardType:  KeyboardType    = KeyboardType.Text,
    isSecure:      Boolean         = false,
    imeAction:     ImeAction       = ImeAction.Next,
    onImeAction:   (() -> Unit)?   = null,
    trailingIcon:  (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(5.dp, RoundedCornerShape(14.dp),
                ambientColor = GlassShadowAmb.copy(alpha = 0.30f),
                spotColor    = GlassShadowSpt.copy(alpha = 0.24f))
            .border(1.5.dp, Color.White, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(FieldBg)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = BcRed, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(11.dp))
        BasicTextField(
            value                = value,
            onValueChange        = onValueChange,
            singleLine           = true,
            textStyle            = TextStyle(fontSize = 15.sp, color = TextPrimary),
            cursorBrush          = SolidColor(BcRed),
            visualTransformation = if (isSecure) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions      = KeyboardOptions(
                keyboardType = if (isSecure) KeyboardType.Password else keyboardType,
                imeAction    = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction?.invoke() },
                onDone = { onImeAction?.invoke() }
            ),
            decorationBox = { innerTextField ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 15.sp, color = TextSecondary.copy(alpha = 0.55f))
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.weight(1f)
        )
        trailingIcon?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 전화번호 세그먼트 필드
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhoneField(
    value:         String,
    onValueChange: (String) -> Unit,
    placeholder:   String,
    modifier:      Modifier       = Modifier,
    imeAction:     ImeAction      = ImeAction.Next,
    onImeAction:   (() -> Unit)?  = null
) {
    BasicTextField(
        value           = value,
        onValueChange   = onValueChange,
        singleLine      = true,
        textStyle       = TextStyle(fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp),
        cursorBrush     = SolidColor(BcRed),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() },
            onDone = { onImeAction?.invoke() }
        ),
        decorationBox   = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(5.dp, RoundedCornerShape(10.dp),
                        ambientColor = GlassShadowAmb.copy(alpha = 0.30f),
                        spotColor    = GlassShadowSpt.copy(alpha = 0.24f))
                    .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(FieldBg)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.4f))
                }
                innerTextField()
            }
        },
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 공통 액션 버튼 (빨간색)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionButton(
    text:      String,
    isLoading: Boolean,
    enabled:   Boolean,
    onClick:   () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) BcRed else BcRed.copy(alpha = 0.45f))
            .then(if (enabled) Modifier.clickableNoRipple(onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
        } else {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.8.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 리플 없는 클릭 modifier
// ─────────────────────────────────────────────────────────────────────────────

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication        = null,
        onClick           = onClick
    )
}
