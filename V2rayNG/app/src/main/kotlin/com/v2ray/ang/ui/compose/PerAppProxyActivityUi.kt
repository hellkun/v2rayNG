package com.v2ray.ang.ui.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.util.AppManagerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppProxyActivityScreen(onBack: () -> Unit) {
    val curOnBack by rememberUpdatedState(newValue = onBack)

    val context = LocalContext.current
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    val blackSet = remember {
        sharedPref.getStringSet(AppConfig.PREF_PER_APP_PROXY_SET, null) ?: emptySet()
    }
    val apps by loadNetworkAppList(blackSet)
    val blacklist = remember(blackSet) {
        blackSet.toMutableStateList()
    }

    var perAppEnabled by remember {
        mutableStateOf(
            sharedPref.getBoolean(AppConfig.PREF_PER_APP_PROXY, false)
        )
    }
    LaunchedEffect(perAppEnabled) {
        sharedPref.edit {
            putBoolean(AppConfig.PREF_PER_APP_PROXY, perAppEnabled)
        }
    }

    var byPassMode by remember {
        mutableStateOf(
            sharedPref.getBoolean(AppConfig.PREF_BYPASS_APPS, false)
        )
    }
    LaunchedEffect(byPassMode) {
        sharedPref.edit {
            putBoolean(AppConfig.PREF_BYPASS_APPS, byPassMode)
        }
    }

    val owner = LocalLifecycleOwner.current
    LaunchedEffect(key1 = true) {
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
            }

            override fun onPause(owner: LifecycleOwner) {
                sharedPref.edit {
                    putStringSet(AppConfig.PREF_PER_APP_PROXY_SET, blacklist.toSet())
                }
            }
        }
        owner.lifecycle.addObserver(observer)
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            // TODO: Search view
        }, navigationIcon = {
            IconButton(onClick = curOnBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        }, actions = {
            /*IconButton(onClick = { }) {
                Icon(Icons.Default.Search, contentDescription = null)
            }*/

            BypassOverflowButton(
                onSelectAll = {
                    blacklist.clear()
                    blacklist.addAll(apps.map { it.packageName })
                }
            )
        })
    }) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PerAppProxyModeSwitch(
                    perAppEnabled,
                    stringResource(id = R.string.title_pref_per_app_proxy),
                ) {
                    perAppEnabled = it
                }

                PerAppProxyModeSwitch(
                    byPassMode,
                    stringResource(id = R.string.switch_bypass_apps_mode),
                ) {
                    byPassMode = it
                }
            }

            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(apps, key = { _, item ->
                    item.packageName
                }) { index, item ->
                    ListItem(headlineContent = {
                        var title = item.appName
                        if (item.isSystemApp) {
                            title = String.format("** %1s", title)
                        }
                        Text(title, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }, leadingContent = {
                        val bitmap = item.appIcon.toBitmap()
                        Image(
                            bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }, supportingContent = {
                        Text(item.packageName, maxLines = 2)
                    }, trailingContent = {
                        Checkbox(
                            checked = blacklist.contains(item.packageName),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    blacklist.add(item.packageName)
                                } else {
                                    blacklist.remove(item.packageName)
                                }
                            })
                    })

                    if (index < apps.size - 1) {
                        Divider()
                    }
                }
            }
        }
    }
}

private fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) {
        return bitmap
    }

    if (intrinsicHeight <= 0 || intrinsicWidth <= 0) {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

@Composable
private fun PerAppProxyModeSwitch(
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val curOnCheckedChange by rememberUpdatedState(newValue = onCheckedChange)

    Row(Modifier.wrapContentSize(), verticalAlignment = Alignment.CenterVertically) {
        Text(text)
        Spacer(modifier = Modifier.width(20.dp))
        Switch(checked = checked, onCheckedChange = curOnCheckedChange)
    }
}

@Composable
private fun loadNetworkAppList(blacklist: Set<String>?): State<List<AppInfo>> {
    val context = LocalContext.current

    return produceState(initialValue = emptyList()) {
        val list = withContext(Dispatchers.IO) { AppManagerUtil.loadNetworkAppList(context) }
            .onEach {
                if (blacklist != null) {
                    it.isSelected = if (blacklist.contains(it.packageName)) 1 else 0
                }
            }

        withContext(Dispatchers.Default) {
            list.sortWith { p1, p2 ->
                if (blacklist != null) {
                    when {
                        p1.isSelected > p2.isSelected -> -1
                        p1.isSelected == p2.isSelected -> 0
                        else -> 1
                    }
                } else {
                    val collator = Collator.getInstance()
                    collator.compare(p1.appName, p2.appName)
                }
            }
        }

        value = list
    }
}

@Composable
private fun BypassOverflowButton(
    onSelectAll: () -> Unit,
) {
    val curOnSelectAll by rememberUpdatedState(newValue = onSelectAll)

    var expanded by remember {
        mutableStateOf(false)
    }

    IconButton(onClick = { expanded = !expanded }) {
        Icon(Icons.Default.MoreVert, contentDescription = null)
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = {
            Text(stringResource(id = R.string.menu_item_select_all))
        }, leadingIcon = {
            Icon(
                painterResource(id = R.drawable.ic_select_all_white_24dp),
                contentDescription = null
            )
        }, onClick = {
            curOnSelectAll()
            expanded = false
        })

        DropdownMenuItem(text = {
            Text(stringResource(id = R.string.menu_item_select_proxy_app))
        }, leadingIcon = {
            Icon(
                painterResource(id = R.drawable.ic_description_white_24dp),
                contentDescription = null
            )
        }, onClick = { /*TODO*/ })

        DropdownMenuItem(text = {
            Text(stringResource(id = R.string.menu_item_import_proxy_app))
        }, leadingIcon = {
            Icon(
                painterResource(id = R.drawable.ic_description_white_24dp),
                contentDescription = null
            )
        }, onClick = { /*TODO*/ })

        DropdownMenuItem(text = {
            Text(stringResource(id = R.string.menu_item_export_proxy_app))
        }, leadingIcon = {
            Icon(
                painterResource(id = R.drawable.ic_description_white_24dp),
                contentDescription = null
            )
        }, onClick = { /*TODO*/ })

    }
}