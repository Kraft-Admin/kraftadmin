package com.kraftadmin.spi

import com.kraftadmin.annotations.KraftAdminMetric
import com.kraftadmin.enums.ProviderType
import java.time.Instant

interface KraftMetricProvider {
    fun supports(providerType: ProviderType): Boolean

    fun computeMetric(
        entityClass: Class<*>,
        metric: KraftAdminMetric,
        now: Instant = Instant.now()
    ): MetricResult
}

enum class MetricMode { SNAPSHOT, TIME_SERIES, GROUPED }

data class MetricBucket(
    val periodStart: Instant,
    val label: String,
    val value: Double
)

data class MetricGroup(
    val key: String,     // raw group value, e.g. "jane@example.com" or "My First Post"
    val label: String,   // display label — usually same as key, but room to diverge later
    val value: Double
)

data class MetricResult(
    val name: String,
    val label: String,
    val chartType: String,
    val mode: MetricMode,
    /** Populated only when mode == TIME_SERIES. */
    val buckets: List<MetricBucket> = emptyList(),
    /** Populated only when mode == GROUPED, ordered by value descending, capped at groupByLimit. */
    val groups: List<MetricGroup> = emptyList(),
    /** Populated only when mode == SNAPSHOT or TIME_SERIES (current bucket value). */
    val currentPeriodValue: Double = 0.0,
    val previousPeriodValue: Double? = null,
    val growthPercent: Double? = null
)

data class DiscoveredMetric(
    val entityClass: Class<*>,
    val provider: ProviderType,
    val metric: KraftAdminMetric
)