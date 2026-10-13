package com.example

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hce.EmulationManager
import com.example.ui.screens.AiAuditScreen
import com.example.ui.screens.DumpEditorScreen
import com.example.ui.screens.EmulationScreen
import com.example.ui.screens.KeyDictionaryScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.WearOsKeyEmulationScreen
import com.example.ui.screens.WriterScreen
import com.example.util.HapticHelper
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.KeyMasterTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.NfcViewModel
import com.example.viewmodel.VaultViewModel

enum class MainTab(val title: String) {
    SCANNER("Сканер"),
    EMULATION("Ключ HCE"),
    EDITOR("HEX Дамп"),
    WRITER("Запись"),
    VAULT("Хранилище"),
    AI_AUDIT("AI Аудит")
}

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private val nfcViewModel: NfcViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()
    private val aiViewModel: AiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HapticHelper.init(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val isWatchDevice = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WATCH)

        // Handle initial NFC tag intent if app was opened by NFC tap
        intent?.let { handleNfcIntent(it) }

        setContent {
            KeyMasterTheme {
                MainAppScreen(
                    nfcViewModel = nfcViewModel,
                    vaultViewModel = vaultViewModel,
                    aiViewModel = aiViewModel,
                    isNfcAvailable = nfcAdapter != null,
                    isNfcEnabled = nfcAdapter?.isEnabled == true,
                    isWatchDevice = isWatchDevice,
                    onToggleReaderMode = { enable ->
                        setReaderModeEnabled(enable)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!EmulationManager.isEmulationActive.value) {
            setReaderModeEnabled(true)
        }
    }

    override fun onPause() {
        super.onPause()
        setReaderModeEnabled(false)
    }

    private fun setReaderModeEnabled(enabled: Boolean) {
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                if (enabled) {
                    val flags = NfcAdapter.FLAG_READER_NFC_A or
                            NfcAdapter.FLAG_READER_NFC_B or
                            NfcAdapter.FLAG_READER_NFC_F or
                            NfcAdapter.FLAG_READER_NFC_V or
                            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                    adapter.enableReaderMode(this, this, flags, null)
                } else {
                    adapter.disableReaderMode(this)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    override fun onTagDiscovered(tag: Tag) {
        runOnUiThread {
            nfcViewModel.onTagDiscovered(tag)
        }
    }

    private fun handleNfcIntent(intent: Intent) {
        val tag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        tag?.let { nfcViewModel.onTagDiscovered(it) }
    }
}

@Composable
fun MainAppScreen(
    nfcViewModel: NfcViewModel,
    vaultViewModel: VaultViewModel,
    aiViewModel: AiViewModel,
    isNfcAvailable: Boolean,
    isNfcEnabled: Boolean,
    isWatchDevice: Boolean = false,
    onToggleReaderMode: (Boolean) -> Unit
) {
    var isWearOsMode by remember { mutableStateOf(isWatchDevice) }
    var currentTab by remember { mutableStateOf(MainTab.SCANNER) }
    var inDictionarySubScreen by remember { mutableStateOf(false) }

    val isEmulationActive by EmulationManager.isEmulationActive.collectAsState()

    // When emulation is active, disable ReaderMode so phone listens to external turnstiles/readers
    LaunchedEffect(isEmulationActive) {
        onToggleReaderMode(!isEmulationActive)
    }

    // Sync dictionary keys from DB into reader ViewModel
    val dbKeys by vaultViewModel.dictionaryKeys.collectAsState()
    LaunchedEffect(dbKeys) {
        if (dbKeys.isNotEmpty()) {
            nfcViewModel.setCustomKeys(dbKeys.map { it.keyHex })
        }
    }

    // Handle Back button if inside Wear OS mode, sub-screens or main tabs
    if (isWearOsMode) {
        BackHandler {
            if (!isWatchDevice) {
                isWearOsMode = false
            }
        }
    } else if (inDictionarySubScreen) {
        BackHandler {
            inDictionarySubScreen = false
        }
    } else if (currentTab != MainTab.SCANNER) {
        BackHandler {
            currentTab = MainTab.SCANNER
        }
    }

    if (isWearOsMode) {
        WearOsKeyEmulationScreen(
            vaultViewModel = vaultViewModel,
            onExitWearMode = if (!isWatchDevice) { { isWearOsMode = false } } else null
        )
        return
    }

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            if (!inDictionarySubScreen) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.SCANNER,
                        onClick = { currentTab = MainTab.SCANNER },
                        icon = { Icon(Icons.Default.Nfc, contentDescription = "Сканер", modifier = Modifier.size(20.dp)) },
                        label = { Text("Сканер", fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_tab_scanner"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyberCyan.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.EMULATION,
                        onClick = { currentTab = MainTab.EMULATION },
                        icon = {
                            Icon(
                                Icons.Default.Contactless,
                                contentDescription = "Ключ HCE",
                                tint = if (isEmulationActive) NeonGreen else if (currentTab == MainTab.EMULATION) CyberCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text("Ключ", fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_tab_emulation"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isEmulationActive) NeonGreen else CyberCyan,
                            selectedTextColor = if (isEmulationActive) NeonGreen else CyberCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = if (isEmulationActive) NeonGreen.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.EDITOR,
                        onClick = { currentTab = MainTab.EDITOR },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Дамп", modifier = Modifier.size(20.dp)) },
                        label = { Text("Дамп", fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_tab_editor"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyberCyan.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.WRITER,
                        onClick = { currentTab = MainTab.WRITER },
                        icon = { Icon(Icons.Default.CopyAll, contentDescription = "Запись", modifier = Modifier.size(20.dp)) },
                        label = { Text("Запись", fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_tab_writer"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyberCyan.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.VAULT,
                        onClick = { currentTab = MainTab.VAULT },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Хранилище", modifier = Modifier.size(20.dp)) },
                        label = { Text("Склад", fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_tab_vault"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyberCyan.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.AI_AUDIT,
                        onClick = { currentTab = MainTab.AI_AUDIT },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Аудит", modifier = Modifier.size(20.dp)) },
                        label = { Text("AI", fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_tab_ai_audit"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyberCyan.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if (inDictionarySubScreen) {
                KeyDictionaryScreen(
                    vaultViewModel = vaultViewModel,
                    onBack = { inDictionarySubScreen = false }
                )
            } else {
                when (currentTab) {
                    MainTab.SCANNER -> ScannerScreen(
                        nfcViewModel = nfcViewModel,
                        vaultViewModel = vaultViewModel,
                        onNavigateToEditor = { currentTab = MainTab.EDITOR },
                        onNavigateToAiAudit = { currentTab = MainTab.AI_AUDIT },
                        onNavigateToEmulation = { currentTab = MainTab.EMULATION },
                        isNfcAvailable = isNfcAvailable,
                        isNfcEnabled = isNfcEnabled
                    )
                    MainTab.EMULATION -> EmulationScreen(
                        vaultViewModel = vaultViewModel,
                        nfcViewModel = nfcViewModel,
                        onEnterWearMode = { isWearOsMode = true }
                    )
                    MainTab.EDITOR -> DumpEditorScreen(
                        nfcViewModel = nfcViewModel,
                        vaultViewModel = vaultViewModel,
                        onNavigateToAiAudit = { currentTab = MainTab.AI_AUDIT },
                        onNavigateToEmulation = { currentTab = MainTab.EMULATION }
                    )
                    MainTab.WRITER -> WriterScreen(
                        nfcViewModel = nfcViewModel,
                        vaultViewModel = vaultViewModel
                    )
                    MainTab.VAULT -> VaultScreen(
                        vaultViewModel = vaultViewModel,
                        nfcViewModel = nfcViewModel,
                        onNavigateToEditor = { currentTab = MainTab.EDITOR },
                        onNavigateToDictionary = { inDictionarySubScreen = true },
                        onNavigateToEmulation = { currentTab = MainTab.EMULATION }
                    )
                    MainTab.AI_AUDIT -> AiAuditScreen(
                        aiViewModel = aiViewModel,
                        nfcViewModel = nfcViewModel
                    )
                }
            }
        }
    }
}
