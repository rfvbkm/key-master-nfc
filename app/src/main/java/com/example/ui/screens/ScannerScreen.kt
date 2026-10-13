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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hce.EmulationManager
import com.example.model.CardDump
import com.example.model.CardType
import com.example.ui.components.RadarAnimation
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.NfcViewModel
import com.example.viewmodel.ScanUiState
import com.example.viewmodel.VaultViewModel

@Composable
fun ScannerScreen(
    nfcViewModel: NfcViewModel,
    vaultViewModel: VaultViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToAiAudit: () -> Unit,
    onNavigateToEmulation: () -> Unit,
    isNfcAvailable: Boolean,
    isNfcEnabled: Boolean
) {
    val context = LocalContext.current
    val scanState by nfcViewModel.scanState.collectAsState()
    val currentDump by nfcViewModel.currentDump.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // NFC Hardware Status Banner
        NfcStatusBanner(isAvailable = isNfcAvailable, isEnabled = isNfcEnabled)

        Spacer(modifier = Modifier.height(20.dp))

        // Central Radar Area
        val isReading = scanState is ScanUiState.Reading
        RadarAnimation(isScanning = isReading || scanState is ScanUiState.Idle)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isReading) "Чтение секторов памяти..." else "Приложите ключ или карту к NFC-антенне",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Поддерживаются MIFARE Classic 1K/4K, Ultralight, NTAG21x, NDEF",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar during read
        if (scanState is ScanUiState.Reading) {
            val state = scanState as ScanUiState.Reading
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { if (state.total > 0) state.current.toFloat() / state.total else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = CyberCyan,
                    trackColor = DarkSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.message,
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Demo test dump generator for emulator/preview testing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = {
                    nfcViewModel.loadDemoDump(CardType.MIFARE_CLASSIC_1K)
                    Toast.makeText(context, "Загружен тестовый дамп MIFARE Classic", Toast.LENGTH_SHORT).show()
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("load_demo_mifare_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Тестовый дамп (MIFARE 1K)", color = CyberCyan, fontSize = 12.sp)
            }
        }

        // Error Banner
        if (scanState is ScanUiState.Error) {
            val error = (scanState as ScanUiState.Error).error
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Detected Card Overview Card
        currentDump?.let { dump ->
            CardDetectedCard(
                dump = dump,
                onOpenEditor = onNavigateToEditor,
                onSave = { showSaveDialog = true },
                onAiAudit = onNavigateToAiAudit,
                onEmulate = {
                    EmulationManager.startEmulation(dump, "Считанный ключ (${dump.uid})")
                    onNavigateToEmulation()
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showSaveDialog && currentDump != null) {
        SaveCardDialog(
            dump = currentDump!!,
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, category, notes ->
                vaultViewModel.saveCard(currentDump!!, name, category, notes)
                showSaveDialog = false
                Toast.makeText(context, "Ключ '$name' зашифрован и сохранен в хранилище", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun NfcStatusBanner(isAvailable: Boolean, isEnabled: Boolean) {
    val (bgColor, icon, title, subtitle) = when {
        !isAvailable -> Quad(
            DarkSurfaceVariant,
            Icons.Default.Warning,
            "NFC модуль не обнаружен",
            "Используйте тестовый дамп для проверки функций"
        )
        !isEnabled -> Quad(
            DarkSurfaceVariant,
            Icons.Default.Nfc,
            "NFC выключен в системе",
            "Включите NFC в системных настройках Android"
        )
        else -> Quad(
            NeonGreen.copy(alpha = 0.1f),
            Icons.Default.CheckCircle,
            "NFC считыватель активен",
            "Готов к чтению и эмуляции протоколов"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isAvailable && isEnabled) NeonGreen else CyberCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun CardDetectedCard(
    dump: CardDump,
    onOpenEditor: () -> Unit,
    onSave: () -> Unit,
    onAiAudit: () -> Unit,
    onEmulate: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detected_card_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Type + Success indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dump.cardType.displayName,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${dump.sectors.size} секторов",
                        color = CyberPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // UID Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "UID КЛЮЧА", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = dump.uid,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("UID", dump.uid))
                        Toast.makeText(context, "UID скопирован", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Копировать UID",
                        tint = CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tech details: ATQA / SAK / Techs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TechChip(label = "ATQA", value = dump.atqa.ifBlank { "N/A" }, modifier = Modifier.weight(1f))
                TechChip(label = "SAK", value = dump.sak.ifBlank { "N/A" }, modifier = Modifier.weight(1f))
                TechChip(
                    label = "ПАМЯТЬ",
                    value = dump.cardType.memorySize,
                    modifier = Modifier.weight(1.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenEditor,
                    modifier = Modifier.weight(1f).testTag("open_dump_editor_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Редактор", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).testTag("save_to_vault_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Сохранить", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Act as this key button
            Button(
                onClick = onEmulate,
                modifier = Modifier.fillMaxWidth().testTag("emulate_scanned_key_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DarkSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Contactless,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Выступить в роли этого ключа (HCE)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Security Audit Button
            Button(
                onClick = onAiAudit,
                modifier = Modifier.fillMaxWidth().testTag("ai_audit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI Анализ безопасности (Gemini 3.1 Pro)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun TechChip(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(text = label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SaveCardDialog(
    dump: CardDump,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("Ключ ${dump.uid}") }
    var category by remember { mutableStateOf("Домофон") }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Домофон", "Офис", "Квартира", "Шлагбаум", "Пропуск", "Транспорт")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Зашифровать и сохранить ключ", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Дамп будет зашифрован аппаратным AES-256-GCM в Android KeyStore.",
                    color = NeonGreen,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название ключа") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Категория:", color = TextSecondary, fontSize = 12.sp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChipSimple(
                            label = cat,
                            selected = category == cat,
                            onClick = { category = cat }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.drop(3).forEach { cat ->
                        FilterChipSimple(
                            label = cat,
                            selected = category == cat,
                            onClick = { category = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметка (номер квартиры, этаж)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, category, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkSurface)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun FilterChipSimple(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) CyberCyan else DarkSurfaceVariant)
            .border(1.dp, if (selected) CyberCyan else DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) DarkSurface else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
