package com.qs

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter(
    private val context: Context,
    private val videos: List<Uri>,
    private val historyStore: HistoryStore?,
    private val sharedPlayer: ExoPlayer?,
    private val onLongPress: () -> Unit
) : RecyclerView.Adapter<VideoAdapter.VH>() {

    private var currentPos = 0

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(androidx.media3.ui.R.id.player_view)
        var boundUri: Uri? = null

        fun bind(uri: Uri, pos: Int) {
            boundUri = uri
            historyStore?.saveHistory(uri.toString())

            if (pos == currentPos) {
                sharedPlayer?.setMediaItem(MediaItem.fromUri(uri))
                sharedPlayer?.prepare()
                sharedPlayer?.playWhenReady = true
                playerView.player = sharedPlayer
            } else {
                if (playerView.player == sharedPlayer) {
                    playerView.player = null
                }
            }
        }

        fun unbind() {
            if (playerView.player == sharedPlayer) {
                playerView.player = null
            }
            boundUri = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(videos[position], position)
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun getItemCount(): Int = videos.size

    fun setCurrent(pos: Int) {
        val old = currentPos
        currentPos = pos
        notifyItemChanged(old)
        notifyItemChanged(pos)
    }
}
