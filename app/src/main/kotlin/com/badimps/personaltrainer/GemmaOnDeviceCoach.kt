package com.badimps.personaltrainer

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig

/**
 * Optional private wording layer. Facts and calorie ranges remain in OfflineFoodCoach; Gemma only
 * turns that fixed local context into a short, supportive explanation. It is never a diagnosis.
 */
object GemmaOnDeviceCoach {
    private var engine: Engine? = null
    private var activePath = ""

    @JvmStatic
    @Synchronized
    fun installed(context: Context): Boolean = LocalAiModelStore.installed(context)

    @JvmStatic
    @Synchronized
    fun explain(context: Context, prompt: String): String {
        check(LocalAiModelStore.installed(context)) { "Install a local Gemma model first." }
        val path = LocalAiModelStore.file(context).absolutePath
        val loaded = engine ?: Engine(
            EngineConfig(
                modelPath = path,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.absolutePath
            )
        ).also {
            it.initialize()
            engine = it
            activePath = path
        }
        check(activePath == path) { "Restart the app after replacing the local model." }
        return loaded.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(
                    "You are a concise fitness-food coach. Use only facts provided in the user's local summary. " +
                        "Do not diagnose, prescribe treatment, invent calories, recommend restriction, or mention body size. " +
                        "Give one neutral, practical sentence."
                ),
                samplerConfig = SamplerConfig(topK = 12, topP = 0.9, temperature = 0.35),
                maxOutputToken = 90
            )
        ).use { conversation ->
            val response = conversation.sendMessage(prompt)
            val parts: List<Content> = response.contents.contents
            parts.filterIsInstance<Content.Text>()
                .joinToString(separator = "") { it.text }
                .trim()
        }
    }

    @JvmStatic
    @Synchronized
    fun close() {
        engine?.close()
        engine = null
        activePath = ""
    }
}
