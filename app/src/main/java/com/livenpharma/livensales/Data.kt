package com.livenpharma.livensales

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Product(
    val id: Int,
    var brand: String,
    var ingredient: String,
    var strength: String,
    var pack: String,
    var form: String,
    var rate: Double,
    var active: Boolean = true
) {
    fun toJson() = JSONObject()
        .put("id", id).put("brand", brand).put("ingredient", ingredient)
        .put("strength", strength).put("pack", pack).put("form", form)
        .put("rate", rate).put("active", active)

    fun display() = listOf(brand, strength, pack, form).filter { it.isNotBlank() }.joinToString(" | ")
}

object Store {
    private const val PREFS = "liven_sales_store"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun products(context: Context): MutableList<Product> {
        val preferences = prefs(context)
        var raw = preferences.getString("products", null)
        if (raw == null) {
            raw = context.assets.open("products.json").bufferedReader().use { it.readText() }
            preferences.edit().putString("products", raw).apply()
        }
        val array = JSONArray(raw)
        return MutableList(array.length()) { index ->
            val item = array.getJSONObject(index)
            Product(
                item.optInt("id"), item.optString("brand"), item.optString("ingredient"),
                item.optString("strength"), item.optString("pack"), item.optString("form"),
                item.optDouble("rate"), item.optBoolean("active", true)
            )
        }
    }

    fun saveProducts(context: Context, products: List<Product>) {
        val array = JSONArray()
        products.forEach { array.put(it.toJson()) }
        prefs(context).edit().putString("products", array.toString()).apply()
    }

    fun names(context: Context, key: String): MutableList<String> {
        val raw = prefs(context).getString(key, null) ?: return mutableListOf()
        val array = JSONArray(raw)
        return MutableList(array.length()) { array.getString(it) }
    }

    fun saveNames(context: Context, key: String, names: List<String>) {
        val array = JSONArray()
        names.forEach { array.put(it) }
        prefs(context).edit().putString(key, array.toString()).apply()
    }

    fun pin(context: Context) = prefs(context).getString("admin_pin", "1234") ?: "1234"
    fun setPin(context: Context, value: String) { prefs(context).edit().putString("admin_pin", value).apply() }
}
