package cc.kesardev.scparchive.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import cc.kesardev.scparchive.BaseActivity
import cc.kesardev.scparchive.BuildConfig
import cc.kesardev.scparchive.R
import cc.kesardev.scparchive.catalog.CatalogActivity
import cc.kesardev.scparchive.catalog.CatalogDatabase
import cc.kesardev.scparchive.data.NetworkClient
import cc.kesardev.scparchive.data.ProxyConfig
import cc.kesardev.scparchive.data.ProxyKind
import cc.kesardev.scparchive.data.ProxySettings
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import cc.kesardev.scparchive.ui.BottomTabs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity() {

    private val preferences by lazy { AppearancePreferences(this) }
    private val proxySettings by lazy { ProxySettings(this) }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var themeGroup: RadioGroup
    private lateinit var catalogStatus: TextView
    private lateinit var proxyEnabled: MaterialSwitch
    private lateinit var proxyKindGroup: RadioGroup
    private lateinit var proxyAddress: EditText
    private lateinit var proxyStatus: TextView
    private lateinit var proxyTest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        themeGroup = findViewById(R.id.theme_group)
        themeGroup.check(checkedIdFor(preferences.mode()))
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = modeFor(checkedId) ?: return@setOnCheckedChangeListener
            if (selected == preferences.mode()) {
                return@setOnCheckedChangeListener
            }
            preferences.setMode(selected)
            AppCompatDelegate.setDefaultNightMode(AppearancePreferences.nightMode(selected))
        }

        catalogStatus = findViewById(R.id.settings_catalog_status)
        findViewById<View>(R.id.settings_sync_card).setOnClickListener {
            startActivity(
                Intent(this, CatalogActivity::class.java)
                    .putExtra("sync", true)
            )
        }

        setupProxy()

        findViewById<TextView>(R.id.settings_version).text =
            getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME

        BottomTabs.bind(this, R.id.tab_settings)
    }

    override fun onResume() {
        super.onResume()
        BottomTabs.select(this, R.id.tab_settings)
        updateCatalogStatus()
    }

    override fun onPause() {
        persistProxy()
        super.onPause()
    }

    private fun setupProxy() {
        proxyEnabled = findViewById(R.id.proxy_enabled)
        proxyKindGroup = findViewById(R.id.proxy_kind_group)
        proxyAddress = findViewById(R.id.proxy_address)
        proxyStatus = findViewById(R.id.proxy_status)
        proxyTest = findViewById(R.id.proxy_test)

        val config = proxySettings.load()
        proxyAddress.setText(config.address)
        proxyHttpOrSocks(config.kind).isChecked = true
        proxyEnabled.isChecked = config.enabled
        updateProxyStatus(config)

        proxyEnabled.setOnCheckedChangeListener { _, _ -> persistProxy() }
        proxyKindGroup.setOnCheckedChangeListener { _, _ -> persistProxy() }
        proxyTest.setOnClickListener { saveAndTestProxy() }

        if (intent.getBooleanExtra("test_proxy", false)) {
            proxyTest.post { saveAndTestProxy() }
        }
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun proxyHttpOrSocks(kind: ProxyKind): RadioButton {
        return findViewById(if (kind == ProxyKind.SOCKS) R.id.proxy_socks else R.id.proxy_http)
    }

    private fun readProxyConfig(): ProxyConfig {
        return ProxyConfig(
            enabled = proxyEnabled.isChecked,
            kind = if (proxyKindGroup.checkedRadioButtonId == R.id.proxy_socks) {
                ProxyKind.SOCKS
            } else {
                ProxyKind.HTTP
            },
            address = proxyAddress.text.toString().trim()
        )
    }

    private fun persistProxy() {
        if (!::proxyEnabled.isInitialized) {
            return
        }
        val config = readProxyConfig()
        proxySettings.save(config)
        NetworkClient.reset()
        updateProxyStatus(config)
    }

    private fun updateProxyStatus(config: ProxyConfig) {
        if (!config.enabled) {
            proxyStatus.text = "未启用"
            return
        }
        val endpoint = config.endpoint()
        proxyStatus.text = if (endpoint == null) {
            "代理地址无效，请填写 host:port"
        } else {
            "已启用 · " + endpoint.label()
        }
    }

    private fun saveAndTestProxy() {
        val config = readProxyConfig()
        proxySettings.save(config)
        NetworkClient.reset()
        updateProxyStatus(config)
        if (config.endpoint() == null) {
            proxyStatus.text = "代理地址无效，请填写 host:port"
            return
        }
        proxyStatus.text = "正在测试代理…"
        lifecycleScope.launch {
            val result = NetworkClient.test(
                this@SettingsActivity,
                config,
                ReaderShellBuilder.BASE_URL
            )
            proxyStatus.text = result.message
        }
    }

    private fun updateCatalogStatus() {
        val count = runCatching { CatalogDatabase(this).count() }.getOrDefault(0)
        catalogStatus.text = if (count > 0) {
            "已索引 " + count + " 篇 · 15 个系列"
        } else {
            "更新 15 个系列目录，供搜索与随机使用"
        }
    }

    private fun checkedIdFor(mode: ThemeMode): Int {
        return when (mode) {
            ThemeMode.SYSTEM -> R.id.theme_system
            ThemeMode.LIGHT -> R.id.theme_light
            ThemeMode.DARK -> R.id.theme_dark
        }
    }

    private fun modeFor(checkedId: Int): ThemeMode? {
        return when (checkedId) {
            R.id.theme_system -> ThemeMode.SYSTEM
            R.id.theme_light -> ThemeMode.LIGHT
            R.id.theme_dark -> ThemeMode.DARK
            else -> null
        }
    }
}