package com.example.ai

import android.content.Context
import com.example.model.CardDump
import com.example.model.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AiResult {
    data class Success(val analysisMarkdown: String) : AiResult()
    data class Error(val message: String) : AiResult()
}

class GeminiApiService(context: Context) {

    private val settingsManager = AiSettingsManager(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeCardDump(
        dump: CardDump,
        customQuery: String? = null
    ): AiResult = withContext(Dispatchers.IO) {
        val apiKey = settingsManager.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext AiResult.Error(
                "Gemini API ключ не настроен. Нажмите иконку ⚙️ Настройки в правом верхнем углу, чтобы ввести свой API ключ, или настройте GEMINI_API_KEY в панели Secrets AI Studio."
            )
        }

        val dumpSummary = buildDumpSummary(dump)
        val prompt = if (customQuery.isNullOrBlank()) {
            """
            Проведи глубокий криптографический аудит и анализ дампа бесконтактной смарт-карты:
            
            $dumpSummary
            
            Выполни структурированный экспертный отчет на русском языке:
            1. **Идентификация чипа и системы**: Тип чипа, производитель, определение типа СКУД/домофона/системы (например, Vizit, Cyfral, Metakom, Sigur, Parsec, парковка или стандартный NXP).
            2. **Анализ криптографии и ключей**: Какие ключи использованы (заводские/дефолтные, транспортные или кастомные). Степень защищенности (Crypto-1, 3DES, AES).
            3. **Аудит условий доступа (Access Bits)**: Безопасность конфигурации секторных трейлеров. Есть ли риск перезаписи или блокировки карты.
            4. **Уязвимости**: Подвержена ли карта атакам (Nested attack, Darkside, Hardnested, Default keys dictionary clone, клонирование UID).
            5. **Рекомендации по безопасности**: Практические советы для администратора системы или владельца ключа.
            """.trimIndent()
        } else {
            """
            Информация о карте и дамп:
            $dumpSummary
            
            Вопрос пользователя: $customQuery
            
            Дай подробный технический ответ эксперта по RFID/NFC безопасности на русском языке.
            """.trimIndent()
        }

        val primaryModel = settingsManager.selectedModel
        val primaryResult = executeGenerateContent(apiKey, primaryModel, prompt)

        if (primaryResult is CallResult.Success) {
            return@withContext AiResult.Success(primaryResult.text)
        }

        val primaryError = primaryResult as CallResult.Error

        // If 429 Rate Limit / Quota Exceeded and primary model was pro-preview, try auto-fallback to flash if enabled
        if (primaryError.statusCode == 429 && settingsManager.autoFallbackOn429 && primaryModel != AiSettingsManager.MODEL_FLASH) {
            val fallbackModel = AiSettingsManager.MODEL_FLASH
            val fallbackResult = executeGenerateContent(apiKey, fallbackModel, prompt)
            if (fallbackResult is CallResult.Success) {
                val combinedText = fallbackResult.text +
                        "\n\n---\n*⚡ Примечание: Модель $primaryModel вернула ошибку 429 (исчерпана квота запросов). Анализ успешно выполнен резервной моделью $fallbackModel.*"
                return@withContext AiResult.Success(combinedText)
            }
        }

        if (primaryError.statusCode == 429) {
            return@withContext AiResult.Error(
                "⚠️ Ошибка 429: Исчерпан лимит запросов / квота модели ($primaryModel).\n\n" +
                "Как исправить:\n" +
                "1. Нажмите ⚙️ Настройки вверху и переключите модель на 'gemini-3.5-flash' (у нее значительно выше лимиты).\n" +
                "2. Или укажите свой личный бесплатный Gemini API ключ (получить на aistudio.google.com/apikey)."
            )
        }

        return@withContext AiResult.Error("Ошибка API (${primaryError.statusCode}): ${primaryError.message}")
    }

    private sealed class CallResult {
        data class Success(val text: String) : CallResult()
        data class Error(val statusCode: Int, val message: String) : CallResult()
    }

    private fun executeGenerateContent(apiKey: String, model: String, prompt: String): CallResult {
        try {
            val root = JSONObject()

            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            // High Thinking mode if gemini-3.1-pro-preview
            if (model == AiSettingsManager.MODEL_PRO_THINKING) {
                val genConfig = JSONObject()
                val thinkingConfig = JSONObject()
                thinkingConfig.put("thinkingLevel", "high")
                genConfig.put("thinkingConfig", thinkingConfig)
                root.put("generationConfig", genConfig)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return CallResult.Error(response.code, responseBody ?: "Empty response")
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return CallResult.Error(response.code, "Модель не вернула кандидатов ответа")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textBuilder = StringBuilder()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) {
                        textBuilder.append(p.getString("text"))
                    }
                }
            }

            val finalText = textBuilder.toString()
            return if (finalText.isNotBlank()) {
                CallResult.Success(finalText)
            } else {
                CallResult.Error(response.code, "Пустой текст в ответе модели")
            }
        } catch (e: Exception) {
            return CallResult.Error(-1, e.message ?: "Сетевая ошибка")
        }
    }

    private fun buildDumpSummary(dump: CardDump): String {
        val sb = StringBuilder()
        sb.append("Тип карты: ").append(dump.cardType.displayName).append("\n")
        sb.append("UID: ").append(dump.uid).append("\n")
        sb.append("ATQA: ").append(dump.atqa).append(" | SAK: ").append(dump.sak).append("\n")
        sb.append("Технологии: ").append(dump.rawTechs.joinToString(", ")).append("\n")

        if (dump.sectors.isNotEmpty()) {
            sb.append("\nСектора MIFARE (всего ${dump.sectors.size}):\n")
            dump.sectors.take(8).forEach { sec ->
                sb.append("Сектор ${sec.sectorIndex}: ")
                sb.append("KeyA=${sec.keyA ?: "???"}, ")
                sb.append("AccessBits=${sec.accessBits ?: "???"}, ")
                sb.append("KeyB=${sec.keyB ?: "???"}\n")
                sec.blocks.forEachIndexed { bIdx, blk ->
                    val ascii = String(blk, Charsets.US_ASCII).filter { it in ' '..'~' }
                    sb.append("  Блок $bIdx: ").append(blk.toHex(" "))
                    if (ascii.isNotBlank()) sb.append(" [ASCII: $ascii]")
                    sb.append("\n")
                }
            }
            if (dump.sectors.size > 8) {
                sb.append("... и еще ${dump.sectors.size - 8} секторов.\n")
            }
        }

        if (dump.pages.isNotEmpty()) {
            sb.append("\nСтраницы Ultralight/NTAG (всего ${dump.pages.size}):\n")
            dump.pages.take(16).forEach { p ->
                sb.append("Стр ${p.pageNumber}: ").append(p.data.toHex(" ")).append("\n")
            }
        }

        if (!dump.ndefPayload.isNullOrBlank()) {
            sb.append("\nNDEF запись: ").append(dump.ndefPayload).append("\n")
        }

        return sb.toString()
    }
}
