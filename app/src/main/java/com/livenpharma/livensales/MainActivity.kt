package com.livenpharma.livensales

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val quantities = mutableMapOf<Int, EditText>()
    private lateinit var customer: EditText
    private lateinit var address: EditText
    private lateinit var distributor: AutoCompleteTextView
    private lateinit var salesperson: AutoCompleteTextView
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun label(textValue: String) = TextView(this).apply {
        text = textValue
        textSize = 14f
        setTextColor(Color.DKGRAY)
        setPadding(0, dp(8), 0, dp(2))
    }

    private fun field(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 15f
        setPadding(dp(12), dp(8), dp(12), dp(8))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildForm()
    }

    private fun buildForm() {
        quantities.clear()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(20))
            setBackgroundColor(Color.WHITE)
        }
        val scroll = ScrollView(this).apply { addView(root) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(ImageView(this).apply {
            setImageResource(R.drawable.liven_logo)
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
            contentDescription = "Liven Pharma logo"
        })
        header.addView(TextView(this).apply {
            text = "LIVEN PHARMA\nSALES ORDER"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(65, 30, 100))
            setPadding(dp(12), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(Button(this).apply {
            text = "Admin"
            setOnClickListener { startActivity(Intent(this@MainActivity, AdminActivity::class.java)) }
        })
        root.addView(header)
        root.addView(label("SALES USER — CUSTOMER DETAILS"))
        customer = field("Customer / Pharmacy name").also { root.addView(it) }
        address = field("Customer address").also { root.addView(it) }
        distributor = AutoCompleteTextView(this).apply {
            hint = "Distributor name"
            threshold = 0
            setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, Store.names(this@MainActivity, "distributors")))
        }.also { root.addView(it) }
        salesperson = AutoCompleteTextView(this).apply {
            hint = "Salesperson name"
            threshold = 0
            setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, Store.names(this@MainActivity, "salespeople")))
        }.also { root.addView(it) }
        root.addView(label("PRODUCTS — ENTER QUANTITY ONLY"))
        Store.products(this).filter { it.active }.forEach { product ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(7), dp(10), dp(7))
                setBackgroundColor(Color.rgb(247, 246, 249))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) }
            }
            card.addView(TextView(this).apply {
                text = product.display()
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.rgb(55, 35, 75))
            })
            card.addView(TextView(this).apply {
                text = "Rate: Rs. ${money(product.rate)}  •  ${product.ingredient}"
                textSize = 12f
                setTextColor(Color.DKGRAY)
            })
            val quantity = EditText(this).apply {
                hint = "Quantity"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setSingleLine(true)
                textSize = 14f
            }
            quantities[product.id] = quantity
            card.addView(quantity)
            root.addView(card)
        }
        root.addView(Button(this).apply {
            text = "SAVE PDF & SHARE"
            setOnClickListener { createOrder() }
        })
        root.addView(Button(this).apply {
            text = "Saved Orders"
            setOnClickListener { startActivity(Intent(this@MainActivity, HistoryActivity::class.java)) }
        })
        setContentView(scroll)
    }

    private fun money(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)

    private fun createOrder() {
        if (customer.text.isBlank() || address.text.isBlank()) {
            Toast.makeText(this, "Enter customer name and address", Toast.LENGTH_LONG).show()
            return
        }
        val lines = Store.products(this).filter { it.active }.mapNotNull { product ->
            val quantity = quantities[product.id]?.text?.toString()?.toIntOrNull() ?: 0
            if (quantity > 0) Triple(product, quantity, product.rate * quantity) else null
        }
        if (lines.isEmpty()) {
            Toast.makeText(this, "Enter quantity for at least one product", Toast.LENGTH_LONG).show()
            return
        }
        val total = lines.sumOf { it.third }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(orderDirectory(), "Liven_Order_$stamp.pdf")
        try {
            file.parentFile?.mkdirs()
            makePdf(file, lines, total)
            Toast.makeText(this, "PDF saved", Toast.LENGTH_SHORT).show()
            share(file)
        } catch (error: Exception) {
            Toast.makeText(this, "Could not create PDF: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun orderDirectory(): File = File(getExternalFilesDir(null) ?: filesDir, "Documents/Orders")

    private fun makePdf(file: File, lines: List<Triple<Product, Int, Double>>, total: Double) {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNumber = 0
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create())
        var canvas = page.canvas
        fun drawHeader() {
            paint.color = Color.rgb(70, 35, 105)
            paint.textSize = 22f
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            canvas.drawText("LIVEN PHARMA", 40f, 55f, paint)
            paint.color = Color.DKGRAY
            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("SALES ORDER", 455f, 55f, paint)
            paint.color = Color.LTGRAY
            canvas.drawLine(40f, 70f, 555f, 70f, paint)
        }
        fun startContinuation() {
            document.finishPage(page)
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create())
            canvas = page.canvas
            drawHeader()
            paint.color = Color.BLACK
            paint.textSize = 11f
        }
        drawHeader()
        paint.color = Color.BLACK
        paint.textSize = 11f
        var y = 100f
        val date = SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date())
        listOf("Customer: ${customer.text}", "Address: ${address.text}", "Distributor: ${distributor.text}", "Salesperson: ${salesperson.text}", "Date: $date").forEach {
            canvas.drawText(it.take(90), 40f, y, paint)
            y += 19f
        }
        y += 10f
        fun tableHeader() {
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            canvas.drawText("Product", 40f, y, paint)
            canvas.drawText("Qty", 350f, y, paint)
            canvas.drawText("Rate", 405f, y, paint)
            canvas.drawText("Amount", 485f, y, paint)
            y += 10f
            canvas.drawLine(40f, y, 555f, y, paint)
            y += 18f
            paint.typeface = Typeface.DEFAULT
        }
        tableHeader()
        lines.forEach { (product, quantity, amount) ->
            if (y > 770f) {
                startContinuation()
                y = 100f
                tableHeader()
            }
            canvas.drawText("${product.brand} ${product.strength} (${product.pack})".take(43), 40f, y, paint)
            canvas.drawText(quantity.toString(), 350f, y, paint)
            canvas.drawText(money(product.rate), 405f, y, paint)
            canvas.drawText(money(amount), 485f, y, paint)
            y += 20f
        }
        if (y > 770f) {
            startContinuation()
            y = 120f
        }
        y += 12f
        paint.color = Color.BLACK
        canvas.drawLine(380f, y, 555f, y, paint)
        y += 22f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("GRAND TOTAL: Rs. ${money(total)}", 380f, y, paint)
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Generated by Liven Sales", 40f, 810f, paint)
        document.finishPage(page)
        try { FileOutputStream(file).use { document.writeTo(it) } } finally { document.close() }
    }

    private fun share(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { startActivity(Intent.createChooser(send, "Share sales order PDF")) }
        catch (_: Exception) { Toast.makeText(this, "No sharing app available", Toast.LENGTH_LONG).show() }
    }
}
