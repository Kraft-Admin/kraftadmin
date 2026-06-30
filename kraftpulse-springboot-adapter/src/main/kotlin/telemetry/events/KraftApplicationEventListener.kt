package telemetry.events

import interceptor.PulseTelemetryCaptor
import model.*
import org.springframework.context.ApplicationEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import util.PulseContextHolder
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean
import java.util.UUID

@Component
class KraftApplicationEventListener(
    private val captor: PulseTelemetryCaptor
) {

    @EventListener
    fun handleSpringEvent(event: ApplicationEvent) {
        val eventName = event.javaClass.simpleName
        if (eventName.startsWith("Servlet") || eventName.startsWith("Availability")) return

        val currentContext = PulseContextHolder.get()
        val currentTraceId = currentContext?.traceId ?: "event-${UUID.randomUUID()}"

        // Capture a snapshot of current system resources
        val osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val usage = ResourceUsage(
            cpuUsagePercent = (osBean as com.sun.management.OperatingSystemMXBean).processCpuLoad * 100,
            memoryUsedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
            threadCount = Thread.activeCount()
        )

        captor.recordTask(
            KraftTaskEvent(
                traceId = currentTraceId,
                name = eventName,
                type = KraftTaskType.APPLICATION_EVENT,
                status = KraftTaskStatus.EMITTED,
                resourceUsage = usage, // Captured snapshot
                nodeIdentifier = System.getenv("HOSTNAME") ?: "unknown-node",
                triggerSource = "SpringApplicationEvent",
                taskMetadata = mapOf(
                    "event_source" to event.source.javaClass.name,
                    "event_timestamp" to event.timestamp.toString()
                ),
            )
        )
    }
}