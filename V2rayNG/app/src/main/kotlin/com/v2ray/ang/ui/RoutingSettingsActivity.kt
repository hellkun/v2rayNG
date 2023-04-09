package com.v2ray.ang.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import com.v2ray.ang.ui.compose.RoutingSettingsActivityScreen

class RoutingSettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoutingSettingsActivityScreen(onBack = this::finish)
        }
    }
}
