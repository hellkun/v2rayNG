package com.v2ray.ang.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.v2ray.ang.R
import com.v2ray.ang.dto.SubscriptionItem
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubEditActivityScreen(onBack: () -> Unit, subId: String?) {
    val curOnBack by rememberUpdatedState(newValue = onBack)

    val mmkv = remember {
        MMKV.mmkvWithID(MmkvManager.ID_SUB, MMKV.MULTI_PROCESS_MODE)
    }
    val subscriptionItem = remember(subId) {
        mmkv?.decodeString(subId.orEmpty())
            ?.let {
                Gson().fromJson(it, SubscriptionItem::class.java)
            }
    }

    var remark by remember(subId) {
        mutableStateOf(subscriptionItem?.remarks ?: "")
    }

    var url by remember(subId) {
        mutableStateOf(subscriptionItem?.url ?: "")
    }

    var enableUpdate by remember(subId) {
        mutableStateOf(subscriptionItem?.enabled ?: true)
    }

    val snackBarHostState = remember {
        SnackbarHostState()
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(stringResource(id = R.string.title_sub_setting))
        }, navigationIcon = {
            IconButton(onClick = curOnBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        }, actions = {
            // delete
            if (subscriptionItem != null) {
                IconButton(onClick = {
                    MmkvManager.removeSubscription(subId!!)
                    curOnBack()
                }) {
                    Icon(
                        painterResource(id = R.drawable.ic_delete_white_24dp),
                        contentDescription = stringResource(
                            id = R.string.menu_item_del_config
                        )
                    )
                }
            }

            // save
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            IconButton(onClick = {
                val item = subscriptionItem ?: SubscriptionItem()
                item.remarks = remark
                item.enabled = enableUpdate
                item.url = url

                if (item.remarks.isEmpty()) {
                    scope.launch {
                        snackBarHostState.showSnackbar(context.getString(R.string.sub_setting_remarks))
                    }
                } else {
                    val id = if (subscriptionItem != null) subId!! else Utils.getUuid()

                    mmkv?.encode(id, Gson().toJson(item))
                    scope.launch {
                        snackBarHostState.showSnackbar(context.getString(R.string.toast_success))
                    }
                    curOnBack()
                }
            }) {
                Icon(
                    painterResource(id = R.drawable.ic_action_done),
                    contentDescription = stringResource(
                        id = R.string.menu_item_save_config
                    )
                )
            }
        })
    }, snackbarHost = {
        SnackbarHost(hostState = snackBarHostState)
    }) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(id = R.string.title_server),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            // remarks
            Text(stringResource(id = R.string.sub_setting_remarks))
            TextField(
                value = remark,
                onValueChange = {
                    remark = it
                },
                modifier = Modifier
                    .height(dimensionResource(id = R.dimen.edit_height))
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // update
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(id = R.string.sub_setting_enable),
                    modifier = Modifier.weight(1f)
                )

                Checkbox(checked = enableUpdate, onCheckedChange = {
                    enableUpdate = it
                })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // url
            Text(stringResource(id = R.string.sub_setting_url))
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = url,
                onValueChange = {
                    url = it
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 10,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}