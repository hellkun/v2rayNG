package com.v2ray.ang.ui.compose

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.text.DateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAssetActivityScreen(onBack: () -> Unit) {
    val curOnBack by rememberUpdatedState(newValue = onBack)

    val assetFiles = remember {
        mutableStateListOf<File>()
    }

    var refreshTrigger by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    LaunchedEffect(refreshTrigger) {
        assetFiles.clear()
        withContext(Dispatchers.IO) {
            val dir = File(Utils.userAssetPath(context))
            dir.listFiles()?.run(assetFiles::addAll)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(stringResource(id = R.string.title_user_asset_setting))
            }, navigationIcon = {
                IconButton(onClick = curOnBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
            }, actions = {
                AddGeoFileButton {
                    refreshTrigger = !refreshTrigger
                }

                DownloadGeoFilesButton {
                    refreshTrigger = !refreshTrigger
                }
            })
        }
    ) { padding ->
        val dateFormat = remember {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
        }

        LazyColumn(modifier = Modifier.padding(padding)) {
            itemsIndexed(assetFiles) { index, file ->
                ListItem(headlineContent = {
                    Text(file.name)
                }, supportingContent = {
                    Text(
                        "${
                            file.length().toTrafficString()
                        }  •  ${dateFormat.format(Date(file.lastModified()))}"
                    )
                }, trailingContent = {
                    // 外部文件，可以删除
                    if (geoFiles.contains(file.name)) {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                })
            }
        }
    }
}

@Composable
private fun AddGeoFileButton(onSuccess: () -> Unit) {
    val curOnSuccess by rememberUpdatedState(newValue = onSuccess)

    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try {
                    copyFile(context, Utils.userAssetPath(context), uri)
                    curOnSuccess()
                } catch (e: Exception) {
                    context.toast(R.string.toast_asset_copy_failed)
                }
            }
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            if (it) {
                try {
                    launcher.launch("*/*")
                } catch (e: ActivityNotFoundException) {
                    context.toast(R.string.toast_require_file_manager)
                }
            }
        }

    IconButton(onClick = {
        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }) {
        Icon(
            Icons.Default.Add,
            contentDescription = stringResource(id = R.string.menu_item_add_file)
        )
    }
}

private fun copyFile(context: Context, extDir: String, uri: Uri): String {
    val cursorName = try {
        context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    val targetFile = File(extDir, cursorName ?: uri.toString())
    context.contentResolver.openInputStream(uri).use { inputStream ->
        targetFile.outputStream().use { fileOut ->
            inputStream?.copyTo(fileOut)
            context.toast(R.string.toast_success)
        }
    }
    return targetFile.path
}

@Composable
private fun DownloadGeoFilesButton(onSuccess: (String) -> Unit) {
    val curOnSuccess by rememberUpdatedState(newValue = onSuccess)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    IconButton(onClick = {
        scope.launch {
            downloadFiles(context, curOnSuccess)
        }
    }) {
        Icon(
            painterResource(id = R.drawable.ic_cloud_download_white_24dp),
            contentDescription = stringResource(
                id = R.string.menu_item_download_file
            )
        )
    }
}

private val geoFiles = arrayOf("geosite.dat", "geoip.dat")

private suspend fun downloadFiles(context: Context, onSuccess: (String) -> Unit) {
    val storage = MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE)

    val httpPort =
        Utils.parseInt(storage?.decodeString(AppConfig.PREF_HTTP_PORT), AppConfig.PORT_HTTP.toInt())

    geoFiles.forEach {
        val result = withContext(Dispatchers.IO) {
            downloadGeo(context, it, 60000, httpPort)
        }

        if (result) {
            context.toast(context.getString(R.string.toast_success) + " " + it)
            onSuccess(it)
        } else {
            context.toast(context.getString(R.string.toast_failure) + " " + it)
        }

    }
}

private fun downloadGeo(context: Context, name: String, timeout: Int, httpPort: Int): Boolean {
    val extDir = File(Utils.userAssetPath(context))
    val url = AppConfig.geoUrl + name
    val targetTemp = File(extDir, name + "_temp")
    val target = File(extDir, name)
    var conn: HttpURLConnection? = null
    //Log.d(AppConfig.ANG_PACKAGE, url)

    try {
        conn = URL(url).openConnection(
            Proxy(
                Proxy.Type.HTTP,
                InetSocketAddress("127.0.0.1", httpPort)
            )
        ) as HttpURLConnection
        conn.connectTimeout = timeout
        conn.readTimeout = timeout
        val inputStream = conn.inputStream
        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            FileOutputStream(targetTemp).use { output ->
                inputStream.copyTo(output)
            }

            targetTemp.renameTo(target)
        }
        return true
    } catch (e: Exception) {
        Log.e(AppConfig.ANG_PACKAGE, Log.getStackTraceString(e))
        return false
    } finally {
        conn?.disconnect()
    }
}