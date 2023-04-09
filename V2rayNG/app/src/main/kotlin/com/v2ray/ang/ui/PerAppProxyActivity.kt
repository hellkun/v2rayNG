package com.v2ray.ang.ui

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityBypassListBinding
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.v2RayApplication
import com.v2ray.ang.ui.compose.PerAppProxyActivityScreen
import com.v2ray.ang.util.AppManagerUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.text.Collator
import java.util.*

class PerAppProxyActivity : BaseActivity() {
    private lateinit var binding: ActivityBypassListBinding

    private var adapter: PerAppProxyAdapter? = null
    private var appsAll: List<AppInfo>? = null
    private val defaultSharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PerAppProxyActivityScreen(onBack = this::finish)
        }
    }

    override fun onPause() {
        super.onPause()
        adapter?.let {
            defaultSharedPreferences.edit().putStringSet(AppConfig.PREF_PER_APP_PROXY_SET, it.blacklist).apply()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bypass_list, menu)

        val searchItem = menu.findItem(R.id.search_view)
        if (searchItem != null) {
            val searchView = searchItem.actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterProxyApp(newText!!)
                    return false
                }
            })
        }


        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.select_all -> adapter?.let {
            val pkgNames = it.apps.map { it.packageName }
            if (it.blacklist.containsAll(pkgNames)) {
                it.apps.forEach {
                    val packageName = it.packageName
                    adapter?.blacklist!!.remove(packageName)
                }
            } else {
                it.apps.forEach {
                    val packageName = it.packageName
                    adapter?.blacklist!!.add(packageName)
                }
            }
            it.notifyDataSetChanged()
            true
        } ?: false
        R.id.select_proxy_app -> {
            selectProxyApp()
            true
        }
        R.id.import_proxy_app -> {
            importProxyApp()
            true
        }
        R.id.export_proxy_app -> {
            exportProxyApp()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun selectProxyApp() {
        toast(R.string.msg_downloading_content)
        val url = AppConfig.androidpackagenamelistUrl
        lifecycleScope.launch(Dispatchers.IO) {
            val content = Utils.getUrlContext(url, 5000)
            launch(Dispatchers.Main) {
                Log.d(ANG_PACKAGE, content)
                selectProxyApp(content, true)
                toast(R.string.toast_success)
            }
        }
    }

    private fun importProxyApp() {
        val content = Utils.getClipboard(applicationContext)
        if (TextUtils.isEmpty(content)) {
            return
        }
        selectProxyApp(content, false)
        toast(R.string.toast_success)
    }

    private fun exportProxyApp() {
        var lst = binding.switchBypassApps.isChecked.toString()

        adapter?.blacklist?.forEach block@{
            lst = lst + System.getProperty("line.separator") + it
        }
        Utils.setClipboard(applicationContext, lst)
        toast(R.string.toast_success)
    }

    private fun selectProxyApp(content: String, force: Boolean): Boolean {
        try {
            val proxyApps = if (TextUtils.isEmpty(content)) {
                Utils.readTextFromAssets(v2RayApplication, "proxy_packagename.txt")
            } else {
                content
            }
            if (TextUtils.isEmpty(proxyApps)) {
                return false
            }

            adapter?.blacklist!!.clear()

            if (binding.switchBypassApps.isChecked) {
                adapter?.let {
                    it.apps.forEach block@{
                        val packageName = it.packageName
                        Log.d(ANG_PACKAGE, packageName)
                        if (!inProxyApps(proxyApps, packageName, force)) {
                            adapter?.blacklist!!.add(packageName)
                            println(packageName)
                            return@block
                        }
                    }
                    it.notifyDataSetChanged()
                }
            } else {
                adapter?.let {
                    it.apps.forEach block@{
                        val packageName = it.packageName
                        Log.d(ANG_PACKAGE, packageName)
                        if (inProxyApps(proxyApps, packageName, force)) {
                            adapter?.blacklist!!.add(packageName)
                            println(packageName)
                            return@block
                        }
                    }
                    it.notifyDataSetChanged()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun inProxyApps(proxyApps: String, packageName: String, force: Boolean): Boolean {
        if (force) {
            if (packageName == "com.google.android.webview") {
                return false
            }
            if (packageName.startsWith("com.google")) {
                return true
            }
        }

        return proxyApps.indexOf(packageName) >= 0
    }

    private fun filterProxyApp(content: String): Boolean {
        val apps = ArrayList<AppInfo>()

        val key = content.uppercase()
        if (key.isNotEmpty()) {
            appsAll?.forEach {
                if (it.appName.uppercase().indexOf(key) >= 0
                        || it.packageName.uppercase().indexOf(key) >= 0) {
                    apps.add(it)
                }
            }
        } else {
            appsAll?.forEach {
                apps.add(it)
            }
        }

        adapter = PerAppProxyAdapter(this, apps, adapter?.blacklist)
        binding.recyclerView.adapter = adapter
        adapter?.notifyDataSetChanged()
        return true
    }
}
