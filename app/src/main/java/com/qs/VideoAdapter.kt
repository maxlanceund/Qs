package com.qs

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter(
    private val context: Context,
    private val videos: List<Uri>,
    private val historyStore: HistoryStore,
    private val onLongPress: () -> Unit
) : RecyclerView.Adapter<VideoAdapter.VH>() {

    // 【核心重构】全场只有一个播放器！不再创建池子！
    private var sharedPlayer: ExoPlayer? = null
    private var currentPos = 0

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.playerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        // 绑定的时候只挂载界面，不操作播放器
        holder.playerView.player = sharedPlayer
        holder.playerView.setKeepContentOnPlayerReset(true)
        holder.playerView.setShutterBackgroundColor(android.graphics.Color.DKGRAY)

        holder.itemView.setOnClickListener {
            sharedPlayer?.let { p ->
                if (p.isPlaying) p.pause() else p.play()
            }
        }

        holder.itemView.setOnLongClickListener {
            onLongPress()
            true
        }
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        // 不要清空 player，保留残影，防止黑屏！
    }

    override fun getItemCount() = videos.size

    // 当页面切换时，由 MainActivity 触发
    fun setCurrent(position: Int) {
        if (position == currentPos && sharedPlayer != null) return
        currentPos = position

        // 初始化唯一的播放器
        if (sharedPlayer == null) {
            val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
            sharedPlayer = ExoPlayer.Builder(context, renderersFactory).build()
            sharedPlayer?.repeatMode = Player.REPEAT_MODE_ONE
        }

        val uri = videos[position]
        // 保存上一个视频的进度
        historyStore.savePosition(uri.toString(), sharedPlayer?.currentPosition ?: 0L)

        // 换台！
        sharedPlayer?.let { player ->
            player.setMediaItem(MediaItem.fromUri(uri))
            val saved = historyStore.getPosition(uri.toString())
            if (saved > 0) player.seekTo(saved)
            player.prepare()
            player.play()
        }
    }

    fun saveCurrentPosition() {
        sharedPlayer?.let {
            historyStore.savePosition(videos[currentPos].toString(), it.currentPosition)
        }
    }

    fun releaseAll() {
        sharedPlayer?.release()
        sharedPlayer = null
    }
}
