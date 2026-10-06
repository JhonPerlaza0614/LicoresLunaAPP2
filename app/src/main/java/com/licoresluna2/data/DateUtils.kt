package com.licoresluna2.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    } catch (e: Exception) {
        iso
    }
}
