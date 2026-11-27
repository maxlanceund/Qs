package com.qs
import android.graphics.Color
import com.google.android.exoplayer2.ui.PlayerView

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        // 只有当前显示的位置才挂载播放器，其他Item不绑定player，避免多Surface冲突
        if (position == currentPos) {
            if (holder.playerView.player !== sharedPlayer) {
                holder.playerView.player = sharedPlayer
            }
        } else {
            if (holder.playerView.player != null) {
                holder.playerView.player = null
            }
        }

        holder.playerView.setShutterBackgroundColor(android.graphics.Color.Color.BLACK)

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
        // 回收Item，强制解绑播放器，杜绝一个ExoPlayer绑定多个PlayerView（修复黑屏根源）
        holder.playerView.player = null
    }

    override fun getItemCount() = videos.size

    fun setCurrent(position: Int) {
        if (position == currentPos && sharedPlayer != null) return
        val oldPos = currentPos
        currentPos = position

        if (sharedPlayer == null) {
            val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
            sharedPlayer = ExoPlayer.Builder(context, renderersFactory)
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    // 让解码器输出画面配合视图缩放，缓解高于屏幕分辨率视频卡顿
                    videoScalingMode = ExoPlayer.PlayerView.PlayerView.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }
        }

        val uri = videos[oldPos]
        historyStore.savePosition(uri.toString(), sharedPlayer?.currentPosition ?: 0L)

        val newUri = videos[position]
        sharedPlayer?.let { player ->
            player.setMediaItem(MediaItem.fromUri(newUri))
            val saved = historyStore.getPosition(newUri.toString())
            if (saved > 0) player.seekTo(saved)
            player.prepare()
            player.play()
        }
        // 通知RecyclerView刷新新旧两个位置，完成播放器挂载切换
        notifyItemChanged(oldPos)
        notifyItemChanged(position)
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
