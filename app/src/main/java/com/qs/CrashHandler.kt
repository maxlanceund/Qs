package com.qs

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler private constructor(private val app: Context) : Thread.UncaughtExceptionHandler {

    private val prefs = app.getSharedPreferences("crash", Context.MODE_PRIVATE)
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        const val KEY_CRASH = "last_crash"

        fun install(app: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(app))
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val fullStack = sw.toString().lines()

        val report = buildString {
            append("异常类型: ").append(e.javaClass.name).append('\n')
            append("异常信息: ").append(e.message ?: "(无 message)").append('\n')
            append("发生线程: ").append(t.name).append("\n\n")
            append("堆栈:\n")
            append(fullStack.take(40).joinToString("\n"))
            if (fullStack.size > 40) append("\n... (其余省略)")
        }

        prefs.edit().putString(KEY_CRASH, report).apply()

        try {
            Handler(Looper.getMainLooper()).post {
                try {
                    val intent = Intent(app, CrashReportActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    app.startActivity(intent)
                } catch (_: Throwable) {
                }
            }
            Thread.sleep(300)
        } catch (_: Throwable) {
        }

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e)
        } else {
            Process.killProcess(Process.myPid())
        }
    }
}
