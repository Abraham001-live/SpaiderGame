package com.example.game.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * Procedural low-latency Audio Engine generating realistic insect, predator, and survival sounds.
 */
class GameAudioEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var isMuted = false
    private val sampleRate = 22050

    private val cachedBuffers = ConcurrentHashMap<String, ShortArray>()

    init {
        // Pre-generate standard sound buffers asynchronously
        scope.launch {
            cachedBuffers["spit"] = generateSpitSound()
            cachedBuffers["web"] = generateWebSound()
            cachedBuffers["eat"] = generateEatSound()
            cachedBuffers["frog"] = generateFrogCroakSound()
            cachedBuffers["jump"] = generateJumpSound()
            cachedBuffers["hit"] = generateHitSound()
            cachedBuffers["heartbeat"] = generateHeartbeatSound()
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun playSpit() = playCached("spit")
    fun playWeb() = playCached("web")
    fun playEat() = playCached("eat")
    fun playFrog() = playCached("frog")
    fun playJump() = playCached("jump")
    fun playHit() = playCached("hit")
    fun playHeartbeat() = playCached("heartbeat")

    private fun playCached(key: String) {
        if (isMuted) return
        scope.launch {
            val buffer = cachedBuffers[key] ?: when (key) {
                "spit" -> generateSpitSound().also { cachedBuffers[key] = it }
                "web" -> generateWebSound().also { cachedBuffers[key] = it }
                "eat" -> generateEatSound().also { cachedBuffers[key] = it }
                "frog" -> generateFrogCroakSound().also { cachedBuffers[key] = it }
                "jump" -> generateJumpSound().also { cachedBuffers[key] = it }
                "hit" -> generateHitSound().also { cachedBuffers[key] = it }
                "heartbeat" -> generateHeartbeatSound().also { cachedBuffers[key] = it }
                else -> return@launch
            }
            playSoundBuffer(buffer)
        }
    }

    private fun playSoundBuffer(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            
            // Release after playback
            scope.launch {
                val durationMs = (buffer.size.toFloat() / sampleRate * 1000).toLong() + 50
                kotlinx.coroutines.delay(durationMs)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // Graceful fallback if device audio cannot allocate
        }
    }

    // Procedural sound generators
    private fun generateSpitSound(): ShortArray {
        val numSamples = (sampleRate * 0.22f).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / numSamples
            val freq = 800f - progress * 450f
            val env = (1f - progress).pow(1.5f)
            val noise = (Math.random().toFloat() * 2f - 1f) * 0.4f
            val sine = sin(2 * Math.PI * freq * t).toFloat() * 0.6f
            val sample = (sine + noise) * env * 24000f
            buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateWebSound(): ShortArray {
        val numSamples = (sampleRate * 0.35f).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / numSamples
            val freq = 1200f - progress * 700f
            val env = sin(progress * Math.PI.toFloat())
            val noise = (Math.random().toFloat() * 2f - 1f) * 0.7f
            val sample = (sin(2 * Math.PI * freq * t).toFloat() * 0.3f + noise) * env * 22000f
            buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateEatSound(): ShortArray {
        val numSamples = (sampleRate * 0.4f).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val env = (sin(progress * Math.PI.toFloat() * 4f).absoluteValue) * (1f - progress)
            val noise = (Math.random().toFloat() * 2f - 1f)
            val crunch = if (i % 300 < 100) noise else noise * 0.3f
            val sample = crunch * env * 28000f
            buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateFrogCroakSound(): ShortArray {
        val numSamples = (sampleRate * 0.55f).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / numSamples
            val freq = 110f + sin(progress * 25f) * 35f
            val env = sin(progress * Math.PI.toFloat()).pow(0.8f)
            val harmonic1 = sin(2 * Math.PI * freq * t).toFloat()
            val harmonic2 = sin(2 * Math.PI * (freq * 2.3f) * t).toFloat() * 0.6f
            val sample = (harmonic1 + harmonic2) * env * 29000f
            buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateJumpSound(): ShortArray {
        val numSamples = (sampleRate * 0.25f).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / numSamples
            val freq = 150f + progress * 400f
            val env = (1f - progress).pow(1.2f)
            val sample = sin(2 * Math.PI * freq * t).toFloat() * env * 22000f
            buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateHitSound(): ShortArray {
        val numSamples = (sampleRate * 0.28f).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / numSamples
            val freq = 90f - progress * 40f
            val env = (1f - progress).pow(2f)
            val noise = (Math.random().toFloat() * 2f - 1f) * 0.5f
            val sample = (sin(2 * Math.PI * freq * t).toFloat() * 0.7f + noise) * env * 30000f
            buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateHeartbeatSound(): ShortArray {
        val numSamples = (sampleRate * 0.35f).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / numSamples
            val freq = 55f
            val env = sin(progress * Math.PI.toFloat()).pow(3f)
            val sample = sin(2 * Math.PI * freq * t).toFloat() * env * 26000f
            buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }
}
