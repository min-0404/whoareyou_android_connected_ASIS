package com.example.whoareyou.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whoareyou.model.Employee
import com.example.whoareyou.model.EmployeeRepository
import com.example.whoareyou.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 임직원 검색 화면
 *
 * ── 한글 입력 해결 방법 ───────────────────────────────────────────────────────
 * Material3 TextField 는 내부 포커스 리셋 로직이 한글 IME composition 을 중단시킵니다.
 * BasicTextField 를 사용하면 Compose 런타임이 IME composition 상태를 직접 관리하여
 * 한글이 정상 입력됩니다.
 * 추가로 TextFieldValue 를 사용해 커서·선택영역·composition 범위까지 보존합니다.
 *
 * ── 검색 방식 ─────────────────────────────────────────────────────────────────
 * 실시간 조회 대신 [검색] 버튼 클릭 또는 키보드 검색(완료) 키를 눌러야 API 를 호출합니다.
 */
@Composable
fun SearchScreen(
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    // TextFieldValue: IME composition 상태(커서, 선택, 조합 중인 글자)를 완전히 보존
    var queryValue  by remember { mutableStateOf(TextFieldValue("")) }
    var results     by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    val scope       = rememberCoroutineScope()
    val keyboardCtrl = LocalSoftwareKeyboardController.current

    // 검색 실행: 버튼 클릭 또는 IME 검색키
    val performSearch: () -> Unit = {
        val keyword = queryValue.text.trim()
        if (keyword.isNotBlank()) {
            keyboardCtrl?.hide()
            scope.launch {
                hasSearched = true
                isLoading   = true
                results     = EmployeeRepository.search(keyword)
                isLoading   = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── 상단 바 ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = TextPrimary)
            }
            Text(
                text       = "임직원 검색",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                modifier   = Modifier.weight(1f)
            )
        }

        // ── 검색 입력 영역 ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 입력 필드 박스
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))

                // ─── BasicTextField: 한글 IME 완전 지원 ───────────────────────
                // Material3 TextField 가 아닌 BasicTextField 를 사용해야
                // 한글 조합(ㄱ→가→갈) 중 포커스가 유지되어 글자가 끊기지 않습니다.
                BasicTextField(
                    value           = queryValue,
                    onValueChange   = { queryValue = it },
                    singleLine      = true,
                    textStyle       = TextStyle(
                        fontSize   = 15.sp,
                        color      = TextPrimary,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush     = SolidColor(Primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    decorationBox   = { innerTextField ->
                        Box(
                            modifier          = Modifier.fillMaxWidth(),
                            contentAlignment  = Alignment.CenterStart
                        ) {
                            if (queryValue.text.isEmpty()) {
                                Text(
                                    text     = "이름, 팀, 직급, 업무 입력",
                                    fontSize = 15.sp,
                                    color    = TextSecondary
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // 지우기 버튼
                if (queryValue.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick  = {
                            queryValue  = TextFieldValue("")
                            results     = emptyList()
                            hasSearched = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "지우기",
                            tint     = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 검색 버튼
            Button(
                onClick        = performSearch,
                enabled        = queryValue.text.isNotBlank(),
                shape          = RoundedCornerShape(12.dp),
                colors         = ButtonDefaults.buttonColors(
                    containerColor         = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.35f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier       = Modifier.height(52.dp)
            ) {
                Text("검색", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // ── 본문 ────────────────────────────────────────────────────────────
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            !hasSearched -> SearchEmptyPrompt()
            results.isEmpty() -> SearchNoResults(
                keyword    = queryValue.text.trim(),
                debugHtml  = EmployeeRepository.debugLastHtml,
                debugError = EmployeeRepository.debugLastError
            )
            else -> {
                Text(
                    text       = "검색 결과 ${results.size}명",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextSecondary,
                    modifier   = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(results) { employee ->
                        EmployeeRow(employee = employee, onClick = { onNavigateToDetail(employee.empNo) })
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchEmptyPrompt() {
    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier         = Modifier.size(72.dp).clip(CircleShape).background(PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Primary, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("임직원을 검색해보세요", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text       = "이름, 팀, 직급, 업무를 입력한 뒤\n검색 버튼을 눌러주세요",
            fontSize   = 14.sp,
            color      = TextSecondary,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SearchNoResults(keyword: String = "", debugHtml: String = "", debugError: String = "") {
    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier         = Modifier.size(72.dp).clip(CircleShape).background(PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Primary, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text       = if (keyword.isNotBlank()) "\"$keyword\" 검색 결과가 없습니다" else "검색 결과가 없습니다",
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("다른 검색어를 입력해보세요", fontSize = 14.sp, color = TextSecondary)
        if (debugError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("⚠ $debugError", fontSize = 11.sp, color = Color.Red)
        }
        if (debugHtml.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(debugHtml, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 직원 행 카드 (공통)
// ─────────────────────────────────────────────────────────────────────────────

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
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(name = employee.name, size = 48, imgdata = employee.imgdata)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(employee.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(employee.position, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(employee.team, fontSize = 13.sp, color = TextSecondary)
                if (employee.jobTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BadgeBackground)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(employee.jobTitle, fontSize = 11.sp, color = BadgeText, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint     = TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
