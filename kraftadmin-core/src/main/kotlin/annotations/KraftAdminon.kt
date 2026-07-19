package annotations

import com.kraftadmin.events.KraftAdminEvent
import kotlin.reflect.KClass

/**
 * Marks a method on any Spring bean as a KraftAdmin event listener.
 *
 * Example:
 * ```kotlin
 * @Component
 * class OrderListeners(private val emailService: EmailService) {
 *
 *     // Fires after any Order is created — sends welcome email
 *     @KraftAdminOn(KraftAdminEvent1.AfterCreate::class, resource = "Order")
 *     fun onOrderCreated(event: KraftAdminEvent1.AfterCreate) {
 *         emailService.sendConfirmation(event.entity as Order)
 *     }
 *
 *     // Veto example — fires before delete, throws to cancel
 *     @KraftAdminOn(KraftAdminEvent1.BeforeDelete::class, resource = "Order")
 *     fun preventDeleteIfShipped(event: KraftAdminEvent1.BeforeDelete) {
 *         val order = event.entity as Order
 *         if (order.status == OrderStatus.SHIPPED) {
 *             throw IllegalStateException("Cannot delete a shipped order")
 *         }
 *     }
 *
 *     // Listens to ALL resources, runs async — audit log
 *     @KraftAdminOn(KraftAdminEvent1.AfterDelete::class, async = true, order = 100)
 *     fun auditDelete(event: KraftAdminEvent1.AfterDelete) {
 *         auditService.log(event)
 *     }
 * }
 * ```
 */
//@Target(AnnotationTarget.FUNCTION)
//@Retention(AnnotationRetention.RUNTIME)
//annotation class KraftAdminOn(
////    val entityClass: KClass<*> = Any::class,
//    vararg val events: KClass<out KraftAdminEvent1>,
//    val resource: String = "",   // blank = all resources
//    val async: Boolean = false,  // true = runs on kraftEventExecutor, errors never propagate
//    val order: Int = 0           // lower = runs first within same event type
//)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KraftAdminOn(
    vararg val events: KClass<out KraftAdminEvent>,

    /**
     * Filter by resource provider string — matches event.resourceName.
     * Blank = all resources.
     * Use this when you don't have a reference to the entity class.
     */
    val resource: String = "",

    /**
     * Filter by entity class — more type-safe than resource string.
     * Defaults to Any::class meaning "no filter, all entities".
     * When set, only fires if event.entity is an instance of this class.
     */
    val entityClass: KClass<*> = Any::class,

    /**
     * Lower order runs first within the same event type.
     */
    val order: Int = 0
)
