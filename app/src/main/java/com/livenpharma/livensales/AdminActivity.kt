package com.livenpharma.livensales

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class AdminActivity : Activity() {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pinEntry = EditText(this).apply {
            hint = "Administrator PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Administrator access")
            .setMessage("Enter administrator PIN")
            .setView(pinEntry)
            .setCancelable(false)
            .setPositiveButton("Unlock") { _, _ ->
                if (pinEntry.text.toString() == Store.pin(this)) showPanel()
                else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .show()
    }

    private fun showPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        root.addView(TextView(this).apply {
            text = "LIVEN SALES — ADMINISTRATOR"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        root.addView(Button(this).apply { text = "Add New Product"; setOnClickListener { editProduct(null) } })
        root.addView(Button(this).apply { text = "Manage Distributors"; setOnClickListener { manageNames("distributors", "Distributors") } })
        root.addView(Button(this).apply { text = "Manage Salespersons"; setOnClickListener { manageNames("salespeople", "Salespersons") } })
        root.addView(Button(this).apply { text = "Change Administrator PIN"; setOnClickListener { changePin() } })
        root.addView(TextView(this).apply {
            text = "PRODUCT MASTER — tap a product to edit details or deactivate it"
            textSize = 13f
            setPadding(0, dp(12), 0, dp(6))
        })
        val products = Store.products(this)
        val list = ListView(this)
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, products.map {
            "${it.brand} ${it.strength} — Rs. ${money(it.rate)}${if (it.active) "" else " (inactive)"}"
        })
        list.setOnItemClickListener { _, _, position, _ -> editProduct(products[position]) }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun money(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.US, "%.2f", value)

    private fun editProduct(existing: Product?) {
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(16), 0) }
        fun input(hintText: String, value: String = "", type: Int = InputType.TYPE_CLASS_TEXT): EditText {
            val edit = EditText(this).apply { hint = hintText; setText(value); inputType = type }
            fields.addView(edit)
            return edit
        }
        val brand = input("Brand name", existing?.brand.orEmpty())
        val ingredient = input("Active ingredient", existing?.ingredient.orEmpty())
        val strength = input("Strength", existing?.strength.orEmpty())
        val pack = input("Pack size", existing?.pack.orEmpty())
        val form = input("Dosage form", existing?.form.orEmpty())
        val rate = input("Net price", existing?.rate?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val active = CheckBox(this).apply { text = "Product active"; isChecked = existing?.active ?: true }
        fields.addView(active)
        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add product" else "Edit product")
            .setView(ScrollView(this).apply { addView(fields) })
            .setPositiveButton("Save") { _, _ ->
                val parsedRate = rate.text.toString().toDoubleOrNull()
                if (brand.text.isBlank() || parsedRate == null || parsedRate < 0) {
                    Toast.makeText(this, "Brand and a valid non-negative rate are required", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val all = Store.products(this)
                if (existing == null) {
                    val id = (all.maxOfOrNull { it.id } ?: 0) + 1
                    all.add(Product(id, brand.text.toString().trim(), ingredient.text.toString().trim(), strength.text.toString().trim(), pack.text.toString().trim(), form.text.toString().trim(), parsedRate, active.isChecked))
                } else {
                    val product = all.firstOrNull { it.id == existing.id } ?: return@setPositiveButton
                    product.brand = brand.text.toString().trim()
                    product.ingredient = ingredient.text.toString().trim()
                    product.strength = strength.text.toString().trim()
                    product.pack = pack.text.toString().trim()
                    product.form = form.text.toString().trim()
                    product.rate = parsedRate
                    product.active = active.isChecked
                }
                Store.saveProducts(this, all)
                showPanel()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun manageNames(key: String, title: String) {
        val names = Store.names(this, key)
        val entry = EditText(this).apply { hint = "Enter name to add" }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Current: ${names.joinToString(", ").ifBlank { "none" }}")
            .setView(entry)
            .setPositiveButton("Add") { _, _ ->
                val name = entry.text.toString().trim()
                if (name.isNotEmpty() && names.none { it.equals(name, true) }) names.add(name)
                Store.saveNames(this, key, names)
                showPanel()
            }
            .setNeutralButton("Clear list") { _, _ -> Store.saveNames(this, key, emptyList()); showPanel() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePin() {
        val entry = EditText(this).apply {
            hint = "New numeric PIN (at least 4 digits)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Change administrator PIN")
            .setView(entry)
            .setPositiveButton("Save") { _, _ ->
                val newPin = entry.text.toString()
                if (newPin.length >= 4) {
                    Store.setPin(this, newPin)
                    Toast.makeText(this, "Administrator PIN updated", Toast.LENGTH_LONG).show()
                } else Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
