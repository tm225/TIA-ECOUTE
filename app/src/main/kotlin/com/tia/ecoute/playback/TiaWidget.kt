package com.tia.ecoute.playback

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.widget.RemoteViews
import com.tia.ecoute.MainActivity
import com.tia.ecoute.R

class TiaWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, build(context)) }
    }

    companion object {
        private const val PREFS = "tia_widget"

        fun build(ctx: Context): RemoteViews {
            val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val v = RemoteViews(ctx.packageName, R.layout.widget_player)
            v.setTextViewText(R.id.w_title, sp.getString("t", "TIA ECOUTE"))
            v.setTextViewText(R.id.w_artist, sp.getString("a", "Touchez pour ouvrir"))
            v.setImageViewResource(
                R.id.w_play,
                if (sp.getBoolean("p", false)) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
            v.setOnClickPendingIntent(R.id.w_prev, key(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 11))
            v.setOnClickPendingIntent(R.id.w_play, key(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 12))
            v.setOnClickPendingIntent(R.id.w_next, key(ctx, KeyEvent.KEYCODE_MEDIA_NEXT, 13))
            val open = PendingIntent.getActivity(
                ctx, 10, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.widget_root, open)
            return v
        }

        private fun key(ctx: Context, code: Int, req: Int): PendingIntent {
            val i = Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(ComponentName(ctx, PlaybackService::class.java))
                .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, code))
            return PendingIntent.getForegroundService(ctx, req, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun push(ctx: Context, title: String, artist: String, playing: Boolean) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("t", title).putString("a", artist).putBoolean("p", playing).apply()
            try {
                val mgr = AppWidgetManager.getInstance(ctx)
                val ids = mgr.getAppWidgetIds(ComponentName(ctx, TiaWidget::class.java))
                if (ids.isNotEmpty()) mgr.updateAppWidget(ids, build(ctx))
            } catch (_: Exception) {
            }
        }
    }
}
