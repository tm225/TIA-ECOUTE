@file:OptIn(ExperimentalFoundationApi::class)

package com.tia.ecoute.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tia.ecoute.MainViewModel
import com.tia.ecoute.data.ArtLoader
import com.tia.ecoute.data.Track
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

fun fmt(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Apparition en fondu + glissement, légèrement décalée selon la position dans la liste. */
fun Modifier.appear(index: Int): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceIn(0, 12) * 35L)
        a.animateTo(1f, tween(380))
    }
    graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 36f
    }
}

fun Modifier.glass(shape: Shape = RoundedCornerShape(20.dp)): Modifier =
    this.clip(shape)
        .background(Color.White.copy(alpha = 0.07f))
        .border(1.dp, Color.White.copy(alpha = 0.10f), shape)

fun shareTrack(ctx: Context, t: Track) {
    try {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, t.uri)
            putExtra(Intent.EXTRA_SUBJECT, t.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(i, "Partager « ${t.title} »"))
    } catch (_: Exception) {
    }
}

fun hsvColor(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))

fun fallbackBrush(id: Long): Brush {
    var h = ((id * 47L) % 360L).toFloat()
    if (h < 0f) h += 360f
    return Brush.linearGradient(
        listOf(hsvColor(h, 0.65f, 0.60f), hsvColor((h + 55f) % 360f, 0.75f, 0.40f))
    )
}

// ===================================================================== Pochette

@Composable
fun TrackArt(track: Track?, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(14.dp)) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(track?.let { ArtLoader.peek(it) }, track?.id) {
        value = track?.let { ArtLoader.peek(it) ?: ArtLoader.load(ctx, it) }
    }
    val fallback = remember(track?.id) { fallbackBrush(track?.id ?: 0L) }
    Box(modifier.clip(shape).background(fallback), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            val img = remember(b) { b.asImageBitmap() }
            Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(
                Icons.Rounded.MusicNote, null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxSize(0.45f)
            )
        }
    }
}

@Composable
fun rememberAccent(track: Track?): State<Color> {
    val ctx = LocalContext.current
    val target = produceState(Violet, track?.id) {
        val b = track?.let { ArtLoader.peek(it) ?: ArtLoader.load(ctx, it) }
        value = if (b != null) ArtLoader.accent(b, Violet) else Violet
    }
    return animateColorAsState(target.value, tween(900), label = "accent")
}

// ===================================================================== Animations

@Composable
fun PlayingBars(modifier: Modifier = Modifier, color: Color = Cyan, active: Boolean = true) {
    val inf = rememberInfiniteTransition(label = "bars")
    val a by inf.animateFloat(0.25f, 1f, infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by inf.animateFloat(0.30f, 1f, infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val c by inf.animateFloat(0.20f, 1f, infiniteRepeatable(tween(640, easing = LinearEasing), RepeatMode.Reverse), label = "c")
    androidx.compose.foundation.Canvas(modifier.size(18.dp)) {
        val w = size.width / 5f
        listOf(a, b, c).forEachIndexed { i, v ->
            val h = size.height * (if (active) v else 0.25f)
            drawRoundRect(color, Offset(i * 2f * w, size.height - h), Size(w, h), CornerRadius(w / 2f))
        }
    }
}

/** Spectre animé (bandes néon qui dansent au rythme de la lecture). */
@Composable
fun SpectrumBars(playing: Boolean, modifier: Modifier = Modifier, bars: Int = 36, energy: () -> Float = { 0.5f }) {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            var last = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                time += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }
    val amp by androidx.compose.animation.core.animateFloatAsState(
        if (playing) 1f else 0.07f, tween(700), label = "amp"
    )
    Canvas(modifier) {
        val gap = 4.dp.toPx()
        val bw = (size.width - gap * (bars - 1)) / bars
        val en = 0.55f + 0.9f * energy()
        for (i in 0 until bars) {
            val f = 1.4f + (i % 7) * 0.55f
            val wave = abs(sin(time * f + i * 0.85f))
            val mod = 0.6f + 0.4f * sin(time * 0.9f + i * 0.4f)
            val h = (size.height * (0.12f + 0.88f * wave * mod) * amp * en).coerceAtLeast(3.dp.toPx())
            val col = lerp(Magenta, Cyan, i / (bars - 1f))
            drawRoundRect(col, Offset(i * (bw + gap), size.height - h), Size(bw, h), CornerRadius(bw / 2f))
        }
    }
}

@Composable
fun AnimatedBackdrop(accent: Color, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "backdrop")
    val t1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "t1")
    val t2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse), label = "t2")
    val t3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(23000, easing = LinearEasing), RepeatMode.Reverse), label = "t3")
    Canvas(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyLight, Navy, Color(0xFF05020F))))
    ) {
        val w = size.width
        val h = size.height
        val c1 = Offset(w * (0.15f + 0.7f * t1), h * (0.10f + 0.25f * t2))
        val c2 = Offset(w * (0.85f - 0.6f * t2), h * (0.55f + 0.25f * t3))
        val c3 = Offset(w * (0.2f + 0.5f * t3), h * (0.85f - 0.2f * t1))
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.38f), Color.Transparent), c1, w * 0.95f), w * 0.95f, c1)
        drawCircle(Brush.radialGradient(listOf(Blue.copy(alpha = 0.26f), Color.Transparent), c2, w * 0.85f), w * 0.85f, c2)
        drawCircle(Brush.radialGradient(listOf(Magenta.copy(alpha = 0.20f), Color.Transparent), c3, w * 0.8f), w * 0.8f, c3)
    }
}

// ===================================================================== Boutons

@Composable
fun GradientButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    brush: Brush = BrandBrush,
    onClick: () -> Unit
) {
    Row(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(brush)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        Arrangement.Center,
        Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun GlassButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(48.dp)
            .glass(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        Arrangement.Center,
        Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .then(if (selected) Modifier.background(BrandBrush) else Modifier.background(Color.White.copy(alpha = 0.08f)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else TextLo,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
fun BigPlayButton(playing: Boolean, size: Int = 78, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "pp")
    val pulse by inf.animateFloat(1f, 1.08f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    Box(
        Modifier
            .size(size.dp)
            .graphicsLayer {
                val v = if (playing) pulse else 1f
                scaleX = v
                scaleY = v
            }
            .shadow(18.dp, CircleShape, ambientColor = Magenta, spotColor = Magenta)
            .clip(CircleShape)
            .background(BrandBrush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(playing, label = "playicon") { p ->
            Icon(
                if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (p) "Pause" else "Lecture",
                tint = Color.White,
                modifier = Modifier.size((size * 0.55f).dp)
            )
        }
    }
}

/** Barre de progression en dégradé néon, tactile (tap + glisser). */
@Composable
fun GradientSeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seek by rememberUpdatedState(onSeek)
    val done by rememberUpdatedState(onSeekFinished)
    Box(
        modifier
            .height(30.dp)
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    seek((off.x / size.width).coerceIn(0f, 1f))
                    done()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { done() },
                    onDragCancel = { done() }
                ) { change, _ ->
                    change.consume()
                    seek((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trackH = 6.dp.toPx()
            val cy = size.height / 2f
            val p = progress.coerceIn(0f, 1f)
            drawRoundRect(
                Color.White.copy(alpha = 0.16f),
                Offset(0f, cy - trackH / 2f), Size(size.width, trackH), CornerRadius(trackH / 2f)
            )
            val w = size.width * p
            if (w > 0f) {
                drawRoundRect(
                    Brush.horizontalGradient(listOf(Magenta, Violet, Cyan), 0f, size.width),
                    Offset(0f, cy - trackH / 2f), Size(w, trackH), CornerRadius(trackH / 2f)
                )
            }
            drawCircle(Magenta.copy(alpha = 0.35f), 13.dp.toPx(), Offset(w, cy))
            drawCircle(Color.White, 8.dp.toPx(), Offset(w, cy))
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, onMore: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextHi
        )
        if (onMore != null) {
            Text(
                "Voir tout",
                color = Cyan,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onMore).padding(8.dp)
            )
        }
    }
}

// ===================================================================== Ligne de titre

@Composable
fun TrackRow(
    vm: MainViewModel,
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val isCurrent = vm.currentId == track.id
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArt(track, Modifier.size(54.dp), RoundedCornerShape(14.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (isCurrent) Cyan else TextHi
            )
            Text(
                "${track.artist} • ${track.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = TextLo
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isCurrent) {
            PlayingBars(active = vm.isPlaying)
        } else {
            Text(fmt(track.durationMs), color = TextLo, fontSize = 12.sp)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, "Plus d'options", tint = TextLo)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Lire ensuite") },
                    leadingIcon = { Icon(Icons.Rounded.SkipNext, null) },
                    onClick = { menu = false; vm.playNext(track) }
                )
                DropdownMenuItem(
                    text = { Text("Ajouter à la file d'attente") },
                    leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) },
                    onClick = { menu = false; vm.enqueue(track) }
                )
                DropdownMenuItem(
                    text = { Text("Ajouter à une playlist") },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                    onClick = { menu = false; vm.playlistTarget = track }
                )
                val fav = vm.isFavorite(track.id)
                DropdownMenuItem(
                    text = { Text(if (fav) "Retirer des favoris" else "Ajouter aux favoris") },
                    leadingIcon = {
                        Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null)
                    },
                    onClick = { menu = false; vm.toggleFavorite(track.id) }
                )
                DropdownMenuItem(
                    text = { Text("Partager") },
                    leadingIcon = { Icon(Icons.Rounded.Share, null) },
                    onClick = { menu = false; shareTrack(ctx, track) }
                )
                DropdownMenuItem(
                    text = { Text("Masquer ce fichier") },
                    leadingIcon = { Icon(Icons.Rounded.VisibilityOff, null) },
                    onClick = { menu = false; vm.hideTrack(track.id) }
                )
                if (onRemove != null) {
                    DropdownMenuItem(
                        text = { Text("Retirer de la playlist") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = { menu = false; onRemove() }
                    )
                }
            }
        }
    }
}
