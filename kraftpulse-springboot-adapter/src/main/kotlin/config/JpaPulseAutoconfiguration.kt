package config

import analytics.AnalyticsProvider
import util.DefaultPulseContextProvider
import interceptors.PulseContextProvider
import interceptors.QueryPulseInterceptor
import jakarta.annotation.PostConstruct
import model.PulseContext
import net.ttddyy.dsproxy.ExecutionInfo
import net.ttddyy.dsproxy.QueryInfo
import net.ttddyy.dsproxy.listener.QueryExecutionListener
import net.ttddyy.dsproxy.support.ProxyDataSource
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import persistence.jpa.SqlQueryEventBuilder
import java.util.concurrent.Executor
import javax.sql.DataSource

@AutoConfiguration
@ConditionalOnExpression(
    "\${kraftpulse.enabled:false} and \${kraftpulse.telemetry-config.enabled:false}"
)
class JpaPulseAutoconfiguration(
    private val beanFactory: ListableBeanFactory
) : BeanPostProcessor, QueryExecutionListener {

    private val builder = SqlQueryEventBuilder()

    private val interceptor by lazy { beanFactory.getBean(QueryPulseInterceptor::class.java) }
    private val contextProvider by lazy { beanFactory.getBean(PulseContextProvider::class.java) }


    @PostConstruct
    fun init() {
        println("🚀 KraftPulse: SQL Sniffer Initialized")
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is DataSource && bean !is ProxyDataSource) {
            println("KraftPulse: Wrapping DataSource [$beanName]")
            return ProxyDataSourceBuilder.create(bean)
                .name(beanName)
                .listener(this)
                .build()
        }
        return bean
    }

    override fun afterQuery(execInfo: ExecutionInfo?, queryInfoList: List<QueryInfo?>?) {
        if (execInfo == null || queryInfoList == null) return


        try {
            // Resolve beans manually if not already cached to avoid proxy casting issues
            val currentContext = contextProvider.currentContext() ?: PulseContext.SYSTEM_DEFAULT
            val events = builder.buildEvents(execInfo, queryInfoList.filterNotNull(), "SQL-Source")

            println("events: $events")
            events.forEach { interceptor.onQuery(currentContext, it) }
        } catch (e: Exception) {
            println("KraftPulse Error: ${e.message}")
            e.printStackTrace() // Log the stack trace to find the exact line causing the cast
        }
    }

    override fun beforeQuery(execInfo: ExecutionInfo?, queryInfoList: List<QueryInfo?>?) {
        // No-op
    }


    /**
     * Define the default provider here.
     * @ConditionalOnMissingBean allows the user to override this in their own app.
     */
    @Bean
    @ConditionalOnMissingBean
    fun pulseContextProvider(): PulseContextProvider = DefaultPulseContextProvider()

    @Bean
    @ConditionalOnMissingBean
    fun queryPulseInterceptor(analyticsProvider: AnalyticsProvider): QueryPulseInterceptor = AsyncQueryPulseInterceptor(analyticsProvider)


    @Bean(name = ["pulseTaskExecutor"])
    fun pulseTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2 // Small core since SQLite handles WAL well
        executor.maxPoolSize = 10
        executor.setQueueCapacity(1000) // Buffer for bursts of SQL activity
        executor.setThreadNamePrefix("KraftPulse-DB-")
        executor.initialize()
        return executor
    }
}

//@AutoConfiguration
//class JpaPulseAutoconfiguration(
//    // Use ObjectProvider for cleaner, null-safe bean retrieval
//    private val interceptorProvider: ObjectProvider<QueryPulseInterceptor>,
//    private val contextProvider: ObjectProvider<PulseContextProvider>
//) : BeanPostProcessor, QueryExecutionListener {
//
//    private val builder = SqlQueryEventBuilder()
//
//    @PostConstruct
//    fun init() {
//        println("KraftPulse: SQL Sniffer Initialized")
//    }
//
////    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
////        if (bean is DataSource && bean !is ProxyDataSource) {
////            println("✅ KraftPulse: Wrapping DataSource [$beanName]")
////            return ProxyDataSourceBuilder.create(bean)
////                .name(beanName)
////                .listener(this)
////                .build()
////        }
////        return bean
////    }
//
//    override fun postProcessAfterInitialization(bean: Any, beanName: String?): Any {
//        // Note: Change beanName to String? to handle potential nulls from Spring safely
//        if (bean is DataSource && bean !is ProxyDataSource) {
//            // Fallback to "default" if beanName is null or empty
//            val safeName = if (beanName.isNullOrBlank()) "dataSource-proxy" else beanName
//
//            println("✅ KraftPulse: Wrapping DataSource [$safeName]")
//            return ProxyDataSourceBuilder.create(bean)
//                .name(safeName) // This is likely where the null cast error occurred
//                .listener(this)
//                .build()
//        }
//        return bean
//    }
//
//    override fun afterQuery(execInfo: ExecutionInfo?, queryInfoList: List<QueryInfo?>?) {
//        if (execInfo == null || queryInfoList == null) return
//
//        try {
//            // getIfAvailable() returns null instead of throwing an exception if the bean is missing
//            val provider = contextProvider.getIfAvailable()
//            val interceptor = interceptorProvider.getIfAvailable()
//
//            if (interceptor == null) {
//                // If no interceptor is found, we just skip.
//                // Alternatively, log once that telemetry is disabled.
//                return
//            }
//
//            val currentContext = provider?.currentContext() ?: PulseContext.SYSTEM_DEFAULT
//            val events = builder.buildEvents(execInfo, queryInfoList.filterNotNull(), "SQL-Source")
//            println("events: $events")
//            events.forEach { interceptor.onQuery(currentContext, it) }
//        } catch (e: Exception) {
//            println("⚠️ KraftPulse Error: ${e.message}")
//        }
//    }
//
//    override fun beforeQuery(execInfo: ExecutionInfo?, queryInfoList: List<QueryInfo?>?) {}
//
//    @Bean
//    @ConditionalOnMissingBean
//    fun pulseContextProvider(): PulseContextProvider = DefaultPulseContextProvider()
//
//    /**
//     * PROVIDE DEFAULT INTERCEPTOR
//     * This was missing/commented out, causing your 'No qualifying bean' error.
//     */
//    @Bean
//    @ConditionalOnMissingBean
//    fun queryPulseInterceptor(): QueryPulseInterceptor = LoggingQueryPulseInterceptor()
//
////    /**
////     * PROVIDE DEFAULT INTERCEPTOR
////     * We inject the AnalyticsProvider here so AsyncQueryPulseInterceptor
////     * can persist telemetry data to the DB.
////     */
////    @Bean
////    @ConditionalOnMissingBean
////    fun queryPulseInterceptor(analytics: AnalyticsProvider): QueryPulseInterceptor {
////        return AsyncQueryPulseInterceptor(analytics)
////    }
//
//    @Bean(name = ["pulseTaskExecutor"])
//    fun pulseTaskExecutor(): Executor {
//        val executor = ThreadPoolTaskExecutor()
//        executor.corePoolSize = 2
//        executor.maxPoolSize = 10
//        executor.setQueueCapacity(1000)
//        executor.setThreadNamePrefix("KraftPulse-DB-")
//        executor.initialize()
//        return executor
//    }
//}