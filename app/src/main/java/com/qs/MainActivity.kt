package com.qs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
                    emptyView.visibility = View.VISIBLE
                    pickBtn.visibility = View.VISIBLE
                    viewPager.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    pickBtn.visibility = View.GONE
                    viewPager.visibility = View.VISIBLE
                    val adapter = VideoAdapter(this, videos, historyStore) { showMenu() }
                    viewPager.adapter = adapter
                    adapter.setCurrent(0)
                }
            }
        }
    }

    private fun showMenu() {
        val options = arrayOf("切换滑动方向", "重新选择文件夹")
        AlertDialog.Builder(this)
            .setTitle("Qs 设置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> toggleOrientation()
                    1 -> pickFolder.launch(null)
                }
            }
            .show()
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
