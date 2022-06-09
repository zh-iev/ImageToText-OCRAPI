package ru.zh_iev.imgtotext_ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast


class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val textResult: TextView = findViewById(R.id.result)
        val resultString: String = intent.extras?.getString("resultString").toString()
        textResult.text = resultString

        val plusSize: Button = findViewById(R.id.plus)
        val copy: Button = findViewById(R.id.copy)

        plusSize.setOnClickListener {
            textResult.textSize++
        }
        copy.setOnClickListener {
            val textToCopy = textResult.text
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("text", textToCopy)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(this@ResultActivity, "Текст скопирован", Toast.LENGTH_SHORT).show()
        }
    }
}