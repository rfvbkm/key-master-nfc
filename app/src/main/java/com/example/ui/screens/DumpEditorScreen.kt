package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.DumpSerializer
import com.example.model.hexToByteArray
import com.example.model.toHex
import com.example.ui.components.SectorCard
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
fun DumpEditorScreen(
    nfcViewModel: NfcViewModel,
    vaultViewModel: VaultViewModel,
    onNavigateToAiAudit: () -> Unit,
    onNavigateToEmulation: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentDump by nfcViewModel.currentDump.collectAsState()

    var editingSectorIndex by remember { mutableStateOf<Int?>(null) }
    var editingBlockIndex by remember { mutableStateOf<Int?>(null) }
    var editingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    if (currentDump == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Дамп карты не выбран",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Считайте карту на вкладке 'Сканер' или выберите сохраненный ключ в 'Хранилище'.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    val dump = currentDump!!

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Top Toolbar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = dump.cardType.displayName,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "UID: ${dump.uid}",
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                val mctText = DumpSerializer.toMctFormat(dump)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MCT Dump", mctText))
                                Toast.makeText(context, "Дамп MCT скопирован в буфер", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Экспорт MCT", tint = CyberCyan)
                        }

                        IconButton(
                            onClick = { showSaveDialog = true }
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Сохранить", tint = NeonGreen)
                        }

                        IconButton(
                            onClick = {
                                EmulationManager.startEmulation(dump, "Дамп ${dump.uid}")
                                Toast.makeText(context, "Включена эмуляция дампа ${dump.uid}", Toast.LENGTH_SHORT).show()
                                onNavigateToEmulation()
                            },
                            modifier = Modifier.testTag("editor_emulate_icon_button")
                        ) {
                            Icon(imageVector = Icons.Default.Contactless, contentDescription = "Выступить в роли ключа", tint = NeonGreen)
                        }

                        IconButton(
                            onClick = onNavigateToAiAudit
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Аудит", tint = CyberPurple)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Direct Button to act as this key
                Button(
                    onClick = {
                        EmulationManager.startEmulation(dump, "Дамп ${dump.uid}")
                        Toast.makeText(context, "Включена эмуляция ключа ${dump.uid}", Toast.LENGTH_SHORT).show()
                        onNavigateToEmulation()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_emulate_full_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DarkSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Contactless, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ВЫСТУПИТЬ В РОЛИ ЭТОГО КЛЮЧА (HCE)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sectors LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dump.sectors) { sector ->
                SectorCard(
                    sector = sector,
                    onBlockClick = { sIdx, bIdx, bytes ->
                        editingSectorIndex = sIdx
                        editingBlockIndex = bIdx
                        editingBytes = bytes.copyOf()
                    }
                )
            }
        }
    }

    // Block Edit Dialog
    if (editingSectorIndex != null && editingBlockIndex != null && editingBytes != null) {
        EditBlockDialog(
            sectorIndex = editingSectorIndex!!,
            blockIndex = editingBlockIndex!!,
            initialBytes = editingBytes!!,
            onDismiss = {
                editingSectorIndex = null
                editingBlockIndex = null
                editingBytes = null
            },
            onSave = { updatedBytes ->
                nfcViewModel.updateBlockData(editingSectorIndex!!, editingBlockIndex!!, updatedBytes)
                editingSectorIndex = null
                editingBlockIndex = null
                editingBytes = null
                Toast.makeText(context, "Блок обновлен", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Save Card Dialog
    if (showSaveDialog && currentDump != null) {
        SaveCardDialog(
            dump = currentDump!!,
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, category, notes ->
                vaultViewModel.saveCard(currentDump!!, name, category, notes)
                showSaveDialog = false
                Toast.makeText(context, "Ключ '$name' сохранен", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun EditBlockDialog(
    sectorIndex: Int,
    blockIndex: Int,
    initialBytes: ByteArray,
    onDismiss: () -> Unit,
    onSave: (ByteArray) -> Unit
) {
    var hexString by remember { mutableStateOf(initialBytes.toHex(" ")) }
    var asciiString by remember {
        mutableStateOf(String(initialBytes, Charsets.US_ASCII).map { if (it in ' '..'~') it else '.' }.joinToString(""))
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Редактировать блок: Сектор $sectorIndex, Блок $blockIndex",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
        },
        text = {
            Column {
                Text(
                    text = "Введите 16 байт в формате HEX (32 символа):",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = hexString,
                    onValueChange = { input ->
                        hexString = input
                        val clean = input.replace(" ", "").replace(":", "")
                        if (clean.length == 32) {
                            val parsed = clean.hexToByteArray()
                            asciiString = String(parsed, Charsets.US_ASCII).map { if (it in ' '..'~') it else '.' }.joinToString("")
                            errorMessage = null
                        } else {
                            errorMessage = "Требуется ровно 16 байт (сейчас ${clean.length / 2})"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("hex_block_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    )
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = androidx.compose.ui.graphics.Color(0xFFFF5252),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                        .padding(8.dp)
                ) {
                    Column {
                        Text(text = "ASCII Представление:", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = asciiString,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreen
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = hexString.replace(" ", "").replace(":", "")
                    if (clean.length == 32) {
                        onSave(clean.hexToByteArray())
                    } else {
                        errorMessage = "Проверьте количество байт (нужно 16 байт)"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkSurface),
                modifier = Modifier.testTag("save_block_button")
            ) {
                Text("Применить")
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
