@file:OptIn(ExperimentalMaterial3Api::class)

package com.tia.ecoute.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Widgets
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tia.ecoute.MainViewModel
import com.tia.ecoute.R
import com.tia.ecoute.playback.TiaWidget

@Composable
fun MenuPageHost(vm: MainViewModel, page: String?) {
    when (page) {
        "themes" -> ThemesPage(vm)
        "widgets" -> WidgetsPage(vm)
        "settings" -> SettingsPage(vm)
        "hidden" -> HiddenPage(vm)
        "playback" -> PlaybackSettingsPage(vm)
        "about" -> AboutPage(vm)
        else -> Box(Modifier.fillMaxSize().background(Navy))
    }
}

@Composable
private fun PageFrame(vm: MainViewModel, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Navy).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.page = null }) { Icon(Icons.Rounded.ArrowBack, "Retour", tint = Color.White) }
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            content = content
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).glass(RoundedCornerShape(18.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextLo, fontSize = 12.sp)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Violet, checkedThumbColor = Color.White))
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

// ------------------------------------------------------------------ Thèmes
@Composable
fun ThemesPage(vm: MainViewModel) {
    PageFrame(vm, "Thèmes") {
        Palettes.all.forEachIndexed { i, p ->
            val sel = ThemeState.index == i
            val shape = RoundedCornerShape(20.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(shape)
                    .background(p.navyLight)
                    .border(if (sel) 2.dp else 1.dp, if (sel) p.cyan else Color.White.copy(alpha = 0.1f), shape)
                    .clickable { vm.setTheme(i) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Row(Modifier.padding(top = 10.dp)) {
                        listOf(p.magenta, p.violet, p.blue, p.cyan, p.orange).forEach {
                            Box(Modifier.padding(end = 8.dp).size(24.dp).clip(CircleShape).background(it))
                        }
                    }
                }
                if (sel) Icon(Icons.Rounded.Check, null, tint = p.cyan)
            }
        }
    }
}

// ------------------------------------------------------------------ Widgets
@Composable
fun WidgetsPage(vm: MainViewModel) {
    val ctx = LocalContext.current
    PageFrame(vm, "Widgets") {
        Column(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.Widgets, null, tint = Magenta, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(10.dp))
            Text("Widget TIA ECOUTE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Affiche le titre en cours et les boutons précédent, lecture/pause et suivant directement sur l'écran d'accueil.",
                color = TextLo, textAlign = TextAlign.Center, fontSize = 14.sp, modifier = Modifier.padding(vertical = 10.dp)
            )
            GradientButton("Ajouter à l'écran d'accueil", Icons.Rounded.Widgets) {
                try {
                    val mgr = AppWidgetManager.getInstance(ctx)
                    if (mgr.isRequestPinAppWidgetSupported) {
                        mgr.requestPinAppWidget(ComponentName(ctx, TiaWidget::class.java), null, null)
                    }
                } catch (_: Exception) {
                }
            }
        }
        Text(
            "Si votre lanceur ne propose pas l'ajout direct : appui long sur l'écran d'accueil → Widgets → TIA ECOUTE. Lancez d'abord un titre une fois pour que les boutons soient actifs.",
            color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp)
        )
    }
}

// ------------------------------------------------------------------ Amplificateur de volume
@Composable
fun BoostSheet(vm: MainViewModel) {
    val fx = vm.fx
    ModalBottomSheet(
        onDismissRequest = { vm.showBoost = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeUp, null, tint = Cyan)
                Spacer(Modifier.width(10.dp))
                Text("Amplificateur de volume", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            if (!fx.boostSupported) {
                Text(
                    "Disponible dès qu'un titre est en lecture. Lancez une musique puis rouvrez cet écran.",
                    color = TextLo, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Text("${fx.boost} %", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 12.dp))
                Slider(
                    value = fx.boost.toFloat(),
                    onValueChange = { fx.setBoost(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Magenta, inactiveTrackColor = Color.White.copy(alpha = 0.18f))
                )
                Text(
                    "Un niveau élevé peut déformer le son et fatiguer l'oreille : montez progressivement.",
                    color = Orange, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Paramètres
@Composable
fun SettingsPage(vm: MainViewModel) {
    PageFrame(vm, "Paramètres") {
        Label("BIBLIOTHÈQUE")
        Text("Durée minimale des titres", color = Color.White, fontWeight = FontWeight.SemiBold)
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 15, 30, 60).forEach { s ->
                Pill(if (s == 0) "Tous" else "$s s", vm.minDurationSec == s, { vm.setMinDuration(s) })
            }
        }
        Text("Masque les notes vocales et les sons très courts.", color = TextLo, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        GlassButton("Actualiser la bibliothèque (${vm.tracks.size} titres)", Icons.Rounded.Refresh) { vm.refresh() }
        Label("AFFICHAGE")
        SwitchRow("Garder l'écran allumé", "Pendant la lecture", vm.keepScreenOn) { vm.setKeepScreenOn(it) }
        Label("HISTORIQUE")
        GlassButton("Effacer l'historique d'écoute", Icons.Rounded.Close) { vm.clearHistory() }
    }
}

// ------------------------------------------------------------------ Fichiers et dossiers masqués
@Composable
fun HiddenPage(vm: MainViewModel) {
    PageFrame(vm, "Fichiers et dossiers masqués") {
        Label("DOSSIERS (activé = masqué)")
        if (vm.allFolders.isEmpty()) Text("Aucun dossier détecté.", color = TextLo, fontSize = 14.sp)
        vm.allFolders.forEach { (name, count) ->
            SwitchRow(name, "$count titre(s)", name in vm.hiddenFolders) { vm.setFolderHidden(name, it) }
        }
        Label("FICHIERS MASQUÉS")
        if (vm.hiddenTracks.isEmpty()) {
            Text("Aucun fichier masqué. Utilisez ⋮ → « Masquer ce fichier » sur un titre.", color = TextLo, fontSize = 14.sp)
        }
        vm.hiddenTracks.forEach { t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, color = TextLo, fontSize = 12.sp, maxLines = 1)
                }
                TextButton(onClick = { vm.unhideTrack(t.id) }) { Text("Afficher", color = Cyan) }
            }
        }
    }
}

// ------------------------------------------------------------------ Paramètres de lecture
@Composable
fun PlaybackSettingsPage(vm: MainViewModel) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> vm.setLive(ok) }
    PageFrame(vm, "Paramètres de lecture") {
        Label("VITESSE")
        Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.75f, 1f, 1.25f, 1.5f).forEach { s -> Pill("${s}x", vm.speed == s, { vm.setSpeed(s) }) }
        }
        Label("LECTURE")
        SwitchRow("Reprendre au démarrage", "Retrouve le dernier titre et la position", vm.resumeOnStart) { vm.setResumeOnStart(it) }
        SwitchRow("Passer les silences", "Saute les blancs dans les enregistrements", vm.skipSilence) { vm.setSkipSilence(it) }
        SwitchRow("Pause si le casque est débranché", "Évite le son sur le haut-parleur", vm.pauseOnUnplug) { vm.setPauseOnUnplug(it) }
        SwitchRow("Disque synchronisé sur le son réel", "Demande l'accès « micro » : seul le niveau sonore est lu, rien n'est enregistré", vm.liveEnabled) { on ->
            if (!on) {
                vm.setLive(false)
            } else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                vm.setLive(true)
            } else {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        Label("DESIGN DU LECTEUR")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton("Choisir le design du disque", Icons.Rounded.VolumeUp, Modifier.weight(1f)) { vm.showStyles = true }
        }
        Label("RACCOURCIS")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton("Égaliseur", Icons.Rounded.VolumeUp, Modifier.weight(1f)) { vm.showEq = true }
            GlassButton("Volume +", Icons.Rounded.VolumeUp, Modifier.weight(1f)) { vm.showBoost = true }
        }
    }
}

// ------------------------------------------------------------------ À propos
@Composable
fun AboutPage(vm: MainViewModel) {
    PageFrame(vm, "À propos") {
        Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.tia_logo), null, Modifier.size(130.dp).clip(RoundedCornerShape(30.dp)))
            Spacer(Modifier.height(12.dp))
            Text("TIA ECOUTE", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(brush = BrandBrush, fontWeight = FontWeight.Black))
            Text("Version 1.0.0", color = TextLo, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                "Lecteur de musique moderne et animé : bibliothèque, playlists, égaliseur, amplificateur de volume, minuterie de sommeil, mode conduite, widgets et thèmes.",
                color = TextHi, textAlign = TextAlign.Center, fontSize = 14.sp
            )
            Spacer(Modifier.height(10.dp))
            Text("Fonctionne hors ligne. Aucune donnée n'est envoyée sur Internet.", color = TextLo, textAlign = TextAlign.Center, fontSize = 12.sp)
        }
    }
}

// ------------------------------------------------------------------ Mode conduite
@Composable
fun DriveScreen(vm: MainViewModel) {
    val t = vm.current
    Column(
        Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("MODE CONDUITE", color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.driveMode = false }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Close, "Quitter", tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(t?.title ?: "Aucun titre", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(t?.artist ?: "", color = TextLo, fontSize = 22.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(18.dp))
        GradientSeekBar(
            progress = vm.positionMs.toFloat() / maxOf(1L, vm.durationMs).toFloat(),
            onSeek = {}, onSeekFinished = {}, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            DriveButton(Icons.Rounded.SkipPrevious, 110) { vm.previous() }
            BigPlayButton(vm.isPlaying, 150) { vm.togglePlay() }
            DriveButton(Icons.Rounded.SkipNext, 110) { vm.next() }
        }
        Spacer(Modifier.height(26.dp))
        val fav = t != null && vm.isFavorite(t.id)
        Row(
            Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.09f))
                .clickable { t?.let { vm.toggleFavorite(it.id) } },
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (fav) Magenta else Color.White, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Text(if (fav) "Dans vos favoris" else "Ajouter aux favoris", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DriveButton(icon: androidx.compose.ui.graphics.vector.ImageVector, size: Int, onClick: () -> Unit) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = 0.10f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size((size * 0.6f).dp)) }
}
