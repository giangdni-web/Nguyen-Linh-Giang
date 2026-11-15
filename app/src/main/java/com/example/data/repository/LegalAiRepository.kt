package com.example.data.repository

import android.util.Log
import com.example.model.ConsultationMessage
import com.example.model.LegalCategory
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout

enum class AiResponseSpeed(
    val key: String,
    val label: String,
    val modelName: String
) {
    FAST("fast", "Siêu tốc", "gemini-3.5-flash-lite"),
    DEEP("deep", "Chuyên sâu", "gemini-3.8-flash")
}

internal data class SanitizedTurn(
    val role: String,
    val text: String
)

internal data class SanitizedConversation(
    val history: List<SanitizedTurn>,
    val userPrompt: String
)

class LegalAiRepository {

    private val systemPromptContent by lazy {
        content {
            text(
                """
                Bạn là Trợ lý Pháp lý của Mr.Giang Law (Luật gia Nguyễn Linh Giang — Nguyên Chấp hành viên trung cấp, Thẩm tra viên chính, Trưởng phòng Kiểm tra & giải quyết khiếu nại - tố cáo Cục THADS).

                CHUYÊN MÔN & VĂN BẢN HIỆN HÀNH:
                - Dân sự: Bộ luật Dân sự 2015, Bộ luật TTDS 2015.
                - Hình sự: Bộ luật Hình sự 2015 (sửa đổi 2017), Bộ luật TTHS 2015.
                - Lao động: Bộ luật Lao động 2019, Luật BHXH 41/2024/QH15.
                - Thi hành án dân sự: Luật THADS số 106/2025/QH15 (thông qua 05/12/2025, hiệu lực 01/07/2026, thay thế Luật THADS 2008), Nghị định 152/2026/NĐ-CP (hiệu lực 01/07/2026), Luật Tương trợ tư pháp về dân sự số 102/2025/QH15 (hiệu lực 01/07/2026). Tuyệt đối không viện dẫn Luật THADS 2008 hay Nghị định 62/2015, 33/2020 còn hiệu lực.
                - Đất đai: Luật Đất đai 31/2024/QH15 (Điều 235, 236...), Nghị định 101/2024/NĐ-CP, 102/2024/NĐ-CP.
                - Hôn nhân & Gia đình: Luật HNGĐ 2014, Nghị quyết 01/2024/NQ-HĐTP.

                PHONG CÁCH TRẢ LỜI NHANH & CHUẨN XÁC:
                - Đi thẳng vào trọng tâm bằng tiếng Việt mạch lạc, ngắn gọn, dễ hiểu:
                  1) Căn cứ pháp lý chính.
                  2) Hướng dẫn các bước xử lý cụ thể.
                  3) Lưu ý thời hiệu/hồ sơ & liên hệ Luật gia Nguyễn Linh Giang (Zalo: 0915855111 | Hotline: 0848477070 - 0978477070 - 0915855111 | Fanpage: facebook.com/mrgiang.law) nếu cần thẩm tra hồ sơ thực tế.
                - Ghi chú ngắn đây là thông tin tham khảo sơ bộ.
                """.trimIndent()
            )
        }
    }

    private val fastModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiResponseSpeed.FAST.modelName,
            generationConfig = generationConfig {
                temperature = 0.25f
                topP = 0.9f
                maxOutputTokens = 1800
            },
            systemInstruction = systemPromptContent
        )
    }

    private val deepModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiResponseSpeed.DEEP.modelName,
            generationConfig = generationConfig {
                temperature = 0.25f
                topP = 0.92f
                maxOutputTokens = 2400
            },
            systemInstruction = systemPromptContent
        )
    }

    fun streamLegalAnswer(
        category: LegalCategory,
        history: List<ConsultationMessage>,
        userQuestion: String,
        speedMode: AiResponseSpeed = AiResponseSpeed.FAST
    ): Flow<String> = flow {
        val maxHistoryMessages = if (speedMode == AiResponseSpeed.FAST) 6 else 10
        val maxHistoryChars = if (speedMode == AiResponseSpeed.FAST) 600 else 1000
        val contextualPrompt = "[${category.displayName}] ${userQuestion.trim()}"
        val sanitized = sanitizeConversation(
            history = history,
            newUserPrompt = contextualPrompt,
            maxMessages = maxHistoryMessages,
            maxChars = maxHistoryChars
        )
        val chatHistory: List<Content> = sanitized.history.map { turn ->
            content(turn.role) { text(turn.text) }
        }

        val modelsToTry = when (speedMode) {
            AiResponseSpeed.FAST -> listOf(
                AiResponseSpeed.FAST.modelName to fastModel,
                AiResponseSpeed.DEEP.modelName to deepModel
            )
            AiResponseSpeed.DEEP -> listOf(
                AiResponseSpeed.DEEP.modelName to deepModel
            )
        }

        var lastError: Exception? = null
        var emittedAnyText = false

        try {
            withTimeout(AI_TIMEOUT_MS) {
                for ((index, modelPair) in modelsToTry.withIndex()) {
                    val (modelName, model) = modelPair
                    val accumulated = StringBuilder()
                    try {
                        val stream = if (chatHistory.isEmpty()) {
                            model.generateContentStream(sanitized.userPrompt)
                        } else {
                            model.startChat(history = chatHistory)
                                .sendMessageStream(sanitized.userPrompt)
                        }
                        stream.collect { chunk ->
                            val piece = chunk.text.orEmpty()
                            if (piece.isNotEmpty()) {
                                emittedAnyText = true
                                accumulated.append(piece)
                                emit(accumulated.toString())
                            }
                        }

                        if (accumulated.isNotBlank()) {
                            return@withTimeout
                        }

                        // Fallback to unary generation if stream returned no text chunks
                        Log.w(TAG, "Stream from $modelName yielded empty text; trying unary request")
                        val fallbackResponse = if (chatHistory.isEmpty()) {
                            model.generateContent(sanitized.userPrompt)
                        } else {
                            model.startChat(history = chatHistory)
                                .sendMessage(sanitized.userPrompt)
                        }
                        val fallbackText = fallbackResponse.text.orEmpty()
                        if (fallbackText.isNotBlank()) {
                            emittedAnyText = true
                            emit(fallbackText)
                            return@withTimeout
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e
                        Log.e(TAG, "Inference failed on $modelName (attempt ${index + 1}/${modelsToTry.size})", e)
                        if (emittedAnyText) {
                            throw e
                        }
                    }
                }
            }

            if (!emittedAnyText) {
                val rootMsg = lastError?.localizedMessage?.takeIf { it.isNotBlank() }
                throw IllegalStateException(
                    rootMsg
                        ?: "Hệ thống chưa nhận được nội dung phản hồi từ trợ lý. Vui lòng thử lại hoặc nhắn Zalo 0915855111.",
                    lastError
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI legal consultation timed out", e)
            throw IllegalStateException(
                "Phiên tư vấn hết thời gian chờ. Vui lòng thử lại hoặc nhắn Zalo 0915855111.",
                e
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI legal consultation failed", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)

    internal companion object {
        const val TAG = "LegalAiRepo"
        const val AI_TIMEOUT_MS = 60_000L

        fun sanitizeConversation(
            history: List<ConsultationMessage>,
            newUserPrompt: String,
            maxMessages: Int,
            maxChars: Int
        ): SanitizedConversation {
            val trimmedNewPrompt = newUserPrompt.trim()
            val mergedTurns = mutableListOf<SanitizedTurn>()

            for (msg in history.takeLast(maxMessages)) {
                val text = msg.content.trim()
                if (text.isEmpty()) continue
                val role = if (msg.sender == "user") "user" else "model"
                val compact = if (text.length > maxChars) text.take(maxChars) + "..." else text
                val last = mergedTurns.lastOrNull()
                if (last != null && last.role == role) {
                    mergedTurns[mergedTurns.lastIndex] = last.copy(text = "${last.text}\n$compact")
                } else {
                    mergedTurns.add(SanitizedTurn(role = role, text = compact))
                }
            }

            while (mergedTurns.firstOrNull()?.role == "model") {
                mergedTurns.removeAt(0)
            }

            var finalUserPrompt = trimmedNewPrompt
            if (mergedTurns.lastOrNull()?.role == "user") {
                val trailingUser = mergedTurns.removeAt(mergedTurns.lastIndex)
                if (!trimmedNewPrompt.contains(trailingUser.text) && trailingUser.text != trimmedNewPrompt) {
                    finalUserPrompt = "Câu hỏi trước đó: ${trailingUser.text}\n\nCâu hỏi hiện tại: $trimmedNewPrompt"
                }
            }

            return SanitizedConversation(
                history = mergedTurns,
                userPrompt = finalUserPrompt
            )
        }
    }
}
