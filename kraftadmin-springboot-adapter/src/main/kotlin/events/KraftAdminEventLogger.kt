package events

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.events.KraftEventConsumer
import com.kraftadmin.logging.KraftAdminLogging
import jakarta.annotation.PreDestroy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class KraftAdminEventLogger : KraftEventConsumer {

    private val logger = KraftAdminLogging.logger(javaClass)


    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val directoryFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC)
    private val hourFormatter = DateTimeFormatter.ofPattern("HH").withZone(ZoneOffset.UTC)
    private val hourKeyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH").withZone(ZoneOffset.UTC)

    // Bounded queue: producers (request threads) NEVER block on disk I/O.
    // consume() just serializes to a String (cheap, in-memory, no locks) and
    // offers it to this queue — the actual write happens on a dedicated
    // background thread, entirely off the request path.
    private val queue = ArrayBlockingQueue<String>(10_000)

    // Single writer thread — no lock contention needed since there's only
    // ever one consumer of the queue and one owner of the BufferedWriter.
    private val writerExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "kraftadmin-event-writer").apply { isDaemon = true }
    }

    @Volatile
    private var running = true

    private var currentHourKey = ""
    private var writer: BufferedWriter? = null

    // Batch flushing: flush on a timer/count basis instead of every single
    // event. Trades a small, bounded durability window (worst case: last
    // ~500ms or ~200 events lost on a hard crash) for a massive reduction in
    // disk I/O
    private var pendingSinceFlush = 0
    private val flushEveryN = 200
    private val flushEveryMillis = 500L
    private var lastFlushAt = System.currentTimeMillis()

    init {
        writerExecutor.submit { runWriterLoop() }
    }

    override fun consume(event: KraftAdminEvent) {
        try {
            val jsonMap = linkedMapOf<String, Any?>(
                "timestamp" to Instant.now(),
                "type" to event::class.simpleName
            )
            jsonMap.putAll(event.toMap())

            val jsonString = mapper.writeValueAsString(jsonMap)

            // Non-blocking handoff. If the queue is ever full (writer thread
            // falling behind, or disk stalled), DROP the event rather than
            // block the request thread — an event logger must never be able
            // to slow down or fail a real request.
            if (!queue.offer(jsonString)) {
                logger.warn("KraftAdminEventLogger: queue full, dropping event {}", event::class.simpleName)
            }
        } catch (e: Exception) {
            logger.warn("Failed to serialize event {}: {}", event::class.simpleName, e.message)
        }
    }

    private fun runWriterLoop() {
        while (running || queue.isNotEmpty()) {
            try {
                // Block up to flushEveryMillis waiting for the next event —
                // this both keeps the thread idle when there's nothing to do
                // AND gives us a natural periodic-flush tick even under low
                // event volume.
                val line = queue.poll(flushEveryMillis, TimeUnit.MILLISECONDS)

                if (line != null) {
                    val activeWriter = getOrUpdateWriter()
                    activeWriter.write(line)
                    activeWriter.newLine()
                    pendingSinceFlush++
                }

                val now = System.currentTimeMillis()
                val shouldFlush = pendingSinceFlush >= flushEveryN ||
                        (pendingSinceFlush > 0 && now - lastFlushAt >= flushEveryMillis)

                if (shouldFlush) {
                    writer?.flush()
                    pendingSinceFlush = 0
                    lastFlushAt = now
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                logger.warn("KraftAdminEventLogger: write failed: {}", e.message)
            }
        }

        // Final drain + flush on shutdown
        try {
            writer?.flush()
            writer?.close()
        } catch (e: Exception) {
            // ignore close errors on shutdown
        }
    }

    private fun getOrUpdateWriter(): BufferedWriter {
        val now = Instant.now()
        val hourKey = hourKeyFormatter.format(now)

        if (writer != null && currentHourKey == hourKey) {
            return writer!!
        }

        try {
            writer?.flush()
            writer?.close()
        } catch (e: Exception) {
            // ignore close errors on rotation
        }

        val directory = Path.of("kraftadmin", "events", directoryFormatter.format(now))
        Files.createDirectories(directory)
        val file = directory.resolve("${hourFormatter.format(now)}.ndjson")

        writer = Files.newBufferedWriter(
            file,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
            StandardOpenOption.WRITE
        )
        currentHourKey = hourKey
        return writer!!
    }

    @PreDestroy
    fun cleanup() {
        running = false
        writerExecutor.shutdown()
        try {
            // Give the writer thread a bounded window to drain the queue and
            // flush before the JVM exits.
            writerExecutor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}