package com.licoresluna2.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidades de fecha compatibles desde Android 7.0 (API 24) en adelante.
 * Produce el formato YYYY-MM-DD sin depender de java.time (requerido para APIs < 26).
 */
fun localDateKey(date: Date = Date()): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(date)
}

/**
 * Extrae la fecha YYYY-MM-DD desde un string ISO-8601 de forma compatible desde API 24.
 */
fun dateKeyFromIso(iso: String): String {
    return try {
        if (iso.length >= 10) {
            iso.substring(0, 10)
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(Date())
        }
    } catch (_: Exception) {
        iso
    }
}

/**
 * Formatea una fecha ISO-8601 de Supabase (UTC) como dd/MM/yyyy HH:mm en hora local.
 * Compatible desde Android 7.0 (API 24) sin depender de java.time.
 */
fun formatIsoDateTime(iso: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val display = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CO"))
        val date = parser.parse(iso.take(19)) ?: return iso.take(10)
        display.format(date)
    } catch (e: Exception) {
        iso.take(10)
    }
}
