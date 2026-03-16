package com.example.whoareyou.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.whoareyou.model.Employee
import com.example.whoareyou.model.EmployeeRepository
import com.example.whoareyou.model.MockData
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

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object EmployeeDetail : Screen("employee_detail/{employeeId}") {
        fun createRoute(employeeId: Int) = "employee_detail/$employeeId"
    }
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Team : Screen("team")
    object OrgChart : Screen("org_chart")
    object Settings : Screen("settings")
    object Info : Screen("info")
    object PhoneSettings : Screen("phone_settings")
    object AddPhone : Screen("add_phone")
    object CallHistory : Screen("call_history")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(false) }
    var loggedInEmployee by remember { mutableStateOf<Employee?>(null) }
    // 초기값은 MockData (API 호출 전 즉시 표시), 이후 LaunchedEffect에서 API 결과로 교체됨
    val employees = remember { mutableStateListOf<Employee>().also { it.addAll(MockData.employees) } }

    // 앱 시작 시 API에서 임직원 목록을 불러옵니다.
    // API 실패 시 초기값 MockData가 그대로 유지됩니다.
    LaunchedEffect(Unit) {
        val loaded = EmployeeRepository.loadEmployees()
        employees.clear()
        employees.addAll(loaded)
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { employee ->
                    loggedInEmployee = employee
                    isLoggedIn = true
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val employee = loggedInEmployee
            if (employee == null) return@composable
            HomeScreen(
                loggedInEmployee = employee,
                onLogout = {
                    isLoggedIn = false
                    loggedInEmployee = null
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToDetail = { emp ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(emp.id))
                },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToTeam = { navController.navigate(Screen.Team.route) },
                onNavigateToOrgChart = { navController.navigate(Screen.OrgChart.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToInfo = { navController.navigate(Screen.Info.route) },
                onNavigateToAddPhone = { navController.navigate(Screen.AddPhone.route) },
                onNavigateToCallHistory = { navController.navigate(Screen.CallHistory.route) }
            )
        }

        composable(
            route = Screen.EmployeeDetail.route,
            arguments = listOf(navArgument("employeeId") { type = NavType.IntType })
        ) { backStackEntry ->
            val employeeId = backStackEntry.arguments?.getInt("employeeId")
            val employee = if (employeeId != null) employees.find { it.id == employeeId } else null
            if (employee == null) return@composable
            EmployeeDetailScreen(
                employee = employee,
                onBack = { navController.popBackStack() },
                onToggleFavorite = { emp ->
                    val index = employees.indexOfFirst { it.id == emp.id }
                    if (index >= 0) {
                        employees[index] = employees[index].copy(isFavorite = !employees[index].isFavorite)
                    }
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                employees = employees,
                onNavigateToDetail = { emp ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(emp.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                employees = employees,
                onNavigateToDetail = { emp ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(emp.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Team.route) {
            TeamScreen(
                employees = employees,
                onNavigateToDetail = { emp ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(emp.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OrgChart.route) {
            OrgChartScreen(
                employees = employees,
                onNavigateToDetail = { emp ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(emp.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    isLoggedIn = false
                    loggedInEmployee = null
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToInfo = { navController.navigate(Screen.Info.route) },
                onNavigateToPhoneSettings = { navController.navigate(Screen.PhoneSettings.route) }
            )
        }

        composable(Screen.Info.route) {
            InfoScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.PhoneSettings.route) {
            PhoneSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AddPhone.route) {
            AddPhoneScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.CallHistory.route) {
            CallHistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
