@file:OptIn(ExperimentalMaterial3Api::class)

package com.tia.ecoute.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tia.ecoute.MainViewModel
import com.tia.ecoute.data.Track
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

val discStyleNames = listOf("Vinyle réaliste", "Platine", "CD", "Cassette", "Pochette & vinyle", "Néon radial")
val discStyleEmoji = listOf("🎵", "🎛", "💿", "📼", "🗂", "✨")

private const val TAU = 6.2831855f

// ===================================================================== Horloges & énergie

/** Énergie musicale 0..1 : son réel (si activé) sinon pulsation calée sur un tempo propre à chaque titre. */
@Composable
fun rememberEnergy(vm: MainViewModel, key: Long): FloatState {
    val e = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(vm.isPlaying, key) {
        if (!vm.isPlaying) {
            e.floatValue = 0f
            return@LaunchedEffect
        }
        val bpm = 72f + (abs(key.hashCode()) % 68)
        var t = 0f
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            t += dt
            val target = if (vm.liveEnabled && vm.liveEnergy > 0.01f) {
                vm.liveEnergy
            } else {
                val phase = (t * bpm / 60f) % 1f
                val kick = (1f - phase) * (1f - phase) * (1f - phase)
                0.22f + 0.55f * kick + 0.1f * abs(sin(t * 3.1f))
            }
            e.floatValue += (target - e.floatValue) * (1f - exp(-dt * 14f))
        }
    }
    return e
}

/** Angle de rotation avec inertie : le plateau accélère au démarrage et ralentit à l'arrêt. 200°/s = 33 tours/min. */
@Composable
fun rememberSpin(playing: Boolean, degPerSec: Float = 200f): FloatState {
    val angle = remember { mutableFloatStateOf(0f) }
    val vel = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            val target = if (playing) degPerSec else 0f
            vel.floatValue += (target - vel.floatValue) * (1f - exp(-dt * 2.2f))
            angle.floatValue = (angle.floatValue + vel.floatValue * dt) % 360f
            if (!playing && vel.floatValue < 0.4f) {
                vel.floatValue = 0f
                break
            }
        }
    }
    return angle
}

@Composable
fun rememberClock(playing: Boolean): FloatState {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            var last = withFrameNanos { it }
            while (true) {
                val n = withFrameNanos { it }
                t.floatValue += (n - last) / 1_000_000_000f
                last = n
            }
        }
    }
    return t
}

// ===================================================================== Vinyle réaliste

private val sheenBrush = Brush.sweepGradient(
    0.00f to Color.Transparent, 0.05f to Color.White.copy(alpha = 0.24f), 0.12f to Color.Transparent,
    0.50f to Color.Transparent, 0.55f to Color.White.copy(alpha = 0.24f), 0.62f to Color.Transparent,
    1.00f to Color.Transparent
)

@Composable
fun VinylCore(
    track: Track?,
    accent: Color,
    angle: FloatState,
    energy: FloatState,
    modifier: Modifier = Modifier,
    glow: Boolean = true
) {
    Box(
        modifier.graphicsLayer {
            val s = 1f + 0.016f * energy.floatValue
            scaleX = s
            scaleY = s
        },
        contentAlignment = Alignment.Center
    ) {
        if (glow) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(Color.Black.copy(alpha = 0.55f), r, Offset(center.x, center.y + r * 0.05f))
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.18f + 0.45f * energy.floatValue), Color.Transparent), center, r * 1.3f),
                    r * 1.3f
                )
            }
        }
        // Partie qui tourne : galette noire, sillons, intervalles entre pistes, étiquette
        Box(
            Modifier.fillMaxSize().graphicsLayer { rotationZ = angle.floatValue },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(Brush.radialGradient(listOf(Color(0xFF1A1A20), Color(0xFF070709)), center, r), r)
                var k = 0
                var rr = r * 0.975f
                while (rr > r * 0.40f) {
                    drawCircle(Color.White.copy(alpha = 0.02f + 0.05f * (((k * 37) % 11) / 10f)), rr, style = Stroke(0.7f))
                    rr -= r * 0.0105f
                    k++
                }
                listOf(0.74f, 0.60f, 0.49f).forEach {
                    drawCircle(Color.Black.copy(alpha = 0.6f), r * it, style = Stroke(r * 0.014f))
                }
                drawCircle(Color.White.copy(alpha = 0.10f), r * 0.985f, style = Stroke(r * 0.015f))
                drawCircle(
                    Brush.radialGradient(listOf(accent, lerp(accent, Color.Black, 0.45f)), center, r * 0.4f),
                    r * 0.38f
                )
                drawCircle(Color.White.copy(alpha = 0.25f), r * 0.38f, style = Stroke(r * 0.01f))
            }
            TrackArt(track, Modifier.fillMaxSize(0.32f), CircleShape)
            Box(Modifier.fillMaxSize(0.035f).clip(CircleShape).background(Color(0xFF050507)))
        }
        // Reflets de lumière : fixes, ils ne tournent pas avec le disque (c'est ce qui donne le réalisme)
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            rotate(-38f) { drawCircle(sheenBrush, r) }
        }
    }
}

// ===================================================================== Platine avec bras

private fun armTip(p: Offset, c: Offset, l: Float, r: Float): Offset {
    val dx = c.x - p.x
    val dy = c.y - p.y
    val d = sqrt(dx * dx + dy * dy)
    val ux = dx / d
    val uy = dy / d
    val a = (l * l - r * r + d * d) / (2f * d)
    val h = sqrt(max(0f, l * l - a * a))
    val p1 = Offset(p.x + a * ux - h * uy, p.y + a * uy + h * ux)
    val p2 = Offset(p.x + a * ux + h * uy, p.y + a * uy - h * ux)
    return if (p1.y > p2.y) p1 else p2
}

@Composable
fun TurntableStyle(vm: MainViewModel, track: Track?, accent: Color, energy: FloatState, modifier: Modifier) {
    val spin = rememberSpin(vm.isPlaying)
    val lowered by animateFloatAsState(if (vm.isPlaying) 1f else 0f, tween(1100), label = "arm")
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val s = maxWidth
        Box(
            Modifier
                .size(s)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2E2833), Color(0xFF14111A))))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val m = size.minDimension
                val c = Offset(m * 0.46f, m * 0.54f)
                drawCircle(Color(0xFF3B3944), m * 0.43f, c)
                drawCircle(Color(0xFF8D93A3), m * 0.43f, c, style = Stroke(m * 0.006f))
                drawCircle(if (vm.isPlaying) Color(0xFF39FF88) else Color(0xFF5A2B2B), m * 0.012f, Offset(m * 0.07f, m * 0.07f))
            }
            VinylCore(track, accent, spin, energy, Modifier.offset(s * 0.06f, s * 0.14f).size(s * 0.80f), glow = false)
            Canvas(Modifier.fillMaxSize()) {
                val m = size.minDimension
                val p = Offset(m * 0.90f, m * 0.10f)
                val c = Offset(m * 0.46f, m * 0.54f)
                val d = (c - p).getDistance()
                val rOuter = m * 0.40f
                val prog = (vm.positionMs.toFloat() / max(1L, vm.durationMs).toFloat()).coerceIn(0f, 1f)
                val groove = rOuter * (0.95f - 0.52f * prog)
                val rest = rOuter * 1.22f
                val q = armTip(p, c, d, rest + (groove - rest) * lowered)
                val dir = q - p
                val len = dir.getDistance()
                val u = Offset(dir.x / len, dir.y / len)
                drawLine(Color.Black.copy(alpha = 0.35f), p + Offset(m * 0.012f, m * 0.016f), q + Offset(m * 0.012f, m * 0.016f), m * 0.016f, StrokeCap.Round)
                drawLine(Color(0xFF8A8F9C), p, p - u * (m * 0.10f), m * 0.05f, StrokeCap.Round)
                drawLine(Color(0xFFD9DCE4), p, q, m * 0.014f, StrokeCap.Round)
                drawLine(Color(0xFF2A2D36), q - u * (m * 0.055f), q, m * 0.032f, StrokeCap.Butt)
                drawCircle(Color(0xFFFFB347), m * 0.008f, q)
                drawCircle(Color(0xFF3A3D48), m * 0.055f, p)
                drawCircle(Color(0xFFB9BDC9), m * 0.03f, p)
                drawCircle(Color.White.copy(alpha = 0.4f), m * 0.012f, p)
            }
        }
    }
}

// ===================================================================== CD

private val rainbowSheen = Brush.sweepGradient(
    0.00f to Color.Transparent, 0.04f to Color(0x6619C2FF), 0.08f to Color(0x66E23CFF), 0.12f to Color(0x66FFD321),
    0.17f to Color.Transparent, 0.50f to Color.Transparent, 0.54f to Color(0x66FFD321), 0.58f to Color(0x6619C2FF),
    0.63f to Color(0x66E23CFF), 0.68f to Color.Transparent, 1.00f to Color.Transparent
)

@Composable
fun CdStyle(vm: MainViewModel, track: Track?, accent: Color, energy: FloatState, modifier: Modifier) {
    val spin = rememberSpin(vm.isPlaying, 420f)
    Box(
        modifier.graphicsLayer {
            val s = 1f + 0.02f * energy.floatValue
            scaleX = s
            scaleY = s
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.15f + 0.5f * energy.floatValue), Color.Transparent), center, r * 1.3f), r * 1.3f)
        }
        Box(Modifier.fillMaxSize(0.94f).graphicsLayer { rotationZ = spin.floatValue }, contentAlignment = Alignment.Center) {
            TrackArt(track, Modifier.fillMaxSize(), CircleShape)
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(Brush.radialGradient(listOf(Color(0xFFF4F6FA), Color(0xFFB9BEC9)), center, r * 0.3f), r * 0.30f)
                drawCircle(Color(0xFF9AA0AE), r * 0.30f, style = Stroke(r * 0.012f))
                drawCircle(Color(0xFF9AA0AE), r * 0.34f, style = Stroke(r * 0.006f))
                drawCircle(Navy, r * 0.075f)
            }
        }
        Canvas(Modifier.fillMaxSize(0.94f)) {
            val r = size.minDimension / 2f
            rotate(-30f) { drawCircle(rainbowSheen, r) }
            drawCircle(Color.White.copy(alpha = 0.55f), r, style = Stroke(2f))
        }
    }
}

// ===================================================================== Cassette

@Composable
fun CassetteStyle(vm: MainViewModel, track: Track?, accent: Color, energy: FloatState, modifier: Modifier) {
    val spin = rememberSpin(vm.isPlaying, 260f)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val w = maxWidth
        val h = w * 0.64f
        val shape = RoundedCornerShape(18.dp)
        Box(
            Modifier
                .size(w, h)
                .graphicsLayer {
                    val s = 1f + 0.015f * energy.floatValue
                    scaleX = s
                    scaleY = s
                }
                .shadow(18.dp, shape, ambientColor = accent, spotColor = accent)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFF2E2A38), Color(0xFF15121C))))
                .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
        ) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = h * 0.07f)
                    .fillMaxWidth(0.9f)
                    .height(h * 0.42f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF3ECDD))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(Modifier.fillMaxWidth().height(5.dp).background(accent))
                Text(track?.title ?: "TIA ECOUTE", color = Color(0xFF241B2F), fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track?.artist ?: "", color = Color(0xFF5B5068), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Canvas(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = h * 0.06f)
                    .fillMaxWidth(0.86f)
                    .height(h * 0.42f)
            ) {
                val ww = size.width
                val hh = size.height
                drawRoundRect(Color(0xFF0C0A12), Offset.Zero, size, CornerRadius(hh * 0.5f))
                val prog = (vm.positionMs.toFloat() / max(1L, vm.durationMs).toFloat()).coerceIn(0f, 1f)
                val maxR = hh * 0.46f
                val cl = Offset(ww * 0.27f, hh * 0.5f)
                val cr = Offset(ww * 0.73f, hh * 0.5f)
                val rl = maxR * (0.95f - 0.5f * prog)
                val rr = maxR * (0.45f + 0.5f * prog)
                drawCircle(Color(0xFF3A2B24), rl, cl)
                drawCircle(Color(0xFF3A2B24), rr, cr)
                drawLine(Color(0xFF3A2B24), Offset(cl.x, cl.y + rl), Offset(cr.x, cr.y + rr), 3f)
                listOf(cl, cr).forEach { ctr ->
                    drawCircle(Color(0xFFE8E8EE), hh * 0.2f, ctr)
                    rotate(spin.floatValue, ctr) {
                        for (i in 0 until 6) {
                            val a = i * 1.0472f
                            drawLine(Color(0xFF15121C), ctr, ctr + Offset(cos(a) * hh * 0.2f, sin(a) * hh * 0.2f), 3f)
                        }
                    }
                    drawCircle(Color(0xFF15121C), hh * 0.06f, ctr)
                }
            }
        }
    }
}

// ===================================================================== Pochette + vinyle qui sort

@Composable
fun SleeveStyle(vm: MainViewModel, track: Track?, accent: Color, energy: FloatState, modifier: Modifier) {
    val spin = rememberSpin(vm.isPlaying)
    val out by animateFloatAsState(if (vm.isPlaying) 1f else 0.16f, tween(900), label = "sleeve")
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val s = maxWidth * 0.74f
        val shape = RoundedCornerShape(10.dp)
        VinylCore(track, accent, spin, energy, Modifier.size(s).offset((maxWidth - s) * out, 0.dp), glow = false)
        Box(
            Modifier
                .size(s)
                .graphicsLayer { rotationZ = (energy.floatValue - 0.4f) * 1.6f }
                .shadow(18.dp, shape, ambientColor = accent, spotColor = accent)
        ) {
            TrackArt(track, Modifier.fillMaxSize(), shape)
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent, Color.Black.copy(alpha = 0.15f))))
            )
        }
    }
}

// ===================================================================== Néon radial

@Composable
fun NeonStyle(vm: MainViewModel, track: Track?, accent: Color, energy: FloatState, modifier: Modifier) {
    val spin = rememberSpin(vm.isPlaying, 36f)
    val time = rememberClock(vm.isPlaying)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val inner = r * 0.60f
            val n = 72
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.20f + 0.4f * energy.floatValue), Color.Transparent), center, r), r)
            for (i in 0 until n) {
                val a = (i.toFloat() / n) * TAU + spin.floatValue * 0.0174533f
                val wave = abs(sin(time.floatValue * (1.2f + (i % 5) * 0.45f) + i * 0.7f))
                val len = r * 0.05f + r * 0.30f * wave * (0.30f + 0.95f * energy.floatValue)
                val col = lerp(Magenta, Cyan, (sin(a) + 1f) / 2f)
                drawLine(
                    col,
                    center + Offset(cos(a) * inner, sin(a) * inner),
                    center + Offset(cos(a) * (inner + len), sin(a) * (inner + len)),
                    r * 0.03f, StrokeCap.Round
                )
            }
            drawCircle(accent.copy(alpha = 0.8f), inner * 1.03f, style = Stroke(3f))
        }
        TrackArt(track, Modifier.fillMaxSize(0.55f).graphicsLayer { rotationZ = spin.floatValue * 0.5f }, CircleShape)
    }
}

// ===================================================================== Sélecteur

@Composable
fun DiscView(vm: MainViewModel, track: Track?, accent: Color, energy: FloatState, modifier: Modifier = Modifier) {
    when (vm.discStyle) {
        1 -> TurntableStyle(vm, track, accent, energy, modifier)
        2 -> CdStyle(vm, track, accent, energy, modifier)
        3 -> CassetteStyle(vm, track, accent, energy, modifier)
        4 -> SleeveStyle(vm, track, accent, energy, modifier)
        5 -> NeonStyle(vm, track, accent, energy, modifier)
        else -> VinylCore(track, accent, rememberSpin(vm.isPlaying), energy, modifier)
    }
}

@Composable
fun DiscStyleSheet(vm: MainViewModel) {
    ModalBottomSheet(
        onDismissRequest = { vm.showStyles = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 6.dp)) {
            Text("Design du lecteur", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
            discStyleNames.indices.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { i ->
                        val sel = vm.discStyle == i
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (sel) BrandBrush else SolidColor(Color.White.copy(alpha = 0.08f)))
                                .clickable { vm.setDiscStyle(i); vm.showStyles = false }
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(discStyleEmoji[i], fontSize = 34.sp)
                            Text(discStyleNames[i], color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
