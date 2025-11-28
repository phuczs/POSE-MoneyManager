package com.example.moneymanager.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Hàm mở rộng để định dạng số Double thành chuỗi tiền tệ.
 * @param currencyCode Mã tiền tệ ("VND" hoặc "USD"). Mặc định là "VND".
 * Ví dụ:
 * 100000.0.toCurrencyString() -> "100.000 ₫"
 * 50.0.toCurrencyString("USD") -> "$50.00"
 */
fun Double.toCurrencyString(currencyCode: String = "VND"): String {
    val format = if (currencyCode.equals("USD", ignoreCase = true)) {
        NumberFormat.getCurrencyInstance(Locale.US)
    } else {
        NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    }
    return format.format(this)
}

/**
 * Hàm mở rộng cho kiểu Long.
 */
fun Long.toCurrencyString(currencyCode: String = "VND"): String {
    return this.toDouble().toCurrencyString(currencyCode)
}

/**
 * Hàm mở rộng cho kiểu Int.
 */
fun Int.toCurrencyString(currencyCode: String = "VND"): String {
    return this.toDouble().toCurrencyString(currencyCode)
}