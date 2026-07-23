package persistence.jpa.metrics

import com.kraftadmin.annotations.KraftAdminMetric
import com.kraftadmin.enums.MetricAggregation
import com.kraftadmin.enums.ProviderType
import com.kraftadmin.spi.KraftMetricProvider
import com.kraftadmin.spi.MetricBucket
import com.kraftadmin.spi.MetricGroup
import com.kraftadmin.spi.MetricResult
import jakarta.persistence.EntityManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import persistence.metrics.MetricPeriodMath
import persistence.metrics.buildGroupedResult
import persistence.metrics.buildSnapshotResult
import persistence.metrics.buildTimeSeriesResult
import java.lang.reflect.Field
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

@Component
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class JpaMetricProvider(
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate
) : KraftMetricProvider {

    override fun supports(providerType: ProviderType) = providerType == ProviderType.JPA

    override fun computeMetric(
        entityClass: Class<*>,
        metric: KraftAdminMetric,
        now: Instant
    ): MetricResult {
        return when {
            metric.groupByField.isNotBlank() -> computeGrouped(entityClass, metric)
            metric.dateField.isNotBlank() -> computeTimeSeries(entityClass, metric, now)
            else -> computeSnapshot(entityClass, metric)
        }
    }

    /**
     * GROUPED mode — real GROUP BY, aggregation pushed to the database.
     * groupByField may be a scalar ("email") or a relation path
     * ("post.title") — JPQL treats both identically via dotted paths.
     */
//    private fun computeGrouped(entityClass: Class<*>, metric: KraftAdminMetric): MetricResult {
//        val alias = "e"
//        val groupExpr = "$alias.${metric.groupByField}"
//
//        val aggExpr = when (metric.type) {
//            MetricAggregation.COUNT -> "COUNT($alias)"
//            MetricAggregation.SUM -> "COALESCE(SUM($alias.${metric.valueField}), 0)"
//            MetricAggregation.AVG -> "COALESCE(AVG($alias.${metric.valueField}), 0)"
//            MetricAggregation.MIN -> "MIN($alias.${metric.valueField})"
//            MetricAggregation.MAX -> "MAX($alias.${metric.valueField})"
//        }
//
//        val jpql = "SELECT $groupExpr, $aggExpr FROM ${entityClass.simpleName} $alias " +
//                "WHERE $groupExpr IS NOT NULL GROUP BY $groupExpr ORDER BY $aggExpr DESC"
//
//        val rows: List<Array<Any?>> = transactionTemplate.execute {
//            @Suppress("UNCHECKED_CAST")
//            entityManager.createQuery(jpql, Array<Any>::class.java)
//                .setMaxResults(metric.groupByLimit)
//                .resultList as List<Array<Any?>>
//        } ?: emptyList()
//
//        val groups = rows.map { row ->
//            val key = row[0]?.toString() ?: "—"
//            val value = (row[1] as? Number)?.toDouble() ?: 0.0
//            MetricGroup(key = key, label = key, value = value)
//        }
//
//        return buildGroupedResult(metric, groups)
//    }

    private fun computeGrouped(entityClass: Class<*>, metric: KraftAdminMetric): MetricResult {
        val alias = "e"
        val groupExpr = "$alias.${metric.groupByField}"

        val aggExpr = when (metric.type) {
            MetricAggregation.COUNT -> "COUNT($alias)"
            MetricAggregation.SUM -> "COALESCE(SUM($alias.${metric.valueField}), 0)"
            MetricAggregation.AVG -> "COALESCE(AVG($alias.${metric.valueField}), 0)"
            MetricAggregation.MIN -> "MIN($alias.${metric.valueField})"
            MetricAggregation.MAX -> "MAX($alias.${metric.valueField})"
        }

        val jpql = "SELECT $groupExpr, $aggExpr FROM ${entityClass.simpleName} $alias " +
                "WHERE $groupExpr IS NOT NULL GROUP BY $groupExpr ORDER BY $aggExpr DESC"

        @Suppress("UNCHECKED_CAST")
        val rows: List<Array<Any?>> = transactionTemplate.execute {
            entityManager.createQuery(jpql)          // untyped — no Array<Any>::class.java
                .setMaxResults(metric.groupByLimit)
                .resultList as List<Array<Any?>>
        } ?: emptyList()

        val groups = rows.map { row ->
            val key = row[0]?.toString() ?: "—"
            val value = (row[1] as? Number)?.toDouble() ?: 0.0
            MetricGroup(key = key, label = key, value = value)
        }

        return buildGroupedResult(metric, groups)
    }

    private fun computeSnapshot(entityClass: Class<*>, metric: KraftAdminMetric): MetricResult {
        val value = transactionTemplate.execute {
            val jpql = when (metric.type) {
                MetricAggregation.COUNT -> "SELECT COUNT(e) FROM ${entityClass.simpleName} e"
                MetricAggregation.SUM -> "SELECT COALESCE(SUM(e.${metric.valueField}), 0) FROM ${entityClass.simpleName} e"
                MetricAggregation.AVG -> "SELECT COALESCE(AVG(e.${metric.valueField}), 0) FROM ${entityClass.simpleName} e"
                MetricAggregation.MIN -> "SELECT MIN(e.${metric.valueField}) FROM ${entityClass.simpleName} e"
                MetricAggregation.MAX -> "SELECT MAX(e.${metric.valueField}) FROM ${entityClass.simpleName} e"
            }
            val result = entityManager.createQuery(jpql, Number::class.java).singleResult
            result?.toDouble() ?: 0.0
        } ?: 0.0

        return buildSnapshotResult(metric, value)
    }

    private fun computeTimeSeries(entityClass: Class<*>, metric: KraftAdminMetric, now: Instant): MetricResult {
        val buckets = MetricPeriodMath.buckets(now, metric.period)
        val earliest = buckets.first().first

        val entities: List<Any> = transactionTemplate.execute {
            val jpql = "SELECT e FROM ${entityClass.simpleName} e WHERE e.${metric.dateField} >= :earliest"
            val query = entityManager.createQuery(jpql, entityClass)
            query.setParameter("earliest", toComparableTime(earliest, entityClass, metric.dateField))
            query.resultList
        } ?: emptyList()

        val resultBuckets = buckets.map { (start, end) ->
            val inRange = entities.filter { entity ->
                val ts = readInstant(entity, metric.dateField)
                ts != null && !ts.isBefore(start) && ts.isBefore(end)
            }
            MetricBucket(
                periodStart = start,
                label = MetricPeriodMath.label(start, metric.period),
                value = aggregateInMemory(inRange, metric)
            )
        }

        return buildTimeSeriesResult(metric, resultBuckets)
    }

    private fun aggregateInMemory(entities: List<Any>, metric: KraftAdminMetric): Double {
        if (metric.type == MetricAggregation.COUNT) return entities.size.toDouble()
        val values = entities.mapNotNull { readNumber(it, metric.valueField) }
        return when (metric.type) {
            MetricAggregation.SUM -> values.sum()
            MetricAggregation.AVG -> if (values.isEmpty()) 0.0 else values.average()
            MetricAggregation.MIN -> values.minOrNull() ?: 0.0
            MetricAggregation.MAX -> values.maxOrNull() ?: 0.0
            MetricAggregation.COUNT -> entities.size.toDouble()
        }
    }

    private fun readInstant(entity: Any, fieldName: String): Instant? {
        val field = findField(entity::class.java, fieldName) ?: return null
        field.isAccessible = true
        return when (val value = field.get(entity)) {
            is Instant -> value
            is LocalDateTime -> value.toInstant(ZoneOffset.UTC)
            is LocalDate -> value.atStartOfDay().toInstant(ZoneOffset.UTC)
            is Date -> value.toInstant()
            else -> null
        }
    }

    private fun readNumber(entity: Any, fieldName: String): Double? {
        val field = findField(entity::class.java, fieldName) ?: return null
        field.isAccessible = true
        return (field.get(entity) as? Number)?.toDouble()
    }

    private fun findField(clazz: Class<*>, name: String): Field? {
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            current.declaredFields.find { it.name == name }?.let { return it }
            current = current.superclass
        }
        return null
    }

    private fun toComparableTime(instant: Instant, entityClass: Class<*>, fieldName: String): Any {
        val field = findField(entityClass, fieldName)
        return when (field?.type) {
            LocalDateTime::class.java -> LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
            LocalDate::class.java -> LocalDate.ofInstant(instant, ZoneOffset.UTC)
            Date::class.java -> Date.from(instant)
            else -> instant
        }
    }
}