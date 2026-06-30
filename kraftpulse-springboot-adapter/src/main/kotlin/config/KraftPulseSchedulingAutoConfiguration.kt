package config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@AutoConfiguration
@EnableScheduling
@ConditionalOnExpression("\${kraftpulse.enabled:false} and \${kraftpulse.telemetry-config.enabled:false}")
class KraftPulseSchedulingAutoconfiguration {

    // ✅ Named bean — referenced explicitly by @Scheduled(scheduler = "kraftPulseScheduler")
    // This is a SEPARATE thread pool from the parent app's default scheduler,
    // so KraftPulse's flush job can never block or be blocked by the consumer's own jobs.
    @Bean(name = ["kraftPulseScheduler"])
    fun kraftPulseScheduler(): ThreadPoolTaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 2 // flushToCloud + headroom for future internal scheduled tasks
        scheduler.setThreadNamePrefix("KraftPulse-Scheduler-")
        scheduler.setErrorHandler { ex ->
            // Never let an uncaught exception kill this scheduler's thread
            org.slf4j.LoggerFactory.getLogger("KraftPulseScheduler").error("Uncaught scheduler error", ex)
        }
        scheduler.initialize()
        return scheduler
    }
}