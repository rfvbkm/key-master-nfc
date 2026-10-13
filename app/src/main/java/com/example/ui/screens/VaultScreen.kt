package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
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
import com.example.db.CardEntity
import com.example.hce.EmulationManager
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(
    vaultViewModel: VaultViewModel,
    nfcViewModel: NfcViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToDictionary: () -> Unit,
    onNavigateToEmulation: () -> Unit
) {
    val context = LocalContext.current
    val savedCards by vaultViewModel.savedCards.collectAsState()
    var selectedCategory by remember { mutableStateOf("Все") }
    var showImportDialog by remember { mutableStateOf(false) }

    val categories = listOf("Все", "Домофон", "Офис", "Квартира", "Шлагбаум", "Пропуск", "Общие")
    val filteredCards = if (selectedCategory == "Все") {
        savedCards
    } else {
        savedCards.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Hardware AES Encryption Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Хранилище AES-256-GCM KeyStore",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Все дампы и ключи зашифрованы аппаратным чипом безопасности",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Toolbar: Keys Dictionary + Import Dump
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToDictionary,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Словарь ключей", fontSize = 12.sp, color = TextPrimary)
            }

            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Upload, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Импорт MCT", fontSize = 12.sp, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                FilterChipSimple(
                    label = cat,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cards List
        if (filteredCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "В хранилище пока нет сохраненных ключей",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCards) { card ->
                    SavedCardItem(
                        card = card,
                        onOpen = {
                            val dump = vaultViewModel.decryptCard(card)
                            if (dump != null) {
                                nfcViewModel.setCurrentDump(dump)
                                onNavigateToEditor()
                            } else {
                                Toast.makeText(context, "Не удалось расшифровать ключ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onEmulate = {
                            val dump = vaultViewModel.decryptCard(card)
                            if (dump != null) {
                                EmulationManager.startEmulation(dump, card.name)
                                onNavigateToEmulation()
                            } else {
                                Toast.makeText(context, "Не удалось расшифровать ключ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = {
                            vaultViewModel.deleteCard(card)
                        }
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        ImportMctDialog(
            onDismiss = { showImportDialog = false },
            onImport = { mctText, name, cat ->
                val dump = vaultViewModel.importFromMct(mctText, name, cat)
                showImportDialog = false
                if (dump != null) {
                    nfcViewModel.setCurrentDump(dump)
                    Toast.makeText(context, "Дамп успешно импортирован", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun SavedCardItem(
    card: CardEntity,
    onOpen: () -> Unit,
    onEmulate: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(card.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_card_${card.id}"),
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
                Column {
                    Text(
                        text = card.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "UID: ${card.uidHex}",
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = card.category,
                        color = CyberPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (card.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = card.notes, color = TextSecondary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${card.tagType} • $dateStr",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Row {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("UID", card.uidHex))
                            Toast.makeText(context, "UID скопирован", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onEmulate,
                        modifier = Modifier.testTag("emulate_card_icon_button_${card.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Contactless, contentDescription = "Выступить в роли ключа", tint = NeonGreen, modifier = Modifier.size(20.dp))
                    }

                    IconButton(onClick = onOpen) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onEmulate,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("emulate_card_button_${card.id}"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f), contentColor = NeonGreen),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Contactless, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Выступить в роли этого ключа (HCE)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ImportMctDialog(
    onDismiss: () -> Unit,
    onImport: (text: String, name: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("Импортированный ключ") }
    var category by remember { mutableStateOf("Импорт") }
    var mctContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Импорт дампа (.mct)", color = CyberCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = mctContent,
                    onValueChange = { mctContent = it },
                    label = { Text("Вставьте текст дампа MCT (+Sector: 0...)") },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(mctContent, name, category) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkSurface)
            ) {
                Text("Импортировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        },
        containerColor = DarkSurface
    )
}
