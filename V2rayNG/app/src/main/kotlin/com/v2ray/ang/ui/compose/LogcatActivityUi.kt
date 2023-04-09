package com.v2ray.ang.ui.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatActivityScreen(onBack: () -> Unit) {
    val curOnBack by rememberUpdatedState(newValue = onBack)
    val scrollState = rememberScrollState()

    var logs by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(key1 = logs) {
        if (logs == null) {
            // 获取日志
            val lst = LinkedHashSet<String>()
            lst.add("logcat")
            lst.add("-d")
            lst.add("-v")
            lst.add("time")
            lst.add("-s")
            lst.add("GoLog,tun2socks,${AppConfig.ANG_PACKAGE},AndroidRuntime,System.err")
            val process = Runtime.getRuntime().exec(lst.toTypedArray())
//                val bufferedReader = BufferedReader(
//                        InputStreamReader(process.inputStream))
//                val allText = bufferedReader.use(BufferedReader::readText)
            logs = process.inputStream.bufferedReader().use { it.readText() }
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(text = stringResource(id = R.string.title_logcat))
            },
            navigationIcon = {
                IconButton(onClick = curOnBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
            },
            actions = {
                // clear all
                IconButton(onClick = {
                    logs = ""
                }) {
                    Icon(
                        painterResource(id = R.drawable.ic_delete_white_24dp),
                        contentDescription = stringResource(id = R.string.logcat_clear)
                    )
                }

                // copy all
                val context = LocalContext.current
                IconButton(onClick = {
                    Utils.setClipboard(context, logs ?: "")
                    context.toast(R.string.toast_success)
                }) {
                    Icon(
                        painterResource(id = R.drawable.ic_copy_white),
                        contentDescription = stringResource(id = R.string.logcat_copy)
                    )
                }
            }
        )
    }) { padding ->
        if (logs == null) {
            CircularProgressIndicator()
        } else {
            Text(
                logs!!, modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            )
        }

    }
}