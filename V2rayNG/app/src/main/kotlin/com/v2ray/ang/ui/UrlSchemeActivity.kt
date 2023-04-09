package com.v2ray.ang.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import com.google.zxing.WriterException
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AngConfigManager

class UrlSchemeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent { 
            Scaffold {
                CircularProgressIndicator()
            }
        }

        var shareUrl: String = ""
        try {
            intent?.apply {
                when (action) {
                    Intent.ACTION_SEND -> {
                        if ("text/plain" == type) {
                            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                                shareUrl = it
                            }
                        }
                    }
                    Intent.ACTION_VIEW -> {
                        val uri: Uri? = intent.data
                        shareUrl = uri?.getQueryParameter("url")!!
                    }
                }
            }
            toast(shareUrl)
            val count = AngConfigManager.importBatchConfig(shareUrl, "", false)
            if (count > 0) {
                toast(R.string.toast_success)
            } else {
                toast(R.string.toast_failure)
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}