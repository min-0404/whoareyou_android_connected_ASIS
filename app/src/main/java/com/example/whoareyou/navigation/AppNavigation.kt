package com.example.whoareyou.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.whoareyou.model.Employee
import com.example.whoareyou.network.AuthManager
import com.example.whoareyou.ui.screens.*

// ─────────────────────────────────────────────────────────────────────────────
// 네비게이션 라우트
// ─────────────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object Home           : Screen("home")
    object EmployeeDetail : Screen("employee_detail/{empNo}") {
        fun createRoute(empNo: String) = "employee_detail/$empNo"
    }
    object Search         : Screen("search")
    object Favorites      : Screen("favorites")
    object Team           : Screen("team")
    object OrgChart       : Screen("org_chart")
    object Settings       : Screen("settings")
    object Info           : Screen("info")
    object PhoneSettings  : Screen("phone_settings")
    object AddPhone       : Screen("add_phone")
    object CallHistory    : Screen("call_history")
    object OrgTeamMembers : Screen("org_team/{orgCd}/{deptName}") {
        fun createRoute(orgCd: String, deptName: String) =
            "org_team/${java.net.URLEncoder.encode(orgCd, "UTF-8")}/${java.net.URLEncoder.encode(deptName, "UTF-8")}"
    }
}

// 하단 내비바를 표시할 주요 화면 목록
private val MAIN_ROUTES = setOf(
    Screen.Home.route,
    Screen.Info.route,
    Screen.Settings.route
)

// 하단 내비바 탭 정의
private data class NavTab(val route: String, val icon: ImageVector, val label: String)
private val NAV_TABS = listOf(
    NavTab(Screen.Info.route,     Icons.Default.Info,     "가이드"),
    NavTab(Screen.Home.route,     Icons.Default.Home,     "홈"),
    NavTab(Screen.Settings.route, Icons.Default.Settings, "설정")
)

// ─────────────────────────────────────────────────────────────────────────────
// AppNavigation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var loggedInEmployee by remember { mutableStateOf<Employee?>(null) }

    val startDestination = if (AuthManager.isLoggedIn()) {
        if (loggedInEmployee == null) loggedInEmployee = buildRestoredEmployee()
        Screen.Home.route
    } else {
        Screen.Login.route
    }

    // 현재 화면 라우트 추적
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in MAIN_ROUTES

    // 내비바 선택 탭 인덱스 (라우트로부터 자동 결정)
    val selectedNavIdx = NAV_TABS.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 1

    // PersistentGlassNavBar 는 전체 화면 기준으로 위치해야 하므로
    // Scaffold 바깥쪽 Box 안에 오버레이로 배치합니다.
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        // Scaffold의 bottomBar는 콘텐츠에 패딩만 부여하는 투명 Spacer
        bottomBar = {
            if (showBottomBar) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .navigationBarsPadding()
                )
            }
        }
    ) { paddingValues ->
            // ── NavHost (전체 화면 콘텐츠) ─────────────────────────────────
            NavHost(
                navController    = navController,
                startDestination = startDestination,
                modifier         = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                // 로그인
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = { employee ->
                            loggedInEmployee = employee
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                // 홈
                composable(Screen.Home.route) {
                    val employee = loggedInEmployee ?: buildRestoredEmployee() ?: return@composable
                    HomeScreen(
                        loggedInEmployee        = employee,
                        onLogout                = {
                            loggedInEmployee = null
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToDetail      = { emp -> navController.navigate(Screen.EmployeeDetail.createRoute(emp.empNo)) },
                        onNavigateToSearch      = { navController.navigate(Screen.Search.route) },
                        onNavigateToFavorites   = { navController.navigate(Screen.Favorites.route) },
                        onNavigateToTeam        = { navController.navigate(Screen.Team.route) },
                        onNavigateToOrgChart    = { navController.navigate(Screen.OrgChart.route) },
                        onNavigateToAddPhone    = { navController.navigate(Screen.AddPhone.route) },
                        onNavigateToCallHistory = { navController.navigate(Screen.CallHistory.route) }
                    )
                }

                // 직원 상세
                composable(
                    route     = Screen.EmployeeDetail.route,
                    arguments = listOf(navArgument("empNo") { type = NavType.StringType })
                ) { entry ->
                    val empNo = entry.arguments?.getString("empNo") ?: return@composable
                    EmployeeDetailScreen(empNo = empNo, onBack = { navController.popBackStack() })
                }

                // 검색
                composable(Screen.Search.route) {
                    SearchScreen(
                        onNavigateToDetail = { empNo -> navController.navigate(Screen.EmployeeDetail.createRoute(empNo)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 즐겨찾기
                composable(Screen.Favorites.route) {
                    FavoritesScreen(
                        onNavigateToDetail = { empNo -> navController.navigate(Screen.EmployeeDetail.createRoute(empNo)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 팀원보기
                composable(Screen.Team.route) {
                    TeamScreen(
                        onNavigateToDetail = { empNo -> navController.navigate(Screen.EmployeeDetail.createRoute(empNo)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 조직도
                composable(Screen.OrgChart.route) {
                    OrgChartScreen(
                        onNavigateToDetail = { empNo -> navController.navigate(Screen.EmployeeDetail.createRoute(empNo)) },
                        onNavigateToTeam   = { orgCd, deptName ->
                            navController.navigate(Screen.OrgTeamMembers.createRoute(orgCd, deptName))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 조직도 팀원 목록 (조직도 → 팀 클릭 시 이동)
                composable(
                    route     = Screen.OrgTeamMembers.route,
                    arguments = listOf(
                        navArgument("orgCd")    { type = NavType.StringType },
                        navArgument("deptName") { type = NavType.StringType }
                    )
                ) { entry ->
                    val orgCd    = java.net.URLDecoder.decode(
                        entry.arguments?.getString("orgCd") ?: "", "UTF-8"
                    )
                    val deptName = java.net.URLDecoder.decode(
                        entry.arguments?.getString("deptName") ?: "", "UTF-8"
                    )
                    OrgTeamMembersScreen(
                        orgCd              = orgCd,
                        deptName           = deptName,
                        onNavigateToDetail = { empNo -> navController.navigate(Screen.EmployeeDetail.createRoute(empNo)) },
                        onBack             = { navController.popBackStack() }
                    )
                }

                // 설정
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack                    = { navController.popBackStack() },
                        onLogout                  = {
                            loggedInEmployee = null
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToInfo          = { navigateMainTab(navController, Screen.Info.route) },
                        onNavigateToPhoneSettings = { navController.navigate(Screen.PhoneSettings.route) }
                    )
                }

                // 가이드
                composable(Screen.Info.route) {
                    InfoScreen(onBack = { navController.popBackStack() })
                }

                // 전화번호 설정
                composable(Screen.PhoneSettings.route) {
                    PhoneSettingsScreen(onBack = { navController.popBackStack() })
                }

                // 전화번호 추가
                composable(Screen.AddPhone.route) {
                    AddPhoneScreen(onBack = { navController.popBackStack() })
                }

                // 통화내역
                composable(Screen.CallHistory.route) {
                    CallHistoryScreen(onBack = { navController.popBackStack() })
                }
            }
    } // Scaffold end

    // ── 플로팅 글래스 내비바 오버레이 (전체 화면 기준 BottomCenter) ───────
    if (showBottomBar) {
        PersistentGlassNavBar(
            modifier     = Modifier.align(Alignment.BottomCenter),
            selectedIdx  = selectedNavIdx,
            onSelect     = { idx ->
                navigateMainTab(navController, NAV_TABS[idx].route)
            }
        )
    }
    } // outer Box end
}

// ─────────────────────────────────────────────────────────────────────────────
// 주요 탭 간 내비게이션 헬퍼 (백스택 중복 방지)
// ─────────────────────────────────────────────────────────────────────────────

private fun navigateMainTab(navController: androidx.navigation.NavController, route: String) {
    navController.navigate(route) {
        // Home 을 루트로 유지, 그 위의 스택을 정리하며 상태 보존
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState    = true
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 퍼시스턴트 플로팅 글래스 내비바
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 모든 주요 화면에서 유지되는 플로팅 글래스 내비게이션 바.
 *
 * 투명한 프로스티드 글래스 스타일:
 *   - 밝은 반투명 흰색 배경 (배경이 은은하게 비침)
 *   - 흰색 상단 테두리 (유리 반사 표현)
 *   - 선택 pill: Spring 애니메이션으로 부드럽게 이동
 *   - 아이콘/텍스트: 다크 (밝은 배경에 어울리도록)
 */
@Composable
fun PersistentGlassNavBar(
    modifier:    Modifier = Modifier,
    selectedIdx: Int,
    onSelect:    (Int) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 14.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                // 부드러운 그림자 (유리 띄우는 효과)
                .shadow(
                    elevation    = 18.dp,
                    shape        = RoundedCornerShape(32.dp),
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor    = Color.Black.copy(alpha = 0.14f)
                )
                .clip(RoundedCornerShape(32.dp))
                // 프로스티드 흰색 글래스 배경
                .background(Color.White.copy(alpha = 0.82f))
                // 흰색 테두리 = 유리 상단 반사 하이라이트
                .border(1.2.dp, Color.White, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp)
        ) {
            val tabWidth = maxWidth / NAV_TABS.size

            // 슬라이딩 선택 pill (매우 미세한 다크 배경)
            val pillOffsetX by animateDpAsState(
                targetValue   = tabWidth * selectedIdx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                ),
                label = "nav_pill_offset"
            )
            Box(
                modifier = Modifier
                    .padding(vertical = 7.dp)
                    .offset(x = pillOffsetX)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1A1A2E).copy(alpha = 0.08f))
            )

            // 탭 아이템
            Row(
                modifier              = Modifier.fillMaxSize(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NAV_TABS.forEachIndexed { idx, tab ->
                    val isSelected = idx == selectedIdx
                    val tint by animateColorAsState(
                        targetValue   = if (isSelected) Color(0xFF1A1A2E) else Color(0xFFAAAAAA),
                        animationSpec = tween(200),
                        label         = "nav_tint_$idx"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) { onSelect(idx) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text       = tab.label,
                            fontSize   = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = tint
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AuthManager 저장 정보로 Employee 복원
// ─────────────────────────────────────────────────────────────────────────────

private fun buildRestoredEmployee(): Employee? {
    val empNo = AuthManager.loginEmpNo ?: return null
    return Employee(
        empNo         = empNo,
        name          = AuthManager.loginEmpNm ?: "",
        team          = "",
        teamCode      = AuthManager.loginOrgCd ?: "",
        position      = "",
        nickname      = "",
        jobTitle      = "",
        internalPhone = "",
        mobilePhone   = "",
        fax           = "",
        email         = "",
        imgdata       = null,
        isFavorite    = false
    )
}
