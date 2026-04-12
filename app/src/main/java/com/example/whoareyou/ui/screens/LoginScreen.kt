package com.example.whoareyou.ui.screens

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whoareyou.R
import com.example.whoareyou.model.Employee
import com.example.whoareyou.network.ApiClient
import com.example.whoareyou.network.AsisLoginParser
import com.example.whoareyou.network.AuthManager
import com.example.whoareyou.network.dto.toEmployee
import com.example.whoareyou.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 로그인 화면 (ASIS 서버 연동) — 모던 리디자인
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * ASIS 서버 로그인 방식:
 *   - POST /app/ubi/member.wru (actnKey=login, isApp=Y)
 *   - 서버가 HTML 로 응답하며, HTML 안에 JSON 이 임베딩되어 있음
 *   - [AsisLoginParser] 로 HTML 에서 authKey 추출
 *   - 성공 시 [AuthManager] 에 세션 저장
 *
 * 전화번호 입력 필요 이유:
 *   ASIS 서버는 로그인 시 HRMS에 등록된 휴대폰번호와 입력한 번호를 비교합니다.
 *   세 칸(010 · XXXX · XXXX)에 나눠 입력하며, 앞 칸이 채워지면 자동으로 다음 칸으로 이동합니다.
 * ──────────────────────────────────────────────────────────────────────────────
 *
 * @param onLoginSuccess 로그인 성공 시 호출되는 콜백. 로그인한 직원 정보를 전달합니다.
 */
@Composable
fun LoginScreen(onLoginSuccess: (Employee) -> Unit) {

    // ── 상태 변수 ────────────────────────────────────────────────────────────
    var empNo        by remember { mutableStateOf("") }
    var passwd       by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // 전화번호 세 칸 (010 · XXXX · XXXX)
    var phone1 by remember { mutableStateOf("") }   // 최대 3자리
    var phone2 by remember { mutableStateOf("") }   // 최대 4자리
    var phone3 by remember { mutableStateOf("") }   // 최대 4자리

    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // 포커스 요청기
    val focusPhone2 = remember { FocusRequester() }
    val focusPhone3 = remember { FocusRequester() }

    // phone1 이 3자리 채워지면 → phone2 로 자동 이동
    LaunchedEffect(phone1.length) {
        if (phone1.length >= 3) {
            try { focusPhone2.requestFocus() } catch (_: Exception) {}
        }
    }
    // phone2 가 4자리 채워지면 → phone3 로 자동 이동
    LaunchedEffect(phone2.length) {
        if (phone2.length >= 4) {
            try { focusPhone3.requestFocus() } catch (_: Exception) {}
        }
    }

    // ── 로그인 로직 ──────────────────────────────────────────────────────────
    fun doLogin() {
        val normalizedPhone = "${phone1}${phone2}${phone3}"
            .replace(Regex("[^0-9]"), "")
            .let { if (it.startsWith("82")) "0${it.substring(2)}" else it }

        if (empNo.isBlank() || passwd.isBlank()) {
            errorMessage = "사번과 비밀번호를 입력해주세요."
            return
        }
        if (normalizedPhone.length < 10) {
            errorMessage = "올바른 휴대폰번호를 입력해주세요.\n(예: 010 · 1234 · 5678)"
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
                    onLoginSuccess(loginData.toEmployee())
                } else {
                    errorMessage = AsisLoginParser.parseErrorMessage(html)
                }
            } catch (e: Exception) {
                errorMessage = "서버 연결에 실패했습니다.\n네트워크 상태를 확인해주세요."
            } finally {
                isLoading = false
            }
        }
    }

    // ── UI 렌더링 ────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── 로고 영역 ────────────────────────────────────────────────────
            Image(
                painter            = painterResource(id = R.drawable.login_logo),
                contentDescription = "BC카드 로고",
                modifier           = Modifier.size(300.dp),
                contentScale       = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(2.dp))

            // ── 앱 제목: BC(붉은 그라디언트) + 후아유(검정) ─────────────────
            // buildAnnotatedString 으로 한 Text 안에서 처리해야 베이스라인이 정확히 일치합니다.
            val bcBrush = Brush.horizontalGradient(listOf(Color(0xFFE53935), Color(0xFFFF6B6B)))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(brush = bcBrush, fontWeight = FontWeight.ExtraBold)) {
                        append("BC")
                    }
                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.ExtraBold)) {
                        append("후아유")
                    }
                },
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text          = "임직원 정보 검색 서비스",
                fontSize      = 12.sp,
                color         = TextSecondary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 로그인 카드 ──────────────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // 사번
                    ModernInputField(
                        icon        = Icons.Default.Person,
                        placeholder = "사번",
                        value       = empNo,
                        onValueChange = { empNo = it.filter { c -> c.isDigit() } },
                        keyboardType  = KeyboardType.Number,
                        imeAction     = ImeAction.Next
                    )

                    // 비밀번호
                    ModernInputField(
                        icon          = Icons.Default.Lock,
                        placeholder   = "비밀번호",
                        value         = passwd,
                        onValueChange = { passwd = it },
                        isSecure      = !showPassword,
                        imeAction     = ImeAction.Next,
                        trailingContent = {
                            IconButton(
                                onClick  = { showPassword = !showPassword },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showPassword) "숨기기" else "보기",
                                    tint     = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )

                    // 구분선
                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // 전화번호 레이블
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Default.Phone,
                            contentDescription = null,
                            tint               = Primary,
                            modifier           = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text       = "HRMS 등록 휴대폰번호",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color      = TextSecondary
                        )
                    }

                    // ── 전화번호 세 칸 (높이 54dp 고정) ─────────────────────
                    Row(
                        modifier            = Modifier.fillMaxWidth().height(54.dp),
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 칸 1: 010 (최대 3자리)
                        PhoneSegmentField(
                            value         = phone1,
                            onValueChange = { if (it.length <= 3) phone1 = it.filter { c -> c.isDigit() } },
                            placeholder   = "010",
                            modifier      = Modifier.weight(2.5f).fillMaxHeight(),
                            imeAction     = ImeAction.Next,
                            onImeAction   = { try { focusPhone2.requestFocus() } catch (_: Exception) {} }
                        )

                        Text("—", fontSize = 16.sp, color = TextSecondary, fontWeight = FontWeight.Light)

                        // 칸 2: XXXX (최대 4자리)
                        PhoneSegmentField(
                            value         = phone2,
                            onValueChange = { if (it.length <= 4) phone2 = it.filter { c -> c.isDigit() } },
                            placeholder   = "0000",
                            modifier      = Modifier
                                .weight(3f)
                                .fillMaxHeight()
                                .focusRequester(focusPhone2),
                            imeAction     = ImeAction.Next,
                            onImeAction   = { try { focusPhone3.requestFocus() } catch (_: Exception) {} }
                        )

                        Text("—", fontSize = 16.sp, color = TextSecondary, fontWeight = FontWeight.Light)

                        // 칸 3: XXXX (최대 4자리)
                        PhoneSegmentField(
                            value         = phone3,
                            onValueChange = { if (it.length <= 4) phone3 = it.filter { c -> c.isDigit() } },
                            placeholder   = "0000",
                            modifier      = Modifier
                                .weight(3f)
                                .fillMaxHeight()
                                .focusRequester(focusPhone3),
                            imeAction     = ImeAction.Done,
                            onImeAction   = { if (!isLoading) doLogin() }
                        )
                    }

                    // 안내 문구
                    Text(
                        text     = "HRMS에 등록된 번호와 일치해야 로그인됩니다",
                        fontSize = 11.sp,
                        color    = TextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 로그인 버튼 (붉은색 배경 + 흰색 글자)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (!isLoading) Color(0xFFE53935)
                                else Color(0xFFE53935).copy(alpha = 0.5f)
                            )
                            .then(
                                if (!isLoading) Modifier.clickableNoRipple { doLogin() }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text          = "로그인",
                                fontSize      = 16.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // 에러 다이얼로그
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = Color.White,
            title            = { Text("알림", fontWeight = FontWeight.Bold) },
            text             = { Text(errorMessage ?: "", fontSize = 14.sp, color = TextSecondary) },
            confirmButton    = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("확인", color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 모던 입력 필드 (BasicTextField 기반)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModernInputField(
    icon: ImageVector,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isSecure: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)                          // ← 모든 입력칸 동일 높이
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF7F8FC))
            .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = Primary,
            modifier           = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            singleLine    = true,
            textStyle     = TextStyle(
                fontSize   = 15.sp,
                color      = TextPrimary,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush   = SolidColor(Primary),
            visualTransformation = if (isSecure) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isSecure) KeyboardType.Password else keyboardType,
                imeAction    = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction?.invoke() },
                onDone = { onImeAction?.invoke() }
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier         = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, fontSize = 15.sp, color = TextSecondary.copy(alpha = 0.6f))
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.weight(1f)
        )
        trailingContent?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 전화번호 단일 세그먼트 필드
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhoneSegmentField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null
) {
    BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        singleLine    = true,
        textStyle     = TextStyle(
            fontSize     = 16.sp,
            color        = TextPrimary,
            fontWeight   = FontWeight.Medium,
            letterSpacing = 2.sp
        ),
        cursorBrush   = SolidColor(Primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction    = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() },
            onDone = { onImeAction?.invoke() }
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF7F8FC))
                    .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (value.isEmpty()) {
                    Text(
                        text      = placeholder,
                        fontSize  = 15.sp,
                        color     = TextSecondary.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                }
                innerTextField()
            }
        },
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 클릭 리플 없이 클릭 처리 (그라디언트 버튼용)
// ─────────────────────────────────────────────────────────────────────────────

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication        = null,
        onClick           = onClick
    )
}
