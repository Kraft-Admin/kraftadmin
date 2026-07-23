package persistence.metrics

import com.kraftadmin.enums.MetricPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object MetricPeriodMath {

    private val ZONE = ZoneOffset.UTC
    const val DEFAULT_LOOKBACK = 12 // annotation has no lookback field — fixed default for now

    fun truncate(instant: Instant, period: MetricPeriod): Instant {
        val date = instant.atZone(ZONE).toLocalDate()
        val truncated = when (period) {
            MetricPeriod.DAY -> date
            MetricPeriod.WEEK -> date.minusDays((date.dayOfWeek.value - 1).toLong())
            MetricPeriod.MONTH -> date.withDayOfMonth(1)
            MetricPeriod.QUARTER -> {
                val quarterStartMonth = ((date.monthValue - 1) / 3) * 3 + 1
                LocalDate.of(date.year, quarterStartMonth, 1)
            }
            MetricPeriod.YEAR -> LocalDate.of(date.year, 1, 1)
        }
        return truncated.atStartOfDay(ZONE).toInstant()
    }

    private fun advance(instant: Instant, period: MetricPeriod, amount: Long): Instant {
        val zdt = instant.atZone(ZONE)
        val advanced = when (period) {
            MetricPeriod.DAY -> zdt.plusDays(amount)
            MetricPeriod.WEEK -> zdt.plusWeeks(amount)
            MetricPeriod.MONTH -> zdt.plusMonths(amount)
            MetricPeriod.QUARTER -> zdt.plusMonths(amount * 3)
            MetricPeriod.YEAR -> zdt.plusYears(amount)
        }
        return advanced.toInstant()
    }

    fun label(instant: Instant, period: MetricPeriod): String {
        val zdt = instant.atZone(ZONE)
        return when (period) {
            MetricPeriod.DAY -> zdt.format(DateTimeFormatter.ofPattern("MMM d"))
            MetricPeriod.WEEK -> "Wk of " + zdt.format(DateTimeFormatter.ofPattern("MMM d"))
            MetricPeriod.MONTH -> zdt.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            MetricPeriod.QUARTER -> "Q${(zdt.monthValue - 1) / 3 + 1} ${zdt.year}"
            MetricPeriod.YEAR -> zdt.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    }

    fun buckets(now: Instant, period: MetricPeriod, lookback: Int = DEFAULT_LOOKBACK): List<Pair<Instant, Instant>> {
        val currentStart = truncate(now, period)
        return (lookback - 1 downTo 0).map { i ->
            val start = advance(currentStart, period, -i.toLong())
            val end = advance(start, period, 1)
            start to end
        }
    }

}