package com.v2ray.ang.ui.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.ui.ScannerActivity
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingSettingsActivityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val titles = remember {
        context.resources.getStringArray(R.array.routing_tag)
    }
    var selectedTabIndex by remember {
        mutableStateOf(0)
    }

    val pref = remember {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    val contents = remember {
        arrayOf(
            AppConfig.PREF_V2RAY_ROUTING_AGENT,
            AppConfig.PREF_V2RAY_ROUTING_DIRECT,
            AppConfig.PREF_V2RAY_ROUTING_BLOCKED
        ).associateWith {
            mutableStateOf(pref.getString(it, "")!!)
        }
    }

    val tag = when (selectedTabIndex) {
        0 -> AppConfig.PREF_V2RAY_ROUTING_AGENT
        1 -> AppConfig.PREF_V2RAY_ROUTING_DIRECT
        2 -> AppConfig.PREF_V2RAY_ROUTING_BLOCKED
        else -> throw RuntimeException("Unknown tab: $selectedTabIndex")
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(text = stringResource(id = R.string.title_pref_routing_custom))
        }, navigationIcon = {
            val curOnBack by rememberUpdatedState(newValue = onBack)
            IconButton(onClick = curOnBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        }, actions = {
            SaveRoutingButton(sharedPreference = pref, tag = tag, content = contents[tag]!!.value)
            RoutingOverflowButton(tag = tag, onClear = {
                contents[tag]!!.value = ""
            }, onUpdateContent = { content, replace ->
                var text = content
                if (!replace) {
                    text = "${contents[tag]!!.value},$text"
                }
                contents[tag]!!.value = text
            })
        })
    }) {
        Column(Modifier.padding(it)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                titles.forEachIndexed { index, s ->
                    Tab(selectedTabIndex == index, onClick = {
                        selectedTabIndex = index
                    }) {
                        Text(s, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            RoutingSettingsContent(
                tag = tag,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                content = contents[tag]!!,
            )
        }
    }
}

@Composable
fun RoutingSettingsContent(
    tag: String, modifier: Modifier = Modifier, content: MutableState<String>
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(id = R.string.routing_settings_tips))

        Spacer(modifier = Modifier.height(10.dp))

        TextField(
            value = content.value,
            onValueChange = {
                content.value = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            maxLines = 1000,
            minLines = 20,
        )
    }
}

@Composable
private fun SaveRoutingButton(sharedPreference: SharedPreferences, tag: String, content: String) {
    IconButton(onClick = {
        sharedPreference.edit {
            putString(tag, content)
        }
    }) {
        Icon(
            Icons.Default.Check,
            contentDescription = stringResource(id = R.string.routing_settings_save)
        )
    }
}

@Composable
private fun RoutingOverflowButton(
    tag: String,
    onClear: () -> Unit,
    onUpdateContent: (content: String, replace: Boolean) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    val curOnClear by rememberUpdatedState(newValue = onClear)
    val curOnUpdate by rememberUpdatedState(newValue = onUpdateContent)
    val launcherForReplace =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val content = it.data?.getStringExtra("SCAN_RESULT")!!
                curOnUpdate(content, true)
            }
        }
    val launcherForAppend =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val content = it.data?.getStringExtra("SCAN_RESULT")!!
                curOnUpdate(content, false)
            }
        }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = null)
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = {
            Text(stringResource(id = R.string.routing_settings_delete))
        }, onClick = {
            curOnClear()
            expanded = false
        })

        DropdownMenuItem(text = {
            Text(stringResource(id = R.string.routing_settings_scan_replace))
        }, onClick = {
            launcherForReplace.launch(Intent(context, ScannerActivity::class.java))
            expanded = false
        })

        DropdownMenuItem(text = {
            Text(stringResource(id = R.string.routing_settings_scan_append))
        }, onClick = {
            launcherForAppend.launch(Intent(context, ScannerActivity::class.java))
            expanded = false
        })

        val scope = rememberCoroutineScope()
        DropdownMenuItem(text = {
            Text(stringResource(id = R.string.routing_settings_default_rules))
        }, onClick = {
            scope.launch {
                val content = getDefaultRules(context, tag)
                curOnUpdate(content, true)
            }
            expanded = false
        })
    }
}

private suspend fun getDefaultRules(context: Context, tag: String): String {
    var url = AppConfig.v2rayCustomRoutingListUrl
    val reqTag = when (tag) {
        AppConfig.PREF_V2RAY_ROUTING_AGENT -> AppConfig.TAG_AGENT
        AppConfig.PREF_V2RAY_ROUTING_DIRECT -> AppConfig.TAG_DIRECT
        AppConfig.PREF_V2RAY_ROUTING_BLOCKED -> AppConfig.TAG_BLOCKED
        else -> throw RuntimeException()
    }
    url += reqTag
    context.toast(R.string.msg_downloading_content)

    var content = withContext(Dispatchers.IO) {
        Utils.getUrlContext(url, 5000)
    }
    if (content.isEmpty()) {
        content = Utils.readTextFromAssets(context, "custom_routing_$reqTag")
    }

    return content
}