import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.dto.ServersCache
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainActivityScreen(
    modifier: Modifier = Modifier,
    isRunning: Boolean,
    testMessage: String,
    configs: List<ServersCache>,
    onItemClick: (ServersCache, ServerConfigAction) -> Unit,
    onSelectAddMethod: (MainAddOption) -> Unit,
    onSelectAction: (MainServerConfigAction) -> Unit,
    onFabClick: () -> Unit,
    onStartTest: () -> Unit,
    selectedServerGuid: String? = null,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val curOnFabClick by rememberUpdatedState(newValue = onFabClick)
    val curOnStartTest by rememberUpdatedState(newValue = onStartTest)

    ModalNavigationDrawer(
        modifier = modifier, drawerContent = {
            MainDrawer()
        }, drawerState = drawerState
    ) {
        Scaffold(topBar = {
            TopAppBar(title = {
                Text(text = stringResource(id = R.string.title_server))
            }, navigationIcon = {
                val coroutineScope = rememberCoroutineScope()

                IconButton(onClick = {
                    coroutineScope.launch {
                        if (drawerState.isOpen) {
                            drawerState.close()
                        } else {
                            drawerState.open()
                        }
                    }
                }) {
                    Icon(Icons.Default.Menu, contentDescription = null)
                }
            }, actions = {
                MainAddDropdownButton(onSelect = onSelectAddMethod)

                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        painterResource(id = R.drawable.ic_outline_filter_alt_white_24),
                        contentDescription = stringResource(
                            R.string.title_filter_config
                        ),
                    )
                }

                MainOverflowButton(onSelect = onSelectAction)
            })
        }, floatingActionButton = {
            val shape = FloatingActionButtonDefaults.shape
            FloatingActionButton(
                onClick = curOnFabClick,
                containerColor = colorResource(id = if (isRunning) R.color.colorSelected else R.color.colorUnselected),
                shape = shape,
            ) {
                Icon(painterResource(id = R.drawable.ic_stat_name), contentDescription = null)
            }
        }) { padding ->
            Column(Modifier.padding(padding)) {
                // lazy list
                LazyColumn(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), content = {
                    itemsIndexed(configs, key = { _, conf ->
                        conf.guid
                    }) { index, item ->
                        ServerConfigItem(Modifier.animateItemPlacement(),
                            server = item,
                            active = item.guid == selectedServerGuid,
                            onSelectAction = {
                                onItemClick(item, it)
                            })
                        if (index < configs.size - 1) Spacer(modifier = Modifier.height(4.dp))
                    }
                })

                // bottom
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(id = R.dimen.connection_test_height))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp)
                        .clickable(onClick = curOnStartTest),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = testMessage,
                        color = Color.White,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
fun ServerConfigItem(
    modifier: Modifier = Modifier,
    server: ServersCache,
    active: Boolean,
    onSelectAction: (ServerConfigAction) -> Unit
) {
    val curOnSelect by rememberUpdatedState(newValue = onSelectAction)

    Card(modifier = modifier, shape = RoundedCornerShape(5.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.server_height))
                .clickable {
                    curOnSelect(ServerConfigAction.Apply)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(10.dp)
                    .background(colorResource(id = if (active) R.color.colorSelected else R.color.colorUnselected))
            )

            Spacer(modifier = Modifier.width(5.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = server.config.remarks,
                    maxLines = 2,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(10.dp))

                val outbound = server.config.getProxyOutbound()
                Text(
                    text = "${outbound?.getServerAddress()} : ${outbound?.getServerPort()}",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val iconModifier = Modifier.size(dimensionResource(id = R.dimen.png_height))
            // share
            IconButton(onClick = {
                curOnSelect(ServerConfigAction.Share)
            }) {
                Icon(
                    modifier = iconModifier,
                    painter = painterResource(id = R.drawable.ic_share_black_24dp),
                    contentDescription = null,
                )
            }

            // edit
            IconButton(onClick = { curOnSelect(ServerConfigAction.Edit) }) {
                Icon(
                    modifier = iconModifier,
                    painter = painterResource(id = R.drawable.ic_edit_black_24dp),
                    contentDescription = null
                )
            }

            // delete
            IconButton(onClick = { curOnSelect(ServerConfigAction.Delete) }) {
                Icon(
                    modifier = iconModifier,
                    painter = painterResource(id = R.drawable.ic_delete_black_24dp),
                    contentDescription = null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDrawer(modifier: Modifier = Modifier) {
    ModalDrawerSheet(modifier) {
        NavigationDrawerItem(label = {
            Text(stringResource(id = R.string.title_sub_setting))
        }, selected = false, onClick = { /*TODO*/ }, icon = {
            Icon(
                painterResource(id = R.drawable.ic_subscriptions_white_24dp),
                contentDescription = null
            )
        })

        NavigationDrawerItem(label = {
            Text(stringResource(id = R.string.title_settings))
        }, selected = false, onClick = { /*TODO*/ }, icon = {
            Icon(
                painterResource(id = R.drawable.ic_settings_white_24dp), contentDescription = null
            )
        })

        NavigationDrawerItem(label = {
            Text(stringResource(id = R.string.title_user_asset_setting))
        }, selected = false, onClick = { /*TODO*/ }, icon = {
            Icon(
                painterResource(id = R.drawable.ic_file_white_24dp), contentDescription = null
            )
        })

        Divider()

        NavigationDrawerItem(label = {
            Text(stringResource(id = R.string.title_pref_promotion))
        }, selected = false, onClick = { /*TODO*/ }, icon = {
            Icon(
                painterResource(id = R.drawable.ic_whatshot_white_24dp), contentDescription = null
            )
        })

        NavigationDrawerItem(label = {
            Text(stringResource(id = R.string.title_logcat))
        }, selected = false, onClick = { /*TODO*/ }, icon = {
            Icon(
                painterResource(id = R.drawable.ic_logcat_white_24dp), contentDescription = null
            )
        })

        NavigationDrawerItem(label = {
            Text(stringResource(id = R.string.title_pref_feedback))
        }, selected = false, onClick = { /*TODO*/ }, icon = {
            Icon(
                painterResource(id = R.drawable.ic_feedback_white_24dp), contentDescription = null
            )
        })
    }
}

@Composable
fun MainAddDropdownButton(onSelect: (MainAddOption) -> Unit) {
    val curOnSelect by rememberUpdatedState(newValue = onSelect)

    var expanded by remember {
        mutableStateOf(false)
    }

    var showSecondaryOptions by remember {
        mutableStateOf(false)
    }

    Box(Modifier.wrapContentSize()) {
        IconButton(onClick = {
            expanded = !expanded
            showSecondaryOptions = false
        }) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(id = R.string.menu_item_add_config)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = {
            expanded = false
        }) {
            MainAddOption.values().filter { it.primary == !showSecondaryOptions }.forEach {
                DropdownMenuItem(text = {
                    Text(
                        stringResource(
                            when (it) {
                                MainAddOption.ImportQR -> R.string.menu_item_import_config_qrcode
                                MainAddOption.ImportClipboard -> R.string.menu_item_import_config_clipboard
                                MainAddOption.ImportVless -> R.string.menu_item_import_config_manually_vless
                                MainAddOption.ImportShadowsocks -> R.string.menu_item_import_config_manually_ss
                                MainAddOption.ImportSocks -> R.string.menu_item_import_config_manually_socks
                                MainAddOption.ImportTrojan -> R.string.menu_item_import_config_manually_trojan
                                MainAddOption.ImportVmess -> R.string.menu_item_import_config_manually_vmess
                                MainAddOption.CustomConfigClipboard -> R.string.menu_item_import_config_custom_clipboard
                                MainAddOption.CustomConfigLocal -> R.string.menu_item_import_config_custom_local
                                MainAddOption.CustomConfigUrl -> R.string.menu_item_import_config_custom_url
                                MainAddOption.CustomConfigUrlScan -> R.string.menu_item_import_config_custom_url_scan
                            }
                        )
                    )
                }, onClick = {
                    expanded = false
                    curOnSelect(it)
                })
            }

            if (!showSecondaryOptions) {
                DropdownMenuItem(text = {
                    Text(stringResource(id = R.string.menu_item_import_config_custom))
                }, onClick = {
                    showSecondaryOptions = true
                })
            }
        }
    }
}

@Composable
fun MainOverflowButton(onSelect: (MainServerConfigAction) -> Unit) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(Modifier.wrapContentSize()) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = {
            expanded = false
        }) {
            val curOnSelect by rememberUpdatedState(newValue = onSelect)
            MainServerConfigAction.values().forEach {
                DropdownMenuItem(text = {
                    Text(
                        stringResource(
                            id = when (it) {
                                MainServerConfigAction.RestartService -> R.string.title_service_restart
                                MainServerConfigAction.DeleteAllConfig -> R.string.title_del_all_config
                                MainServerConfigAction.DeleteDuplicateConfig -> R.string.title_del_duplicate_config
                                MainServerConfigAction.DeleteInvalidConfig -> R.string.title_del_invalid_config
                                MainServerConfigAction.ExportAllConfig -> R.string.title_export_all
                                MainServerConfigAction.PingAllConfig -> R.string.title_ping_all_server
                                MainServerConfigAction.RealPingAllConfig -> R.string.title_real_ping_all_server
                                MainServerConfigAction.SortByTestResults -> R.string.title_sort_by_test_results
                                MainServerConfigAction.UpdateSubscription -> R.string.title_sub_update
                            }
                        )
                    )
                }, leadingIcon = {
                    when (it) {
                        MainServerConfigAction.DeleteAllConfig, MainServerConfigAction.DeleteDuplicateConfig, MainServerConfigAction.DeleteInvalidConfig -> Icon(
                            painterResource(id = R.drawable.ic_delete_white_24dp),
                            contentDescription = null
                        )
                        MainServerConfigAction.ExportAllConfig -> Icon(
                            painterResource(id = R.drawable.ic_share_white_24dp),
                            contentDescription = null
                        )
                        else -> {}
                    }
                }, onClick = {
                    expanded = false
                    curOnSelect(it)
                })
            }
        }
    }


}

enum class MainAddOption(val primary: Boolean = true) {
    ImportQR, ImportClipboard, ImportVmess, ImportVless, ImportShadowsocks, ImportSocks, ImportTrojan, CustomConfigClipboard(
        false
    ),
    CustomConfigLocal(false), CustomConfigUrl(false), CustomConfigUrlScan(false),
}

enum class MainServerConfigAction {
    RestartService, DeleteAllConfig, DeleteDuplicateConfig, DeleteInvalidConfig, ExportAllConfig, PingAllConfig, RealPingAllConfig, SortByTestResults, UpdateSubscription,
}

/**
 * 对单个服务器配置的操作
 */
enum class ServerConfigAction {
    /**
     * 选中
     */
    Apply,

    /**
     * 分享
     */
    Share,

    /**
     * 编辑
     */
    Edit,

    /**
     * 删除
     */
    Delete
}