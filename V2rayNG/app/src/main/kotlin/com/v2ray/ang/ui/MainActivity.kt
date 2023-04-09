package com.v2ray.ang.ui

import ConfigManagementAction
import MainActivityScreen
import MainAddOption
import MainNavigationDestination
import ServerConfigAction
import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions.RxPermissions
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.R
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.dto.ServersCache
import com.v2ray.ang.extension.toast
import com.v2ray.ang.service.V2RayServiceManager
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import com.v2ray.ang.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.drakeet.support.toast.ToastCompat
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity() {

    private val mainStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_MAIN,
            MMKV.MULTI_PROCESS_MODE
        )
    }
    private val settingsStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_SETTING,
            MMKV.MULTI_PROCESS_MODE
        )
    }
    val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViewModel()
        copyAssets()
        migrateLegacy()

        setContent {
            val isRunning by mainViewModel.isRunning.observeAsState()
            val serversCache by mainViewModel.serverCacheFlow.collectAsState()

            // 标记v2ray是否正在启动
            // TODO: 重构这里
            var isStarting by remember {
                mutableStateOf(false)
            }

            var selectedServerGuid by remember {
                mutableStateOf(mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER))
            }

            val initialTestMessage = stringResource(id = R.string.connection_test_pending)
            var testMessage by remember {
                mutableStateOf(initialTestMessage)
            }
            val owner = LocalLifecycleOwner.current
            LaunchedEffect(Unit) {
                mainViewModel.updateTestResultAction.observe(owner) {
                    testMessage = it
                }
                mainViewModel.isRunning.observe(owner) {
                    testMessage = if (it == true) {
                        getString(R.string.connection_connected)
                    } else {
                        getString(R.string.connection_not_connected)
                    }
                }
            }

            val requestVpnPermission = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
                onResult = {
                    if (it.resultCode == Activity.RESULT_OK) {
                        isStarting = true
                        startV2Ray()
                        isStarting = false
                    }
                })

            MainActivityScreen(
                isRunning = isRunning ?: false,
                testMessage = testMessage,
                configs = serversCache,
                onNavigationItemSelect = this::onNavigate,
                onItemClick = { s, serverConfigAction ->
                    handleSingleServerAction(isRunning ?: false, s, serverConfigAction) {
                        selectedServerGuid = s.guid
                    }
                },
                onSelectAddMethod = this::handleAddMethod,
                onSelectAction = this::handleConfigActions,
                onFabClick = {
                    if (isRunning == true) {
                        Utils.stopVService(this)
                    } else if ((settingsStorage?.decodeString(AppConfig.PREF_MODE)
                            ?: "VPN") == "VPN"
                    ) {
                        val intent = VpnService.prepare(this)
                        if (intent == null) {
                            isStarting = true
                            startV2Ray()
                            isStarting = false
                        } else {
                            requestVpnPermission.launch(intent)
                        }
                    } else {
                        isStarting = true
                        startV2Ray()
                        isStarting = false
                    }
                },
                onStartTest = {
                    if (isRunning == true) {
                        testMessage = getString(R.string.connection_test_testing)
                        mainViewModel.testCurrentServerRealPing()
                    } else {
//                tv_test_state.text = getString(R.string.connection_test_fail)
                    }
                },
                selectedServerGuid = selectedServerGuid,
                onBackPressed = {
                    moveTaskToBack(false)
                }
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RxPermissions(this)
                .request(Manifest.permission.POST_NOTIFICATIONS)
                .subscribe {
                    if (!it)
                        toast(R.string.toast_permission_denied)
                }
        }
    }

    private fun setupViewModel() {
        mainViewModel.startListenBroadcast()
    }

    private fun copyAssets() {
        val extFolder = Utils.userAssetPath(this)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geo = arrayOf("geosite.dat", "geoip.dat")
                assets.list("")
                    ?.filter { geo.contains(it) }
                    ?.filter { !File(extFolder, it).exists() }
                    ?.forEach {
                        val target = File(extFolder, it)
                        assets.open(it).use { input ->
                            FileOutputStream(target).use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.i(
                            ANG_PACKAGE,
                            "Copied from apk assets folder to ${target.absolutePath}"
                        )
                    }
            } catch (e: Exception) {
                Log.e(ANG_PACKAGE, "asset copy failed", e)
            }
        }
    }

    private fun migrateLegacy() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = AngConfigManager.migrateLegacyConfig(this@MainActivity)
            if (result != null) {
                launch(Dispatchers.Main) {
                    if (result) {
                        toast(getString(R.string.migration_success))
                        mainViewModel.reloadServerList()
                    } else {
                        toast(getString(R.string.migration_fail))
                    }
                }
            }
        }
    }

    fun startV2Ray() {
        if (mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER).isNullOrEmpty()) {
            return
        }
//        showCircle()
//        toast(R.string.toast_services_start)
        V2RayServiceManager.startV2Ray(this)
        hideCircle()
    }

    fun restartV2Ray() {
        if (mainViewModel.isRunning.value == true) {
            Utils.stopVService(this)
        }
        Observable.timer(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                startV2Ray()
            }
    }

    public override fun onResume() {
        super.onResume()
        mainViewModel.reloadServerList()
    }

    public override fun onPause() {
        super.onPause()
    }

    private fun handleSingleServerAction(
        isRunning: Boolean,
        server: ServersCache,
        action: ServerConfigAction,
        onSelect: () -> Unit
    ) {
        val guid = server.guid
        val config = server.config

        when (action) {
            ServerConfigAction.Apply -> {
                val selected = mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER)
                if (guid != selected) {
                    mainStorage?.encode(MmkvManager.KEY_SELECTED_SERVER, guid)
                    onSelect()

                    if (isRunning) {
                        showCircle()
                        Utils.stopVService(this)

                        lifecycleScope.launch {
                            delay(500)
                            V2RayServiceManager.startV2Ray(this@MainActivity)
                            hideCircle()
                        }
                    }
                }
            }
            is ServerConfigAction.Share -> {
                when (action.type) {
                    0 -> {
                        if (config.configType == EConfigType.CUSTOM) {
                            shareFullContent(guid)
                        } else {
                            val bitmap = AngConfigManager.share2QRCode(guid)

                            AlertDialog.Builder(this).setView(ImageView(this).apply {
                                setImageBitmap(bitmap)
                            }).show()
                        }
                    }
                    1 -> {
                        if (AngConfigManager.share2Clipboard(this, guid) == 0) {
                            toast(R.string.toast_success)
                        } else {
                            toast(R.string.toast_failure)
                        }
                    }
                    2 -> shareFullContent(guid)
                    else -> toast("else")
                }
            }
            ServerConfigAction.Edit -> {
                val intent = Intent().putExtra("guid", guid)
                    .putExtra("isRunning", isRunning)
                if (config.configType == EConfigType.CUSTOM) {
                    startActivity(intent.setClass(this, ServerCustomConfigActivity::class.java))
                } else {
                    startActivity(intent.setClass(this, ServerActivity::class.java))
                }
            }
            ServerConfigAction.Delete -> {
                if (guid != mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER)) {
                    if (settingsStorage?.decodeBool(AppConfig.PREF_CONFIRM_REMOVE) == true) {
                        AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                removeServer(guid)
                            }
                            .show()
                    } else {
                        removeServer(guid)
                    }
                }
            }
            else -> {}
        }
    }

    private fun shareFullContent(guid: String) {
        if (AngConfigManager.shareFullContent2Clipboard(this, guid) == 0) {
            toast(R.string.toast_success)
        } else {
            toast(R.string.toast_failure)
        }
    }

    private fun removeServer(guid: String) {
        mainViewModel.removeServer(guid)
    }

    private fun handleAddMethod(method: MainAddOption) {
        when (method) {
            MainAddOption.ImportQR -> importQRcode(true)
            MainAddOption.ImportClipboard -> importClipboard()
            MainAddOption.ImportVmess -> importManually(EConfigType.VMESS.value)
            MainAddOption.ImportVless -> importManually(EConfigType.VLESS.value)
            MainAddOption.ImportShadowsocks -> importManually(EConfigType.SHADOWSOCKS.value)
            MainAddOption.ImportSocks -> importManually(EConfigType.SOCKS.value)
            MainAddOption.ImportTrojan -> importManually(EConfigType.TROJAN.value)
            MainAddOption.CustomConfigClipboard -> importConfigCustomClipboard()
            MainAddOption.CustomConfigLocal -> importConfigCustomLocal()
            MainAddOption.CustomConfigUrl -> importConfigCustomUrlClipboard()
            MainAddOption.CustomConfigUrlScan -> importQRcode(false)
        }
    }

    private fun handleConfigActions(action: ConfigManagementAction) {
        when (action) {
            ConfigManagementAction.RestartService -> restartV2Ray()
            ConfigManagementAction.DeleteAllConfig -> {
                AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        MmkvManager.removeAllServer()
                        mainViewModel.reloadServerList()
                    }
                    .show()
            }
            ConfigManagementAction.DeleteDuplicateConfig -> {
                AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        mainViewModel.removeDuplicateServer()
                    }
                    .show()
            }
            ConfigManagementAction.DeleteInvalidConfig -> {
                AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        MmkvManager.removeInvalidServer()
                        mainViewModel.reloadServerList()
                    }
                    .show()
            }
            ConfigManagementAction.ExportAllConfig -> {
                if (AngConfigManager.shareNonCustomConfigsToClipboard(
                        this,
                        mainViewModel.serverList
                    ) == 0
                ) {
                    toast(R.string.toast_success)
                } else {
                    toast(R.string.toast_failure)
                }
            }
            ConfigManagementAction.PingAllConfig -> mainViewModel.testAllTcping()
            ConfigManagementAction.RealPingAllConfig -> mainViewModel.testAllRealPing()
            ConfigManagementAction.SortByTestResults -> {
                MmkvManager.sortByTestResults()
                mainViewModel.reloadServerList()
            }
            ConfigManagementAction.UpdateSubscription -> importConfigViaSub()
        }
    }

    private fun importManually(createConfigType: Int) {
        startActivity(
            Intent()
                .putExtra("createConfigType", createConfigType)
                .putExtra("subscriptionId", mainViewModel.subscriptionId)
                .setClass(this, ServerActivity::class.java)
        )
    }

    /**
     * import config from qrcode
     */
    fun importQRcode(forConfig: Boolean): Boolean {
//        try {
//            startActivityForResult(Intent("com.google.zxing.client.android.SCAN")
//                    .addCategory(Intent.CATEGORY_DEFAULT)
//                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), requestCode)
//        } catch (e: Exception) {
        RxPermissions(this)
            .request(Manifest.permission.CAMERA)
            .subscribe {
                if (it)
                    if (forConfig)
                        scanQRCodeForConfig.launch(Intent(this, ScannerActivity::class.java))
                    else
                        scanQRCodeForUrlToCustomConfig.launch(
                            Intent(
                                this,
                                ScannerActivity::class.java
                            )
                        )
                else
                    toast(R.string.toast_permission_denied)
            }
//        }
        return true
    }

    private val scanQRCodeForConfig =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                importBatchConfig(it.data?.getStringExtra("SCAN_RESULT"))
            }
        }

    private val scanQRCodeForUrlToCustomConfig =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                importConfigCustomUrl(it.data?.getStringExtra("SCAN_RESULT"))
            }
        }

    /**
     * import config from clipboard
     */
    fun importClipboard()
            : Boolean {
        try {
            val clipboard = Utils.getClipboard(this)
            importBatchConfig(clipboard)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun importBatchConfig(server: String?, subid: String = "") {
        val subid2 = if (subid.isNullOrEmpty()) {
            mainViewModel.subscriptionId
        } else {
            subid
        }
        val append = subid.isNullOrEmpty()

        var count = AngConfigManager.importBatchConfig(server, subid2, append)
        if (count <= 0) {
            count = AngConfigManager.importBatchConfig(Utils.decode(server!!), subid2, append)
        }
        if (count > 0) {
            toast(R.string.toast_success)
            mainViewModel.reloadServerList()
        } else {
            toast(R.string.toast_failure)
        }
    }

    fun importConfigCustomClipboard()
            : Boolean {
        try {
            val configText = Utils.getClipboard(this)
            if (TextUtils.isEmpty(configText)) {
                toast(R.string.toast_none_data_clipboard)
                return false
            }
            importCustomizeConfig(configText)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * import config from local config file
     */
    fun importConfigCustomLocal(): Boolean {
        try {
            showFileChooser()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun importConfigCustomUrlClipboard()
            : Boolean {
        try {
            val url = Utils.getClipboard(this)
            if (TextUtils.isEmpty(url)) {
                toast(R.string.toast_none_data_clipboard)
                return false
            }
            return importConfigCustomUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * import config from url
     */
    fun importConfigCustomUrl(url: String?): Boolean {
        try {
            if (!Utils.isValidUrl(url)) {
                toast(R.string.toast_invalid_url)
                return false
            }
            lifecycleScope.launch(Dispatchers.IO) {
                val configText = try {
                    Utils.getUrlContentWithCustomUserAgent(url)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
                launch(Dispatchers.Main) {
                    importCustomizeConfig(configText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * import config from sub
     */
    fun importConfigViaSub()
            : Boolean {
        try {
            toast(R.string.title_sub_update)
            MmkvManager.decodeSubscriptions().forEach {
                if (TextUtils.isEmpty(it.first)
                    || TextUtils.isEmpty(it.second.remarks)
                    || TextUtils.isEmpty(it.second.url)
                ) {
                    return@forEach
                }
                if (!it.second.enabled) {
                    return@forEach
                }
                val url = Utils.idnToASCII(it.second.url)
                if (!Utils.isValidUrl(url)) {
                    return@forEach
                }
                Log.d(ANG_PACKAGE, url)
                lifecycleScope.launch(Dispatchers.IO) {
                    val configText = try {
                        Utils.getUrlContentWithCustomUserAgent(url)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            toast("\"" + it.second.remarks + "\" " + getString(R.string.toast_failure))
                        }
                        return@launch
                    }
                    launch(Dispatchers.Main) {
                        importBatchConfig(configText, it.first)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * show file chooser
     */
    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            chooseFileForCustomConfig.launch(
                Intent.createChooser(
                    intent,
                    getString(R.string.title_file_chooser)
                )
            )
        } catch (ex: ActivityNotFoundException) {
            toast(R.string.toast_require_file_manager)
        }
    }

    private val chooseFileForCustomConfig =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri = it.data?.data
            if (it.resultCode == RESULT_OK && uri != null) {
                readContentFromUri(uri)
            }
        }

    /**
     * read content from uri
     */
    private fun readContentFromUri(uri: Uri) {
        RxPermissions(this)
            .request(Manifest.permission.READ_EXTERNAL_STORAGE)
            .subscribe {
                if (it) {
                    try {
                        contentResolver.openInputStream(uri).use { input ->
                            importCustomizeConfig(input?.bufferedReader()?.readText())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else
                    toast(R.string.toast_permission_denied)
            }
    }

    /**
     * import customize config
     */
    fun importCustomizeConfig(server: String?) {
        try {
            if (server == null || TextUtils.isEmpty(server)) {
                toast(R.string.toast_none_data)
                return
            }
            mainViewModel.appendCustomConfigServer(server)
            mainViewModel.reloadServerList()
            toast(R.string.toast_success)
            //adapter.notifyItemInserted(mainViewModel.serverList.lastIndex)
        } catch (e: Exception) {
            ToastCompat.makeText(
                this,
                "${getString(R.string.toast_malformed_josn)} ${e.cause?.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
            return
        }
    }


//    val mConnection = object : ServiceConnection {
//        override fun onServiceDisconnected(name: ComponentName?) {
//        }
//
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            sendMsg(AppConfig.MSG_REGISTER_CLIENT, "")
//        }
//    }

    fun showCircle() {
        //binding.fabProgressCircle.show()
    }

    fun hideCircle() {
        /*try {
            Observable.timer(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    try {
                        if (binding.fabProgressCircle.isShown) {
                            binding.fabProgressCircle.hide()
                        }
                    } catch (e: Exception) {
                        Log.w(ANG_PACKAGE, e)
                    }
                }
        } catch (e: Exception) {
            Log.d(ANG_PACKAGE, e.toString())
        }*/
    }

    private fun onNavigate(destination: MainNavigationDestination) {
        when (destination) {
            MainNavigationDestination.SubSettings -> {
                startActivity(Intent(this, SubSettingActivity::class.java))
            }
            MainNavigationDestination.Settings -> {
                startActivity(
                    Intent(this, SettingsActivity::class.java)
                        .putExtra("isRunning", mainViewModel.isRunning.value == true)
                )
            }
            MainNavigationDestination.UserAssetSettings -> {
                startActivity(Intent(this, UserAssetActivity::class.java))
            }
            MainNavigationDestination.Feedback -> {
                Utils.openUri(this, AppConfig.v2rayNGIssues)
            }
            MainNavigationDestination.Promotion -> {
                Utils.openUri(
                    this,
                    "${Utils.decode(AppConfig.promotionUrl)}?t=${System.currentTimeMillis()}"
                )
            }
            MainNavigationDestination.Logcat -> {
                startActivity(Intent(this, LogcatActivity::class.java))
            }
        }
    }
}
