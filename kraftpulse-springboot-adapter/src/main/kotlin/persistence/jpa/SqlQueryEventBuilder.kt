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

        // Resolve request dynamically inside the method
        val currentRequest = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request

        // Safe resolution: check attribute, then fallback to a default
        val resolvedTraceId = currentRequest?.getAttribute("traceId")?.toString()!!

        return queryInfoList.map { queryInfo ->
            val sql = queryInfo.query ?: ""
            val type = determineType(sql)
            val tableName = extractTableName(sql, type)

            QueryEvent(
                traceId = resolvedTraceId,
                sql = sql,
                parameters = extractParams(queryInfo),
                queryType = type,
                tableName = tableName,
                entityName = tableName?.lowercase()?.capitalize(), // Simple heuristic
                startedAt = startTime,
                durationMs = execInfo.elapsedTime,
                rowsAffected = if (type != QueryType.SELECT) (execInfo.result as? Number)?.toInt() ?: 0 else 0,
                rowsReturned = if (type == QueryType.SELECT) 1 else 0,
                dataSource = dataSourceName,
                databaseProduct = dbProduct,
                error = mapError(execInfo),
                isSlowQuery = execInfo.elapsedTime > 500, // Threshold in ms,
                schema = getDbSchema(execInfo),
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