package com.v2ray.ang.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import com.v2ray.ang.ui.compose.SubEditActivityScreen

class SubEditActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SubEditActivityScreen(onBack = this::finish, subId = intent.getStringExtra("subId"))
        }
    }

}
