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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ai.AiSettingsManager
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AiUiState
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.NfcViewModel

@Composable
fun AiAuditScreen(
    aiViewModel: AiViewModel,
    nfcViewModel: NfcViewModel
) {
    val context = LocalContext.current
    val currentDump by nfcViewModel.currentDump.collectAsState()
    val aiState by aiViewModel.uiState.collectAsState()
    val currentModel by aiViewModel.currentModelName.collectAsState()

    var userCustomQuery by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val quickQueries = listOf(
        "Полный аудит безопасности",
        "Проверка уязвимостей клонирования",
        "Определение системы (Vizit / СКУД)",
        "Аудит условий доступа (Access Bits)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Model & Thinking Mode Banner with Settings Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberPurple.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CyberPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (currentModel == AiSettingsManager.MODEL_PRO_THINKING)
                                    "Gemini 3.1 Pro Preview"
                                else
                                    "Gemini 3.5 Flash",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (currentModel == AiSettingsManager.MODEL_PRO_THINKING)
                                    "Режим мышления: HIGH"
                                else
                                    "Высокая квота и скорость",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Settings Gear Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("gemini_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки Gemini AI",
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Глубокий анализ секторов, расшифровка форматов домофонов и оценка криптографической стойкости ключа.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card Context Info
        if (currentDump != null) {
            val dump = currentDump!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "АНАЛИЗИРУЕМЫЙ КЛЮЧ", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "UID: ${dump.uid}",
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(text = "${dump.cardType.displayName} (${dump.sectors.size} секторов)", color = TextPrimary, fontSize = 12.sp)
                }
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonGreen)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Text(
                    text = "Карта не выбрана. Выберите ключ в сканере или хранилище, либо загрузите тестовый дамп.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Analysis Query Chips
        Text(
            text = "БЫСТРЫЕ ШАБЛОНЫ АУДИТА:",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickQueries) { query ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable {
                            if (currentDump != null) {
                                aiViewModel.analyzeCard(currentDump!!, query)
                            } else {
                                Toast.makeText(context, "Сначала считайте или загрузите дамп", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(text = query, color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom Question Input
        OutlinedTextField(
            value = userCustomQuery,
            onValueChange = { userCustomQuery = it },
            label = { Text("Задайте вопрос по дампу или протоколу...") },
            modifier = Modifier.fillMaxWidth().testTag("ai_custom_query_input"),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (currentDump != null && userCustomQuery.isNotBlank()) {
                            aiViewModel.analyzeCard(currentDump!!, userCustomQuery)
                            userCustomQuery = ""
                        } else if (currentDump == null) {
                            Toast.makeText(context, "Сначала выберите дамп карты", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", tint = CyberCyan)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = DarkSurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // State Feedback
        when (aiState) {
            is AiUiState.Thinking -> {
                val query = (aiState as AiUiState.Thinking).query
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberPurple)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = CyberPurple,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Gemini выполняет глубокий анализ...",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = query,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            is AiUiState.Success -> {
                val markdown = (aiState as AiUiState.Success).markdown
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("ai_result_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = NeonGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Отчет экспертного аудита",
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("AI Audit", markdown))
                                    Toast.makeText(context, "Отчет скопирован", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Копировать", tint = CyberCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = markdown,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            is AiUiState.Error -> {
                val errorMsg = (aiState as AiUiState.Error).message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Ошибка выполнения запроса",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showSettingsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkSurface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Настройки Gemini", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (currentModel != AiSettingsManager.MODEL_FLASH) {
                                OutlinedButton(
                                    onClick = {
                                        aiViewModel.updateSelectedModel(AiSettingsManager.MODEL_FLASH)
                                        if (currentDump != null) {
                                            aiViewModel.analyzeCard(currentDump!!)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Переключить на Flash", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            is AiUiState.Idle -> {}
        }
    }

    if (showSettingsDialog) {
        AiSettingsDialog(
            aiViewModel = aiViewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun AiSettingsDialog(
    aiViewModel: AiViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = aiViewModel.settingsManager

    var customKeyInput by remember { mutableStateOf(settings.customApiKey) }
    var selectedModel by remember { mutableStateOf(settings.selectedModel) }
    var autoFallback by remember { mutableStateOf(settings.autoFallbackOn429) }

    val hasCustomKey = customKeyInput.isNotBlank()
    val hasBuildKey = settings.getEffectiveApiKey().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Настройки Gemini AI", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Управление API ключом и моделью для обхода лимитов (429 Rate Limit):",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Custom API Key field
                OutlinedTextField(
                    value = customKeyInput,
                    onValueChange = { customKeyInput = it },
                    label = { Text("Пользовательский Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_api_key_input"),
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val item = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!item.isNullOrBlank()) {
                                        customKeyInput = item.trim()
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Вставить", tint = CyberCyan, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Key Status description
                val (statusColor, statusText) = when {
                    hasCustomKey -> NeonGreen to "✓ Используется ваш персональный ключ"
                    hasBuildKey -> CyberCyan to "✓ Используется ключ из Secrets AI Studio"
                    else -> MaterialTheme.colorScheme.error to "✗ Ключ не настроен"
                }

                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Model Selection
                Text(text = "Активная модель:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                ModelOptionRow(
                    title = "gemini-3.1-pro-preview",
                    subtitle = "Глубокое мышление (Thinking: HIGH). Строгие лимиты RPM.",
                    isSelected = selectedModel == AiSettingsManager.MODEL_PRO_THINKING,
                    onClick = { selectedModel = AiSettingsManager.MODEL_PRO_THINKING }
                )

                Spacer(modifier = Modifier.height(6.dp))

                ModelOptionRow(
                    title = "gemini-3.5-flash",
                    subtitle = "Быстрый анализ, высокие лимиты квот (решение при 429).",
                    isSelected = selectedModel == AiSettingsManager.MODEL_FLASH,
                    onClick = { selectedModel = AiSettingsManager.MODEL_FLASH }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Auto fallback switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Авто-переключение при 429",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "При превышении квоты Pro автоматически вызывать Flash",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = autoFallback,
                        onCheckedChange = { autoFallback = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "💡 Бесплатный API ключ можно создать за 1 минуту на сайте ai.google.dev",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    aiViewModel.updateApiKey(customKeyInput)
                    aiViewModel.updateSelectedModel(selectedModel)
                    aiViewModel.updateAutoFallback(autoFallback)
                    onDismiss()
                    Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkSurface),
                modifier = Modifier.testTag("save_ai_settings_button")
            ) {
                Text("Сохранить", fontWeight = FontWeight.Bold)
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
fun ModelOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CyberCyan.copy(alpha = 0.12f) else DarkSurfaceVariant)
            .border(1.dp, if (isSelected) CyberCyan else DarkSurfaceBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) CyberCyan else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
