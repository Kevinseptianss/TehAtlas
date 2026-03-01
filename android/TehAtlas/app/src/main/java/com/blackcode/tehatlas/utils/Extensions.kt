package com.blackcode.tehatlas.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Formats a Double as Indonesian Rupiah (Rp) without decimals and with thousand separators.
 * Example: 10000.0 -> "Rp 10.000"
 */
fun Double?.formatRp(): String {
    val value = this ?: 0.0
    val localeID = Locale("in", "ID")
    val numberFormat = NumberFormat.getCurrencyInstance(localeID)
    numberFormat.maximumFractionDigits = 0
    
    // Some locales might add ",00" or other symbols, so we ensure standard format
    // or manually format to guarantee "Rp 10.000" style
    return try {
        val formatted = numberFormat.format(value)
        // Ensure "Rp" prefix and no decimal
        formatted.replace(",00", "").replace("Rp", "Rp ")
    } catch (e: Exception) {
        "Rp 0"
    }
}

/**
 * Formats an Int as Indonesian Rupiah (Rp).
 */
fun Int?.formatRp(): String = this?.toDouble().formatRp()

/**
 * Formats a Long as Indonesian Rupiah (Rp).
 */
fun Long?.formatRp(): String = this?.toDouble().formatRp()
