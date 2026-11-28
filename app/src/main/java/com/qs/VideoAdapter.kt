package com.qs

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter(
    private val context: Context,
    private val videos: List<Uri>,
    private val historyStore: HistoryStore,
    private val onLongPress: () -> Unit
) : RecyclerView.Adapter<VideoAdapter.VH>() {

    private var sharedPlayer: ExoPlayer? = null
    private var currentPos = 0

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.playerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val uri = videos[position]
        holder.itemView.setOnLongClickListener {
            onLongPress()
            true
        }
        if (position == currentPos) {
            playAt(holder, uri)
        } else {
            holder.playerView.player = null
        }
    }

    private fun playAt(holder: VH, uri: Uri) {
        sharedPlayer?.release()
        val player = ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context))
            .build()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
        holder.playerView.player = player
        sharedPlayer = player
    }

    fun setCurrent(pos: Int) {
        currentPos = pos
        notifyDataSetChanged()
    }

    fun saveCurrentPosition() {
        // 暂时注释：historyStore没有save方法，先跳过历史存储
        /*
        sharedPlayer?.let {
            historyStore.save(videos[currentPos], it.currentPosition)
        }
        */
    }

    fun releaseAll() {
        sharedPlayer?.release()
        sharedPlayer = null
    }

    override fun getItemCount(): Int = videos.size
}
