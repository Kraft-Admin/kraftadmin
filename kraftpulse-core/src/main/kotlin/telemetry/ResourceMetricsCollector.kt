package telemetry.telemetry

import java.lang.management.ManagementFactory
import com.sun.management.OperatingSystemMXBean
import java.io.File

class ResourceMetricsCollector {
    private val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean

    fun collect(): SystemMetricsPayload {
        val totalMemory = osBean.totalMemorySize
        val freeMemory = osBean.freeMemorySize
        val rootFile = File("/")

        return SystemMetricsPayload(
            timestamp = System.currentTimeMillis(),
            cpuUsage = osBean.cpuLoad * 100.0, // System-wide CPU load
            processCpuUsage = osBean.processCpuLoad * 100.0, // JVM specific usage
            memoryUsedBytes = totalMemory - freeMemory,
            memoryTotalBytes = totalMemory,
            diskUsedBytes = rootFile.totalSpace - rootFile.freeSpace,
            diskTotalBytes = rootFile.totalSpace
        )
    }
}

data class SystemMetricsPayload(
    val timestamp: Long,
    val cpuUsage: Double,
    val processCpuUsage: Double,
    val memoryUsedBytes: Long,
    val memoryTotalBytes: Long,
    val diskUsedBytes: Long,
    val diskTotalBytes: Long
)
