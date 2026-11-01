package com.tia.ecoute

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.tia.ecoute.data.AlbumInfo
import com.tia.ecoute.data.ArtistInfo
import com.tia.ecoute.data.FolderInfo
import com.tia.ecoute.data.MediaScanner
import com.tia.ecoute.data.Playlist
import com.tia.ecoute.data.Prefs
import com.tia.ecoute.data.SortMode
import com.tia.ecoute.data.Track
import com.tia.ecoute.playback.AudioFx
import com.tia.ecoute.playback.PlaybackService
import com.tia.ecoute.playback.toMediaItem
import com.tia.ecoute.ui.ThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Écrans de détail (liste de titres avec en-tête). */
sealed class Detail {
    data class Album(val id: Long, val name: String) : Detail()
    data class Artist(val name: String) : Detail()
    data class Folder(val name: String) : Detail()
    data class PlaylistD(val id: Long) : Detail()
    object Favorites : Detail()
    object MostPlayed : Detail()
    object RecentAdded : Detail()
    object RecentPlayed : Detail()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Context = app.applicationContext
    val prefs = Prefs(ctx)
    val fx = AudioFx(prefs)

    // ---------------------------------------------------------------- Bibliothèque
    var hasPermission by mutableStateOf(false)
    var loading by mutableStateOf(false)
        private set
    var rawTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var hiddenFolders by mutableStateOf(prefs.hiddenFolders())
        private set
    var hiddenIds by mutableStateOf(prefs.hiddenIds())
        private set
    val tracks: List<Track> by derivedStateOf {
        rawTracks.filter { it.folder !in hiddenFolders && it.id !in hiddenIds }
    }
    val hiddenTracks: List<Track> by derivedStateOf { rawTracks.filter { it.id in hiddenIds } }
    val allFolders: List<Pair<String, Int>> by derivedStateOf {
        rawTracks.groupBy { it.folder }.map { it.key to it.value.size }.sortedBy { it.first.lowercase() }
    }
    var minDurationSec by mutableIntStateOf(prefs.minDurationSec)
        private set

    private val byId by derivedStateOf { tracks.associateBy { it.id } }

    var query by mutableStateOf("")
    var sort by mutableStateOf(SortMode.TITLE)

    val visibleTracks by derivedStateOf {
        val q = query.trim().lowercase()
        val base = if (q.isEmpty()) tracks else tracks.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }
        when (sort) {
            SortMode.TITLE -> base.sortedBy { it.title.lowercase() }
            SortMode.ARTIST -> base.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            SortMode.RECENT -> base.sortedByDescending { it.dateAdded }
            SortMode.DURATION -> base.sortedByDescending { it.durationMs }
        }
    }

    val albums: List<AlbumInfo> by derivedStateOf {
        tracks.groupBy { it.albumId to it.album }
            .map { (_, l) -> AlbumInfo(l.first().albumId, l.first().album, l.first().artist, l) }
            .sortedBy { it.name.lowercase() }
    }
    val artists: List<ArtistInfo> by derivedStateOf {
        tracks.groupBy { it.artist }.map { ArtistInfo(it.key, it.value) }.sortedBy { it.name.lowercase() }
    }
    val folders: List<FolderInfo> by derivedStateOf {
        tracks.groupBy { it.folder }.map { FolderInfo(it.key, it.value) }.sortedBy { it.name.lowercase() }
    }

    // ---------------------------------------------------------------- Favoris, playlists, stats
    var favorites by mutableStateOf(prefs.favorites())
        private set
    var playlists by mutableStateOf(prefs.playlists())
        private set
    private var playCounts by mutableStateOf(prefs.playCounts())
    private var recentIds by mutableStateOf(prefs.recents())

    val favoriteTracks: List<Track> by derivedStateOf { tracks.filter { it.id in favorites } }
    val recentlyAdded: List<Track> by derivedStateOf { tracks.sortedByDescending { it.dateAdded }.take(40) }
    val mostPlayed: List<Track> by derivedStateOf {
        tracks.filter { (playCounts[it.id] ?: 0) > 0 }.sortedByDescending { playCounts[it.id] ?: 0 }.take(40)
    }
    val recentlyPlayed: List<Track> by derivedStateOf { recentIds.mapNotNull { byId[it] } }

    // ---------------------------------------------------------------- Lecture
    private var controller: MediaController? = null
    private var connecting = false
    private var restored = false
    private var counted: Long? = null

    var isPlaying by mutableStateOf(false)
        private set
    var currentId by mutableStateOf<Long?>(null)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var queueIds by mutableStateOf<List<Long>>(emptyList())
        private set
    var queueIndex by mutableIntStateOf(0)
        private set

    val current: Track? get() = currentId?.let { byId[it] }
    val queueTracks: List<Track> get() = queueIds.mapNotNull { byId[it] }

    // ---------------------------------------------------------------- Minuteur de veille
    var sleepRemaining by mutableIntStateOf(0)
        private set
    private var sleepJob: Job? = null

    // ---------------------------------------------------------------- État de l'interface
    var tab by mutableIntStateOf(0)
    var detail by mutableStateOf<Detail?>(null)
    var nowPlayingOpen by mutableStateOf(false)
    var showQueue by mutableStateOf(false)
    var showEq by mutableStateOf(false)
    var showSleep by mutableStateOf(false)
    var showSpeed by mutableStateOf(false)
    var showNewPlaylist by mutableStateOf(false)
    var playlistTarget by mutableStateOf<Track?>(null)
    var page by mutableStateOf<String?>(null)
    var driveMode by mutableStateOf(false)
    var showBoost by mutableStateOf(false)
    var showStyles by mutableStateOf(false)
    var discStyle by mutableIntStateOf(prefs.discStyle)
        private set
    var liveEnabled by mutableStateOf(prefs.live)
        private set
    var liveEnergy by mutableFloatStateOf(0f)
        private set

    @JvmName("do_setDiscStyle")
    fun setDiscStyle(i: Int) {
        discStyle = i
        prefs.discStyle = i
    }

    fun setLive(on: Boolean) {
        liveEnabled = on
        prefs.live = on
        fx.liveWanted = on
        if (on) {
            if (!fx.startLive()) liveEnergy = 0f
        } else {
            fx.stopLive()
            liveEnergy = 0f
        }
    }

    // ---------------------------------------------------------------- Réglages
    var resumeOnStart by mutableStateOf(prefs.resumeOnStart)
        private set
    var keepScreenOn by mutableStateOf(prefs.keepScreenOn)
        private set
    var skipSilence by mutableStateOf(prefs.skipSilence)
        private set
    var pauseOnUnplug by mutableStateOf(prefs.pauseOnUnplug)
        private set
    var themeIndex by mutableIntStateOf(prefs.themeIndex)
        private set

    fun setTheme(i: Int) {
        themeIndex = i
        ThemeState.index = i
        prefs.themeIndex = i
    }

    @JvmName("do_setResumeOnStart")
    fun setResumeOnStart(v: Boolean) { resumeOnStart = v; prefs.resumeOnStart = v }
    @JvmName("do_setKeepScreenOn")
    fun setKeepScreenOn(v: Boolean) { keepScreenOn = v; prefs.keepScreenOn = v }
    @JvmName("do_setSkipSilence")
    fun setSkipSilence(v: Boolean) {
        skipSilence = v
        prefs.skipSilence = v
        PlaybackService.exoRef?.skipSilenceEnabled = v
    }
    @JvmName("do_setPauseOnUnplug")
    fun setPauseOnUnplug(v: Boolean) {
        pauseOnUnplug = v
        prefs.pauseOnUnplug = v
        PlaybackService.exoRef?.setHandleAudioBecomingNoisy(v)
    }

    fun setFolderHidden(name: String, hidden: Boolean) {
        hiddenFolders = if (hidden) hiddenFolders + name else hiddenFolders - name
        prefs.saveHiddenFolders(hiddenFolders)
    }
    fun hideTrack(id: Long) {
        hiddenIds = hiddenIds + id
        prefs.saveHiddenIds(hiddenIds)
    }
    fun unhideTrack(id: Long) {
        hiddenIds = hiddenIds - id
        prefs.saveHiddenIds(hiddenIds)
    }

    fun clearHistory() {
        playCounts = emptyMap()
        recentIds = emptyList()
        prefs.savePlayCounts(playCounts)
        prefs.saveRecents(recentIds)
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            if (playing) maybeCount() else persist()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) counted = null
            syncCurrent()
            maybeCount()
            persist()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            syncQueue()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            shuffle = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(mode: Int) {
            repeatMode = mode
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            speed = playbackParameters.speed
        }
    }

    init {
        ThemeState.index = prefs.themeIndex
        viewModelScope.launch {
            while (true) {
                delay(40)
                liveEnergy = if (liveEnabled && isPlaying) liveEnergy * 0.5f + fx.energy() * 0.5f else 0f
            }
        }
        // Horloge de position (250 ms)
        viewModelScope.launch {
            while (true) {
                delay(250)
                val c = controller ?: continue
                val d = c.duration
                positionMs = c.currentPosition.coerceAtLeast(0L)
                durationMs = if (d < 0) (current?.durationMs ?: 0L) else d
            }
        }
    }

    // ================================================================= Connexion au service
    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                attachFx()
                syncAll()
                tryRestore()
            } catch (_: Exception) {
            }
            connecting = false
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun attachFx() {
        val exo = PlaybackService.exoRef ?: return
        exo.skipSilenceEnabled = skipSilence
        exo.setHandleAudioBecomingNoisy(pauseOnUnplug)
        fx.liveWanted = liveEnabled
        fx.attach(exo.audioSessionId)
        exo.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                fx.attach(audioSessionId)
            }
        })
    }

    private fun syncAll() {
        val c = controller ?: return
        isPlaying = c.isPlaying
        shuffle = c.shuffleModeEnabled
        repeatMode = c.repeatMode
        speed = c.playbackParameters.speed
        syncCurrent()
        syncQueue()
    }

    private fun syncCurrent() {
        val c = controller ?: return
        currentId = c.currentMediaItem?.mediaId?.toLongOrNull()
        queueIndex = c.currentMediaItemIndex
        positionMs = c.currentPosition.coerceAtLeast(0L)
        val d = c.duration
        durationMs = if (d < 0) (current?.durationMs ?: 0L) else d
    }

    private fun syncQueue() {
        val c = controller ?: return
        queueIds = (0 until c.mediaItemCount).mapNotNull { c.getMediaItemAt(it).mediaId.toLongOrNull() }
        queueIndex = c.currentMediaItemIndex
    }

    private fun maybeCount() {
        val c = controller ?: return
        val id = currentId ?: return
        if (c.playWhenReady && counted != id) {
            counted = id
            playCounts = playCounts + (id to ((playCounts[id] ?: 0) + 1))
            prefs.savePlayCounts(playCounts)
            recentIds = (listOf(id) + recentIds.filter { it != id }).take(40)
            prefs.saveRecents(recentIds)
        }
    }

    fun persist() {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return
        prefs.saveSession(queueIds, currentId ?: 0L, c.currentPosition.coerceAtLeast(0L))
    }

    private fun tryRestore() {
        val c = controller ?: return
        if (restored || !resumeOnStart || tracks.isEmpty()) return
        restored = true
        if (c.mediaItemCount > 0) return
        val items = prefs.sessionIds().mapNotNull { byId[it] }
        if (items.isEmpty()) return
        val idx = items.indexOfFirst { it.id == prefs.sessionCurrent() }.coerceAtLeast(0)
        c.setMediaItems(items.map { it.toMediaItem() }, idx, prefs.sessionPosition())
        c.prepare()
    }

    // ================================================================= Bibliothèque
    fun refresh() {
        if (!hasPermission) return
        viewModelScope.launch {
            loading = rawTracks.isEmpty()
            val list = withContext(Dispatchers.IO) { MediaScanner.scan(ctx, prefs.minDurationSec * 1000L) }
            if (list != rawTracks) rawTracks = list
            loading = false
            tryRestore()
        }
    }

    fun setMinDuration(sec: Int) {
        prefs.minDurationSec = sec
        minDurationSec = sec
        refresh()
    }

    // ================================================================= Commandes de lecture
    fun play(list: List<Track>, start: Int, shuffleOn: Boolean? = null) {
        val c = controller ?: return
        if (list.isEmpty()) return
        val max = 300
        var source = list
        var idx = start.coerceIn(0, list.lastIndex)
        if (list.size > max) {
            val from = (idx - max / 4).coerceIn(0, list.size - max)
            source = list.subList(from, from + max)
            idx -= from
        }
        c.setMediaItems(source.map { it.toMediaItem() }, idx, 0L)
        if (shuffleOn != null) c.shuffleModeEnabled = shuffleOn
        c.prepare()
        c.play()
    }

    fun playShuffled(list: List<Track>) {
        if (list.isEmpty()) return
        play(list, Random.nextInt(list.size), true)
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0L) else c.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    fun seekFraction(f: Float) {
        seekTo((durationMs * f.coerceIn(0f, 1f)).toLong())
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    @JvmName("do_setSpeed")
    fun setSpeed(s: Float) {
        controller?.setPlaybackSpeed(s)
        speed = s
    }

    fun skipTo(index: Int) {
        val c = controller ?: return
        c.seekTo(index, 0L)
        c.play()
    }

    fun playNext(t: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            play(listOf(t), 0)
        } else {
            c.addMediaItem(c.currentMediaItemIndex + 1, t.toMediaItem())
        }
    }

    fun enqueue(t: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) play(listOf(t), 0) else c.addMediaItem(t.toMediaItem())
    }

    fun removeFromQueue(index: Int) {
        controller?.removeMediaItem(index)
    }

    // ================================================================= Minuteur
    fun startSleep(minutes: Int) {
        sleepJob?.cancel()
        controller?.volume = 1f
        if (minutes <= 0) {
            sleepRemaining = 0
            return
        }
        sleepRemaining = minutes * 60
        sleepJob = viewModelScope.launch {
            while (sleepRemaining > 0) {
                delay(1000)
                sleepRemaining -= 1
                if (sleepRemaining in 1..10) controller?.volume = sleepRemaining / 10f
            }
            controller?.pause()
            controller?.volume = 1f
        }
    }

    // ================================================================= Favoris & playlists
    fun isFavorite(id: Long): Boolean = id in favorites

    fun toggleFavorite(id: Long) {
        favorites = if (id in favorites) favorites - id else favorites + id
        prefs.saveFavorites(favorites)
    }

    fun createPlaylist(name: String, firstTrackId: Long? = null): Long {
        val id = System.currentTimeMillis()
        val clean = name.trim().ifEmpty { "Ma playlist" }
        playlists = playlists + Playlist(id, clean, listOfNotNull(firstTrackId))
        prefs.savePlaylists(playlists)
        return id
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        playlists = playlists.map {
            if (it.id == playlistId && trackId !in it.trackIds) it.copy(trackIds = it.trackIds + trackId) else it
        }
        prefs.savePlaylists(playlists)
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        playlists = playlists.map {
            if (it.id == playlistId) it.copy(trackIds = it.trackIds.filter { id -> id != trackId }) else it
        }
        prefs.savePlaylists(playlists)
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        playlists = playlists.map { if (it.id == playlistId) it.copy(name = name.trim().ifEmpty { it.name }) else it }
        prefs.savePlaylists(playlists)
    }

    fun deletePlaylist(playlistId: Long) {
        playlists = playlists.filter { it.id != playlistId }
        prefs.savePlaylists(playlists)
    }

    fun playlistTracks(p: Playlist): List<Track> = p.trackIds.mapNotNull { byId[it] }

    override fun onCleared() {
        persist()
        sleepJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
