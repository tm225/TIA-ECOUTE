package com.tia.ecoute.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.tia.ecoute.MainActivity
import com.tia.ecoute.data.ArtLoader
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exoRef = exo
        exo.addListener(object : Player.Listener {
            private fun refresh() {
                val m = exo.mediaMetadata
                TiaWidget.push(
                    this@PlaybackService,
                    m.title?.toString() ?: "TIA ECOUTE",
                    m.artist?.toString() ?: "",
                    exo.isPlaying
                )
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refresh()
        })

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, exo)
            .setSessionActivity(openApp)
            .setBitmapLoader(ArtBitmapLoader(this))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.let {
            it.player.release()
            it.release()
        }
        session = null
        exoRef = null
        super.onDestroy()
    }

    companion object {
        /** Même processus que l'application : sert à brancher l'égaliseur sur la session audio. */
        @Volatile
        var exoRef: ExoPlayer? = null
    }
}

/** Fournit la pochette à la notification et à l'écran de verrouillage. */
@OptIn(UnstableApi::class)
class ArtBitmapLoader(private val context: Context) : BitmapLoader {
    private val executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2))

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        executor.submit(Callable<Bitmap> {
            BitmapFactory.decodeByteArray(data, 0, data.size) ?: throw IllegalStateException("decode")
        })

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        executor.submit(Callable<Bitmap> {
            ArtLoader.decodeUri(context, uri, 512) ?: throw IllegalStateException("no art")
        })
}
