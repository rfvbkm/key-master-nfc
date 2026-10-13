package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.CardEntity
import com.example.model.CardDump
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
import com.example.viewmodel.VaultViewModel
import com.example.viewmodel.WriteUiState

@Composable
fun WriterScreen(
    nfcViewModel: NfcViewModel,
    vaultViewModel: VaultViewModel
) {
    val context = LocalContext.current
    val writeState by nfcViewModel.writeState.collectAsState()
    val isWritePending by nfcViewModel.isWritePending.collectAsState()
    val currentDump by nfcViewModel.currentDump.collectAsState()
    val savedCards by vaultViewModel.savedCards.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var allowMagicBlock0 by remember { mutableStateOf(false) }
    var ndefTextInput by remember { mutableStateOf("https://") }
    var selectedCardForWriting by remember { mutableStateOf<CardEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tab row: Clone Dump vs Write NDEF
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = DarkSurface,
            contentColor = CyberCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = CyberCyan
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Клонирование дампа", fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Запись NDEF метки", fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTabIndex == 0) {
            // CLONE DUMP MODE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ИСХОДНЫЙ ДАМП ДЛЯ ЗАПИСИ",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (currentDump != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentDump!!.cardType.displayName,
                                    color = CyberCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "UID: ${currentDump!!.uid} (${currentDump!!.sectors.size} секторов)",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Icon(imageVector = Icons.Default.CopyAll, contentDescription = null, tint = NeonGreen)
                        }
                    } else {
                        Text(
                            text = "Дамп не выбран. Выберите ключ из списка ниже или отсканируйте оригинал.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Magic card option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { allowMagicBlock0 = !allowMagicBlock0 }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allowMagicBlock0,
                            onCheckedChange = { allowMagicBlock0 = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CyberCyan,
                                uncheckedColor = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Запись сектора 0 (Magic / CUID заготовки)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Позволяет клонировать UID на болванку с перезаписываемым сектором 0",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isWritePending) {
                        OutlinedButton(
                            onClick = { nfcViewModel.cancelWrite() },
                            modifier = Modifier.fillMaxWidth().testTag("cancel_write_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Отменить ожидание записи")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (currentDump != null) {
                                    nfcViewModel.startWritingDump(currentDump!!, allowMagicBlock0)
                                    Toast.makeText(context, "Поднесите чип-заготовку к телефону", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Сначала выберите дамп", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = currentDump != null,
                            modifier = Modifier.fillMaxWidth().testTag("start_clone_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkSurface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Записать на заготовку", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick select from Vault
            Text(
                text = "ИЛИ ВЫБЕРИТЕ ИЗ ХРАНИЛИЩА:",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            savedCards.take(5).forEach { card ->
                SavedCardPickItem(
                    card = card,
                    onPick = {
                        val decrypted = vaultViewModel.decryptCard(card)
                        if (decrypted != null) {
                            nfcViewModel.setCurrentDump(decrypted)
                            Toast.makeText(context, "Выбран дамп: ${card.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        } else {
            // NDEF WRITE MODE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ЗАПИСЬ NDEF СООБЩЕНИЯ",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ndefTextInput,
                        onValueChange = { ndefTextInput = it },
                        label = { Text("Текст или ссылка (URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkSurfaceBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Поднесите пустую или перезаписываемую NTAG / Ultralight / NDEF метку для записи.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Writer status card
        when (writeState) {
            is WriteUiState.WaitingForTag -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberPurple.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberPurple)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RadarAnimation(isScanning = true, modifier = Modifier.size(100.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ожидание карты-заготовки...",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Приложите карту к задней панели телефона и не убирайте до завершения",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            is WriteUiState.Writing -> {
                val state = writeState as WriteUiState.Writing
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Идет запись секторов (${state.current}/${state.total})...",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (state.total > 0) state.current.toFloat() / state.total else 0f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = CyberCyan
                        )
                    }
                }
            }
            is WriteUiState.Success -> {
                val msg = (writeState as WriteUiState.Success).message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = msg, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            is WriteUiState.Error -> {
                val err = (writeState as WriteUiState.Error).reason
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                    }
                }
            }
            is WriteUiState.Idle -> {}
        }
    }
}

@Composable
fun SavedCardPickItem(card: CardEntity, onPick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onPick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = card.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = "UID: ${card.uidHex} • ${card.category}", color = TextSecondary, fontSize = 11.sp)
        }
        Text(text = "Выбрать", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
