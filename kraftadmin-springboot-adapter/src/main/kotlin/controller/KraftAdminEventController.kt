package controller

import com.kraftadmin.model.KraftEventPage
import events.KraftAdminEventStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/admin/api/events")
class KraftAdminEventController(
    private val eventStore: KraftAdminEventStore
) {

    /**
     * Search and paginate historical events.
     *
     * Used by:
     * - Search
     * - Filters
     * - Pagination
     * - Historical browsing
     */
    @GetMapping
    fun search(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) resourceName: String?,
        @RequestParam(required = false) actorUsername: String?,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): KraftEventPage {

        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)

        return eventStore.search(
            query = query,
            type = type,
            resourceName = resourceName,
            actorUsername = actorUsername,
            from = from,
            to = to,
            page = safePage,
            size = safeSize
        )
    }

    /**
     * Fetch events created after a specific timestamp.
     *
     * Used by the live System Pulse polling mechanism.
     */
    @GetMapping("/latest")
    fun latest(
        @RequestParam(required = false) after: Instant?,
        @RequestParam(defaultValue = "100") limit: Int
    ): List<Map<String, Any?>> {

//        return eventStore.(
//            after = after,
//            limit = limit.coerceIn(1, 200)
//        )
        return emptyList()
    }
}