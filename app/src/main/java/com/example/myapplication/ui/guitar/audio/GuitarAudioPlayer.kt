package com.example.myapplication.ui.guitar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.myapplication.ui.guitar.chord.ChordNote
import com.example.myapplication.ui.guitar.chord.Tuning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.pow
import kotlin.random.Random

class GuitarAudioPlayer(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 44100

    private var soundPool: SoundPool = createSoundPool()
    private val soundIds = IntArray(Tuning.STRING_COUNT) { -1 }
    private var currentTuning: Tuning? = null
    private var currentAudioSettings: AudioSettings? = null

    init {
        initialize(Tuning.Standard, AudioSettings())
    }

    fun initialize(tuning: Tuning, audioSettings: AudioSettings = AudioSettings()) {
        if (currentTuning == tuning && currentAudioSettings == audioSettings) return
        currentTuning = tuning
        currentAudioSettings = audioSettings

        releaseSamples()

        scope.launch {
            tuning.frequencies.forEachIndexed { index, freq ->
                val file = createWavFile(
                    context = context,
                    frequency = freq,
                    audioSettings = audioSettings,
                    fileName = "guitar_string_${tuning.name}_${audioSettings.hashCode()}_$index.wav"
                )
                soundIds[index] = soundPool.load(file.absolutePath, 1)
            }
        }
    }

    fun playString(stringIndex: Int, fret: Int) {
        val soundId = soundIds.getOrNull(stringIndex) ?: return
        if (soundId <= 0) return
        val rate = 2.0.pow(fret / 12.0).toFloat()
        soundPool.play(soundId, 1f, 1f, 1, 0, rate)
    }

    fun playChord(chordNotes: List<ChordNote>) {
        // 显式从 6 弦（stringIndex=5）到 1 弦（stringIndex=0）依次拨出
        val active = (5 downTo 0).mapNotNull { stringIndex ->
            val fret = chordNotes[stringIndex].fretOrNull() ?: return@mapNotNull null
            stringIndex to fret
        }
        if (active.isEmpty()) return

        scope.launch {
            active.forEachIndexed { i, (string, fret) ->
                // 从 6 弦到 1 弦，每根弦间隔 60ms 依次拨出
                if (i > 0) delay(60L)
                playString(string, fret)
            }
        }
    }

    fun release() {
        scope.cancel()
        releaseSamples()
        soundPool.release()
    }

    private fun releaseSamples() {
        soundIds.forEachIndexed { index, soundId ->
            if (soundId > 0) {
                soundPool.unload(soundId)
                soundIds[index] = -1
            }
        }
    }

    private fun createSoundPool(): SoundPool {
        return SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()
    }

    private fun createWavFile(
        context: Context,
        frequency: Double,
        audioSettings: AudioSettings,
        fileName: String
    ): File {
        val pcm = generatePluck(frequency, 2500, audioSettings)
        val wavBytes = makeWav(pcm, sampleRate)
        val file = File(context.cacheDir, fileName)
        file.writeBytes(wavBytes)
        return file
    }

    private fun generatePluck(
        frequency: Double,
        durationMs: Int,
        audioSettings: AudioSettings
    ): ShortArray {
        val n = (sampleRate / frequency).toInt().coerceAtLeast(2)
        val totalSamples = sampleRate * durationMs / 1000
        val output = ShortArray(totalSamples)

        val decay = audioSettings.decay.toDouble()

        // 基础厚度控制
        val baseMix = 0.25 + audioSettings.thickness * 0.35
        // 频率相关：低频更浑厚，中高频更明亮，避免 3/4 弦过小
        val frequencyFactor = (1.0 - (frequency / 400.0).coerceIn(0.0, 1.0))
        val mix = baseMix * (0.35 + 0.65 * frequencyFactor)

        // 初始激励：白噪声 + 快速指数衰减窗
        val buffer = DoubleArray(n) { index ->
            val noise = Random.nextDouble() * 2.0 - 1.0
            val pluckEnvelope = kotlin.math.exp(-index * 6.0 / n).coerceAtLeast(0.02)
            noise * pluckEnvelope
        }

        var ptr = Random.nextInt(n)

        repeat(totalSamples) { i ->
            val current = buffer[ptr]
            val next = buffer[(ptr + 1) % n]
            buffer[ptr] = (current * (1.0 - mix) + next * mix) * decay
            ptr = (ptr + 1) % n

            output[i] = (current * 28000).toInt().toShort()
        }

        return output
    }

    private fun makeWav(pcm: ShortArray, sampleRate: Int): ByteArray {
        val dataSize = pcm.size * 2
        val headerSize = 44
        val out = ByteArray(headerSize + dataSize)

        fun writeString(offset: Int, value: String) {
            value.toByteArray().copyInto(out, offset)
        }

        fun writeInt(offset: Int, value: Int) {
            out[offset] = (value and 0xFF).toByte()
            out[offset + 1] = ((value shr 8) and 0xFF).toByte()
            out[offset + 2] = ((value shr 16) and 0xFF).toByte()
            out[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }

        fun writeShort(offset: Int, value: Int) {
            out[offset] = (value and 0xFF).toByte()
            out[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }

        writeString(0, "RIFF")
        writeInt(4, 36 + dataSize)
        writeString(8, "WAVE")
        writeString(12, "fmt ")
        writeInt(16, 16)
        writeShort(20, 1)
        writeShort(22, 1)
        writeInt(24, sampleRate)
        writeInt(28, sampleRate * 2)
        writeShort(32, 2)
        writeShort(34, 16)
        writeString(36, "data")
        writeInt(40, dataSize)

        pcm.forEachIndexed { index, sample ->
            val i = headerSize + index * 2
            out[i] = (sample.toInt() and 0xFF).toByte()
            out[i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }

        return out
    }
}
