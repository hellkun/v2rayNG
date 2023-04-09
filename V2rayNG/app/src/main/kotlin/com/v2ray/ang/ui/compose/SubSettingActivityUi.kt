package com.v2ray.ang.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.v2ray.ang.R
import com.v2ray.ang.dto.SubscriptionItem
import com.v2ray.ang.util.MmkvManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubSettingActivityScreen(
    onBack: () -> Unit,
    onEditSubscription: (String?) -> Unit
) {
    val curOnAddSubs by rememberUpdatedState(newValue = onEditSubscription)
    val curOnBack by rememberUpdatedState(newValue = onBack)

    val subStorage = remember {
        MMKV.mmkvWithID(MmkvManager.ID_SUB, MMKV.MULTI_PROCESS_MODE)
    }

    var subscriptions by remember {
        mutableStateOf<List<Pair<String, SubscriptionItem>>>(emptyList())
    }

    var selectedSubscriptionId by remember {
        mutableStateOf<String?>(null)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(key1 = lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            subscriptions = MmkvManager.decodeSubscriptions()
            selectedSubscriptionId = subscriptions.firstOrNull {
                it.second.enabled
            }?.first
        }
    }


    Scaffold(topBar = {
        TopAppBar(title = {
            Text(stringResource(id = R.string.title_sub_setting))
        }, navigationIcon = {
            IconButton(onClick = curOnBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        }, actions = {
            // add
            IconButton(onClick = {
                curOnAddSubs(null)
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        })
    }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp),
            content = {
                itemsIndexed(subscriptions) { index, item ->
                    Subscription(
                        modifier = Modifier
                            .height(dimensionResource(id = R.dimen.server_height))
                            .clickable {
                                item.second.enabled = !item.second.enabled
                                subStorage?.encode(item.first, Gson().toJson(item.second))
                                if (item.second.enabled) {
                                    selectedSubscriptionId = item.first
                                }
                            }, subscription = item.second,
                        selected = item.first == selectedSubscriptionId
                    ) {
                        curOnAddSubs(item.first)
                    }

                    if (index < subscriptions.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            })
    }
}

@Composable
fun Subscription(
    modifier: Modifier = Modifier,
    subscription: SubscriptionItem,
    selected: Boolean,
    onEdit: () -> Unit
) {
    val curOnEdit by rememberUpdatedState(newValue = onEdit)

    Card(modifier) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(
                        colorResource(id = if (selected) R.color.colorSelected else R.color.colorUnselected)
                    )
            )

            Spacer(modifier = Modifier.width(5.dp))

            // name & url
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(subscription.remarks, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(10.dp))
                Text(subscription.url, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }

            // share

            // edit
            IconButton(onClick = curOnEdit) {
                Icon(painterResource(id = R.drawable.ic_edit_black_24dp), contentDescription = null)
            }
        }

    }
}