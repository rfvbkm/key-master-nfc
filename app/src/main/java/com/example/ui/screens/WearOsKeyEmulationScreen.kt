package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hce.EmulationManager
import com.example.hce.KeyPreset
import com.example.model.CardDump
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.HapticHelper
import com.example.viewmodel.VaultViewModel

// Galaxy Watch Ultra Accent Colors
val GalaxyUltraOrange = Color(0xFFFF5722)
val GalaxyUltraDark = Color(0xFF000000)
val GalaxySurfaceDark = Color(0xFF141414)
val GalaxySurfaceBorder = Color(0xFF282828)

@Composable
fun WearOsKeyEmulationScreen(
    vaultViewModel: VaultViewModel,
    onExitWearMode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isEmulationActive by EmulationManager.isEmulationActive.collectAsState()
    val activeDump by EmulationManager.activeDump.collectAsState()
    val activeKeyName by EmulationManager.activeKeyName.collectAsState()
    val interactionCount by EmulationManager.interactionCount.collectAsState()
    val savedCards by vaultViewModel.savedCards.collectAsState()

    var showWatchKeyList by remember { mutableStateOf(false) }

    // Aggregate available keys for watch: Presets + Saved Cards
    val presets = EmulationManager.presets

    // Pulsing circle animation when transmitting
    val infiniteTransition = rememberInfiniteTransition(label = "watch_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GalaxyUltraDark)
            .testTag("wear_os_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer decorative ring mimicking Galaxy Watch Ultra circular bezel
        Box(
            modifier = Modifier
                .size(380.dp)
                .clip(CircleShape)
                .background(GalaxyUltraDark)
                .border(
                    width = 2.dp,
                    color = if (isEmulationActive) NeonGreen.copy(alpha = 0.8f) else GalaxyUltraOrange.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )

        // Main Circular Content Container
        Column(
            modifier = Modifier
                .size(360.dp)
                .clip(CircleShape)
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Watch Brand & Phone switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isEmulationActive) NeonGreen else GalaxyUltraOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WATCH ULTRA HCE",
                        color = if (isEmulationActive) NeonGreen else GalaxyUltraOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (onExitWearMode != null) {
                    IconButton(
                        onClick = onExitWearMode,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "Режим телефона",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Central Animated NFC Key Icon & UID
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(if (isEmulationActive) pulseScale else 1.0f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isEmulationActive)
                                listOf(NeonGreen.copy(alpha = 0.35f), GalaxySurfaceDark)
                            else
                                listOf(GalaxyUltraOrange.copy(alpha = 0.25f), GalaxySurfaceDark)
                        )
                    )
                    .border(
                        2.dp,
                        if (isEmulationActive) NeonGreen else GalaxyUltraOrange.copy(alpha = 0.6f),
                        CircleShape
                    )
                    .clickable {
                        // Fast toggle on tap of central ring
                        if (isEmulationActive) {
                            EmulationManager.stopEmulation()
                        } else {
                            if (activeDump != null) {
                                EmulationManager.startEmulation(activeDump!!, activeKeyName)
                            } else {
                                val first = presets.first()
                                EmulationManager.startEmulationFromUid(first.uidHex, first.title, first.cardType)
                            }
                            HapticHelper.vibrateSuccess()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEmulationActive) Icons.Default.CastConnected else Icons.Default.Contactless,
                    contentDescription = null,
                    tint = if (isEmulationActive) NeonGreen else GalaxyUltraOrange,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Key Name & UID Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = activeKeyName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = activeDump?.uid ?: "12:34:56:78",
                    color = if (isEmulationActive) NeonGreen else CyberCyan,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                if (interactionCount > 0) {
                    Text(
                        text = "Считываний СКУД: $interactionCount",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Big Button for Wear OS
            Button(
                onClick = {
                    if (isEmulationActive) {
                        EmulationManager.stopEmulation()
                        HapticHelper.vibrateSuccess()
                        Toast.makeText(context, "Эмуляция остановлена", Toast.LENGTH_SHORT).show()
                    } else {
                        if (activeDump != null) {
                            EmulationManager.startEmulation(activeDump!!, activeKeyName)
                        } else {
                            val first = presets.first()
                            EmulationManager.startEmulationFromUid(first.uidHex, first.title, first.cardType)
                        }
                        HapticHelper.vibrateSuccess()
                        Toast.makeText(context, "Трансляция ключа включена", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(48.dp)
                    .testTag("wear_toggle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEmulationActive) MaterialTheme.colorScheme.error else NeonGreen,
                    contentColor = GalaxyUltraDark
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = if (isEmulationActive) Icons.Default.CastConnected else Icons.Default.Contactless,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEmulationActive) "ОСТАНОВИТЬ" else "ТРАНСЛЯЦИЯ ВКЛ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Carousel / Bezel Quick Switch Row
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Key Button
                IconButton(
                    onClick = {
                        val allKeys = presets.map { it.title to it.uidHex }
                        val currentIdx = allKeys.indexOfFirst { it.second == (activeDump?.uid ?: "") }
                        val prevIdx = if (currentIdx > 0) currentIdx - 1 else allKeys.size - 1
                        val selected = presets[prevIdx]
                        EmulationManager.startEmulationFromUid(selected.uidHex, selected.title, selected.cardType)
                        HapticHelper.vibrateKeyRead()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Предыдущий", tint = CyberCyan)
                }

                // Open full watch key list
                Card(
                    modifier = Modifier
                        .clickable { showWatchKeyList = true }
                        .testTag("wear_open_list_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GalaxySurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GalaxySurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = GalaxyUltraOrange, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сменить ключ", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Next Key Button
                IconButton(
                    onClick = {
                        val allKeys = presets.map { it.title to it.uidHex }
                        val currentIdx = allKeys.indexOfFirst { it.second == (activeDump?.uid ?: "") }
                        val nextIdx = if (currentIdx in 0 until (allKeys.size - 1)) currentIdx + 1 else 0
                        val selected = presets[nextIdx]
                        EmulationManager.startEmulationFromUid(selected.uidHex, selected.title, selected.cardType)
                        HapticHelper.vibrateKeyRead()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Следующий", tint = CyberCyan)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Modal List for Wear OS
    if (showWatchKeyList) {
        WearKeyPickerModal(
            presets = presets,
            savedCards = savedCards,
            activeUid = activeDump?.uid ?: "",
            onDismiss = { showWatchKeyList = false },
            onSelect = { dump, name ->
                EmulationManager.startEmulation(dump, name)
                HapticHelper.vibrateSuccess()
                showWatchKeyList = false
                Toast.makeText(context, "Выбран ключ: $name", Toast.LENGTH_SHORT).show()
            },
            vaultViewModel = vaultViewModel
        )
    }
}

@Composable
fun WearKeyPickerModal(
    presets: List<KeyPreset>,
    savedCards: List<com.example.db.CardEntity>,
    activeUid: String,
    onDismiss: () -> Unit,
    onSelect: (dump: CardDump, name: String) -> Unit,
    vaultViewModel: VaultViewModel
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ключи на часах",
                color = GalaxyUltraOrange,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Presets
                item {
                    Text(
                        text = "ПРЕСЕТЫ ДОМОФОНОВ И СКУД:",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(presets) { preset ->
                    val isSelected = activeUid == preset.uidHex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else GalaxySurfaceDark)
                            .border(1.dp, if (isSelected) NeonGreen else GalaxySurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                val dump = EmulationManager.createDumpFromUid(preset.uidHex, preset.cardType)
                                onSelect(dump, preset.title)
                            }
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(preset.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(preset.uidHex, color = if (isSelected) NeonGreen else CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Saved Cards from Vault
                if (savedCards.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ИЗ ХРАНИЛИЩА:",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(savedCards) { card ->
                        val isSelected = activeUid == card.uidHex
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else GalaxySurfaceDark)
                                .border(1.dp, if (isSelected) NeonGreen else GalaxySurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                val decrypted = vaultViewModel.decryptCard(card)
                                if (decrypted != null) {
                                    onSelect(decrypted, card.name)
                                }
                            }
                            .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(card.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(card.uidHex, color = if (isSelected) NeonGreen else CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GalaxySurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Закрыть", color = TextSecondary, fontSize = 12.sp)
            }
        },
        containerColor = GalaxyUltraDark
    )
}
