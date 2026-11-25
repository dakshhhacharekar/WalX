package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

object FinanceMath {
    fun roundCurrency(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        val rounded = round(value * 100.0) / 100.0
        return if (rounded == -0.0) 0.0 else rounded
    }

    fun safePercent(part: Double, total: Double): Double {
        if (part.isNaN() || part.isInfinite() || total.isNaN() || total.isInfinite() || total == 0.0) {
            return 0.0
        }
        val pct = (part / total) * 100.0
        if (pct.isNaN() || pct.isInfinite()) return 0.0
        return roundCurrency(pct)
    }

    fun safeMomChange(current: Double, previous: Double): Double {
        if (current.isNaN() || previous.isNaN() || current.isInfinite() || previous.isInfinite()) return 0.0
        if (previous == 0.0) return if (current > 0.0) 100.0 else 0.0
        return roundCurrency(((current - previous) / abs(previous)) * 100.0)
    }

    fun formatCurrency(
        amount: Double,
        currency: CurrencyCode,
        showSign: Boolean = false,
        compact: Boolean = false
    ): String {
        val safe = roundCurrency(amount)
        val isNeg = safe < 0.0
        val absVal = abs(safe)
        val signPrefix = when {
            isNeg -> "-"
            showSign && safe > 0.0 -> "+"
            else -> ""
        }

        if (compact) {
            return when {
                currency == CurrencyCode.INR && absVal >= 10_000_000.0 ->
                    "$signPrefix${currency.symbol}${formatPlain(absVal / 10_000_000.0, 2)}Cr"
                currency == CurrencyCode.INR && absVal >= 100_000.0 ->
                    "$signPrefix${currency.symbol}${formatPlain(absVal / 100_000.0, 2)}L"
                absVal >= 1_000_000.0 ->
                    "$signPrefix${currency.symbol}${formatPlain(absVal / 1_000_000.0, 2)}M"
                absVal >= 1_000.0 ->
                    "$signPrefix${currency.symbol}${formatPlain(absVal / 1_000.0, 1)}k"
                else -> formatFullCurrency(signPrefix, absVal, currency)
            }
        }
        return formatFullCurrency(signPrefix, absVal, currency)
    }

    private fun formatFullCurrency(signPrefix: String, absVal: Double, currency: CurrencyCode): String {
        return when (currency) {
            CurrencyCode.INR -> "$signPrefix₹${formatIndianNumber(absVal)}"
            CurrencyCode.JPY -> {
                val df = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
                "$signPrefix¥${df.format(round(absVal))}"
            }
            else -> {
                val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
                "$signPrefix${currency.symbol}${df.format(absVal)}"
            }
        }
    }

    private fun formatIndianNumber(value: Double): String {
        val rounded = roundCurrency(value)
        val whole = rounded.toLong()
        val frac = round((rounded - whole) * 100.0).toInt().coerceIn(0, 99)
        val wholeStr = whole.toString()
        val formattedWhole = if (wholeStr.length <= 3) {
            wholeStr
        } else {
            val lastThree = wholeStr.takeLast(3)
            val rest = wholeStr.dropLast(3)
            val groupedRest = rest.reversed().chunked(2).joinToString(",").reversed()
            "$groupedRest,$lastThree"
        }
        return "$formattedWhole.${frac.toString().padStart(2, '0')}"
    }

    private fun formatPlain(v: Double, decimals: Int): String {
        return String.format(Locale.US, "%.${decimals}f", v).trimEnd('0').trimEnd('.')
    }
}

object WalXDateUtils {
    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    private val shortDayFormat: SimpleDateFormat
        get() = SimpleDateFormat("dd MMM", Locale.US)

    private val monthShortFormat: SimpleDateFormat
        get() = SimpleDateFormat("MMM", Locale.US)

    fun todayIso(): String = isoFormat.format(Calendar.getInstance().time)

    fun daysAgoIso(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return isoFormat.format(cal.time)
    }

    fun daysFromNowIso(daysFromNow: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysFromNow)
        return isoFormat.format(cal.time)
    }

    fun monthsAgoIso(monthsAgo: Int, dayOfMonth: Int = 15): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthsAgo)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
        return isoFormat.format(cal.time)
    }

    fun currentYearMonth(): String = todayIso().take(7)

    fun previousYearMonth(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        return isoFormat.format(cal.time).take(7)
    }

    fun daysRemainingInCurrentMonth(): Int {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (totalDays - today + 1).coerceAtLeast(1)
    }

    fun isValidIsoDate(date: String): Boolean {
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(date)) return false
        return try {
            isoFormat.parse(date) != null
        } catch (_: Exception) {
            false
        }
    }

    fun formatFriendlyDate(isoDate: String): String {
        return try {
            val parsed = isoFormat.parse(isoDate) ?: return isoDate
            shortDayFormat.format(parsed)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatMonthLabel(yearMonth: String): String {
        return try {
            val parsed = isoFormat.parse("$yearMonth-01") ?: return yearMonth
            monthShortFormat.format(parsed)
        } catch (_: Exception) {
            yearMonth
        }
    }

    fun advanceDate(isoDate: String, frequency: RecurringFrequency): String {
        val cal = Calendar.getInstance()
        try {
            val parsed = isoFormat.parse(isoDate)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
            // Keep today
        }
        when (frequency) {
            RecurringFrequency.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
            RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return isoFormat.format(cal.time)
    }

    fun lastNYearMonths(count: Int): List<String> {
        val result = mutableListOf<String>()
        for (i in (count - 1) downTo 0) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MONTH, -i)
            result.add(isoFormat.format(cal.time).take(7))
        }
        return result
    }

    fun lastNDaysIso(count: Int): List<String> {
        return ((count - 1) downTo 0).map { daysAgoIso(it) }
    }
}
