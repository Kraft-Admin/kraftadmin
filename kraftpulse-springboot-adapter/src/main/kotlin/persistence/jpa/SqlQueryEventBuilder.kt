package persistence.jpa

import model.QueryError
import model.QueryEvent
import model.QueryType
import net.ttddyy.dsproxy.ExecutionInfo
import net.ttddyy.dsproxy.QueryInfo
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class SqlQueryEventBuilder {

    val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request

    fun buildEvents(execInfo: ExecutionInfo, queryInfoList: List<QueryInfo>, dataSourceName: String): List<QueryEvent> {
        val startTime = System.currentTimeMillis() - execInfo.elapsedTime
        val dbProduct = getDbProduct(execInfo)
        val currentRequest = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request

        // Resolve contextual data
        val resolvedTraceId = currentRequest?.getAttribute("traceId")?.toString() ?: "system"
        val tenantId = currentRequest?.getAttribute("tenantId")?.toString() ?: "default"
        val threadName = Thread.currentThread().name
        val isolationLevel = execInfo.isolationLevel.toString() // Or use a mapping function for names

        return queryInfoList.map { queryInfo ->
            val sql = queryInfo.query ?: ""
            val type = determineType(sql)
            val tableName = extractTableName(sql, type)

            QueryEvent(
                id = java.util.UUID.randomUUID().toString(),
                traceId = resolvedTraceId,
                sql = sql,
                parameters = extractParams(queryInfo),
                queryType = type,
                tableName = tableName,
                entityName = tableName?.lowercase()?.capitalize(),
                startedAt = startTime,
                durationMs = execInfo.elapsedTime,
                rowsAffected = if (type != QueryType.SELECT) (execInfo.result as? Number)?.toInt() ?: 0 else 0,
                rowsReturned = if (type == QueryType.SELECT) 1 else 0,
                isSlowQuery = execInfo.elapsedTime > 500,
                isPotentialNPlusOne = false, // You may want to integrate your detection logic here
                error = mapError(execInfo),
                dataSource = dataSourceName,
                databaseProduct = dbProduct,
                schema = getDbSchema(execInfo),
                tenantId = tenantId,
                threadName = threadName,
                isolationLevel = isolationLevel,
                isReadOnly = try { execInfo.statement?.connection?.isReadOnly ?: false } catch (e: Exception) { false },
                isBatch = execInfo.isBatch,
                batchSize = if (execInfo.isBatch) execInfo.batchSize else null,
                transactionId = currentRequest?.getAttribute("transactionId")?.toString(),
                executionPlan = null // Optional: Only fetch if isSlowQuery is true
            )
        }
    }

    private fun extractTableName(sql: String, type: QueryType): String? {
        return try {
            val normalized = sql.replace(Regex("\\s+"), " ").uppercase()
            when (type) {
                QueryType.SELECT -> normalized.substringAfter("FROM ").substringBefore(" ").substringBefore(",")
                QueryType.INSERT -> normalized.substringAfter("INTO ").substringBefore(" ").substringBefore("(")
                QueryType.UPDATE -> normalized.substringAfter("UPDATE ").substringBefore(" ")
                QueryType.DELETE -> normalized.substringAfter("FROM ").substringBefore(" ")
                else -> null
            }?.removeSurrounding("\"")?.removeSurrounding("`")?.trim()
        } catch (e: Exception) {
            null
        }
    }

    private fun getDbProduct(execInfo: ExecutionInfo): String? {
        return try {
            // Reaching into the JDBC connection metadata
            execInfo.statement?.connection?.metaData?.databaseProductName
        } catch (e: Exception) {
            null
        }
    }

    private fun extractParams(info: QueryInfo): List<String?> {
        return info.parametersList.flatMap { batch ->
            batch.map { it.args?.contentToString() }
        }
    }

    private fun mapError(info: ExecutionInfo): QueryError? {
        val ex = info.throwable ?: return null
        return QueryError(
            sqlState = (ex as? java.sql.SQLException)?.sqlState,
            errorCode = (ex as? java.sql.SQLException)?.errorCode ?: 0,
            message = ex.message ?: "Unknown Error",
            exceptionClass = ex.javaClass.simpleName
        )
    }

    private fun determineType(sql: String?): QueryType {
        val firstWord = sql?.trimStart()?.take(10)?.uppercase() ?: ""
        return when {
            firstWord.startsWith("SELECT") -> QueryType.SELECT
            firstWord.startsWith("INSERT") -> QueryType.INSERT
            firstWord.startsWith("UPDATE") -> QueryType.UPDATE
            firstWord.startsWith("DELETE") -> QueryType.DELETE
            else -> QueryType.UNKNOWN
        }
    }

    private fun getDbSchema(execInfo: ExecutionInfo): String? {
        return try {
            // Most JDBC drivers provide the current schema on the connection object
            execInfo.statement?.connection?.schema
        } catch (e: Exception) {
            null
        }
    }
}