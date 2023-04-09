package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.v2ray.ang.ui.compose.SubSettingActivityScreen

class SubSettingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SubSettingActivityScreen(
                onBack = this::finish,
                onEditSubscription = { subId ->
                    val intent = Intent(this, SubEditActivity::class.java)
                    if (subId != null) {
                        intent.putExtra("subId", subId)
                    }
                    startActivity(intent)
                }
            )
        }
    }
}
