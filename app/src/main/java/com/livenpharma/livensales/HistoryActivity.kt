package com.livenpharma.livensales

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File

class HistoryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        root.addView(TextView(this).apply { text = "SAVED SALES ORDERS"; textSize = 20f; setTypeface(null, android.graphics.Typeface.BOLD) })
        val directory = File(getExternalFilesDir(null) ?: filesDir, "Documents/Orders")
        directory.mkdirs()
        val files = directory.listFiles()?.filter { it.extension.equals("pdf", true) }?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (files.isEmpty()) {
            root.addView(TextView(this).apply { text = "No saved orders yet."; setPadding(0, 30, 0, 0) })
        } else {
            val list = ListView(this)
            list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, files.map { it.name })
            list.setOnItemClickListener { _, _, position, _ -> share(files[position]) }
            root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(root)
    }

    private fun share(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "Share order PDF"))
    }
}
