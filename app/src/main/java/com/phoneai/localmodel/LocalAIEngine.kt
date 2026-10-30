package com.phoneai.localmodel

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class EngineType {
    LLAMA_CPP,
    MEDIAPIPE
}

class LocalAIEngine(private val context: Context) {

    private var llamaModel: LlamaModel? = null
    private var mediaPipeInference: LlmInference? = null

    var currentEngineType: EngineType? = null
        private set

    var isInitialized: Boolean = false
        private set

    var currentModelPath: String? = null
        private set

    var currentModelName: String = ""
        private set

    /**
     * Initializes the appropriate on-device engine based on file format:
     * - .gguf files (SmolLM2 360M, Gemma 2 2B, Qwen 2.5 0.5B) -> llama.cpp native runner
     * - .bin / .task files -> MediaPipe Tasks GenAI
     */
    suspend fun initializeEngine(
        modelPath: String,
        maxTokens: Int = 1024,
        temperature: Float = 0.7f,
        threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(4, 6)
    ): Result<String> = withContext(Dispatchers.IO) {
        val file = File(modelPath)
        if (!file.exists()) {
            return@withContext Result.failure(IllegalArgumentException("Model file not found at: $modelPath"))
        }

        close() // cleanup previous active instance

        val isGguf = file.name.lowercase().endsWith(".gguf")
        currentModelPath = file.absolutePath
        currentModelName = file.name

        if (isGguf) {
            try {
                // Initialize high-performance llama.cpp for compact GGUF models
                val contextSize = if (file.name.contains("gemma", ignoreCase = true)) 2048 else 2048
                val config = LlamaConfig(
                    contextSize = contextSize,
                    threads = threads,
                    gpuLayers = 0,
                    temperature = temperature,
                    topP = 0.9f,
                    topK = 40,
                    seed = 42
                )
                llamaModel = Llama.loadModel(file.absolutePath, config)
                currentEngineType = EngineType.LLAMA_CPP
                isInitialized = true
                val label = if (file.name.contains("gemma", ignoreCase = true)) "Gemma-2 GGUF" else "SmolLM2 GGUF"
                Result.success("$label (${file.name})")
            } catch (e: Exception) {
                isInitialized = false
                llamaModel = null
                Result.failure(Exception("llama.cpp load error: ${e.localizedMessage ?: e.message}", e))
            }
        } else {
            try {
                // Initialize MediaPipe Tasks GenAI for .bin / .task models
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(file.absolutePath)
                    .setMaxTokens(maxTokens)
                    .setTemperature(temperature)
                    .setTopK(40)
                    .setRandomSeed(42)
                    .build()

                mediaPipeInference = LlmInference.createFromOptions(context, options)
                currentEngineType = EngineType.MEDIAPIPE
                isInitialized = true
                Result.success("MediaPipe (${file.name})")
            } catch (e: Exception) {
                isInitialized = false
                mediaPipeInference = null
                val msg = e.localizedMessage ?: e.message ?: "Unknown error"
                val detailedMsg = if (msg.contains("OpenCL", ignoreCase = true) || msg.contains("clSetPerfHintQCOM", ignoreCase = true)) {
                    "MediaPipe GPU OpenCL failed ($msg).\n\nYour device GPU (e.g. MediaTek Mali or unsupported OpenCL driver) does not support Qualcomm OpenCL extensions. Switch to a CPU GGUF model like SmolLM2-360M or Gemma-2-2B GGUF."
                } else {
                    "MediaPipe load error: $msg"
                }
                Result.failure(Exception(detailedMsg, e))
            }
        }
    }

    /**
     * Formats the prompt with model-specific Chat Templates (Gemma-2, SmolLM2 ChatML)
     * so that the model doesn't terminate prematurely or hallucinate empty turns.
     */
    private fun formatPrompt(rawPrompt: String, systemInstruction: String): Pair<String, String> {
        val lowerName = currentModelName.lowercase()
        return when {
            lowerName.contains("gemma") -> {
                // Gemma 2 Turn Format
                val gemmaPrompt = "<start_of_turn>user\n$systemInstruction\n\n$rawPrompt<end_of_turn>\n<start_of_turn>model\n"
                Pair(gemmaPrompt, "")
            }
            lowerName.contains("smollm") || lowerName.contains("qwen") -> {
                // ChatML Format
                val chatMlPrompt = "<|im_start|>system\n$systemInstruction<|im_end|>\n<|im_start|>user\n$rawPrompt<|im_end|>\n<|im_start|>assistant\n"
                Pair(chatMlPrompt, "")
            }
            else -> {
                Pair(rawPrompt, systemInstruction)
            }
        }
    }

    suspend fun generateText(prompt: String, maxTokens: Int = 1024): String = withContext(Dispatchers.Default) {
        val (text, _) = generateTextWithStats(prompt, maxTokens)
        return@withContext text
    }

    suspend fun generateTextWithStats(
        prompt: String,
        maxTokens: Int = 1024,
        systemPrompt: String = "You are Physics Buddy, an insightful, concise, and mathematically rigorous physics AI mentor. Provide complete explanations and code/derivations without cutting off."
    ): Pair<String, String> = withContext(Dispatchers.Default) {
        if (!isInitialized) return@withContext Pair("Engine not initialized yet. Please load a model first.", "Error: Not ready")

        return@withContext when (currentEngineType) {
            EngineType.LLAMA_CPP -> {
                val model = llamaModel ?: return@withContext Pair("Llama model not loaded.", "Error")
                try {
                    val (formattedPrompt, sys) = formatPrompt(prompt, systemPrompt)
                    val result = Llama.complete(
                        model = model,
                        prompt = formattedPrompt,
                        systemPrompt = sys,
                        maxTokens = maxTokens
                    )
                    var cleanedText = result.text.trim()
                    // Strip trailing stop tokens if any
                    cleanedText = cleanedText
                        .replace("<end_of_turn>", "")
                        .replace("<|im_end|>", "")
                        .replace("<|endoftext|>", "")
                        .trim()

                    val speed = String.format("%.1f tok/s", result.tokensPerSecond)
                    val modelName = if (currentModelName.contains("gemma", ignoreCase = true)) "Gemma-2 2B" else "SmolLM2"
                    val stats = "$modelName • ${result.tokensGenerated} tokens • $speed • ${result.generateTimeMs}ms"
                    Pair(cleanedText, stats)
                } catch (e: Exception) {
                    Pair("Inference failed: ${e.localizedMessage ?: e.message}", "Error")
                }
            }
            EngineType.MEDIAPIPE -> {
                val inference = mediaPipeInference ?: return@withContext Pair("MediaPipe engine not loaded.", "Error")
                try {
                    val startTime = System.currentTimeMillis()
                    val response = inference.generateResponse(prompt)
                    val elapsedMs = System.currentTimeMillis() - startTime
                    val elapsedSec = elapsedMs / 1000.0

                    val approxTokens = (response.length / 4.0).toInt().coerceAtLeast(1)
                    val speed = if (elapsedSec > 0) String.format("%.1f tok/s", approxTokens / elapsedSec) else "-- tok/s"
                    val stats = "MediaPipe • ~$approxTokens tokens • $speed • ${elapsedMs}ms"
                    Pair(response, stats)
                } catch (e: Exception) {
                    Pair("Inference failed: ${e.localizedMessage ?: e.message}", "Error")
                }
            }
            null -> Pair("Engine not initialized.", "Error")
        }
    }

    /**
     * Generates an analytical Physics Buddy hypothesis/critique for a community post.
     */
    suspend fun generatePhysicsHypothesis(title: String, content: String): Pair<String, String> = withContext(Dispatchers.Default) {
        val prompt = "Analyze this physics discussion:\n\nTitle: $title\nTopic: $content\n\nProvide a concise 2-3 paragraph theoretical hypothesis, citing key governing equations or physics principles."
        val sysPrompt = "You are Physics Buddy AI, a theoretical physicist providing peer-review hypotheses on physics community posts."
        return@withContext generateTextWithStats(prompt, maxTokens = 512, systemPrompt = sysPrompt)
    }

    fun close() {
        llamaModel?.let {
            try {
                Llama.releaseModel(it)
            } catch (_: Exception) {}
        }
        llamaModel = null

        mediaPipeInference?.let {
            try {
                it.close()
            } catch (_: Exception) {}
        }
        mediaPipeInference = null

        isInitialized = false
        currentEngineType = null
    }
}
