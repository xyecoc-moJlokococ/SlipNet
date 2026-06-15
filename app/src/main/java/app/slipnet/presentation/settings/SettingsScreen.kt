package app.slipnet.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.FontFamily
import android.provider.Settings
import app.slipnet.BuildConfig
import app.slipnet.presentation.common.components.AboutDialogContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.slipnet.data.local.datastore.AppLanguage
import app.slipnet.data.local.datastore.DarkMode
import app.slipnet.data.local.datastore.DnsWorkerMode
import app.slipnet.data.local.datastore.DomainRoutingMode
import app.slipnet.data.local.datastore.SplitTunnelingMode
import app.slipnet.data.local.datastore.SshCipher
import app.slipnet.tunnel.GeoBypassCountry
import app.slipnet.presentation.localization.tx
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScanner: (() -> Unit)? = null,
    onNavigateToAppSelector: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showSshCipherDialog by remember { mutableStateOf(false) }
    var showSplitModeDialog by remember { mutableStateOf(false) }
    var showDomainRoutingModeDialog by remember { mutableStateOf(false) }
    var showDomainManagementDialog by remember { mutableStateOf(false) }
    var showGeoBypassCountryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showRemoteDnsDialog by remember { mutableStateOf(false) }
    var showGlobalResolverDialog by remember { mutableStateOf(false) }
    var showDnsPoolDialog by remember { mutableStateOf(false) }
    var showMtuDialog by remember { mutableStateOf(false) }
    var showBandwidthLimitDialog by remember { mutableStateOf(false) }
    var showDnsWorkerDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }

    // Proxy settings - local state for text fields to avoid cursor jumps from async DataStore round-trip
    var proxyPort by remember { mutableStateOf(uiState.proxyListenPort.toString()) }
    var httpProxyPort by remember { mutableStateOf(uiState.httpProxyPort.toString()) }
    var proxyAuthUsername by remember { mutableStateOf(uiState.proxyAuthUsername) }
    var proxyAuthPassword by remember { mutableStateOf(uiState.proxyAuthPassword) }

    // Sync local state when DataStore values load (initial default → actual saved value)
    LaunchedEffect(uiState.proxyListenPort) {
        proxyPort = uiState.proxyListenPort.toString()
    }
    LaunchedEffect(uiState.httpProxyPort) {
        httpProxyPort = uiState.httpProxyPort.toString()
    }
    LaunchedEffect(uiState.proxyAuthUsername) {
        if (uiState.proxyAuthUsername != proxyAuthUsername) proxyAuthUsername = uiState.proxyAuthUsername
    }
    LaunchedEffect(uiState.proxyAuthPassword) {
        if (uiState.proxyAuthPassword != proxyAuthPassword) proxyAuthPassword = uiState.proxyAuthPassword
    }

    val addressOptions = getAddressOptions()

    // Battery optimization state
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var isBatteryOptimized by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                isBatteryOptimized = !pm.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tx("Settings", "Настройки")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tx("Back", "Назад"))
                    }
                },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.Default.Help, contentDescription = tx("About", "О программе"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp + navBarPadding.calculateBottomPadding()
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Donate Card
            DonateCard()

            // Connection Settings
            SettingsSection(title = tx("Connection", "Подключение")) {
                SwitchSettingItem(
                    icon = Icons.Default.PowerSettingsNew,
                    title = tx("Auto-connect on boot", "Автоподключение при запуске"),
                    description = tx("Automatically connect when device starts", "Автоматически подключаться после запуска устройства"),
                    checked = uiState.autoConnectOnBoot,
                    onCheckedChange = { viewModel.setAutoConnectOnBoot(it) }
                )

                SettingsDivider()

                SwitchSettingItem(
                    icon = Icons.Default.SettingsEthernet,
                    title = tx("Proxy-only mode", "Только прокси"),
                    description = tx("Expose SOCKS5 proxy without creating VPN tunnel", "Запускать SOCKS5-прокси без VPN-туннеля"),
                    checked = uiState.proxyOnlyMode,
                    onCheckedChange = { viewModel.setProxyOnlyMode(it) }
                )

                SettingsDivider()

                SwitchSettingItem(
                    icon = Icons.Default.Shield,
                    title = tx("Kill switch", "Kill switch"),
                    description = tx("Block all traffic if VPN connection drops", "Блокировать весь трафик при обрыве VPN"),
                    checked = uiState.killSwitch,
                    onCheckedChange = { viewModel.setKillSwitch(it) }
                )

                SettingsDivider()

                SwitchSettingItem(
                    icon = Icons.Default.Sync,
                    title = tx("Auto-reconnect", "Автопереподключение"),
                    description = tx("Automatically reconnect if VPN drops unexpectedly", "Автоматически переподключаться при неожиданном обрыве VPN"),
                    checked = uiState.autoReconnect,
                    onCheckedChange = { viewModel.setAutoReconnect(it) }
                )

                SettingsDivider()

                SwitchSettingItem(
                    icon = Icons.Default.Notifications,
                    title = tx("Notification traffic counter", "Счетчик трафика в уведомлении"),
                    description = tx("Show upload/download speed and data usage in notification", "Показывать скорость и расход трафика в уведомлении"),
                    checked = uiState.showNotificationTraffic,
                    onCheckedChange = { viewModel.setShowNotificationTraffic(it) }
                )

                SettingsDivider()

                val sleepTimerOffLabel = tx("Off", "Выкл")
                StepperSettingItem(
                    icon = Icons.Default.Timer,
                    title = tx("Sleep timer", "Таймер отключения"),
                    description = tx("Auto-disconnect after a set time", "Автоматически отключать VPN через заданное время"),
                    value = uiState.sleepTimerMinutes,
                    step = 5,
                    range = 0..120,
                    valueFormatter = { if (it == 0) sleepTimerOffLabel else "$it min" },
                    onValueChange = { viewModel.setSleepTimerMinutes(it) }
                )

                SettingsDivider()

                ClickableSettingItem(
                    icon = Icons.Default.BatteryAlert,
                    title = tx("Battery optimization", "Оптимизация батареи"),
                    description = if (isBatteryOptimized) {
                        tx("Not exempted - VPN may disconnect in background", "Не отключена - VPN может отключаться в фоне")
                    } else {
                        tx("Exempted - VPN will run reliably in background", "Отключена - VPN будет надежнее работать в фоне")
                    },
                    onClick = {
                        if (isBatteryOptimized) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        } else {
                            // Already exempted — open system battery settings so user can change it
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // Tools Section
            if (onNavigateToScanner != null) {
                SettingsSection(title = tx("Tools", "Инструменты")) {
                    ClickableSettingItem(
                        icon = Icons.Default.Search,
                        title = tx("DNS Resolver Scanner", "Сканер DNS-резолверов"),
                        description = tx("Find working DNS resolvers for your profiles", "Найти рабочие DNS-резолверы для профилей"),
                        onClick = onNavigateToScanner
                    )
                }
            }

            // Proxy Settings
            SettingsSection(
                title = tx("Proxy Settings", "Настройки прокси")
            ) {
                AddressSettingItem(
                    value = uiState.proxyListenAddress,
                    options = addressOptions,
                    onValueChange = {
                        viewModel.setProxyListenAddress(it)
                    }
                )

                SettingsDivider()

                val portsConflict = proxyPort.toIntOrNull() == httpProxyPort.toIntOrNull() && proxyPort.isNotBlank()

                TextFieldSettingItem(
                    icon = Icons.Default.Numbers,
                    title = tx("Listen Port", "Порт прослушивания"),
                    value = proxyPort,
                    placeholder = "10880",
                    supportingText = if (portsConflict) tx("Must differ from HTTP proxy port", "Должен отличаться от HTTP proxy port") else tx("Local SOCKS5 proxy port", "Локальный порт SOCKS5-прокси"),
                    isError = portsConflict,
                    keyboardType = KeyboardType.Number,
                    onValueChange = { text ->
                        proxyPort = text
                        text.toIntOrNull()?.let { viewModel.setProxyListenPort(it) }
                    }
                )

                SettingsDivider()

                SwitchSettingItem(
                    icon = Icons.Default.Lock,
                    title = tx("Proxy Authentication", "Аутентификация прокси"),
                    description = if (uiState.proxyAuthEnabled) tx("Username/password required to use the proxy", "Для прокси нужен логин и пароль")
                        else tx("Any app can use the local proxy without credentials", "Любое приложение может использовать локальный прокси без пароля"),
                    checked = uiState.proxyAuthEnabled,
                    onCheckedChange = { viewModel.setProxyAuthEnabled(it) }
                )

                if (uiState.proxyAuthEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = proxyAuthUsername,
                            onValueChange = { text ->
                                proxyAuthUsername = text
                                viewModel.setProxyAuthUsername(text)
                            },
                            label = { Text("Username") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = proxyAuthPassword,
                            onValueChange = { text ->
                                proxyAuthPassword = text
                                viewModel.setProxyAuthPassword(text)
                            },
                            label = { Text("Password") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = "Keeping authentication enabled is recommended for security.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                SettingsDivider()

                SwitchSettingItem(
                    icon = Icons.Default.Lan,
                    title = tx("HTTP proxy", "HTTP-прокси"),
                    description = tx("Enable HTTP proxy for devices that don't support SOCKS5", "Включить HTTP-прокси для устройств без SOCKS5"),
                    checked = uiState.httpProxyEnabled,
                    onCheckedChange = { viewModel.setHttpProxyEnabled(it) }
                )

                if (uiState.httpProxyEnabled) {
                    SettingsDivider()

                    TextFieldSettingItem(
                        icon = Icons.Default.Numbers,
                        title = "HTTP Proxy Port",
                        value = httpProxyPort,
                        placeholder = "8080",
                        supportingText = if (portsConflict) "Must differ from SOCKS5 listen port" else "Local HTTP proxy port",
                        isError = portsConflict,
                        keyboardType = KeyboardType.Number,
                        onValueChange = { text ->
                            httpProxyPort = text
                            text.toIntOrNull()?.let { viewModel.setHttpProxyPort(it) }
                        }
                    )
                }

                if (uiState.proxyListenAddress == "0.0.0.0") {
                    HotspotInfoCard(
                        socksPort = uiState.proxyListenPort,
                        httpProxyEnabled = uiState.httpProxyEnabled,
                        httpProxyPort = uiState.httpProxyPort
                    )
                } else {
                    LocalProxyInfoCard(
                        listenAddress = uiState.proxyListenAddress,
                        socksPort = uiState.proxyListenPort,
                        httpProxyEnabled = uiState.httpProxyEnabled,
                        httpProxyPort = uiState.httpProxyPort
                    )
                }
            }

            // DNS Settings
            SettingsSection(
                title = "DNS",
                subtitle = tx("Changes apply on next connection", "Изменения применятся при следующем подключении")
            ) {
                // The override only actually applies when the toggle is on AND
                // the IP list is non-empty (matches SlipNetVpnService's
                // `parsedGlobalResolvers().takeIf { it.isNotEmpty() }`). When
                // active, the pool scan is short-circuited in applyDnsPoolIfEnabled.
                val overrideActive = uiState.globalResolverEnabled &&
                    uiState.globalResolverList.isNotBlank()

                // Global resolver override
                SwitchSettingItem(
                    icon = Icons.Default.AltRoute,
                    title = tx("Global resolver override", "Принудительные резолверы"),
                    description = if (uiState.globalResolverEnabled) {
                        uiState.globalResolverList.ifBlank { tx("No IPs set - tap Edit IPs below", "IP не заданы - нажми «Изменить IP» ниже") }
                    } else {
                        tx("Force a fixed resolver list across all profiles", "Использовать один список резолверов для всех профилей")
                    },
                    checked = uiState.globalResolverEnabled,
                    onCheckedChange = { viewModel.setGlobalResolverEnabled(it) }
                )
                if (uiState.globalResolverEnabled) {
                    IndentedSettingItem(
                        icon = Icons.Default.Edit,
                        title = tx("Edit IPs", "Изменить IP"),
                        description = uiState.globalResolverList.ifBlank { tx("Tap to set resolver IPs", "Нажми, чтобы задать IP резолверов") },
                        onClick = { showGlobalResolverDialog = true }
                    )
                }

                // DNS pool — muted when the override is actually active
                SwitchSettingItem(
                    icon = Icons.Default.AutoAwesome,
                    title = tx("DNS pool", "DNS-пул"),
                    description = tx("Auto-pick the fastest resolvers on each connect", "Автоматически выбирать самые быстрые резолверы при подключении"),
                    checked = uiState.dnsPoolEnabled,
                    onCheckedChange = { viewModel.setDnsPoolEnabled(it) },
                    inactive = overrideActive
                )
                if (uiState.dnsPoolEnabled) {
                    IndentedSettingItem(
                        icon = Icons.Default.Edit,
                        title = tx("Edit pool", "Изменить пул"),
                        description = tx("Tap to manage the candidate list", "Нажми, чтобы изменить список кандидатов"),
                        onClick = { showDnsPoolDialog = true },
                        inactive = overrideActive
                    )
                    SwitchSettingItem(
                        icon = Icons.Default.VerifiedUser,
                        title = tx("HTTP/SSH verification", "HTTP/SSH-проверка"),
                        description = if (uiState.dnsPoolFullVerification)
                            tx("Verifies real traffic through tunnel - 18s timeout per resolver", "Проверяет реальный трафик через туннель - таймаут 18 с на резолвер")
                        else
                            tx("Handshake-only - faster scan, 10s timeout per resolver", "Только handshake - быстрее, таймаут 10 с на резолвер"),
                        checked = uiState.dnsPoolFullVerification,
                        onCheckedChange = { viewModel.setDnsPoolFullVerification(it) },
                        inactive = overrideActive
                    )
                }

                // Conflict notice — only when override is *actually* active
                if (overrideActive && uiState.dnsPoolEnabled) {
                    InfoNoticeRow(
                        text = tx("Override active - pool is ignored.", "Принудительные резолверы активны - пул игнорируется.")
                    )
                }

                // Remote DNS server (always shown — system-level fallback)
                ClickableSettingItem(
                    icon = Icons.Default.Dns,
                    title = tx("Remote DNS server", "Удаленный DNS-сервер"),
                    description = if (uiState.remoteDnsMode == "custom") {
                        val primary = uiState.customRemoteDns.ifBlank { "8.8.8.8" }
                        val fallback = uiState.customRemoteDnsFallback.ifBlank { "1.1.1.1" }
                        tx("Custom ($primary, $fallback)", "Свой ($primary, $fallback)")
                    } else {
                        tx("Default (8.8.8.8, 1.1.1.1)", "По умолчанию (8.8.8.8, 1.1.1.1)")
                    },
                    onClick = { showRemoteDnsDialog = true }
                )

                SettingsDivider()

                ClickableSettingItem(
                    icon = Icons.Default.Hub,
                    title = tx("DNS workers", "DNS-воркеры"),
                    description = dnsWorkerDescription(uiState.dnsWorkerMode),
                    onClick = { showDnsWorkerDialog = true }
                )
            }

            // Network Settings
            SettingsSection(
                title = tx("Network", "Сеть"),
                subtitle = tx("Changes apply on next connection", "Изменения применятся при следующем подключении")
            ) {
                SwitchSettingItem(
                    icon = Icons.Default.Block,
                    title = tx("Disable QUIC", "Отключить QUIC"),
                    description = tx("Block QUIC protocol to force TCP (faster page loads over tunnels)", "Блокировать QUIC, чтобы принудить TCP (страницы быстрее грузятся через туннели)"),
                    checked = uiState.disableQuic,
                    onCheckedChange = { viewModel.setDisableQuic(it) }
                )

                SettingsDivider()

                ClickableSettingItem(
                    icon = Icons.Default.SettingsEthernet,
                    title = tx("VPN MTU", "VPN MTU"),
                    description = tx(
                        "VPN packet size: ${uiState.vpnMtu}. Lower values improve compatibility on mobile networks.",
                        "Размер VPN-пакета: ${uiState.vpnMtu}. Меньшие значения улучшают совместимость с мобильными сетями."
                    ),
                    onClick = { showMtuDialog = true }
                )

                SettingsDivider()

                val bandwidthUnlimitedLabel = tx("Unlimited", "Без лимита")
                val bandwidthUploadLabel = if (uiState.uploadLimitKbps > 0) "${uiState.uploadLimitKbps} KB/s" else bandwidthUnlimitedLabel
                val bandwidthDownloadLabel = if (uiState.downloadLimitKbps > 0) "${uiState.downloadLimitKbps} KB/s" else bandwidthUnlimitedLabel
                ClickableSettingItem(
                    icon = Icons.Default.Speed,
                    title = tx("Bandwidth Limit", "Ограничение скорости"),
                    description = tx(
                        "Upload: $bandwidthUploadLabel / Download: $bandwidthDownloadLabel",
                        "Исходящая: $bandwidthUploadLabel / входящая: $bandwidthDownloadLabel"
                    ),
                    onClick = { showBandwidthLimitDialog = true }
                )

                SettingsDivider()

                SwitchSettingItem(
                    icon = Icons.Default.Lan,
                    title = tx("Append HTTP Proxy to VPN", "Добавить HTTP-прокси в VPN"),
                    description = tx("Route app traffic through HTTP proxy directly, bypassing TUN for better speeds (Android 10+)", "Пускать трафик приложений напрямую через HTTP-прокси, минуя TUN для лучшей скорости (Android 10+)"),
                    checked = uiState.appendHttpProxyToVpn,
                    onCheckedChange = { viewModel.setAppendHttpProxyToVpn(it) }
                )

                if (uiState.appendHttpProxyToVpn && !uiState.httpProxyEnabled) {
                    Text(
                        text = tx(
                            "To also share the HTTP proxy with other devices, enable \"HTTP proxy\" in Proxy Settings above.",
                            "Чтобы раздавать HTTP-прокси другим устройствам, включи «HTTP-прокси» в настройках прокси выше."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 8.dp)
                    )
                }
            }

            // Split Tunneling Settings
            SettingsSection(
                title = tx("Split Tunneling", "Раздельное туннелирование"),
                subtitle = tx("Changes apply on next connection", "Изменения применятся при следующем подключении")
            ) {
                SwitchSettingItem(
                    icon = Icons.Default.CallSplit,
                    title = tx("Enable split tunneling", "Включить раздельное туннелирование"),
                    description = tx("Choose which apps use the VPN", "Выбрать, какие приложения используют VPN"),
                    checked = uiState.splitTunnelingEnabled,
                    onCheckedChange = { viewModel.setSplitTunnelingEnabled(it) }
                )

                if (uiState.splitTunnelingEnabled) {
                    SettingsDivider()

                    ClickableSettingItem(
                        icon = Icons.Default.FilterList,
                        title = tx("Mode", "Режим"),
                        description = when (uiState.splitTunnelingMode) {
                            SplitTunnelingMode.DISALLOW -> tx("Selected apps bypass VPN", "Выбранные приложения обходят VPN")
                            SplitTunnelingMode.ALLOW -> tx("Only selected apps use VPN", "Только выбранные приложения используют VPN")
                        },
                        onClick = { showSplitModeDialog = true }
                    )

                    SettingsDivider()

                    ClickableSettingItem(
                        icon = Icons.Default.Apps,
                        title = tx("Select apps", "Выбрать приложения"),
                        description = tx(
                            "${uiState.splitTunnelingApps.size} apps selected",
                            "Выбрано приложений: ${uiState.splitTunnelingApps.size}"
                        ),
                        onClick = onNavigateToAppSelector
                    )
                }
            }

            // Domain Routing Settings
            SettingsSection(
                title = tx("Domain Routing", "Маршрутизация доменов"),
                subtitle = tx("Changes apply on next connection", "Изменения применятся при следующем подключении")
            ) {
                SwitchSettingItem(
                    icon = Icons.Default.Language,
                    title = tx("Enable domain routing", "Включить маршрутизацию доменов"),
                    description = tx("Route specific domains through or around the VPN", "Пускать отдельные домены через VPN или в обход"),
                    checked = uiState.domainRoutingEnabled,
                    onCheckedChange = { viewModel.setDomainRoutingEnabled(it) }
                )

                if (uiState.domainRoutingEnabled) {
                    SettingsDivider()

                    ClickableSettingItem(
                        icon = Icons.Default.FilterList,
                        title = tx("Routing mode", "Режим маршрутизации"),
                        description = when (uiState.domainRoutingMode) {
                            DomainRoutingMode.BYPASS -> tx("Listed domains bypass VPN", "Домены из списка обходят VPN")
                            DomainRoutingMode.ONLY_VPN -> tx("Only listed domains use VPN", "Только домены из списка используют VPN")
                        },
                        onClick = { showDomainRoutingModeDialog = true }
                    )

                    SettingsDivider()

                    ClickableSettingItem(
                        icon = Icons.Default.TravelExplore,
                        title = tx("Manage domains", "Управление доменами"),
                        description = tx(
                            "${uiState.domainRoutingDomains.size} domains configured",
                            "Доменов настроено: ${uiState.domainRoutingDomains.size}"
                        ),
                        onClick = { showDomainManagementDialog = true }
                    )
                }
            }

            // Geo-Bypass Settings
            SettingsSection(
                title = tx("Geo-Bypass", "Гео-обход"),
                subtitle = tx("Changes apply on next connection", "Изменения применятся при следующем подключении")
            ) {
                SwitchSettingItem(
                    icon = Icons.Default.Public,
                    title = tx("Enable geo-bypass", "Включить гео-обход"),
                    description = tx("Route domestic traffic directly, bypass VPN for local sites", "Пускать локальный трафик напрямую, обходя VPN для местных сайтов"),
                    checked = uiState.geoBypassEnabled,
                    onCheckedChange = { viewModel.setGeoBypassEnabled(it) }
                )

                if (uiState.geoBypassEnabled) {
                    SettingsDivider()

                    ClickableSettingItem(
                        icon = Icons.Default.Language,
                        title = tx("Country", "Страна"),
                        description = geoBypassCountryLabel(uiState.geoBypassCountry),
                        onClick = { showGeoBypassCountryDialog = true }
                    )
                }
            }

            // SSH Tunnel Settings
            SettingsSection(
                title = tx("SSH Tunnel", "SSH-туннель"),
                subtitle = tx("Changes apply on next connection", "Изменения применятся при следующем подключении")
            ) {
                ClickableSettingItem(
                    icon = Icons.Default.Lock,
                    title = tx("Cipher", "Шифр"),
                    description = sshCipherLabel(uiState.sshCipher),
                    onClick = { showSshCipherDialog = true }
                )

                SettingsDivider()

                SwitchSettingItem(
                    icon = Icons.Default.Compress,
                    title = tx("Compression", "Сжатие"),
                    description = tx("Compress data through SSH (helps on slow links, hurts with HTTPS)", "Сжимать данные через SSH (помогает на медленных каналах, мешает HTTPS)"),
                    checked = uiState.sshCompression,
                    onCheckedChange = { viewModel.setSshCompression(it) }
                )

                SettingsDivider()

                SliderSettingItem(
                    icon = Icons.Default.Hub,
                    title = tx("Max Channels", "Макс. каналов"),
                    subtitle = when {
                        !uiState.sshMaxChannelsIsCustom -> tx("Auto (adapts per tunnel type)", "Авто (подстраивается под тип туннеля)")
                        uiState.sshMaxChannels > 12 -> tx("High values may cause instability on DNS tunnels", "Высокие значения могут быть нестабильны на DNS-туннелях")
                        else -> null
                    },
                    subtitleColor = if (uiState.sshMaxChannelsIsCustom && uiState.sshMaxChannels > 12) Color(0xFFFF9800) else null,
                    value = uiState.sshMaxChannels,
                    valueRange = 1f..64f,
                    steps = 63,
                    valueFormatter = { "${it.roundToInt()}" },
                    onValueChange = { viewModel.setSshMaxChannels(it.roundToInt()) },
                    onReset = if (uiState.sshMaxChannelsIsCustom) {{ viewModel.resetSshMaxChannelsToAuto() }} else null
                )

                SettingsDivider()

                SwitchSettingItem(
                    icon = Icons.Default.Shield,
                    title = tx("Prevent DNS Fallback", "Запретить DNS fallback"),
                    description = if (uiState.preventDnsFallback)
                        tx("DNS queries fail if SSH tunnel is down (no leak)", "DNS-запросы падают, если SSH-туннель недоступен (без утечки)")
                    else
                        tx("Falls back to direct DNS if SSH fails (may expose queries)", "При сбое SSH используется прямой DNS (запросы могут раскрыться)"),
                    checked = uiState.preventDnsFallback,
                    onCheckedChange = { viewModel.setPreventDnsFallback(it) }
                )
            }

            // Appearance Settings
            SettingsSection(title = tx("Appearance", "Внешний вид")) {
                SwitchSettingItem(
                    icon = Icons.Default.Language,
                    title = tx("Language", "Язык"),
                    description = uiState.appLanguage.displayName,
                    checked = uiState.appLanguage == AppLanguage.ENGLISH,
                    onCheckedChange = {
                        viewModel.setAppLanguage(if (it) AppLanguage.ENGLISH else AppLanguage.RUSSIAN)
                    }
                )

                SettingsDivider()

                ClickableSettingItem(
                    icon = Icons.Default.DarkMode,
                    title = tx("Dark mode", "Темная тема"),
                    description = when (uiState.darkMode) {
                        DarkMode.LIGHT -> tx("Light", "Светлая")
                        DarkMode.DARK -> tx("Dark", "Темная")
                        DarkMode.AMOLED -> tx("AMOLED Dark", "AMOLED темная")
                        DarkMode.SYSTEM -> tx("Follow system", "Как в системе")
                    },
                    onClick = { showDarkModeDialog = true }
                )
            }

            // Debug Settings
            SettingsSection(title = tx("Debug", "Отладка")) {
                SwitchSettingItem(
                    icon = Icons.Default.BugReport,
                    title = tx("Debug logging", "Отладочные логи"),
                    description = tx("Enable verbose logging for troubleshooting", "Включить подробные логи для диагностики"),
                    checked = uiState.debugLogging,
                    onCheckedChange = { viewModel.setDebugLogging(it) }
                )
            }

            // Reset Settings
            SettingsSection(title = tx("Reset", "Сброс")) {
                ClickableSettingItem(
                    icon = Icons.Default.RestartAlt,
                    title = tx("Reset all settings", "Сбросить все настройки"),
                    description = tx("Restore all settings to their default values", "Вернуть настройки к значениям по умолчанию"),
                    onClick = { showResetSettingsDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Device ID
            val deviceId = remember {
                app.slipnet.util.DeviceIdUtil.getScrambledDeviceId(context)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Device ID: $deviceId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        val clipboardManager = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboardManager?.setPrimaryClip(android.content.ClipData.newPlainText("Device ID", deviceId))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy device ID",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // App Info + original project attribution
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SlipNet VPN v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tx("Original created by anonvector", "Оригинал был создан anonvector"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "github.com/anonvector/SlipNet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/anonvector/SlipNet")
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Dark Mode Dialog
    if (showMtuDialog) {
        val mtuPresets = listOf(
            1500 to tx("Best throughput on clean networks", "Лучшая скорость на чистых сетях"),
            1400 to tx("Recommended for most mobile networks", "Рекомендуется для большинства мобильных сетей"),
            1350 to tx("Conservative, for double-NAT or PPPoE", "Осторожный вариант для double-NAT или PPPoE"),
            1280 to tx("Maximum compatibility", "Максимальная совместимость")
        )
        val isCustom = mtuPresets.none { it.first == uiState.vpnMtu }
        var customMtuText by remember { mutableStateOf(if (isCustom) uiState.vpnMtu.toString() else "") }
        var useCustom by remember { mutableStateOf(isCustom) }
        AlertDialog(
            onDismissRequest = { showMtuDialog = false },
            title = { Text(tx("VPN MTU", "VPN MTU")) },
            text = {
                Column {
                    mtuPresets.forEach { (mtu, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    useCustom = false
                                    viewModel.setVpnMtu(mtu)
                                    showMtuDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !useCustom && uiState.vpnMtu == mtu,
                                onClick = {
                                    useCustom = false
                                    viewModel.setVpnMtu(mtu)
                                    showMtuDialog = false
                                }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(text = "$mtu")
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { useCustom = true }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = useCustom,
                            onClick = { useCustom = true }
                        )
                        OutlinedTextField(
                            value = customMtuText,
                            onValueChange = { customMtuText = it.filter { c -> c.isDigit() }.take(5) },
                            enabled = useCustom,
                            label = { Text(tx("Custom", "Свой")) },
                            placeholder = { Text("512-1500") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (useCustom) {
                    TextButton(
                        onClick = {
                            val value = customMtuText.toIntOrNull()
                            if (value != null && value in 512..1500) {
                                viewModel.setVpnMtu(value)
                                showMtuDialog = false
                            }
                        }
                    ) {
                        Text(tx("Apply", "Применить"))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showMtuDialog = false }) {
                    Text(tx("Cancel", "Отмена"))
                }
            }
        )
    }

    if (showBandwidthLimitDialog) {
        BandwidthLimitDialog(
            currentUploadKbps = uiState.uploadLimitKbps,
            currentDownloadKbps = uiState.downloadLimitKbps,
            onDismiss = { showBandwidthLimitDialog = false },
            onApply = { upKbps, downKbps ->
                viewModel.setUploadLimitKbps(upKbps)
                viewModel.setDownloadLimitKbps(downKbps)
                showBandwidthLimitDialog = false
            }
        )
    }

    if (showDnsWorkerDialog) {
        AlertDialog(
            onDismissRequest = { showDnsWorkerDialog = false },
            title = { Text(tx("DNS Workers", "DNS-воркеры")) },
            text = {
                Column {
                    Text(
                        text = tx(
                            "Controls how DNS is resolved through the tunnel. Fewer workers = more stable on restricted networks. Per-query creates a fresh connection for each DNS lookup.",
                            "Управляет тем, как DNS проходит через туннель. Меньше воркеров - стабильнее на ограниченных сетях. Per-query создает новое соединение для каждого DNS-запроса."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    DnsWorkerMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setDnsWorkerMode(mode)
                                    showDnsWorkerDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.dnsWorkerMode == mode,
                                onClick = {
                                    viewModel.setDnsWorkerMode(mode)
                                    showDnsWorkerDialog = false
                                }
                            )
                            Text(
                                text = dnsWorkerModeLabel(mode),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    if (uiState.dnsWorkerMode.poolSize >= 3) {
                        Text(
                            text = tx(
                                "Higher worker counts increase background data usage due to keepalive traffic on each connection. Use 2 or per-query if data usage is a concern.",
                                "Большое число воркеров увеличивает фоновый расход трафика из-за keepalive на каждом соединении. Если трафик важен, используй 2 или per-query."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDnsWorkerDialog = false }) {
                    Text(tx("Cancel", "Отмена"))
                }
            }
        )
    }

    if (showDarkModeDialog) {
        AlertDialog(
            onDismissRequest = { showDarkModeDialog = false },
            title = { Text(tx("Dark Mode", "Темная тема")) },
            text = {
                Column {
                    DarkMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setDarkMode(mode)
                                    showDarkModeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.darkMode == mode,
                                onClick = {
                                    viewModel.setDarkMode(mode)
                                    showDarkModeDialog = false
                                }
                            )
                            Text(
                                text = when (mode) {
                                    DarkMode.LIGHT -> tx("Light", "Светлая")
                                    DarkMode.DARK -> tx("Dark", "Темная")
                                    DarkMode.AMOLED -> tx("AMOLED Dark", "AMOLED темная")
                                    DarkMode.SYSTEM -> tx("Follow system", "Как в системе")
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDarkModeDialog = false }) {
                    Text(tx("Cancel", "Отмена"))
                }
            }
        )
    }

    // Split Tunneling Mode Dialog
    if (showSplitModeDialog) {
        AlertDialog(
            onDismissRequest = { showSplitModeDialog = false },
            title = { Text(tx("Split Tunneling Mode", "Режим раздельного туннелирования")) },
            text = {
                Column {
                    SplitTunnelingMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setSplitTunnelingMode(mode)
                                    showSplitModeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.splitTunnelingMode == mode,
                                onClick = {
                                    viewModel.setSplitTunnelingMode(mode)
                                    showSplitModeDialog = false
                                }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = when (mode) {
                                        SplitTunnelingMode.DISALLOW -> tx("Bypass", "В обход")
                                        SplitTunnelingMode.ALLOW -> tx("Only", "Только")
                                    }
                                )
                                Text(
                                    text = when (mode) {
                                        SplitTunnelingMode.DISALLOW -> tx("Selected apps bypass VPN", "Выбранные приложения обходят VPN")
                                        SplitTunnelingMode.ALLOW -> tx("Only selected apps use VPN", "Только выбранные приложения используют VPN")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSplitModeDialog = false }) {
                    Text(tx("Cancel", "Отмена"))
                }
            }
        )
    }

    // Domain Routing Mode Dialog
    if (showDomainRoutingModeDialog) {
        AlertDialog(
            onDismissRequest = { showDomainRoutingModeDialog = false },
            title = { Text(tx("Domain Routing Mode", "Режим маршрутизации доменов")) },
            text = {
                Column {
                    DomainRoutingMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setDomainRoutingMode(mode)
                                    showDomainRoutingModeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.domainRoutingMode == mode,
                                onClick = {
                                    viewModel.setDomainRoutingMode(mode)
                                    showDomainRoutingModeDialog = false
                                }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = when (mode) {
                                        DomainRoutingMode.BYPASS -> tx("Bypass VPN", "В обход VPN")
                                        DomainRoutingMode.ONLY_VPN -> tx("Only VPN", "Только VPN")
                                    }
                                )
                                Text(
                                    text = when (mode) {
                                        DomainRoutingMode.BYPASS -> tx("Listed domains connect directly", "Домены из списка подключаются напрямую")
                                        DomainRoutingMode.ONLY_VPN -> tx("Only listed domains use the VPN", "Только домены из списка используют VPN")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDomainRoutingModeDialog = false }) {
                    Text(tx("Cancel", "Отмена"))
                }
            }
        )
    }

    // Domain Management Dialog
    if (showDomainManagementDialog) {
        DomainManagementDialog(
            domains = uiState.domainRoutingDomains,
            onAddDomain = { viewModel.addDomainRoutingDomain(it) },
            onRemoveDomain = { viewModel.removeDomainRoutingDomain(it) },
            onDismiss = { showDomainManagementDialog = false }
        )
    }

    // Geo-Bypass Country Dialog
    if (showGeoBypassCountryDialog) {
        AlertDialog(
            onDismissRequest = { showGeoBypassCountryDialog = false },
            title = { Text(tx("Select Country", "Выбрать страну")) },
            text = {
                Column {
                    GeoBypassCountry.entries.forEach { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setGeoBypassCountry(country)
                                    showGeoBypassCountryDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.geoBypassCountry == country,
                                onClick = {
                                    viewModel.setGeoBypassCountry(country)
                                    showGeoBypassCountryDialog = false
                                }
                            )
                            Text(
                                text = geoBypassCountryLabel(country),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGeoBypassCountryDialog = false }) {
                    Text(tx("Cancel", "Отмена"))
                }
            }
        )
    }

    // Global Resolver Override Dialog
    if (showGlobalResolverDialog) {
        var resolverText by remember { mutableStateOf(uiState.globalResolverList) }
        val ipPattern = remember { Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(:\d+)?$""") }
        val entries = resolverText.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
        val resolverCount = entries.size
        val tooMany = resolverCount > 10
        val invalidEntries = entries.filter { !ipPattern.matches(it) }
        val hasInvalid = invalidEntries.isNotEmpty()
        AlertDialog(
            onDismissRequest = { showGlobalResolverDialog = false },
            title = { Text(tx("Global DNS Resolvers", "Глобальные DNS-резолверы")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tx(
                            "Enter DNS resolver IPs, one per line or comma-separated (max 10). These override the resolvers in all DNS tunnel profiles and are used to resolve SSH hostnames.",
                            "Введи IP DNS-резолверов, по одному в строке или через запятую (до 10). Они заменяют резолверы во всех DNS-туннельных профилях и используются для SSH-хостов."
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = resolverText,
                        onValueChange = { resolverText = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("e.g. 8.8.8.8, 1.1.1.1") },
                        singleLine = false,
                        maxLines = 8,
                        isError = tooMany || hasInvalid
                    )
                    if (tooMany) {
                        Text(
                            tx(
                                "Maximum 10 resolvers ($resolverCount entered)",
                                "Максимум 10 резолверов (введено: $resolverCount)"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (hasInvalid) {
                        Text(
                            tx("Invalid IP: ${invalidEntries.first()}", "Некорректный IP: ${invalidEntries.first()}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (resolverCount > 0) {
                        Text(
                            tx(
                                "$resolverCount resolver${if (resolverCount > 1) "s" else ""}",
                                "Резолверов: $resolverCount"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Normalize: strip ports, clean up separators
                        val cleaned = entries.joinToString(", ") { it.split(":").first() }
                        viewModel.setGlobalResolverList(cleaned)
                        showGlobalResolverDialog = false
                    },
                    enabled = !tooMany && !hasInvalid && resolverCount > 0
                ) { Text(tx("Save", "Сохранить")) }
            },
            dismissButton = {
                TextButton(onClick = { showGlobalResolverDialog = false }) { Text(tx("Cancel", "Отмена")) }
            }
        )
    }

    // DNS Pool Dialog
    if (showDnsPoolDialog) {
        DnsPoolDialog(
            initialText = uiState.dnsPoolText,
            onSave = { text ->
                viewModel.setDnsPoolText(text)
                showDnsPoolDialog = false
            },
            onDismiss = { showDnsPoolDialog = false }
        )
    }

    // Remote DNS Dialog
    if (showRemoteDnsDialog) {
        RemoteDnsDialog(
            currentMode = uiState.remoteDnsMode,
            currentCustomDns = uiState.customRemoteDns,
            currentCustomDnsFallback = uiState.customRemoteDnsFallback,
            onSelectDefault = {
                viewModel.setRemoteDnsMode("default")
                showRemoteDnsDialog = false
            },
            onSelectCustom = { dns, fallback ->
                viewModel.setRemoteDnsMode("custom")
                viewModel.setCustomRemoteDns(dns)
                viewModel.setCustomRemoteDnsFallback(fallback)
                showRemoteDnsDialog = false
            },
            onDismiss = { showRemoteDnsDialog = false }
        )
    }

    // SSH Cipher Dialog
    if (showSshCipherDialog) {
        AlertDialog(
            onDismissRequest = { showSshCipherDialog = false },
            title = { Text(tx("SSH Cipher", "SSH-шифр")) },
            text = {
                Column {
                    SshCipher.entries.forEach { cipher ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setSshCipher(cipher)
                                    showSshCipherDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.sshCipher == cipher,
                                onClick = {
                                    viewModel.setSshCipher(cipher)
                                    showSshCipherDialog = false
                                }
                            )
                            Text(
                                text = sshCipherLabel(cipher),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSshCipherDialog = false }) {
                    Text(tx("Cancel", "Отмена"))
                }
            }
        )
    }

    // Reset Settings Confirmation Dialog
    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showResetSettingsDialog = false },
            title = { Text(tx("Reset all settings?", "Сбросить все настройки?")) },
            text = {
                Text(tx(
                    "This will restore all settings to their default values. Your profiles and connection stats will not be affected.",
                    "Это вернет настройки к значениям по умолчанию. Профили и статистика подключений не изменятся."
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllSettings()
                    showResetSettingsDialog = false
                }) {
                    Text(tx("Reset", "Сбросить"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetSettingsDialog = false }) {
                    Text(tx("Cancel", "Отмена"))
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(tx("About SlipNet", "О SlipNet")) },
            text = { AboutDialogContent() },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(tx("Close", "Закрыть"))
                }
            }
        )
    }

}

@Composable
private fun dnsWorkerDescription(mode: DnsWorkerMode): String {
    return when (mode) {
        DnsWorkerMode.PER_QUERY -> tx("Per-query (default) (DNSTT/Slipstream, SSH always uses 5)", "Per-query (по умолчанию) (DNSTT/Slipstream, SSH всегда использует 5)")
        DnsWorkerMode.TWO -> tx("2 workers (more stable, lower background traffic)", "2 воркера (стабильнее, меньше фонового трафика)")
        DnsWorkerMode.THREE -> tx("3 workers (balanced)", "3 воркера (баланс)")
        DnsWorkerMode.FIVE -> tx("5 workers (fastest)", "5 воркеров (быстрее всего)")
    }
}

@Composable
private fun dnsWorkerModeLabel(mode: DnsWorkerMode): String {
    return when (mode) {
        DnsWorkerMode.PER_QUERY -> tx("Per-query (default)", "Per-query (по умолчанию)")
        DnsWorkerMode.TWO -> tx("2 workers", "2 воркера")
        DnsWorkerMode.THREE -> tx("3 workers", "3 воркера")
        DnsWorkerMode.FIVE -> tx("5 workers (fastest)", "5 воркеров (быстрее всего)")
    }
}

@Composable
private fun sshCipherLabel(cipher: SshCipher): String {
    return when (cipher) {
        SshCipher.AUTO -> tx("Auto (Fastest)", "Авто (самый быстрый)")
        SshCipher.AES_128_GCM -> "AES-128-GCM"
        SshCipher.CHACHA20 -> "ChaCha20-Poly1305"
        SshCipher.AES_128_CTR -> tx("AES-128-CTR (Legacy)", "AES-128-CTR (Legacy)")
    }
}

@Composable
private fun geoBypassCountryLabel(country: GeoBypassCountry): String {
    return when (country) {
        GeoBypassCountry.IR -> tx("Iran", "Иран")
        GeoBypassCountry.CN -> tx("China", "Китай")
        GeoBypassCountry.RU -> tx("Russia", "Россия")
    }
}

@Composable
private fun DomainManagementDialog(
    domains: Set<String>,
    onAddDomain: (String) -> Unit,
    onRemoveDomain: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newDomain by remember { mutableStateOf("") }
    val sortedDomains = remember(domains) { domains.sorted() }

    val addDomain = {
        val trimmed = newDomain.trim().lowercase()
        if (trimmed.isNotEmpty()) {
            onAddDomain(trimmed)
            newDomain = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tx("Manage Domains", "Управление доменами")) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Add domain input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newDomain,
                        onValueChange = { newDomain = it },
                        placeholder = { Text("e.g., google.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default,
                        keyboardActions = KeyboardActions(onDone = { addDomain() }),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { addDomain() }) {
                        Icon(Icons.Default.Add, contentDescription = tx("Add domain", "Добавить домен"))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (sortedDomains.isEmpty()) {
                    Text(
                        text = tx("No domains configured", "Домены не настроены"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(sortedDomains) { domain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = domain,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onRemoveDomain(domain) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = tx("Remove $domain", "Удалить $domain"),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { addDomain(); onDismiss() }) {
                Text(tx("Done", "Готово"))
            }
        }
    )
}

@Composable
private fun RemoteDnsDialog(
    currentMode: String,
    currentCustomDns: String,
    currentCustomDnsFallback: String,
    onSelectDefault: () -> Unit,
    onSelectCustom: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    var customDns by remember { mutableStateOf(currentCustomDns) }
    var customDnsFallback by remember { mutableStateOf(currentCustomDnsFallback) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tx("Remote DNS Server", "Удаленный DNS-сервер")) },
        text = {
            Column {
                Text(
                    text = tx(
                        "DNS servers used on the remote side of the tunnel for resolving domain names.",
                        "DNS-серверы на удаленной стороне туннеля для разрешения доменных имен."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Default option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedMode = "default" }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == "default",
                        onClick = { selectedMode = "default" }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(tx("Default (8.8.8.8, 1.1.1.1)", "По умолчанию (8.8.8.8, 1.1.1.1)"))
                        Text(
                            text = tx("Google primary, Cloudflare fallback", "Основной Google, резервный Cloudflare"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Custom option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedMode = "custom" }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == "custom",
                        onClick = { selectedMode = "custom" }
                    )
                    Text(
                        text = tx("Custom", "Свой"),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (selectedMode == "custom") {
                    OutlinedTextField(
                        value = customDns,
                        onValueChange = { customDns = it },
                        placeholder = { Text("e.g., 9.9.9.9") },
                        supportingText = { Text(tx("Primary DNS server", "Основной DNS-сервер")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp, top = 4.dp)
                    )

                    OutlinedTextField(
                        value = customDnsFallback,
                        onValueChange = { customDnsFallback = it },
                        placeholder = { Text("e.g., 8.8.8.8") },
                        supportingText = { Text(tx("Fallback DNS server", "Резервный DNS-сервер")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedMode == "default") {
                        onSelectDefault()
                    } else {
                        onSelectCustom(customDns.trim(), customDnsFallback.trim())
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tx("Cancel", "Отмена"))
            }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (subtitle != null) {
                Text(
                    text = " · $subtitle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    inactive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (inactive) 0.5f else 1f)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ClickableSettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IndentedSettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    inactive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp)
            .alpha(if (inactive) 0.5f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun InfoNoticeRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, top = 4.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SliderSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    subtitleColor: Color? = null,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String,
    onValueChange: (Float) -> Unit,
    onReset: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onReset != null) {
                TextButton(onClick = onReset) {
                    Text(tx("Auto", "Авто"), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                text = valueFormatter(value.toFloat()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, top = 4.dp)
        )
    }
}

@Composable
private fun StepperSettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    value: Int,
    step: Int,
    range: IntRange,
    valueFormatter: (Int) -> String,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onValueChange((value - step).coerceIn(range)) },
                enabled = value > range.first,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = tx("Decrease", "Уменьшить"),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 48.dp)
            )
            IconButton(
                onClick = { onValueChange((value + step).coerceIn(range)) },
                enabled = value < range.last,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = tx("Increase", "Увеличить"),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
/**
 * Detect available network addresses for the listen address picker.
 * Returns list of (label, ip) pairs.
 */
private fun getAddressOptions(): List<Pair<String, String>> {
    return listOf(
        "Localhost" to "127.0.0.1",
        "All interfaces" to "0.0.0.0"
    )
}

/**
 * Detect the device's shareable IP address.
 * Priority: hotspot interface > Wi-Fi > any non-loopback IPv4 interface.
 * Returns a pair of (ip, isHotspot) or null if no suitable interface is found.
 */
private fun detectShareableIp(): Pair<String, Boolean>? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
        val hotspotPrefixes = listOf("wlan1", "ap0", "swlan0", "softap", "wlan-ap", "rndis")

        // First try hotspot interfaces
        for (iface in interfaces) {
            if (!iface.isUp) continue
            if (hotspotPrefixes.any { iface.name.startsWith(it) }) {
                val ip = iface.inetAddresses.toList()
                    .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                    ?.hostAddress
                if (ip != null) return ip to true
            }
        }

        // Fall back to Wi-Fi (wlan0)
        for (iface in interfaces) {
            if (!iface.isUp) continue
            if (iface.name.startsWith("wlan0") || iface.name.startsWith("wlan")) {
                val ip = iface.inetAddresses.toList()
                    .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                    ?.hostAddress
                if (ip != null) return ip to false
            }
        }

        // Fall back to any non-loopback IPv4 interface (mobile data, USB, ethernet, etc.)
        for (iface in interfaces) {
            if (!iface.isUp || iface.isLoopback) continue
            val ip = iface.inetAddresses.toList()
                .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                ?.hostAddress
            if (ip != null) return ip to false
        }
    } catch (_: Exception) { }
    return null
}

@Composable
private fun HotspotInfoCard(
    socksPort: Int,
    httpProxyEnabled: Boolean = false,
    httpProxyPort: Int = 8080
) {
    val shareableIp = remember { detectShareableIp() }
    if (shareableIp == null) return

    val (ip, isHotspot) = shareableIp
    val socksAddress = "$ip:$socksPort"
    val httpAddress = "$ip:$httpProxyPort"
    val copyText = if (httpProxyEnabled) "SOCKS5: $socksAddress | HTTP: $httpAddress" else socksAddress
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHotspot)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = if (isHotspot)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = if (isHotspot) "Hotspot proxy address" else "Device IP",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isHotspot)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "SOCKS5: $socksAddress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isHotspot)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (httpProxyEnabled) {
                    Text(
                        text = "HTTP: $httpAddress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isHotspot)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isHotspot) {
                    Text(
                        text = "Use as proxy on connected devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "Enable hotspot to share with other devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(copyText)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy address",
                    tint = if (isHotspot)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LocalProxyInfoCard(
    listenAddress: String,
    socksPort: Int,
    httpProxyEnabled: Boolean = false,
    httpProxyPort: Int = 8080
) {
    val socksAddress = "$listenAddress:$socksPort"
    val httpAddress = "$listenAddress:$httpProxyPort"
    val copyText = if (httpProxyEnabled) "SOCKS5: $socksAddress | HTTP: $httpAddress" else socksAddress
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SettingsEthernet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "Proxy address",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "SOCKS5: $socksAddress",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (httpProxyEnabled) {
                    Text(
                        text = "HTTP: $httpAddress",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Configure this in your app's proxy settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(copyText)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy address",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressSettingItem(
    value: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Find the label for the current value, if it matches a known option
    val displayText = options.find { it.second == value }?.let { (label, ip) ->
        if (label == "All interfaces" || label == "Localhost") "$label ($ip)" else "$label: $ip"
    } ?: value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lan,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Listen Address",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.padding(start = 40.dp)
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (label, ip) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "$label ($ip)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onValueChange(ip)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextFieldSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    placeholder: String,
    supportingText: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            supportingText = { Text(supportingText) },
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp)
        )
    }
}


@Composable
private fun BandwidthLimitDialog(
    currentUploadKbps: Int,
    currentDownloadKbps: Int,
    onDismiss: () -> Unit,
    onApply: (uploadKbps: Int, downloadKbps: Int) -> Unit
) {
    var uploadText by remember { mutableStateOf(if (currentUploadKbps > 0) currentUploadKbps.toString() else "") }
    var downloadText by remember { mutableStateOf(if (currentDownloadKbps > 0) currentDownloadKbps.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tx("Bandwidth Limit", "Ограничение скорости")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tx(
                        "Set speed limits in KB/s. Leave empty or 0 for unlimited. Applies on next connection.",
                        "Задай лимиты скорости в KB/s. Оставь пустым или 0 для безлимита. Применится при следующем подключении."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = uploadText,
                    onValueChange = { uploadText = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text(tx("Upload (KB/s)", "Исходящая (KB/s)")) },
                    placeholder = { Text(tx("Unlimited", "Без лимита")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = downloadText,
                    onValueChange = { downloadText = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text(tx("Download (KB/s)", "Входящая (KB/s)")) },
                    placeholder = { Text(tx("Unlimited", "Без лимита")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val up = uploadText.toIntOrNull() ?: 0
                    val down = downloadText.toIntOrNull() ?: 0
                    onApply(up.coerceAtLeast(0), down.coerceAtLeast(0))
                }
            ) {
                Text(tx("Apply", "Применить"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tx("Cancel", "Отмена"))
            }
        }
    )
}

@Composable
private fun DonateCard() {
    val clipboardManager = LocalClipboardManager.current
    val donationAddress = "0xd4140058389572D50dC8716e768e687C050Dd5C9"
    var showDonateDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDonateDialog = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "Support SlipNet",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Help keep this project free and maintained",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    if (showDonateDialog) {
        AlertDialog(
            onDismissRequest = { showDonateDialog = false },
            title = { Text("Support SlipNet") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SlipNet is free, source-available, and built to fight internet censorship. No ads, no data collection, no subscriptions. Your donation helps keep this tool free and improving for everyone who needs it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = "USDT (BEP20 / ERC20 / Arbitrum)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = donationAddress,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(donationAddress))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy address",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    val xmrAddress = "48wa9asF4AdZCq8KvPqBmqN3s98XFQ2MG7pL8MY6hAc6ZXBd8D61LArebdmAwCk5jBBbR2BuiHkSraEYFhx5AdDqLxDB4GU"
                    Text(
                        text = "Monero (XMR)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = xmrAddress,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(xmrAddress))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy address",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "Even a small amount makes a difference. Thank you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDonateDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DnsPoolDialog(
    initialText: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val max = app.slipnet.tunnel.DnsPoolScanner.MAX_POOL_SIZE
    var text by remember { mutableStateOf(initialText) }
    val parsedCount = remember(text) {
        app.slipnet.tunnel.DnsPoolScanner.parsePool(text).size
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)
                    ?.bufferedReader()?.readText() ?: ""
                text = capPoolText(content, max)
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DNS Resolver Pool",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                CountBadge(count = parsedCount, max = max)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Candidates scanned on each connect — top 10 lowest-latency win.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PoolActionButton(
                        icon = Icons.Default.UploadFile,
                        label = "Import",
                        onClick = { importLauncher.launch(arrayOf("text/plain", "*/*")) },
                        modifier = Modifier.weight(1f)
                    )
                    PoolActionButton(
                        icon = Icons.Default.Shuffle,
                        label = "Random",
                        onClick = {
                            val sampled = sampleRandomResolversFromBuiltIn(context, count = max)
                            if (sampled.isNotBlank()) text = sampled
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PoolActionButton(
                        icon = Icons.Default.Delete,
                        label = "Clear",
                        onClick = { text = "" },
                        modifier = Modifier.weight(1f),
                        enabled = text.isNotEmpty()
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = capPoolText(it, max) },
                    placeholder = { Text("One IP or host[:port] per line or comma-separated.\nLines starting with # are comments.") },
                    minLines = 8,
                    maxLines = 12,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CountBadge(count: Int, max: Int) {
    val nearLimit = count >= max
    val containerColor = if (nearLimit)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (nearLimit)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer
    Text(
        text = "$count / $max",
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun PoolActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun capPoolText(text: String, max: Int): String {
    val lines = text.lines()
    var validCount = 0
    val out = mutableListOf<String>()
    for (line in lines) {
        val trimmed = line.trim()
        val isEntry = trimmed.isNotEmpty() && !trimmed.startsWith("#")
        if (isEntry) {
            if (validCount >= max) continue
            validCount++
        }
        out.add(line)
    }
    return out.joinToString("\n")
}

private val POOL_PINNED_DNS = listOf(
    "208.67.222.222", "208.67.220.220",
    "8.8.8.8", "8.8.4.4",
    "77.88.8.2", "77.88.8.88",
    "1.1.1.2", "1.0.0.2",
    "223.5.5.5", "223.6.6.6",
    "80.80.80.80", "80.80.81.81",
    "9.9.9.9", "149.112.112.112"
)

private fun sampleRandomResolversFromBuiltIn(
    context: android.content.Context,
    count: Int,
): String = try {
    // Collect tier 1 + tier 2 only (stop at the second "# SHUFFLE_BELOW" marker).
    // Tier 3 has 55k+ IPs that are not useful for random pool picks.
    val pool = ArrayList<String>(count * 2)
    var shuffleBelowCount = 0
    context.resources.openRawResource(app.slipnet.R.raw.resolvers)
        .bufferedReader().useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (line == "# SHUFFLE_BELOW") {
                    shuffleBelowCount++
                    if (shuffleBelowCount >= 2) return@useLines  // stop after tier 2
                    continue
                }
                if (line.isEmpty() || line.startsWith("#")) continue
                pool.add(line)
            }
        }
    val sampled = pool.shuffled().take(count).toMutableList()
    val sampledSet = sampled.toHashSet()
    val missing = POOL_PINNED_DNS.filter { it !in sampledSet }
    // Prepend pinned IPs, trim tail to stay within cap
    val result = (missing + sampled).take(count)
    result.joinToString("\n")
} catch (_: Exception) {
    ""
}
