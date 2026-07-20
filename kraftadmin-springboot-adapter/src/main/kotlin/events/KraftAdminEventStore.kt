package events

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kraftadmin.model.KraftEventPage
import com.kraftadmin.model.KraftEventRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.isRegularFile

@Service
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class KraftAdminEventStore {

    private val mapper = jacksonObjectMapper()

    private val root = Path.of("kraftadmin", "events")

    fun search(
        query: String? = null,
        type: String? = null,
        resourceName: String? = null,
        actorUsername: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        page: Int = 0,
        size: Int = 50
    ): KraftEventPage {

        if (!Files.exists(root)) {
            return emptyPage(page, size)
        }

        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 200)

        val offset = safePage * safeSize

        val events = mutableListOf<KraftEventRecord>()

        /*
         * Read files newest first.
         *
         * Directory structure:
         *
         * kraftadmin/events/
         * └── 2026/
         *     └── 07/
         *         └── 18/
         *             ├── 18.ndjson
         *             ├── 19.ndjson
         *             └── 20.ndjson
         */
        val files = Files.walk(root).use { paths ->
            paths
                .filter { it.isRegularFile() }
                .filter { it.toString().endsWith(".ndjson") }
                .sorted(Comparator.reverseOrder())
                .toList()
        }

        /*
         * We only need enough matching events to satisfy:
         *
         * page offset + page size
         *
         * This avoids parsing the entire event history for normal requests.
         */
        val required = offset + safeSize

        var hasMoreFiles = true

        for (file in files) {

            if (!hasMoreFiles) {
                break
            }

            try {

                val fileEvents = mutableListOf<KraftEventRecord>()

                Files.newBufferedReader(file).useLines { lines ->

                    lines.forEach { line ->

                        if (line.isBlank()) {
                            return@forEach
                        }

                        try {

                            val node = mapper.readTree(line)

                            if (
                                matches(
                                    node = node,
                                    query = query,
                                    type = type,
                                    resourceName = resourceName,
                                    actorUsername = actorUsername,
                                    from = from,
                                    to = to
                                )
                            ) {
                                fileEvents += toRecord(node)
                            }

                        } catch (_: Exception) {
                            /*
                             * Ignore malformed events.
                             *
                             * A malformed telemetry line should never
                             * break the event viewer.
                             */
                        }
                    }
                }

                /*
                 * Newest files are processed first.
                 * Sort each file so newest events are first.
                 */
                fileEvents.sortByDescending { it.timestamp }

                events += fileEvents

                /*
                 * Once we have enough events for the requested page,
                 * we do not need to read older files.
                 */
                if (events.size >= required) {
                    hasMoreFiles = false
                }

            } catch (_: Exception) {
                /*
                 * Ignore files that cannot be read.
                 */
            }
        }

        /*
         * The files themselves are newest-first, but events within
         * each file have also been sorted newest-first.
         *
         * Sort the collected result to guarantee global ordering.
         */
        val sorted = events
            .sortedByDescending { it.timestamp }

        val content = sorted
            .drop(offset)
            .take(safeSize)

        /*
         * This is an approximate total when using the optimized
         * early-stop strategy.
         *
         * For a live telemetry UI, this is usually preferable to
         * scanning the entire event history on every poll.
         */
        val total = if (sorted.size < required) {
            offset + sorted.size
        } else {
            offset + sorted.size + 1
        }

        return KraftEventPage(
            content = content,
            page = safePage,
            size = safeSize,
            total = total
        )
    }

    private fun matches(
        node: JsonNode,
        query: String?,
        type: String?,
        resourceName: String?,
        actorUsername: String?,
        from: Instant?,
        to: Instant?
    ): Boolean {

        val timestamp = node["timestamp"]
            ?.asText()
            ?.let {
                try {
                    Instant.parse(it)
                } catch (_: Exception) {
                    null
                }
            }

        if (from != null && timestamp != null && timestamp.isBefore(from)) {
            return false
        }

        if (to != null && timestamp != null && timestamp.isAfter(to)) {
            return false
        }

        if (
            !type.isNullOrBlank() &&
            !node["type"]?.asText().equals(type, ignoreCase = true)
        ) {
            return false
        }

        if (
            !resourceName.isNullOrBlank() &&
            !node["resourceName"]?.asText()
                .equals(resourceName, ignoreCase = true)
        ) {
            return false
        }

        if (
            !actorUsername.isNullOrBlank() &&
            !node["actorUsername"]?.asText()
                .equals(actorUsername, ignoreCase = true)
        ) {
            return false
        }

        if (!query.isNullOrBlank()) {

            val raw = node.toString()

            if (!raw.contains(query, ignoreCase = true)) {
                return false
            }
        }

        return true
    }

    private fun toRecord(node: JsonNode): KraftEventRecord {

        val timestamp = node["timestamp"]
            ?.asText()
            ?.let {
                try {
                    Instant.parse(it)
                } catch (_: Exception) {
                    Instant.EPOCH
                }
            }
            ?: Instant.EPOCH

        return KraftEventRecord(
            timestamp = timestamp,
            type = node["type"]?.asText() ?: "Unknown",
            resourceName = node["resourceName"]?.asText(),
            traceId = node["traceId"]?.asText(),
            actorUsername = node["actorUsername"]?.asText(),
            actorRoles = node["actorRoles"]
                ?.map { it.asText() }
                ?.toSet()
                ?: emptySet(),
            tenantId = node["tenantId"]?.asText(),
            ipAddress = node["ipAddress"]?.asText(),
            userAgent = node["userAgent"]?.asText(),
            payload = mapper.convertValue(
                node,
                Map::class.java
            ) as Map<String, Any?>
        )
    }

    private fun emptyPage(
        page: Int,
        size: Int
    ): KraftEventPage {
        return KraftEventPage(
            content = emptyList(),
            page = page,
            size = size,
            total = 0
        )
    }
}