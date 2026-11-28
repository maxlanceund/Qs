package com.qs

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CrashReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("crash", MODE_PRIVATE)
        val report = prefs.getString(CrashHandler.KEY_CRASH, null) ?: "未记录到崩溃堆栈。"

        val title = TextView(this).apply {
            text = "App 刚刚发生崩溃"
            setTextColor(0xFFFF5555.toInt())
            textSize = 18f
            setPadding(32, 48, 32, 16)
        }

        val body = TextView(this).apply {
            text = report
            setTextColor(0xFFEEEEEE.toInt())
            textSize = 13f
            setLineSpacing(6f, 1f)
            setPadding(32, 16, 32, 16)
        }

        val retry = Button(this).apply {
            text = "清除记录并重新进入"
            setOnClickListener {
                prefs.edit().remove(CrashHandler.KEY_CRASH).apply()
                startActivity(
                    Intent(this@CrashReportActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0A0A.toInt())
            addView(title)
            addView(body)
            addView(retry)
        }

        val scroll = ScrollView(this)
        scroll.addView(container)
        setContentView(scroll)
    }
}
