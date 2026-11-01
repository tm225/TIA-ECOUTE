@file:OptIn(ExperimentalFoundationApi::class)

package com.tia.ecoute.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.tia.ecoute.MainViewModel
import com.tia.ecoute.data.Track
import kotlin.math.max
import kotlin.math.min

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (active) Cyan else Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = if (active) Cyan else TextLo, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun NowPlayingScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val track = vm.current
    val accent by rememberAccent(track)
    val energy = rememberEnergy(vm, track?.id ?: 0L)
    var drag by remember { mutableStateOf<Float?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragX by remember { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Navy)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragY = 0f },
                    onDragEnd = { if (dragY > 220f) vm.nowPlayingOpen = false }
                ) { _, dy -> dragY += dy }
            }
    ) {
        AnimatedBackdrop(accent)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- Barre du haut
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.nowPlayingOpen = false }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Réduire", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("EN LECTURE", color = TextLo, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(
                        track?.album ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                IconButton(onClick = { vm.showQueue = true }) {
                    Icon(Icons.Rounded.QueueMusic, "File d'attente", tint = Color.White)
                }
            }

            // ---- Disque (balayer à gauche/droite = titre suivant/précédent)
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragX = 0f },
                            onDragEnd = {
                                if (dragX < -130f) vm.next() else if (dragX > 130f) vm.previous()
                            }
                        ) { _, dx -> dragX += dx }
                    },
                contentAlignment = Alignment.Center
            ) {
                val side = min(maxWidth.value * 0.88f, maxHeight.value).dp
                DiscView(vm, track, accent, energy, Modifier.size(side))
            }

            // ---- Spectre
            SpectrumBars(vm.isPlaying, Modifier.fillMaxWidth().height(40.dp), energy = { energy.floatValue })
            Spacer(Modifier.height(14.dp))

            // ---- Titre / artiste / favori
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track?.title ?: "Aucun titre",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        track?.artist ?: "",
                        color = TextLo,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val fav = track != null && vm.isFavorite(track.id)
                IconButton(onClick = { track?.let { vm.toggleFavorite(it.id) } }) {
                    Icon(
                        if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "Favori",
                        tint = if (fav) Magenta else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // ---- Progression
            val dur = max(1L, vm.durationMs)
            val frac = drag ?: (vm.positionMs.toFloat() / dur.toFloat())
            GradientSeekBar(
                progress = frac,
                onSeek = { drag = it },
                onSeekFinished = {
                    drag?.let { vm.seekFraction(it) }
                    drag = null
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Row(Modifier.fillMaxWidth()) {
                Text(fmt(drag?.let { (dur * it).toLong() } ?: vm.positionMs), color = TextLo, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(fmt(vm.durationMs), color = TextLo, fontSize = 12.sp)
            }

            // ---- Commandes principales
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(Icons.Rounded.Shuffle, "Aléatoire", tint = if (vm.shuffle) Cyan else Color.White.copy(alpha = 0.8f), modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = { vm.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Précédent", tint = Color.White, modifier = Modifier.size(42.dp))
                }
                BigPlayButton(vm.isPlaying) { vm.togglePlay() }
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Suivant", tint = Color.White, modifier = Modifier.size(42.dp))
                }
                IconButton(onClick = { vm.cycleRepeat() }) {
                    val on = vm.repeatMode != Player.REPEAT_MODE_OFF
                    Icon(
                        if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Répéter",
                        tint = if (on) Cyan else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // ---- Outils
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ToolButton(
                    Icons.Rounded.Timer,
                    if (vm.sleepRemaining > 0) fmt(vm.sleepRemaining * 1000L) else "Minuteur",
                    vm.sleepRemaining > 0
                ) { vm.showSleep = true }
                ToolButton(
                    Icons.Rounded.Speed,
                    "${"%.2f".format(java.util.Locale.US, vm.speed).trimEnd('0').trimEnd('.')}x",
                    vm.speed != 1f
                ) { vm.showSpeed = true }
                ToolButton(Icons.Rounded.Equalizer, "Égaliseur", vm.fx.enabled) { vm.showEq = true }
                ToolButton(Icons.Rounded.Album, "Design", false) { vm.showStyles = true }
                ToolButton(Icons.Rounded.Share, "Partager", false) { track?.let { shareTrack(ctx, it) } }
            }
        }
    }
}

/** Mini-lecteur flottant au-dessus de la barre de navigation. */
@Composable
fun MiniPlayer(vm: MainViewModel, modifier: Modifier = Modifier) {
    val track = vm.current ?: return
    var dragX by remember { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .glass(shape)
            .background(Color(0xFF1C0F45).copy(alpha = 0.65f))
            .clickable { vm.nowPlayingOpen = true }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onDragEnd = { if (dragX < -90f) vm.next() else if (dragX > 90f) vm.previous() }
                ) { _, dx -> dragX += dx }
            }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArt(track, Modifier.size(46.dp), RoundedCornerShape(13.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = TextLo, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (vm.isPlaying) PlayingBars(Modifier.padding(horizontal = 6.dp), Magenta)
            IconButton(onClick = { vm.previous() }) {
                Icon(Icons.Rounded.SkipPrevious, "Précédent", tint = Color.White)
            }
            BigPlayButton(vm.isPlaying, 44) { vm.togglePlay() }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Rounded.SkipNext, "Suivant", tint = Color.White)
            }
        }
        // fine barre de progression en bas
        Canvas(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp)) {
            val f = vm.positionMs.toFloat() / max(1L, vm.durationMs).toFloat()
            drawRect(Color.White.copy(alpha = 0.10f))
            drawRect(Brush.horizontalGradient(listOf(Magenta, Cyan), 0f, size.width), size = androidx.compose.ui.geometry.Size(size.width * f.coerceIn(0f, 1f), size.height))
        }
    }
}
