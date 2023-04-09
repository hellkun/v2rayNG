package com.v2ray.ang.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.v2ray.ang.ui.compose.SubEditActivityScreen
import com.v2ray.ang.ui.compose.SubSettingActivityScreen

class SubSettingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "subscriptions") {
                composable("subscriptions") {
                    SubSettingActivityScreen(
                        onBack = this@SubSettingActivity::finish,
                        onEditSubscription = { subId ->
                            navController.navigate("subscription/$subId")
                        }
                    )
                }

                composable("subscription/{id}", arguments = listOf(
                    navArgument("id") {
                        type = NavType.StringType
                        nullable = true
                    }
                )) { entry ->
                    SubEditActivityScreen(onBack = {
                        navController.popBackStack()
                    }, subId = entry.arguments?.getString("id"))
                }
            }
        }
    }
}
