package com.v2ray.ang.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.dto.EConfigType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerActivityScreen(type: EConfigType, onBack: () -> Unit) {
    val context = LocalContext.current
    // TODO: 加载初始值
    val contents = remember {
        FieldContentType.values().associateWith { mutableStateOf("") }
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(stringResource(id = R.string.title_server))
        }, navigationIcon = {
            val curOnBack by rememberUpdatedState(newValue = onBack)
            IconButton(onClick = curOnBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        })
    }) { padding ->


        Column(
            Modifier
                .padding(padding)
        ) {
            MainFormContent(type = type, contents = contents, modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .height(72.dp),
                shadowElevation = 48.dp
            ) {
                Button(
                    onClick = { /*TODO*/ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun MainFormContent(
    type: EConfigType,
    contents: Map<FieldContentType, MutableState<String>>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
    ) {
        // common fields
        ServerConfigField(
            type = FieldContentType.Remarks, content = contents[FieldContentType.Remarks]!!
        )
        Spacer(modifier = Modifier.height(16.dp))
        ServerConfigField(
            type = FieldContentType.Address, content = contents[FieldContentType.Address]!!
        )
        Spacer(modifier = Modifier.height(16.dp))
        ServerConfigField(
            type = FieldContentType.Port, content = contents[FieldContentType.Port]!!
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (type) {
            EConfigType.VMESS -> {
                ServerConfigField(
                    type = FieldContentType.Id, content = contents[FieldContentType.Id]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
                ServerConfigField(
                    type = FieldContentType.AlterId,
                    content = contents[FieldContentType.AlterId]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
                V2RaySecurities()
                Spacer(modifier = Modifier.height(16.dp))
                NetworkSecurities()
                Spacer(modifier = Modifier.height(16.dp))
                ServerConfigField(
                    type = FieldContentType.RequestHost,
                    content = contents[FieldContentType.RequestHost]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
                ServerConfigField(
                    type = FieldContentType.Path, content = contents[FieldContentType.Path]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
                TlsFields(contents = contents)
            }
            EConfigType.SHADOWSOCKS -> {
                ServerConfigField(
                    type = FieldContentType.Password,
                    content = contents[FieldContentType.Password]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
                ShadowsocksSecurities()
            }
            EConfigType.SOCKS -> {
                ServerConfigField(
                    type = FieldContentType.Username,
                    content = contents[FieldContentType.Username]!!
                )
                Spacer(modifier = Modifier.height(16.dp))

                ServerConfigField(
                    type = FieldContentType.PasswordOptional,
                    content = contents[FieldContentType.PasswordOptional]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            EConfigType.VLESS -> {
                ServerConfigField(
                    type = FieldContentType.Id, content = contents[FieldContentType.Id]!!
                )
                Spacer(modifier = Modifier.height(16.dp))

                val selectableFlows = remember {
                    context.resources.getStringArray(R.array.flows)
                }
                var selectedFlow by remember {
                    mutableStateOf(selectableFlows.first())
                }
                DropdownText(
                    title = stringResource(id = R.string.server_lab_flow),
                    selected = selectedFlow,
                    selectables = selectableFlows,
                    onSelect = { selectedFlow = it }
                )
                Spacer(modifier = Modifier.height(16.dp))

                ServerConfigField(
                    type = FieldContentType.Encryption,
                    content = contents[FieldContentType.Encryption]!!
                )
                Spacer(modifier = Modifier.height(16.dp))

                V2RaySecurities()
                Spacer(modifier = Modifier.height(16.dp))
                NetworkSecurities()
                Spacer(modifier = Modifier.height(16.dp))
                ServerConfigField(
                    type = FieldContentType.RequestHost,
                    content = contents[FieldContentType.RequestHost]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
                ServerConfigField(
                    type = FieldContentType.Path, content = contents[FieldContentType.Path]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
                TlsFields(contents = contents)
            }
            EConfigType.TROJAN -> {
                ServerConfigField(
                    type = FieldContentType.Password,
                    content = contents[FieldContentType.Password]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
                NetworkSecurities()
                Spacer(modifier = Modifier.height(16.dp))
                ServerConfigField(
                    type = FieldContentType.RequestHost,
                    content = contents[FieldContentType.RequestHost]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
                ServerConfigField(
                    type = FieldContentType.Path, content = contents[FieldContentType.Path]!!
                )
                Spacer(modifier = Modifier.height(16.dp))
                TlsFields(contents = contents)
            }
            else -> {}
        }
    }
}

private enum class FieldContentType {
    Remarks, Address, Port, Id, AlterId, RequestHost, Path, SNI, PublicKey, ShortId, SpiderX, Encryption, Username, PasswordOptional, Password
}

private val textFieldModifier = Modifier
    .fillMaxWidth()
    .height(50.dp)

@Composable
private fun ColumnScope.TlsFields(contents: Map<FieldContentType, MutableState<String>>) {
    val context = LocalContext.current

    val selectableTls = remember {
        context.resources.getStringArray(R.array.streamsecurityxs)
    }
    var selectedTls by remember {
        mutableStateOf(selectableTls.first())
    }

    val selectableFingerprint = remember {
        context.resources.getStringArray(R.array.streamsecurity_utls)
    }
    var selectedFingerprint by remember {
        mutableStateOf(selectableFingerprint.first())
    }

    val selectableAlpn = remember {
        context.resources.getStringArray(R.array.streamsecurity_alpn)
    }
    var selectedAlpn by remember {
        mutableStateOf(selectableAlpn.first())
    }

    DropdownText(title = stringResource(id = R.string.server_lab_stream_security),
        selected = selectedTls,
        selectables = selectableTls,
        onSelect = { selectedTls = it })
    Spacer(modifier = Modifier.height(16.dp))

    if (selectedTls.isNotBlank()) {
        ServerConfigField(
            type = FieldContentType.SNI, content = contents[FieldContentType.SNI]!!
        )
        Spacer(modifier = Modifier.height(16.dp))

        DropdownText(title = stringResource(id = R.string.server_lab_stream_fingerprint),
            selected = selectedFingerprint,
            selectables = selectableFingerprint,
            onSelect = { selectedFingerprint = it })
        Spacer(modifier = Modifier.height(16.dp))

        DropdownText(title = stringResource(id = R.string.server_lab_stream_alpn),
            selected = selectedAlpn,
            selectables = selectableAlpn,
            onSelect = { selectedAlpn = it })
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTls == "tls") {
            AllowInsecureSwitch()
        } else {
            ServerConfigField(
                type = FieldContentType.PublicKey, content = contents[FieldContentType.PublicKey]!!
            )
            Spacer(modifier = Modifier.height(16.dp))

            ServerConfigField(
                type = FieldContentType.ShortId, content = contents[FieldContentType.ShortId]!!
            )
            Spacer(modifier = Modifier.height(16.dp))

            ServerConfigField(
                type = FieldContentType.SpiderX, content = contents[FieldContentType.SpiderX]!!
            )
        }
    }
}

@Composable
private fun ServerConfigField(
    type: FieldContentType, content: MutableState<String>, modifier: Modifier = Modifier
) {
    val titleResId = when (type) {
        FieldContentType.Remarks -> R.string.server_lab_remarks
        FieldContentType.Address -> R.string.server_lab_address3
        FieldContentType.Port -> R.string.server_lab_port3
        FieldContentType.Id -> R.string.server_lab_id
        FieldContentType.AlterId -> R.string.server_lab_alterid
        FieldContentType.RequestHost -> R.string.server_lab_request_host
        FieldContentType.Path -> R.string.server_lab_path
        FieldContentType.SNI -> R.string.server_lab_sni
        FieldContentType.PublicKey -> R.string.server_lab_public_key
        FieldContentType.ShortId -> R.string.server_lab_short_id
        FieldContentType.SpiderX -> R.string.server_lab_spider_x
        FieldContentType.Encryption -> R.string.server_lab_encryption
        FieldContentType.Username -> R.string.server_lab_security4
        FieldContentType.PasswordOptional -> R.string.server_lab_id4
        FieldContentType.Password -> R.string.server_lab_id3
    }

    Column(modifier = modifier) {
        Text(text = stringResource(id = titleResId))
        TextField(
            value = content.value,
            onValueChange = {
                content.value = it
            },
            modifier = textFieldModifier,
            keyboardOptions = if (type == FieldContentType.Port) KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.NumberPassword,
            ) else KeyboardOptions.Default,
        )
    }
}

@Composable
private fun V2RaySecurities() {
    val context = LocalContext.current
    val selectables = remember {
        context.resources.getStringArray(R.array.securitys)
    }

    var selected by remember {
        mutableStateOf(selectables.first())
    }

    DropdownText<String>(
        title = stringResource(id = R.string.server_lab_security),
        selected = selected,
        selectables = selectables,
        onSelect = {
            selected = it
        },
    )
}

@Composable
private fun ShadowsocksSecurities() {
    val context = LocalContext.current
    val selectables = remember {
        context.resources.getStringArray(R.array.ss_securitys)
    }

    var selected by remember {
        mutableStateOf(selectables.first())
    }

    DropdownText<String>(
        title = stringResource(id = R.string.server_lab_security3),
        selected = selected,
        selectables = selectables,
        onSelect = {
            selected = it
        },
    )
}

@Composable
private fun NetworkSecurities() {
    val context = LocalContext.current
    val selectables = remember {
        context.resources.getStringArray(R.array.networks)
    }

    var selected by remember {
        mutableStateOf(selectables.first())
    }

    val headTypes = when (selected) {
        "tcp" -> context.resources.getStringArray(R.array.header_type_tcp)
        "kcp", "quic" -> context.resources.getStringArray(R.array.header_type_kcp_and_quic)
        "grpc" -> context.resources.getStringArray(R.array.mode_type_grpc)
        else -> arrayOf("---")
    }
    var selectedHeadType by remember(selected) {
        mutableStateOf(headTypes.first())
    }

    DropdownText<String>(
        title = stringResource(id = R.string.server_lab_network),
        selected = selected,
        selectables = selectables,
        onSelect = {
            selected = it
        },
    )

    if (headTypes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        DropdownText<String>(
            title = stringResource(id = if (selected == "grpc") R.string.server_lab_mode_type else R.string.server_lab_head_type),
            selected = selectedHeadType,
            selectables = headTypes,
            onSelect = {
                selectedHeadType = it
            },
        )
    }
}

@Composable
private fun <T> DropdownText(
    title: String,
    selected: T,
    selectables: Array<T>,
    modifier: Modifier = Modifier,
    textParser: (T) -> String = { it.toString() },
    onSelect: (T) -> Unit,
) {
    val curTextParser by rememberUpdatedState(newValue = textParser)
    val curOnSelect by rememberUpdatedState(newValue = onSelect)

    var expanded by remember {
        mutableStateOf(false)
    }
    Column(modifier = modifier) {
        Text(text = title)

        Row(
            textFieldModifier.clickable(role = Role.DropdownList) {
                expanded = true
            }, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(curTextParser(selected), modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            selectables.forEach {
                DropdownMenuItem(text = {
                    Text(curTextParser(it))
                }, onClick = {
                    curOnSelect(it)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun AllowInsecureSwitch() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(id = R.string.server_lab_allow_insecure),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = true, onCheckedChange = {

        })
    }
}