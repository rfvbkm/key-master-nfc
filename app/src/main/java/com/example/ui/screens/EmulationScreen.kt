package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.CardEntity
import com.example.hce.EmulationManager
import com.example.hce.KeyPreset
import com.example.model.CardDump
import com.example.model.CardType
import com.example.model.toHex
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.NfcViewModel
import com.example.viewmodel.VaultViewModel

@Composable
fun EmulationScreen(
    vaultViewModel: VaultViewModel,
    nfcViewModel: NfcViewModel,
    onEnterWearMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val isEmulationActive by EmulationManager.isEmulationActive.collectAsState()
    val activeDump by EmulationManager.activeDump.collectAsState()
    val activeKeyName by EmulationManager.activeKeyName.collectAsState()
    val interactionCount by EmulationManager.interactionCount.collectAsState()
    val apduLogs by EmulationManager.logs.collectAsState()

    val savedCards by vaultViewModel.savedCards.collectAsState()
    val currentScannedDump by nfcViewModel.currentDump.collectAsState()

    var showKeyPickerModal by remember { mutableStateOf(false) }
    var customApduInput by remember { mutableStateOf("FF CA 00 00 00") }
    var showApduTester by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isEmulationActive) NeonGreen else CyberCyan.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isEmulationActive) Icons.Default.CastConnected else Icons.Default.Cast,
                    contentDescription = null,
                    tint = if (isEmulationActive) NeonGreen else CyberCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEmulationActive) "ТРАНСЛЯЦИЯ КЛЮЧА АКТИВНА" else "Эмуляция ключа выключена",
                        color = if (isEmulationActive) NeonGreen else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isEmulationActive)
                            "Смартфон транслирует UID и данные дампа. Поднесите к считывателю/домофону"
                        else
                            "Выберите нужный дамп или ключ и включите передачу",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Virtual Card Graphic
        VirtualKeyCard(
            isActive = isEmulationActive,
            keyName = activeKeyName,
            uid = activeDump?.uid ?: "НЕ ВЫБРАН",
            cardType = activeDump?.cardType?.displayName ?: "MIFARE / ISO-14443-4",
            sectorCount = activeDump?.sectors?.size ?: 0,
            onPickKeyClick = { showKeyPickerModal = true }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Key Selector Strip ("Связка ключей")
        Text(
            text = "СВЯЗКА КЛЮЧЕЙ ДЛЯ БЫСТРОГО ВЫБОРА:",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Button to open full selector
            item {
                Card(
                    modifier = Modifier
                        .clickable { showKeyPickerModal = true }
                        .testTag("quick_open_picker_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Все дампы…", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Current scanned dump if available
            currentScannedDump?.let { scanned ->
                val isSelected = activeDump?.uid == scanned.uid
                item {
                    QuickKeyChip(
                        title = "Считанный дамп",
                        uid = scanned.uid,
                        isSelected = isSelected,
                        onClick = {
                            EmulationManager.startEmulation(scanned, "Считанный ключ (${scanned.uid})")
                            Toast.makeText(context, "Выбран считанный дамп ${scanned.uid}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Presets
            items(EmulationManager.presets) { preset ->
                val isSelected = activeDump?.uid == preset.uidHex
                QuickKeyChip(
                    title = preset.title,
                    uid = preset.uidHex,
                    isSelected = isSelected,
                    onClick = {
                        EmulationManager.startEmulationFromUid(preset.uidHex, preset.title, preset.cardType)
                        Toast.makeText(context, "Выбран пресет: ${preset.title}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Saved cards from Vault
            items(savedCards) { card ->
                val isSelected = activeDump?.uid == card.uidHex
                QuickKeyChip(
                    title = card.name,
                    uid = card.uidHex,
                    isSelected = isSelected,
                    onClick = {
                        val decrypted = vaultViewModel.decryptCard(card)
                        if (decrypted != null) {
                            EmulationManager.startEmulation(decrypted, card.name)
                            Toast.makeText(context, "Выбран ключ из хранилища: ${card.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Ошибка расшифровки ключа", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Primary Toggle Button
        Button(
            onClick = {
                if (isEmulationActive) {
                    EmulationManager.stopEmulation()
                    Toast.makeText(context, "Эмуляция ключа остановлена", Toast.LENGTH_SHORT).show()
                } else {
                    if (activeDump != null) {
                        EmulationManager.startEmulation(activeDump!!, activeKeyName)
                        Toast.makeText(context, "Телефон выступает в роли ключа: $activeKeyName", Toast.LENGTH_SHORT).show()
                    } else if (currentScannedDump != null) {
                        EmulationManager.startEmulation(currentScannedDump!!, "Считанный ключ (${currentScannedDump!!.uid})")
                        Toast.makeText(context, "Эмуляция считанного ключа активирована", Toast.LENGTH_SHORT).show()
                    } else if (savedCards.isNotEmpty()) {
                        val first = savedCards.first()
                        val decrypted = vaultViewModel.decryptCard(first)
                        if (decrypted != null) {
                            EmulationManager.startEmulation(decrypted, first.name)
                            Toast.makeText(context, "Эмуляция ключа '${first.name}' запущена", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Use default preset
                        val preset = EmulationManager.presets.first()
                        EmulationManager.startEmulationFromUid(preset.uidHex, preset.title, preset.cardType)
                        Toast.makeText(context, "Активирован ключ: ${preset.title}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("toggle_emulation_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEmulationActive) MaterialTheme.colorScheme.error else NeonGreen,
                contentColor = if (isEmulationActive) TextPrimary else DarkSurface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (isEmulationActive) Icons.Default.CastConnected else Icons.Default.Contactless,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEmulationActive) "ОСТАНОВИТЬ ЭМУЛЯЦИЮ" else "АКТИВИРОВАТЬ РЕЖИМ КЛЮЧА",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Change Active Key Button
        OutlinedButton(
            onClick = { showKeyPickerModal = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("select_key_for_emulation_button"),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
        ) {
            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выбрать другой ключ / дамп для эмуляции", color = CyberCyan, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dedicated Galaxy Watch Ultra (Wear OS) Mode Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEnterWearMode)
                .testTag("open_wear_os_mode_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GalaxyUltraOrange.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = "Galaxy Watch Ultra",
                    tint = GalaxyUltraOrange,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "РЕЖИМ GALAXY WATCH ULTRA (Wear OS)",
                        color = GalaxyUltraOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Круглый экран, быстрый выбор ключей безелем, AMOLED black и виброотклик",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = GalaxyUltraOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Reader APDU Simulator & Test Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ПРОВЕРКА ОТВЕТА СЧИТЫВАТЕЛЮ (APDU)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    TextButton(onClick = { showApduTester = !showApduTester }) {
                        Text(if (showApduTester) "Свернуть" else "Открыть тест", color = CyberCyan, fontSize = 11.sp)
                    }
                }

                if (showApduTester) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Протестируйте, как эмулируемый ключ отвечает на запросы турникета/считывателя прямо в приложении:",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isEmulationActive && activeDump != null) {
                                    EmulationManager.startEmulation(activeDump!!, activeKeyName)
                                }
                                EmulationManager.simulateReaderCommand("FF CA 00 00 00")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Text("GET UID", fontSize = 11.sp, color = CyberCyan)
                        }

                        Button(
                            onClick = {
                                if (!isEmulationActive && activeDump != null) {
                                    EmulationManager.startEmulation(activeDump!!, activeKeyName)
                                }
                                EmulationManager.simulateReaderCommand("00 A4 04 00 07 D2 76 00 00 85 01 01")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Text("Type 4 AID", fontSize = 11.sp, color = NeonGreen)
                        }

                        Button(
                            onClick = {
                                if (!isEmulationActive && activeDump != null) {
                                    EmulationManager.startEmulation(activeDump!!, activeKeyName)
                                }
                                EmulationManager.simulateReaderCommand("00 B0 00 00 10")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Text("READ BLOCK 0", fontSize = 10.sp, color = CyberPurple)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customApduInput,
                            onValueChange = { customApduInput = it },
                            label = { Text("Команда APDU (HEX)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkSurfaceBorder
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (!isEmulationActive && activeDump != null) {
                                    EmulationManager.startEmulation(activeDump!!, activeKeyName)
                                }
                                EmulationManager.simulateReaderCommand(customApduInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // APDU Realtime Traffic Log
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isEmulationActive) NeonGreen else TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APDU ТРАФИК ОБМЕНА ($interactionCount)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (apduLogs.isNotEmpty()) {
                        IconButton(onClick = { EmulationManager.clearLogs() }) {
                            Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Очистить", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (apduLogs.isEmpty()) {
                    Text(
                        text = "Ожидание поднесения к считывателю... Запросы домофона/турникета отобразятся здесь в реальном времени.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        apduLogs.take(10).forEach { log ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = log.description, color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(text = log.timestamp, color = TextSecondary, fontSize = 10.sp)
                                    }
                                    if (log.commandHex != "-") {
                                        Text(
                                            text = "CMD: ${log.commandHex}",
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (log.responseHex != "-") {
                                        Text(
                                            text = "RESP: ${log.responseHex}",
                                            color = NeonGreen,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Technical Notice Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ℹ️ Как работает эмуляция ключа:",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Используется протокол Android Host Card Emulation (HCE). Смартфон выступает в роли бесконтактного ключа и отвечает на запросы считывателя по стандарту ISO 14443-4 / ISO 7816. Он передает выбранный UID, структуру блоков дампа и NDEF идентификатор. Совместимо со считывателями СКУД, умными домофонами (Спутник, Beward, Sigur) и турникетами с поддержкой бесконтактных смарт-карт.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }

    if (showKeyPickerModal) {
        SelectKeyForEmulationModal(
            savedCards = savedCards,
            currentScannedDump = currentScannedDump,
            onDismiss = { showKeyPickerModal = false },
            onSelect = { dump, name ->
                EmulationManager.startEmulation(dump, name)
                showKeyPickerModal = false
                Toast.makeText(context, "Выбран ключ для эмуляции: $name (UID: ${dump.uid})", Toast.LENGTH_SHORT).show()
            },
            vaultViewModel = vaultViewModel
        )
    }
}

@Composable
fun QuickKeyChip(
    title: String,
    uid: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("quick_key_$uid"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) NeonGreen.copy(alpha = 0.2f) else DarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) NeonGreen else DarkSurfaceBorder
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = title,
                color = if (isSelected) NeonGreen else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = uid,
                color = if (isSelected) NeonGreen else CyberCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun VirtualKeyCard(
    isActive: Boolean,
    keyName: String,
    uid: String,
    cardType: String,
    sectorCount: Int,
    onPickKeyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onPickKeyClick)
            .testTag("virtual_key_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (isActive) NeonGreen else CyberPurple.copy(alpha = 0.7f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Logo & Broadcast Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = if (isActive) NeonGreen else CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KeyMaster HCE",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isActive) NeonGreen.copy(alpha = 0.2f) else DarkSurfaceVariant
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isActive) "● ТРАНСЛЯЦИЯ" else "○ ВЫКЛЮЧЕН",
                            color = if (isActive) NeonGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Middle: UID
                Column {
                    Text(
                        text = "UID КЛЮЧА",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = uid,
                        color = if (isActive) NeonGreen else CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }

                // Bottom Row: Key Name & Card Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = keyName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (sectorCount > 0) "$cardType • $sectorCount секторов" else cardType,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Contactless,
                        contentDescription = null,
                        tint = if (isActive) NeonGreen else TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectKeyForEmulationModal(
    savedCards: List<CardEntity>,
    currentScannedDump: CardDump?,
    onDismiss: () -> Unit,
    onSelect: (dump: CardDump, name: String) -> Unit,
    vaultViewModel: VaultViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Из хранилища", "Пресеты", "Свой UID")

    var customUidInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("Мой ключ") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Выбор ключа из дампа", color = CyberCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = CyberCyan
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.height(300.dp)) {
                    when (selectedTab) {
                        0 -> {
                            // Vault & Current Scanned
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                if (currentScannedDump != null) {
                                    Text(
                                        text = "ТЕКУЩИЙ СЧИТАННЫЙ ДАМП:",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurfaceVariant)
                                            .clickable {
                                                onSelect(currentScannedDump, "Считанный ключ (${currentScannedDump.uid})")
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text(text = "Текущий дамп со сканера", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = "UID: ${currentScannedDump.uid} • ${currentScannedDump.cardType.displayName}", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Text(
                                    text = "ИЗ ЗАШИФРОВАННОГО ХРАНИЛИЩА:",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (savedCards.isEmpty()) {
                                    Text(
                                        text = "В хранилище пока нет сохраненных ключей. Вы можете выбрать готовый пресет или ввести свой UID на соседних вкладках.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    savedCards.forEach { card ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(DarkSurfaceVariant)
                                                .clickable {
                                                    val decrypted = vaultViewModel.decryptCard(card)
                                                    if (decrypted != null) {
                                                        onSelect(decrypted, card.name)
                                                    }
                                                }
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = card.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                    Text(text = "UID: ${card.uidHex} • ${card.category}", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                                }
                                                Text(text = "Выбрать", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Presets list
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(
                                    text = "СТАНДАРТНЫЕ ПРОПУСКА И ДОМОФОНЫ:",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                EmulationManager.presets.forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurfaceVariant)
                                            .clickable {
                                                val dump = EmulationManager.createDumpFromUid(preset.uidHex, preset.cardType)
                                                onSelect(dump, preset.title)
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = preset.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(text = preset.category, color = CyberPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(text = "UID: ${preset.uidHex} • ${preset.cardType.displayName}", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            Text(text = preset.description, color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                        2 -> {
                            // Custom UID
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(
                                    text = "СОЗДАТЬ КЛЮЧ ПО ПРОИЗВОЛЬНОМУ UID:",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = customNameInput,
                                    onValueChange = { customNameInput = it },
                                    label = { Text("Название ключа") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkSurfaceBorder
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = customUidInput,
                                    onValueChange = { customUidInput = it },
                                    label = { Text("UID в HEX (например: 12:34:56:78)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkSurfaceBorder
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (customUidInput.isNotBlank()) {
                                            val dump = EmulationManager.createDumpFromUid(customUidInput.trim(), CardType.MIFARE_CLASSIC_1K)
                                            onSelect(dump, customNameInput.ifBlank { "Ключ $customUidInput" })
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DarkSurface),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = customUidInput.trim().replace(":", "").replace(" ", "").length >= 2
                                ) {
                                    Text("Активировать этот UID", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
