package events

import com.kraftadmin.events.KraftEventLogger
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnMissingBean(KraftEventLogger::class)
class NoOpKraftEventLogger : KraftEventLogger {

    override fun log(event: com.kraftadmin.events.KraftAdminEvent)  = Unit
}