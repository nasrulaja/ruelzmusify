/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import moe.ruelz.ruelzmusify.playback.MusicService

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        sendWidgetAction(context, WidgetServiceActions.PLAY_PAUSE)
    }
}

class SkipNextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        sendWidgetAction(context, WidgetServiceActions.SKIP_NEXT)
    }
}

class SkipPrevAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        sendWidgetAction(context, WidgetServiceActions.SKIP_PREV)
    }
}

class PlayQueueItemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val mediaId = parameters[WidgetActionKeys.MediaId]?.takeIf { it.isNotBlank() } ?: return
        sendWidgetAction(context, WidgetServiceActions.PLAY_QUEUE_ITEM) {
            putExtra(WidgetServiceActions.EXTRA_MEDIA_ID, mediaId)
            putExtra(WidgetServiceActions.EXTRA_QUEUE_INDEX, parameters[WidgetActionKeys.QueueIndex] ?: NoQueueIndex)
        }
    }
}

class PlaySongAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val mediaId = parameters[WidgetActionKeys.MediaId]?.takeIf { it.isNotBlank() } ?: return
        sendWidgetAction(context, WidgetServiceActions.PLAY_SONG) {
            putExtra(WidgetServiceActions.EXTRA_MEDIA_ID, mediaId)
        }
    }
}

internal object WidgetServiceActions {
    const val PLAY_PAUSE = "moe.ruelz.ruelzmusify.WIDGET_PLAY_PAUSE"
    const val SKIP_NEXT = "moe.ruelz.ruelzmusify.WIDGET_SKIP_NEXT"
    const val SKIP_PREV = "moe.ruelz.ruelzmusify.WIDGET_SKIP_PREV"
    const val PLAY_QUEUE_ITEM = "moe.ruelz.ruelzmusify.WIDGET_PLAY_QUEUE_ITEM"
    const val PLAY_SONG = "moe.ruelz.ruelzmusify.WIDGET_PLAY_SONG"
    const val EXTRA_MEDIA_ID = "moe.ruelz.ruelzmusify.extra.WIDGET_MEDIA_ID"
    const val EXTRA_QUEUE_INDEX = "moe.ruelz.ruelzmusify.extra.WIDGET_QUEUE_INDEX"
}

private const val TAG = "MusicWidgetActions"

private inline fun sendWidgetAction(
    context: Context,
    action: String,
    configure: Intent.() -> Unit = {},
) {
    val intent = Intent(action).setClass(context, MusicService::class.java).apply(configure)
    runCatching {
        context.startService(intent)
    }.onFailure { error ->
        Log.e(TAG, "Failed to send widget action: $action", error)
    }
}
