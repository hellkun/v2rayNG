package com.v2ray.ang.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import com.v2ray.ang.ui.compose.UserAssetActivityScreen

class UserAssetActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UserAssetActivityScreen(onBack = this::finish)
        }
    }
}
