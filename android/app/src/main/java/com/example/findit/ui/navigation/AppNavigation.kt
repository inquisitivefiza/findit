package com.example.findit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.findit.ui.auth.LoginScreen
import com.example.findit.ui.auth.RegisterScreen
import com.example.findit.ui.feed.FeedScreen
import com.example.findit.ui.matches.MatchesScreen
import com.example.findit.ui.post.PostItemScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FEED = "feed"
    const val POST_ITEM = "post_item"
    const val MATCHES = "matches/{itemId}"
    fun matches(itemId: Int) = "matches/$itemId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Routes.FEED) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }},
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Routes.FEED) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }},
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Routes.FEED) {
            FeedScreen(
                onNavigateToPost = { navController.navigate(Routes.POST_ITEM) },
                onNavigateToMatches = { itemId ->
                    navController.navigate(Routes.matches(itemId))
                }
            )
        }
        composable(Routes.POST_ITEM) {
            PostItemScreen(
                onPostSuccess = { navController.popBackStack() }
            )
        }
        composable(Routes.MATCHES) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")?.toInt() ?: 0
            MatchesScreen(itemId = itemId)
        }
    }
}

