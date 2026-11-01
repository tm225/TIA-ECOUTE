@file:OptIn(ExperimentalMaterial3Api::class)

package com.tia.ecoute.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tia.ecoute.MainViewModel
import com.tia.ecoute.R
import com.tia.ecoute.data.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val dialogBg = Color(0xFF1B1040)

// ===================================================================== FILE D'ATTENTE

@Composable
fun QueueSheet(vm: MainViewModel) {
    ModalBottomSheet(
        onDismissRequest = { vm.showQueue = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                "File d'attente (${vm.queueIds.size})",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
            )
            val q = vm.queueTracks
            if (q.isEmpty()) {
                Text("La file est vide.", color = TextLo, modifier = Modifier.padding(22.dp))
            } else {
                LazyColumn(Modifier.heightIn(max = 560.dp)) {
                    itemsIndexed(q, key = { i, t -> "$i-${t.id}" }) { i, t ->
                        val cur = i == vm.queueIndex
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.skipTo(i) }
                                .background(if (cur) Color.White.copy(alpha = 0.07f) else Color.Transparent)
                                .padding(start = 22.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackArt(t, Modifier.size(46.dp), RoundedCornerShape(12.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.title, color = if (cur) Cyan else Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(t.artist, color = TextLo, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (cur) PlayingBars(Modifier.padding(horizontal = 6.dp), Cyan, vm.isPlaying)
                            IconButton(onClick = { vm.removeFromQueue(i) }) {
                                Icon(Icons.Rounded.Close, "Retirer", tint = TextLo)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================================== ÉGALISEUR

private fun freqLabel(hz: Int): String = if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz"

@Composable
fun EqSheet(vm: MainViewModel) {
    val fx = vm.fx
    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Magenta,
        inactiveTrackColor = Color.White.copy(alpha = 0.18f)
    )
    ModalBottomSheet(
        onDismissRequest = { vm.showEq = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Equalizer, null, tint = Cyan)
                Spacer(Modifier.width(10.dp))
                Text("Égaliseur", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = fx.enabled,
                    onCheckedChange = { fx.setEnabled(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Violet, checkedThumbColor = Color.White)
                )
            }
            if (!fx.supported) {
                Text(
                    "L'égaliseur devient disponible dès qu'un titre est en lecture. Lancez une musique puis rouvrez cet écran.",
                    color = TextLo, fontSize = 14.sp, modifier = Modifier.padding(vertical = 18.dp)
                )
            } else {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fx.presets.forEachIndexed { i, name -> Pill(name, false, { fx.usePreset(i) }) }
                }
                fx.levels.forEachIndexed { i, lvl ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(freqLabel(fx.freqsHz.getOrElse(i) { 0 }), color = TextLo, fontSize = 12.sp, modifier = Modifier.width(62.dp))
                        Slider(
                            value = lvl.toFloat(),
                            onValueChange = { fx.setLevel(i, it.toInt()) },
                            valueRange = fx.minLevel.toFloat()..fx.maxLevel.toFloat(),
                            colors = sliderColors,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${lvl / 100} dB", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                    }
                }
            }
            if (fx.bassSupported) {
                Spacer(Modifier.height(10.dp))
                Text("Bass boost", color = Color.White, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = fx.bass.toFloat(),
                    onValueChange = { fx.setBass(it.toInt()) },
                    valueRange = 0f..1000f,
                    colors = sliderColors
                )
            }
            if (fx.virtSupported) {
                Text("Spatialisation (Virtualizer)", color = Color.White, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = fx.virt.toFloat(),
                    onValueChange = { fx.setVirt(it.toInt()) },
                    valueRange = 0f..1000f,
                    colors = sliderColors
                )
            }
            if (fx.supported) {
                TextButton(onClick = { fx.reset() }) { Text("Réinitialiser", color = Cyan) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ===================================================================== DIALOGUES

@Composable
fun SleepDialog(vm: MainViewModel) {
    AlertDialog(
        onDismissRequest = { vm.showSleep = false },
        containerColor = dialogBg,
        title = { Text("Minuteur de veille", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (vm.sleepRemaining > 0) {
                    Text("Arrêt dans ${fmt(vm.sleepRemaining * 1000L)}", color = Cyan, modifier = Modifier.padding(bottom = 10.dp))
                }
                listOf(5, 10, 15, 30, 45, 60, 90).chunked(3).forEach { row ->
                    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { m ->
                            Pill("$m min", false, { vm.startSleep(m); vm.showSleep = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (vm.sleepRemaining > 0) {
                TextButton(onClick = { vm.startSleep(0); vm.showSleep = false }) { Text("Annuler le minuteur", color = Orange) }
            }
        },
        dismissButton = { TextButton(onClick = { vm.showSleep = false }) { Text("Fermer", color = TextLo) } }
    )
}

@Composable
fun SpeedDialog(vm: MainViewModel) {
    AlertDialog(
        onDismissRequest = { vm.showSpeed = false },
        containerColor = dialogBg,
        title = { Text("Vitesse de lecture", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                listOf(listOf(0.5f, 0.75f, 1f), listOf(1.25f, 1.5f, 2f)).forEach { row ->
                    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { s ->
                            Pill("${s}x", vm.speed == s, { vm.setSpeed(s); vm.showSpeed = false })
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { vm.showSpeed = false }) { Text("Fermer", color = TextLo) } }
    )
}

@Composable
fun TextPromptDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedIndicatorColor = Magenta,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Magenta,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(confirmLabel, color = Cyan, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = TextLo) } }
    )
}

@Composable
fun NewPlaylistDialog(vm: MainViewModel) {
    TextPromptDialog(
        title = "Nouvelle playlist",
        initial = "",
        confirmLabel = "Créer",
        onDismiss = { vm.showNewPlaylist = false },
        onConfirm = { vm.createPlaylist(it); vm.showNewPlaylist = false }
    )
}

@Composable
fun PlaylistPickerDialog(vm: MainViewModel, track: Track) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        TextPromptDialog(
            title = "Nouvelle playlist",
            initial = "",
            confirmLabel = "Créer et ajouter",
            onDismiss = { creating = false },
            onConfirm = { vm.createPlaylist(it, track.id); creating = false; vm.playlistTarget = null }
        )
        return
    }
    AlertDialog(
        onDismissRequest = { vm.playlistTarget = null },
        containerColor = dialogBg,
        title = { Text("Ajouter à une playlist", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { creating = true }
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Magenta)
                    Spacer(Modifier.width(12.dp))
                    Text("Nouvelle playlist…", color = Magenta, fontWeight = FontWeight.Bold)
                }
                vm.playlists.forEach { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { vm.addToPlaylist(p.id, track.id); vm.playlistTarget = null }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlaylistAdd, null, tint = Cyan)
                        Spacer(Modifier.width(12.dp))
                        Text(p.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { vm.playlistTarget = null }) { Text("Fermer", color = TextLo) } }
    )
}

// ===================================================================== PERMISSION

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "perm")
    val s by inf.animateFloat(1f, 1.07f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "s")
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(R.drawable.tia_logo), null,
            Modifier.size(170.dp).graphicsLayer { scaleX = s; scaleY = s }
        )
        Spacer(Modifier.height(24.dp))
        Text("TIA ECOUTE", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Text(
            "Pour afficher votre musique, TIA ECOUTE a besoin d'accéder aux fichiers audio de votre téléphone. Aucune donnée n'est envoyée sur Internet.",
            color = TextLo, textAlign = TextAlign.Center, fontSize = 15.sp
        )
        Spacer(Modifier.height(26.dp))
        GradientButton("Autoriser l'accès", Icons.Rounded.MusicNote, onClick = onRequest)
    }
}

// ===================================================================== SPLASH ANIMÉ

@Composable
fun Splash(onDone: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val overall = remember { Animatable(1f) }
    val reveal = remember { Animatable(0f) }
    val inf = rememberInfiniteTransition(label = "splash")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "spin")
    val name = "TIA ECOUTE"

    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
        launch { logoAlpha.animateTo(1f, tween(600)) }
        delay(500)
        reveal.animateTo(name.length.toFloat(), tween(900, easing = LinearEasing))
        delay(900)
        overall.animateTo(0f, tween(450))
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overall.value }
            .background(Color(0xFF080419))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(260.dp).graphicsLayer { rotationZ = spin; alpha = logoAlpha.value }) {
                    drawCircle(
                        Brush.sweepGradient(listOf(Magenta, Blue, Cyan, Orange, Yellow, Magenta)),
                        size.minDimension / 2f - 4.dp.toPx(),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
                Image(
                    painterResource(R.drawable.tia_logo), null,
                    Modifier
                        .size(190.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            alpha = logoAlpha.value
                        }
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                name.take(reveal.value.toInt()),
                color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Votre musique, avec de l'énergie",
                color = TextLo, fontSize = 14.sp,
                modifier = Modifier.graphicsLayer { alpha = (reveal.value / name.length) }
            )
        }
    }
}
