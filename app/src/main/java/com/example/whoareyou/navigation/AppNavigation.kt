package com.example.whoareyou.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.whoareyou.model.Employee
import com.example.whoareyou.network.AuthManager
import com.example.whoareyou.ui.screens.AddPhoneScreen
import com.example.whoareyou.ui.screens.CallHistoryScreen
import com.example.whoareyou.ui.screens.EmployeeDetailScreen
import com.example.whoareyou.ui.screens.FavoritesScreen
import com.example.whoareyou.ui.screens.HomeScreen
import com.example.whoareyou.ui.screens.InfoScreen
import com.example.whoareyou.ui.screens.LoginScreen
import com.example.whoareyou.ui.screens.OrgChartScreen
import com.example.whoareyou.ui.screens.PhoneSettingsScreen
import com.example.whoareyou.ui.screens.SearchScreen
import com.example.whoareyou.ui.screens.SettingsScreen
import com.example.whoareyou.ui.screens.TeamScreen

// ─────────────────────────────────────────────────────────────────────────────
// 네비게이션 라우트 정의
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 앱 내 모든 화면의 라우트 경로를 정의합니다.
 *
 * EmployeeDetail 은 사번(empNo) 을 String 타입으로 받습니다.
 * (구버전의 Int employeeId 에서 String empNo 로 변경됨)
 */
sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object Home           : Screen("home")
    object EmployeeDetail : Screen("employee_detail/{empNo}") {
        /**
         * empNo 를 포함한 실제 라우트 경로를 생성합니다.
         * @param empNo 상세 조회할 직원의 사번
         */
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
}

// ─────────────────────────────────────────────────────────────────────────────
// AppNavigation 컴포저블
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 앱 전체 네비게이션 그래프를 설정합니다.
 *
 * 앱 시작 시:
 *   - AuthManager.isLoggedIn() == true → Home 화면으로 바로 이동 (세션 복원)
 *   - AuthManager.isLoggedIn() == false → Login 화면에서 시작
 *
 * 로그인 성공 시:
 *   - onLoginSuccess 콜백에서 loggedInEmployee 상태를 설정하고 Home 으로 이동
 *
 * 이미 로그인된 상태에서 Home 화면 진입 시:
 *   - loggedInEmployee 가 null 이면 AuthManager 저장 정보로 최소 Employee 객체 생성
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // 로그인한 직원 상태 (로그인 성공 시 또는 세션 복원 시 설정됨)
    var loggedInEmployee by remember { mutableStateOf<Employee?>(null) }

    // 앱 시작 시 이미 로그인된 상태이면 AuthManager 에 저장된 정보로 Employee 복원
    val startDestination = if (AuthManager.isLoggedIn()) {
        if (loggedInEmployee == null) {
            loggedInEmployee = buildRestoredEmployee()
        }
        Screen.Home.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

        // ── 로그인 화면 ──────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { employee ->
                    // 로그인 성공: Employee 상태 저장 후 Home 으로 이동
                    loggedInEmployee = employee
                    navController.navigate(Screen.Home.route) {
                        // 로그인 화면은 백스택에서 제거 (뒤로가기로 돌아올 수 없게)
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── 홈 화면 ──────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            // loggedInEmployee 가 null 이면 AuthManager 복원 시도
            val employee = loggedInEmployee
                ?: buildRestoredEmployee()
                ?: return@composable   // 복원도 불가능하면 렌더링 스킵

            HomeScreen(
                loggedInEmployee        = employee,
                onLogout                = {
                    // 로그아웃: 상태 초기화 후 Login 화면으로 이동
                    loggedInEmployee = null
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToDetail      = { emp ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(emp.empNo))
                },
                onNavigateToSearch      = { navController.navigate(Screen.Search.route) },
                onNavigateToFavorites   = { navController.navigate(Screen.Favorites.route) },
                onNavigateToTeam        = { navController.navigate(Screen.Team.route) },
                onNavigateToOrgChart    = { navController.navigate(Screen.OrgChart.route) },
                onNavigateToSettings    = { navController.navigate(Screen.Settings.route) },
                onNavigateToInfo        = { navController.navigate(Screen.Info.route) },
                onNavigateToAddPhone    = { navController.navigate(Screen.AddPhone.route) },
                onNavigateToCallHistory = { navController.navigate(Screen.CallHistory.route) }
            )
        }

        // ── 직원 상세 화면 (empNo: String) ───────────────────────────────────
        composable(
            route     = Screen.EmployeeDetail.route,
            arguments = listOf(
                navArgument("empNo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val empNo = backStackEntry.arguments?.getString("empNo") ?: return@composable
            EmployeeDetailScreen(
                empNo  = empNo,
                onBack = { navController.popBackStack() }
            )
        }

        // ── 검색 화면 ─────────────────────────────────────────────────────────
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToDetail = { empNo ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(empNo))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 즐겨찾기 화면 ─────────────────────────────────────────────────────
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateToDetail = { empNo ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(empNo))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 팀원보기 화면 ─────────────────────────────────────────────────────
        composable(Screen.Team.route) {
            TeamScreen(
                onNavigateToDetail = { empNo ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(empNo))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 조직도 화면 ───────────────────────────────────────────────────────
        composable(Screen.OrgChart.route) {
            OrgChartScreen(
                onNavigateToDetail = { empNo ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(empNo))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 설정 화면 ─────────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack                    = { navController.popBackStack() },
                onLogout                  = {
                    loggedInEmployee = null
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToInfo          = { navController.navigate(Screen.Info.route) },
                onNavigateToPhoneSettings = { navController.navigate(Screen.PhoneSettings.route) }
            )
        }

        // ── 가이드(정보) 화면 ─────────────────────────────────────────────────
        composable(Screen.Info.route) {
            InfoScreen(onBack = { navController.popBackStack() })
        }

        // ── 전화번호 설정 화면 ────────────────────────────────────────────────
        composable(Screen.PhoneSettings.route) {
            PhoneSettingsScreen(onBack = { navController.popBackStack() })
        }

        // ── 전화번호 추가 화면 ────────────────────────────────────────────────
        composable(Screen.AddPhone.route) {
            AddPhoneScreen(onBack = { navController.popBackStack() })
        }

        // ── 통화내역 화면 ─────────────────────────────────────────────────────
        composable(Screen.CallHistory.route) {
            CallHistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 헬퍼: AuthManager 저장 정보로 최소 Employee 복원
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 앱 재시작 후 SharedPreferences 에 저장된 기본 정보로 Employee 를 복원합니다.
 *
 * 로그인 API 를 다시 호출하지 않고 저장된 이름·사번·조직 코드만으로 임시 객체를 생성합니다.
 * HomeScreen 에서 "내 상세보기" 등 API 연동 시 최신 정보는 별도 API 로 조회됩니다.
 *
 * @return 복원된 Employee 객체. 저장된 사번이 없으면 null.
 */
private fun buildRestoredEmployee(): Employee? {
    val empNo = AuthManager.loginEmpNo ?: return null
    val empNm = AuthManager.loginEmpNm ?: ""
    val orgCd = AuthManager.loginOrgCd ?: ""

    return Employee(
        empNo         = empNo,
        name          = empNm,
        team          = "",       // 팀명은 API 조회 전까지 빈 문자열
        teamCode      = orgCd,
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
