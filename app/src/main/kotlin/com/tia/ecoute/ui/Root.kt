package com.tia.ecoute.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tia.ecoute.Detail
import com.tia.ecoute.MainViewModel

private data class NavItem(val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("Accueil", Icons.Rounded.Home),
    NavItem("Bibliothèque", Icons.Rounded.LibraryMusic),
    NavItem("Playlists", Icons.Rounded.PlaylistPlay),
    NavItem("Menu", Icons.Rounded.Menu)
)

@Composable
fun TiaRoot(vm: MainViewModel, requestPermission: () -> Unit) {
    var splashDone by remember { mutableStateOf(false) }
    val accent by rememberAccent(vm.current)

    val view = LocalView.current
    val keepOn = (vm.keepScreenOn && vm.isPlaying) || vm.driveMode
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = vm.detail != null) { vm.detail = null }
    BackHandler(enabled = vm.page != null) { vm.page = null }
    BackHandler(enabled = vm.nowPlayingOpen) { vm.nowPlayingOpen = false }
    BackHandler(enabled = vm.driveMode) { vm.driveMode = false }

    Box(Modifier.fillMaxSize().background(Navy)) {
        AnimatedBackdrop(accent)

        if (!vm.hasPermission) {
            PermissionScreen(requestPermission)
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    Crossfade(targetState = vm.tab, label = "tabs") { t ->
                        when (t) {
                            0 -> HomeScreen(vm)
                            1 -> LibraryScreen(vm)
                            2 -> PlaylistsScreen(vm)
                            else -> MenuScreen(vm)
                        }
                    }
                    DetailOverlay(vm)
                }
                AnimatedVisibility(
                    visible = vm.current != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) { MiniPlayer(vm) }
                BottomBar(vm)
            }

            AnimatedVisibility(
                visible = vm.nowPlayingOpen && vm.current != null,
                enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) { NowPlayingScreen(vm) }

            var lastPage by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(vm.page) { if (vm.page != null) lastPage = vm.page }
            AnimatedVisibility(
                visible = vm.page != null,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut()
            ) { MenuPageHost(vm, vm.page ?: lastPage) }

            AnimatedVisibility(visible = vm.driveMode, enter = fadeIn(), exit = fadeOut()) { DriveScreen(vm) }
        }

        if (!splashDone) Splash { splashDone = true }
    }

    if (vm.showQueue) QueueSheet(vm)
    if (vm.showEq) EqSheet(vm)
    if (vm.showBoost) BoostSheet(vm)
    if (vm.showStyles) DiscStyleSheet(vm)
    if (vm.showSleep) SleepDialog(vm)
    if (vm.showSpeed) SpeedDialog(vm)
    if (vm.showNewPlaylist) NewPlaylistDialog(vm)
    vm.playlistTarget?.let { PlaylistPickerDialog(vm, it) }
}

@Composable
private fun BottomBar(vm: MainViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Navy.copy(alpha = 0.92f))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEachIndexed { i, item ->
            val selected = vm.tab == i
            val s by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = 0.5f), label = "navscale")
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { vm.tab = i; vm.detail = null }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .then(if (selected) Modifier.background(BrandBrush) else Modifier)
                        .padding(horizontal = 18.dp, vertical = 5.dp)
                        .graphicsLayer { scaleX = s; scaleY = s }
                ) {
                    Icon(item.icon, item.label, tint = if (selected) Color.White else TextLo, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    item.label,
                    color = if (selected) Color.White else TextLo,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

/** Écran de détail glissant. Défini à part pour éviter le conflit avec AnimatedVisibility de Column. */
@Composable
private fun DetailOverlay(vm: MainViewModel) {
    var last by remember { mutableStateOf<Detail?>(null) }
    LaunchedEffect(vm.detail) { if (vm.detail != null) last = vm.detail }
    val shown = vm.detail ?: last
    AnimatedVisibility(
        visible = vm.detail != null,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut()
    ) {
        shown?.let { DetailScreen(vm, it) }
    }
}
