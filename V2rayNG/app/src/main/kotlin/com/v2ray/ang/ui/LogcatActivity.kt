package com.v2ray.ang.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import com.v2ray.ang.ui.compose.LogcatActivityScreen

class LogcatActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LogcatActivityScreen(onBack = this::finish)
        }
    }
}
