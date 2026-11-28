package com.qs

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter(
    private val context: Context,
    private val videos: List<Uri>,
    private val historyStore: HistoryStore?,
    private val onLongPress: () -> Unit
) : RecyclerView.Adapter<VideoAdapter.VH>() {

    private var player: ExoPlayer? = null
    private var currentPos = 0

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.playerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val uri = videos[position]
        holder.playerView.setShutterBackgroundColor(android.graphics.Color.Black)

        if (position == currentPos) {
            ensurePlayer()
            holder.playerView.player = player
            try {
                player?.setMediaItem(MediaItem.fromUri(uri))
                val saved = historyStore?.getPosition(uri.toString()) ?: 0L
                if (saved > 0) player?.seekTo(saved)
                player?.prepare()
                player?.playWhenReady = true
            } catch (e: Throwable) {
                Toast.makeText(context, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (holder.playerView.player != null) {
                holder.playerView.player = null
            }
        }

        holder.itemView.setOnClickListener {
            player?.let { p -> if (p.isPlaying) p.pause() else p.play() }
        }
        holder.itemView.setOnLongClickListener {
            onLongPress()
            true
        }
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.playerView.player = null
    }

    override fun getItemCount(): Int = videos.size

    private fun ensurePlayer() {
        if (player != null) return
        val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        player = ExoPlayer.Builder(context, renderersFactory)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                videoScalingMode = ExoPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
    }

    fun setCurrent(position: Int) {
        if (position == currentPos && player != null) return
        val oldPos = currentPos
        currentPos = position

        try {
            player?.let {
                historyStore?.savePosition(videos[oldPos].toString(), it.currentPosition)
            }
        } catch (_: Throwable) {
        }

        notifyItemChanged(oldPos)
        notifyItemChanged(position)
    }

    fun saveCurrentPosition() {
        try {
            player?.let {
                historyStore?.savePosition(videos[currentPos].toString(), it.currentPosition)
            }
        } catch (_: Throwable) {
        }
    }

    fun releaseAll() {
        try {
            player?.release()
        } catch (_: Throwable) {
        }
        player = null
    }
}
