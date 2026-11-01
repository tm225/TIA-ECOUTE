@file:OptIn(ExperimentalFoundationApi::class)

package com.tia.ecoute.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tia.ecoute.Detail
import com.tia.ecoute.MainViewModel
import com.tia.ecoute.R
import com.tia.ecoute.data.AlbumInfo
import com.tia.ecoute.data.SortMode
import com.tia.ecoute.data.Track
import java.util.Calendar

// ===================================================================== ACCUEIL

@Composable
fun HomeScreen(vm: MainViewModel) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = if (hour < 5) "Bonne nuit" else if (hour < 18) "Bonjour" else "Bonsoir"

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painterResource(R.drawable.tia_logo), null,
                    Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(greeting, color = TextLo, fontSize = 14.sp)
                    Text(
                        "TIA ECOUTE",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            brush = BrandBrush,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                }
                IconButton(onClick = { vm.tab = 1 }) {
                    Icon(Icons.Rounded.Search, "Rechercher", tint = Color.White)
                }
            }
        }

        if (vm.tracks.isEmpty()) {
            item { EmptyLibrary(vm) }
        } else {
            // ---- Boutons principaux
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).appear(0),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientButton("Tout lire", Icons.Rounded.PlayArrow, Modifier.weight(1f)) {
                        vm.play(vm.tracks.sortedBy { it.title.lowercase() }, 0, false)
                    }
                    GradientButton("Aléatoire", Icons.Rounded.Shuffle, Modifier.weight(1f), WarmBrush) {
                        vm.playShuffled(vm.tracks)
                    }
                }
            }
            // ---- Reprendre
            vm.current?.let { cur ->
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .appear(1)
                            .glass(RoundedCornerShape(24.dp))
                            .clickable { vm.nowPlayingOpen = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackArt(cur, Modifier.size(72.dp), RoundedCornerShape(18.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("REPRENDRE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(cur.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(cur.artist, color = TextLo, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        BigPlayButton(vm.isPlaying, 52) { vm.togglePlay() }
                    }
                }
            }
            // ---- Statistiques
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).appear(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip("${vm.tracks.size}", "titres", Modifier.weight(1f))
                    StatChip("${vm.albums.size}", "albums", Modifier.weight(1f))
                    StatChip("${vm.artists.size}", "artistes", Modifier.weight(1f))
                }
            }
            if (vm.recentlyPlayed.isNotEmpty()) {
                item { SectionHeader("Écoutés récemment") { vm.detail = Detail.RecentPlayed } }
                item { TrackCardsRow(vm, vm.recentlyPlayed.take(15)) }
            }
            if (vm.favoriteTracks.isNotEmpty()) {
                item { SectionHeader("Vos favoris") { vm.detail = Detail.Favorites } }
                item { TrackCardsRow(vm, vm.favoriteTracks.take(15)) }
            }
            if (vm.mostPlayed.isNotEmpty()) {
                item { SectionHeader("Les plus écoutés") { vm.detail = Detail.MostPlayed } }
                item { TrackCardsRow(vm, vm.mostPlayed.take(15)) }
            }
            item { SectionHeader("Ajoutés récemment") { vm.detail = Detail.RecentAdded } }
            item { TrackCardsRow(vm, vm.recentlyAdded.take(15)) }
            if (vm.albums.isNotEmpty()) {
                item { SectionHeader("Albums") { vm.tab = 1 } }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(vm.albums.take(15), key = { "${it.id}-${it.name}" }) { a ->
                            Column(
                                Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { vm.detail = Detail.Album(a.id, a.name) }
                            ) {
                                TrackArt(a.tracks.first(), Modifier.size(140.dp), RoundedCornerShape(22.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(a.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(a.artist, color = TextLo, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.glass(RoundedCornerShape(18.dp)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(brush = WarmBrush, fontWeight = FontWeight.Black))
        Text(label, color = TextLo, fontSize = 12.sp)
    }
}

@Composable
private fun TrackCardsRow(vm: MainViewModel, list: List<Track>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
            Column(
                Modifier
                    .width(140.dp)
                    .appear(i)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { vm.play(list, i) }
            ) {
                TrackArt(t, Modifier.size(140.dp), RoundedCornerShape(22.dp))
                Spacer(Modifier.height(8.dp))
                Text(t.title, color = if (vm.currentId == t.id) Cyan else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, color = TextLo, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EmptyLibrary(vm: MainViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))
        Icon(Icons.Rounded.MusicNote, null, tint = Magenta, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            if (vm.loading) "Analyse de votre musique…" else "Aucune musique trouvée",
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Copiez des fichiers audio sur le téléphone, puis actualisez. Les titres de moins de ${vm.minDurationSec} s sont masqués (réglable dans « Plus »).",
            color = TextLo, textAlign = TextAlign.Center, fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))
        GradientButton("Actualiser", Icons.Rounded.Refresh) { vm.refresh() }
    }
}

// ===================================================================== BIBLIOTHÈQUE

@Composable
fun LibraryScreen(vm: MainViewModel) {
    var sub by remember { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Bibliothèque",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            Box {
                IconButton(onClick = { sortMenu = true }) {
                    Icon(Icons.Rounded.Sort, "Trier", tint = Color.White)
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortMode.values().forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.label, color = if (vm.sort == m) Cyan else Color.Unspecified) },
                            trailingIcon = { if (vm.sort == m) Icon(Icons.Rounded.Check, null, tint = Cyan) },
                            onClick = { vm.sort = m; sortMenu = false; sub = 0 }
                        )
                    }
                }
            }
        }
        // Recherche
        TextField(
            value = vm.query,
            onValueChange = { vm.query = it; if (it.isNotEmpty()) sub = 0 },
            placeholder = { Text("Titre, artiste, album…", color = TextLo) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextLo) },
            trailingIcon = {
                if (vm.query.isNotEmpty()) {
                    IconButton(onClick = { vm.query = "" }) { Icon(Icons.Rounded.Close, "Effacer", tint = TextLo) }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.09f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Magenta,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
        )
        // Onglets
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val names = listOf("Titres", "Albums", "Artistes", "Dossiers")
            items(names.size) { i -> Pill(names[i], sub == i, { sub = i }) }
        }
        Spacer(Modifier.height(8.dp))

        when (sub) {
            0 -> {
                val list = vm.visibleTracks
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (vm.query.isNotEmpty()) "Aucun résultat" else "Aucun titre", color = TextLo)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GradientButton("Lire (${list.size})", Icons.Rounded.PlayArrow, Modifier.weight(1f)) {
                                    vm.play(list, 0, false)
                                }
                                GlassButton("Aléatoire", Icons.Rounded.Shuffle, Modifier.weight(1f)) {
                                    vm.playShuffled(list)
                                }
                            }
                        }
                        itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
                            TrackRow(vm, t, { vm.play(list, i) }, Modifier.appear(i))
                        }
                    }
                }
            }
            1 -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(vm.albums, key = { "${it.id}-${it.name}" }) { a -> AlbumCard(vm, a) }
            }
            2 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                itemsIndexed(vm.artists, key = { _, a -> a.name }) { i, a ->
                    ListRow(
                        art = a.tracks.first(),
                        circle = true,
                        title = a.name,
                        subtitle = "${a.tracks.size} titre(s)",
                        modifier = Modifier.appear(i)
                    ) { vm.detail = Detail.Artist(a.name) }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                itemsIndexed(vm.folders, key = { _, f -> f.name }) { i, f ->
                    ListRow(
                        art = f.tracks.first(),
                        circle = false,
                        title = f.name,
                        subtitle = "${f.tracks.size} titre(s)",
                        modifier = Modifier.appear(i)
                    ) { vm.detail = Detail.Folder(f.name) }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(vm: MainViewModel, a: AlbumInfo) {
    Column(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .glass(RoundedCornerShape(22.dp))
            .clickable { vm.detail = Detail.Album(a.id, a.name) }
            .padding(10.dp)
    ) {
        TrackArt(a.tracks.first(), Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(16.dp))
        Spacer(Modifier.height(8.dp))
        Text(a.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${a.artist} • ${a.tracks.size}", color = TextLo, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ListRow(
    art: Track?,
    circle: Boolean,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArt(art, Modifier.size(56.dp), if (circle) CircleShape else RoundedCornerShape(16.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextLo, fontSize = 13.sp)
        }
    }
}

// ===================================================================== PLAYLISTS

@Composable
fun PlaylistsScreen(vm: MainViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Playlists",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                GradientButton("Nouvelle", Icons.Rounded.Add) { vm.showNewPlaylist = true }
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            SmartCard("Favoris", "${vm.favoriteTracks.size} titre(s)", Icons.Rounded.Favorite, WarmBrush) { vm.detail = Detail.Favorites }
            SmartCard("Les plus écoutés", "Vos titres préférés", Icons.Rounded.TrendingUp, CoolBrush) { vm.detail = Detail.MostPlayed }
            SmartCard("Ajoutés récemment", "Dernières nouveautés", Icons.Rounded.MusicNote, BrandBrush) { vm.detail = Detail.RecentAdded }
            SmartCard("Écoutés récemment", "Votre historique", Icons.Rounded.History, CoolBrush) { vm.detail = Detail.RecentPlayed }
        }
        item { SectionHeader("Mes playlists") }
        if (vm.playlists.isEmpty()) {
            item {
                Text(
                    "Aucune playlist pour l'instant. Créez-en une avec « Nouvelle », ou depuis le menu ⋮ d'un titre.",
                    color = TextLo, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        itemsIndexed(vm.playlists, key = { _, p -> p.id }) { i, p ->
            val tracks = vm.playlistTracks(p)
            ListRow(
                art = tracks.firstOrNull(),
                circle = false,
                title = p.name,
                subtitle = "${tracks.size} titre(s)",
                modifier = Modifier.appear(i)
            ) { vm.detail = Detail.PlaylistD(p.id) }
        }
    }
}

@Composable
private fun SmartCard(title: String, subtitle: String, icon: ImageVector, brush: Brush, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .glass(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(brush),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = TextLo, fontSize = 13.sp)
        }
    }
}

// ===================================================================== MENU

@Composable
fun MenuScreen(vm: MainViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
    ) {
        item {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.tia_logo), null, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Text("Menu", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = Color.White)
            }
            Spacer(Modifier.height(14.dp))
        }
        item {
            SmartCard("Thèmes", "6 ambiances : néon, océan, forêt, AMOLED…", Icons.Rounded.Palette, BrandBrush) { vm.page = "themes" }
            SmartCard("Widgets", "Contrôler la musique depuis l'écran d'accueil", Icons.Rounded.Widgets, CoolBrush) { vm.page = "widgets" }
            SmartCard("Égaliseur", "Préréglages, graves, spatialisation", Icons.Rounded.Equalizer, WarmBrush) { vm.showEq = true }
            SmartCard("Amplificateur de volume", if (vm.fx.boost > 0) "Actif : ${vm.fx.boost} %" else "Augmenter le volume maximal", Icons.Rounded.VolumeUp, BrandBrush) { vm.showBoost = true }
            SmartCard("Minuterie de sommeil", if (vm.sleepRemaining > 0) "Arrêt dans ${fmt(vm.sleepRemaining * 1000L)}" else "Arrêter la musique automatiquement", Icons.Rounded.Timer, CoolBrush) { vm.showSleep = true }
            SmartCard("Mode conduite", "Gros boutons, écran simplifié", Icons.Rounded.DirectionsCar, WarmBrush) { vm.driveMode = true }
            SmartCard("Paramètres", "Bibliothèque, affichage, historique", Icons.Rounded.Settings, BrandBrush) { vm.page = "settings" }
            SmartCard("Fichiers et dossiers masqués", "${vm.hiddenFolders.size} dossier(s), ${vm.hiddenTracks.size} fichier(s)", Icons.Rounded.VisibilityOff, CoolBrush) { vm.page = "hidden" }
            SmartCard("Paramètres de lecture", "Vitesse, silences, reprise, casque", Icons.Rounded.PlayCircle, WarmBrush) { vm.page = "playback" }
            SmartCard("À propos", "TIA ECOUTE – version 1.0.0", Icons.Rounded.Info, BrandBrush) { vm.page = "about" }
        }
    }
}

// ===================================================================== DÉTAIL

@Composable
fun DetailScreen(vm: MainViewModel, d: Detail) {
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    val playlist = (d as? Detail.PlaylistD)?.let { pd -> vm.playlists.firstOrNull { it.id == pd.id } }

    val (title, subtitle, list) = when (d) {
        is Detail.Album -> {
            val l = vm.tracks.filter { it.albumId == d.id && it.album == d.name }
            Triple(d.name, l.firstOrNull()?.artist ?: "", l)
        }
        is Detail.Artist -> Triple(d.name, "Artiste", vm.tracks.filter { it.artist == d.name })
        is Detail.Folder -> Triple(d.name, "Dossier", vm.tracks.filter { it.folder == d.name })
        is Detail.PlaylistD -> Triple(playlist?.name ?: "Playlist", "Playlist", playlist?.let { vm.playlistTracks(it) } ?: emptyList())
        Detail.Favorites -> Triple("Favoris", "Vos coups de cœur", vm.favoriteTracks)
        Detail.MostPlayed -> Triple("Les plus écoutés", "Selon votre historique", vm.mostPlayed)
        Detail.RecentAdded -> Triple("Ajoutés récemment", "Nouveautés", vm.recentlyAdded)
        Detail.RecentPlayed -> Triple("Écoutés récemment", "Historique", vm.recentlyPlayed)
    }

    Column(
        Modifier.fillMaxSize().background(Navy).statusBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.detail = null }) {
                Icon(Icons.Rounded.ArrowBack, "Retour", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (playlist != null) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Options", tint = Color.White) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Renommer") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { menu = false; renameText = playlist.name; renaming = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer la playlist") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            onClick = { menu = false; vm.deletePlaylist(playlist.id); vm.detail = null }
                        )
                    }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TrackArt(list.firstOrNull(), Modifier.size(190.dp), RoundedCornerShape(30.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("$subtitle • ${list.size} titre(s)", color = TextLo, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GradientButton("Lire", Icons.Rounded.PlayArrow, Modifier.weight(1f)) { vm.play(list, 0, false) }
                        GlassButton("Aléatoire", Icons.Rounded.Shuffle, Modifier.weight(1f)) { vm.playShuffled(list) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            if (list.isEmpty()) {
                item {
                    Text(
                        "Rien ici pour le moment.",
                        color = TextLo, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center
                    )
                }
            }
            itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
                TrackRow(
                    vm, t, { vm.play(list, i) }, Modifier.appear(i),
                    onRemove = if (playlist != null) ({ vm.removeFromPlaylist(playlist.id, t.id) }) else null
                )
            }
        }
    }

    if (renaming && playlist != null) {
        TextPromptDialog(
            title = "Renommer la playlist",
            initial = renameText,
            confirmLabel = "Renommer",
            onDismiss = { renaming = false },
            onConfirm = { vm.renamePlaylist(playlist.id, it); renaming = false }
        )
    }
}
