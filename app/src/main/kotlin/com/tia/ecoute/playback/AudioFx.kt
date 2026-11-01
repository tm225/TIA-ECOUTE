package com.tia.ecoute.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import kotlin.math.sqrt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tia.ecoute.data.Prefs

/** Égaliseur 5 bandes (selon l'appareil), Bass Boost et Virtualizer. */
class AudioFx(private val prefs: Prefs) {
    var supported by mutableStateOf(false)
        private set
    var enabled by mutableStateOf(prefs.eqEnabled)
        private set
    var levels by mutableStateOf<List<Int>>(emptyList())
        private set
    var freqsHz by mutableStateOf<List<Int>>(emptyList())
        private set
    var presets by mutableStateOf<List<String>>(emptyList())
        private set
    var minLevel by mutableIntStateOf(-1500)
        private set
    var maxLevel by mutableIntStateOf(1500)
        private set
    var bass by mutableIntStateOf(prefs.bass)
        private set
    var virt by mutableIntStateOf(prefs.virt)
        private set
    var bassSupported by mutableStateOf(false)
        private set
    var virtSupported by mutableStateOf(false)
        private set

    var boost by mutableIntStateOf(prefs.boost)
        private set
    var boostSupported by mutableStateOf(false)
        private set
    private var loud: LoudnessEnhancer? = null
    var liveWanted = false
    private var viz: Visualizer? = null
    private var wave = ByteArray(0)
    private var eq: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var session = 0

    fun attach(sessionId: Int) {
        if (sessionId == 0 || sessionId == session) return
        release()
        session = sessionId
        try {
            val e = Equalizer(0, sessionId)
            eq = e
            val n = e.numberOfBands.toInt()
            val range = e.bandLevelRange
            minLevel = range[0].toInt()
            maxLevel = range[1].toInt()
            freqsHz = (0 until n).map { e.getCenterFreq(it.toShort()) / 1000 }
            presets = (0 until e.numberOfPresets.toInt()).map { e.getPresetName(it.toShort()) }
            val saved = prefs.eqLevels()
            levels = if (saved.size == n) saved else (0 until n).map { e.getBandLevel(it.toShort()).toInt() }
            levels.forEachIndexed { i, l -> e.setBandLevel(i.toShort(), l.toShort()) }
            e.setEnabled(enabled)
            supported = true
        } catch (_: Throwable) {
            supported = false
        }
        try {
            val l = LoudnessEnhancer(sessionId)
            loud = l
            l.setTargetGain(boost * 15)
            l.setEnabled(boost > 0)
            boostSupported = true
        } catch (_: Throwable) {
            boostSupported = false
        }
        if (liveWanted) startLive()
        try {
            val b = BassBoost(0, sessionId)
            bassBoost = b
            bassSupported = b.strengthSupported
            if (bassSupported) {
                b.setStrength(bass.toShort())
                b.setEnabled(enabled && bass > 0)
            }
        } catch (_: Throwable) {
            bassSupported = false
        }
        try {
            val v = Virtualizer(0, sessionId)
            virtualizer = v
            virtSupported = v.strengthSupported
            if (virtSupported) {
                v.setStrength(virt.toShort())
                v.setEnabled(enabled && virt > 0)
            }
        } catch (_: Throwable) {
            virtSupported = false
        }
    }

    /** Visualiseur réel (nécessite la permission micro ; seul le niveau sonore est lu, rien n'est enregistré). */
    fun startLive(): Boolean {
        stopLive()
        if (session == 0) return false
        return try {
            val v = Visualizer(session)
            v.setCaptureSize(Visualizer.getCaptureSizeRange()[0])
            wave = ByteArray(v.captureSize)
            v.setEnabled(true)
            viz = v
            true
        } catch (_: Throwable) {
            viz = null
            false
        }
    }

    fun stopLive() {
        try { viz?.setEnabled(false) } catch (_: Throwable) {}
        try { viz?.release() } catch (_: Throwable) {}
        viz = null
    }

    /** Niveau sonore instantané 0..1. */
    fun energy(): Float {
        val v = viz ?: return 0f
        return try {
            v.getWaveForm(wave)
            var sum = 0f
            for (b in wave) {
                val x = ((b.toInt() and 0xFF) - 128) / 128f
                sum += x * x
            }
            (sqrt(sum / wave.size) * 3.4f).coerceIn(0f, 1f)
        } catch (_: Throwable) {
            0f
        }
    }

    fun release() {
        stopLive()
        try { loud?.release() } catch (_: Throwable) {}
        loud = null
        try { eq?.release() } catch (_: Throwable) {}
        try { bassBoost?.release() } catch (_: Throwable) {}
        try { virtualizer?.release() } catch (_: Throwable) {}
        eq = null
        bassBoost = null
        virtualizer = null
        session = 0
    }

    fun setEnabled(on: Boolean) {
        enabled = on
        prefs.eqEnabled = on
        try { eq?.setEnabled(on) } catch (_: Throwable) {}
        try { bassBoost?.setEnabled(on && bass > 0) } catch (_: Throwable) {}
        try { virtualizer?.setEnabled(on && virt > 0) } catch (_: Throwable) {}
    }

    fun setLevel(band: Int, level: Int) {
        val l = level.coerceIn(minLevel, maxLevel)
        levels = levels.toMutableList().also { if (band in it.indices) it[band] = l }
        try { eq?.setBandLevel(band.toShort(), l.toShort()) } catch (_: Throwable) {}
        prefs.saveEqLevels(levels)
    }

    fun usePreset(index: Int) {
        val e = eq ?: return
        try {
            e.usePreset(index.toShort())
            levels = (0 until levels.size).map { e.getBandLevel(it.toShort()).toInt() }
            prefs.saveEqLevels(levels)
            if (!enabled) setEnabled(true)
        } catch (_: Throwable) {}
    }

    /** Amplification du volume : 0 à 100 % (jusqu'à +15 dB). */
    fun setBoost(percent: Int) {
        boost = percent.coerceIn(0, 100)
        prefs.boost = boost
        try {
            loud?.setTargetGain(boost * 15)
            loud?.setEnabled(boost > 0)
        } catch (_: Throwable) {}
    }

    fun reset() {
        levels.indices.forEach { setLevel(it, 0) }
        setBass(0)
        setVirt(0)
    }

    fun setBass(v: Int) {
        bass = v.coerceIn(0, 1000)
        prefs.bass = bass
        try {
            bassBoost?.setStrength(bass.toShort())
            bassBoost?.setEnabled(enabled && bass > 0)
        } catch (_: Throwable) {}
    }

    fun setVirt(v: Int) {
        virt = v.coerceIn(0, 1000)
        prefs.virt = virt
        try {
            virtualizer?.setStrength(virt.toShort())
            virtualizer?.setEnabled(enabled && virt > 0)
        } catch (_: Throwable) {}
    }
}
