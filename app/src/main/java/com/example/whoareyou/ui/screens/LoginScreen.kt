package com.example.whoareyou.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
 * 로그인 화면 (ASIS 서버 연동)
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
 *   두 번호가 일치해야만 로그인이 허용됩니다.
 *   → 하이픈 없이 숫자만 입력 (예: 01012345678)
 * ──────────────────────────────────────────────────────────────────────────────
 *
 * @param onLoginSuccess 로그인 성공 시 호출되는 콜백. 로그인한 직원 정보를 전달합니다.
 */
@Composable
fun LoginScreen(onLoginSuccess: (Employee) -> Unit) {

    // ── 상태 변수 ────────────────────────────────────────────────────────────
    var empNo        by remember { mutableStateOf("") }
    var passwd       by remember { mutableStateOf("") }
    var phoneNo      by remember { mutableStateOf("") }   // HRMS 등록 휴대폰번호
    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // ── 로그인 로직 ──────────────────────────────────────────────────────────
    /**
     * ASIS 서버 로그인 처리.
     *
     * 처리 순서:
     *   1. 입력값 유효성 검사
     *   2. 전화번호 정규화 (하이픈/공백 제거)
     *   3. ASIS loginAsis() API 호출 → HTML 응답
     *   4. AsisLoginParser 로 HTML 에서 JSON 추출
     *   5. AuthManager 에 세션 저장 (authKey, empNo, orgCd, empNm)
     *   6. onLoginSuccess 콜백 호출
     */
    fun doLogin() {
        if (empNo.isBlank() || passwd.isBlank() || phoneNo.isBlank()) {
            errorMessage = "사번, 비밀번호, 휴대폰번호를 모두 입력해주세요."
            return
        }

        // 전화번호 정규화: 하이픈·공백·국가코드 제거 → 숫자만
        val normalizedPhone = phoneNo
            .replace(Regex("[^0-9]"), "")       // 숫자 외 제거
            .let { if (it.startsWith("82")) "0${it.substring(2)}" else it }  // +82 → 0

        if (normalizedPhone.length < 10) {
            errorMessage = "올바른 휴대폰번호를 입력해주세요.\n(예: 01012345678)"
            return
        }

        scope.launch {
            isLoading = true
            try {
                // ASIS 서버 호출 (HTML 응답)
                val responseBody = ApiClient.api.loginAsis(
                    empNo   = empNo.trim(),
                    passwd  = passwd,
                    phoneNo = normalizedPhone
                )
                val html = responseBody.string()

                // HTML 에서 authKey·직원정보 추출
                val loginData = AsisLoginParser.parse(
                    html        = html,
                    inputEmpNo  = empNo.trim()
                )

                if (loginData != null && loginData.authKey.isNotBlank()) {
                    // ✅ 로그인 성공: 세션 저장
                    AuthManager.saveSession(
                        authKey = loginData.authKey,
                        empNo   = loginData.empNo,
                        orgCd   = loginData.orgCd,   // ASIS 는 빈 문자열 (orgCd 미제공)
                        empNm   = loginData.empNm,
                        phoneNo = normalizedPhone     // search.wru 호출 시 매번 필요
                    )
                    // LoginDataDto → Employee 변환 후 콜백 호출
                    onLoginSuccess(loginData.toEmployee())
                } else {
                    // ❌ 로그인 실패: HTML 에서 에러 메시지 추출
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
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            // BC카드 로고
            Image(
                painter            = painterResource(id = R.drawable.login_logo),
                contentDescription = "BC카드 로고",
                modifier           = Modifier.size(240.dp),
                contentScale       = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text       = "BC후아유",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text     = "임직원 정보 서비스",
                fontSize = 14.sp,
                color    = TextSecondary
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 로그인 입력 카드
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 사번 입력
                    InputField(
                        icon          = Icons.Default.Person,
                        placeholder   = "사번",
                        value         = empNo,
                        onValueChange = { empNo = it },
                        keyboardType  = KeyboardType.Number
                    )

                    // 비밀번호 입력
                    InputField(
                        icon          = Icons.Default.Lock,
                        placeholder   = "비밀번호",
                        value         = passwd,
                        onValueChange = { passwd = it },
                        isSecure      = true
                    )

                    // 휴대폰번호 입력 (HRMS 등록 번호)
                    InputField(
                        icon          = Icons.Default.Phone,
                        placeholder   = "HRMS 등록 휴대폰번호 (예: 01012345678)",
                        value         = phoneNo,
                        onValueChange = { phoneNo = it },
                        keyboardType  = KeyboardType.Phone
                    )

                    // 전화번호 안내 문구
                    Text(
                        text     = "※ HRMS에 등록된 휴대폰번호와 일치해야 로그인됩니다.",
                        fontSize = 11.sp,
                        color    = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // 로그인 버튼
                    Button(
                        onClick  = { if (!isLoading) doLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape   = RoundedCornerShape(16.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = Primary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text       = "로그인",
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
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
            title            = { Text("알림") },
            text             = { Text(errorMessage ?: "") },
            confirmButton    = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("확인", color = Primary)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 공통 입력 필드 컴포넌트
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 아이콘 + TextField 로 구성된 입력 필드.
 *
 * @param icon          왼쪽 아이콘
 * @param placeholder   힌트 텍스트
 * @param value         현재 입력값
 * @param onValueChange 입력값 변경 콜백
 * @param keyboardType  키보드 타입 (기본: Text)
 * @param isSecure      비밀번호 마스킹 여부
 */
@Composable
fun InputField(
    icon: ImageVector,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isSecure: Boolean = false
) {
    val resolvedKeyboardType = if (isSecure) KeyboardType.Password else keyboardType

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = Primary,
            modifier           = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        TextField(
            value                = value,
            onValueChange        = onValueChange,
            placeholder          = { Text(text = placeholder, color = TextSecondary) },
            singleLine           = true,
            visualTransformation = if (isSecure) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions      = KeyboardOptions(keyboardType = resolvedKeyboardType),
            colors               = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
