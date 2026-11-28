package com.qs

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.exoplayer.ExoPlayer
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var emptyView: TextView
    private lateinit var pickBtn: Button

    private var sharedPlayer: ExoPlayer? = null
    // 直接置空，暂时移除HistoryStore，先跑通基础播放
    private val historyStore: HistoryStore? = null

    private val prefs by lazy { getSharedPreferences("app", MODE_PRIVATE) }

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            prefs.edit().putString("folder", uri.toString()).apply()
            emptyView.visibility = View.GONE
            loadVideos(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }

        setContentView(R.layout.activity_main)
        viewPager = findViewById(R.id.viewPager)
        emptyView = findViewById(R.id.emptyView)
        pickBtn = findViewById(R.id.pickBtn)

        sharedPlayer = ExoPlayer.Builder(this).build()
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewPager.offscreenPageLimit = 1

        pickBtn.setOnClickListener { pickFolder.launch(null) }

        val savedUri = prefs.getString("folder", null)
        if (savedUri != null) {
            pickBtn.visibility = View.GONE
            emptyView.visibility = View.GONE
            loadVideos(Uri.parse(savedUri))
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                (viewPager.adapter as? VideoAdapter)?.setCurrent(position)
            }
        })
    }

    private fun loadVideos(uri: Uri) {
        val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, uri)
        val list = try {
            docFile?.listFiles()
                ?.filter { it.isFile && it.type?.startsWith("video/") == true }
                ?.map { it.uri } ?: emptyList()
        } catch (e: Exception) {
            Toast.makeText(this, "读取文件夹失败", Toast.LENGTH_SHORT).show()
            emptyView.text = "读取视频目录出错"
            emptyView.visibility = View.VISIBLE
            return
        }
        if (list.isEmpty()) {
            emptyView.text = "文件夹内没有找到视频"
            emptyView.visibility = View.VISIBLE
            return
        }
        viewPager.adapter = VideoAdapter(this, list, historyStore, sharedPlayer, {})
    }

    override fun onPause() {
        super.onPause()
        sharedPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPlayer?.release()
        sharedPlayer = null
    }
}
