package com.qs

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 临时使用一个极简布局，不带PlayerView！
        setContentView(android.R.layout.simple_list_item_1)
        Toast.makeText(this, "✅ 空壳测试启动", Toast.LENGTH_LONG).show()
    }
}
