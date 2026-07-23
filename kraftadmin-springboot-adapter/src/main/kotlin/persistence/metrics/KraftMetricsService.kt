package com.kraftadmin.persistence.metrics

import com.kraftadmin.spi.DiscoveredMetric
import com.kraftadmin.spi.KraftMetricProvider
import com.kraftadmin.spi.MetricResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service


@Service
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class KraftMetricService(
    private val metricProviders: List<KraftMetricProvider>
) {

    fun compute(discoveredMetrics: List<DiscoveredMetric>): List<MetricResult> =
        discoveredMetrics.mapNotNull { dm ->
            metricProviders.firstOrNull { it.supports(dm.provider) }
                ?.computeMetric(dm.entityClass, dm.metric)
        }

}