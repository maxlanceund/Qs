package com.qs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.viewpager2.widget.ViewPager2
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var emptyView: TextView
    private lateinit var pickBtn: Button
    private lateinit var historyStore: HistoryStore
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
        
        // 【UI 优化】真·全屏沉浸式
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
        historyStore = HistoryStore(this)

        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewPager.offscreenPageLimit = 1
        pickBtn.setOnClickListener { pickFolder.launch(null) }

        val savedUri = prefs.getString("folder", null)
        if (savedUri != null) {
            pickBtn.visibility = View.GONE
            emptyView.visibility = View.GONE
            loadVideos(Uri.parse(savedUri))
        } else {
            pickFolder.launch(null)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                (viewPager.adapter as? VideoAdapter)?.setCurrent(position)
            }
        })
    }

    private fun loadVideos(treeUri: Uri) {
        Executors.newSingleThreadExecutor().execute {
            val root = DocumentFile.fromTreeUri(this, treeUri)
            val videos = mutableListOf<Uri>()
            if (root != null) collectVideos(root, videos)
            runOnUiThread {
                if (videos.isEmpty()) {
                    emptyView.text = "文件夹里没有视频\n长按重新选择"
                    emptyView.visibility = View.VISIBLE
                    pickBtn.visibility = View.VISIBLE
                    viewPager.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    pickBtn.visibility = View.GONE
                    viewPager.visibility = View.VISIBLE
                    val adapter = VideoAdapter(this, videos, historyStore) { showCustomMenu() }
                    viewPager.adapter = adapter
                    adapter.setCurrent(0)
                }
            }
        }
    }

    // 【UI 优化】自定义高级黑底抽屉菜单
    private fun showCustomMenu() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val view = layoutInflater.inflate(R.layout.dialog_menu, null)
        dialog.setContentView(view)
        
        dialog.window?.let { window ->
            window.setGravity(Gravity.BOTTOM)
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            // 底部滑入动画
            window.setWindowAnimations(android.R.style.Animation_Dialog) 
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val params = window.attributes
            params.dimAmount = 0.6f // 背景变暗程度
            window.attributes = params
        }

        view.findViewById<View>(R.id.btn_toggle).setOnClickListener {
            toggleOrientation()
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_folder).setOnClickListener {
            pickFolder.launch(null)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun toggleOrientation() {
        if (viewPager.orientation == ViewPager2.ORIENTATION_VERTICAL) {
            viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            Toast.makeText(this, "左右滑动", Toast.LENGTH_SHORT).show()
        } else {
            viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
            Toast.makeText(this, "上下滑动", Toast.LENGTH_SHORT).show()
        }
        (viewPager.adapter as? VideoAdapter)?.setCurrent(viewPager.currentItem)
    }

    private fun collectVideos(dir: DocumentFile, out: MutableList<Uri>) {
        val exts = listOf(".mp4", ".webm", ".mkv", ".mov", ".m4v")
        for (f in dir.listFiles()) {
            if (f.isDirectory) collectVideos(f, out)
            else if (f.isFile && exts.any { f.name?.lowercase()?.endsWith(it) == true }) {
                val fileName = f.name ?: ""
                if (!fileName.startsWith(".") && f.length() > 1024) out.add(f.uri)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (viewPager.adapter as? VideoAdapter)?.saveCurrentPosition()
    }

    override fun onDestroy() {
        super.onDestroy()
        (viewPager.adapter as? VideoAdapter)?.releaseAll()
    }
}
