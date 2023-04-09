package com.v2ray.ang.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.view.View
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.databinding.ActivityLogcatBinding
import com.v2ray.ang.ui.compose.LogcatActivityScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class LogcatActivity : BaseActivity() {
    private lateinit var binding: ActivityLogcatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LogcatActivityScreen(onBack = this::finish)
        }
        /*binding = ActivityLogcatBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

         title = getString(R.string.title_logcat)

         supportActionBar?.setDisplayHomeAsUpEnabled(true)
         logcat(false)*/
    }

    private fun logcat(shouldFlushLog: Boolean) {

        try {
            binding.pbWaiting.visibility = View.VISIBLE

            lifecycleScope.launch(Dispatchers.Default) {
                if (shouldFlushLog) {
                    val lst = LinkedHashSet<String>()
                    lst.add("logcat")
                    lst.add("-c")
                    val process = Runtime.getRuntime().exec(lst.toTypedArray())
                    process.waitFor()
                }
                val lst = LinkedHashSet<String>()
                lst.add("logcat")
                lst.add("-d")
                lst.add("-v")
                lst.add("time")
                lst.add("-s")
                lst.add("GoLog,tun2socks,${ANG_PACKAGE},AndroidRuntime,System.err")
                val process = Runtime.getRuntime().exec(lst.toTypedArray())
//                val bufferedReader = BufferedReader(
//                        InputStreamReader(process.inputStream))
//                val allText = bufferedReader.use(BufferedReader::readText)
                val allText = process.inputStream.bufferedReader().use { it.readText() }
                launch(Dispatchers.Main) {
                    binding.tvLogcat.text = allText
                    binding.tvLogcat.movementMethod = ScrollingMovementMethod()
                    binding.pbWaiting.visibility = View.GONE
                    Handler(Looper.getMainLooper()).post { binding.svLogcat.fullScroll(View.FOCUS_DOWN) }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
