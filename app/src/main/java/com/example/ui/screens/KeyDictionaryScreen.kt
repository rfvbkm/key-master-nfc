package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.DictionaryKeyEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.VaultViewModel

@Composable
fun KeyDictionaryScreen(
    vaultViewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val keys by vaultViewModel.dictionaryKeys.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = CyberCyan
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Словарь ключей MIFARE",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Используются для подбора и аутентификации секторов (${keys.size} ключей)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Add key button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth().testTag("add_key_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkSurface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Добавить ключ в словарь", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keys List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(keys) { key ->
                DictionaryKeyItem(
                    key = key,
                    onDelete = {
                        vaultViewModel.deleteDictionaryKey(key)
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddKeyDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { hex, desc ->
                vaultViewModel.addDictionaryKey(hex, desc)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DictionaryKeyItem(key: DictionaryKeyEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = if (key.isCustom) CyberCyan else NeonGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = key.keyHex,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = key.description.ifBlank { if (key.isCustom) "Пользовательский" else "Заводской" },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (key.isCustom) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = "Базовый", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun AddKeyDialog(
    onDismiss: () -> Unit,
    onAdd: (hex: String, description: String) -> Unit
) {
    var keyHex by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Новый ключ MIFARE (6 байт)", color = CyberCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = keyHex,
                    onValueChange = { input ->
                        keyHex = input.uppercase()
                        val clean = input.replace(" ", "")
                        error = if (clean.length == 12) null else "Нужно 12 символов HEX (6 байт)"
                    },
                    label = { Text("12 HEX символов (например, A0A1A2A3A4A5)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    )
                )
                error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (например, Ключ офиса)") },
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
                onClick = {
                    val clean = keyHex.replace(" ", "")
                    if (clean.length == 12) {
                        onAdd(clean, description)
                    } else {
                        error = "Ключ должен быть ровно 12 символов"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkSurface)
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        },
        containerColor = DarkSurface
    )
}
