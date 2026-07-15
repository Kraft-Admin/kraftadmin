//package controller
//
//import actions.KraftActionResponse
//import api.responses.KraftOperationResponse
//import api.responses.ResourceDataResponse
//import api.utils.ObjectResponse
//import api.utils.ResourceRow
//import com.kraftadmin.api.responses.DashboardStat
//import com.kraftadmin.api.responses.KraftDashboardResponse
//import com.kraftadmin.api.responses.LibraryFeature
//import com.kraftadmin.api.responses.SystemStatus
//import security.SecurityProviderChain
//import com.kraftadmin.ui_descriptors.KraftAdminDescriptor
//import com.kraftadmin.ui_descriptors.KraftAdminDescriptorFactory
//import config.KraftAdminProperties
//import org.slf4j.LoggerFactory
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
//import org.springframework.context.ApplicationContext
//import org.springframework.http.HttpStatus
//import org.springframework.http.ResponseEntity
//import org.springframework.web.bind.annotation.DeleteMapping
//import org.springframework.web.bind.annotation.GetMapping
//import org.springframework.web.bind.annotation.PathVariable
//import org.springframework.web.bind.annotation.PostMapping
//import org.springframework.web.bind.annotation.RequestBody
//import org.springframework.web.bind.annotation.RequestMapping
//import org.springframework.web.bind.annotation.RequestParam
//import org.springframework.web.bind.annotation.ResponseStatus
//import org.springframework.web.bind.annotation.RestController
//import events.SpringKraftCustomActionService
//import exception.KraftPersistenceException
//import persistence.error.PersistenceException
//
//
//@RestController
//@RequestMapping("\${kraftadmin.base-path:/admin}/api")
//@ConditionalOnProperty(prefix = "kraftpulse", name = ["enabled"], havingValue = "true")
//class KraftAdminSpringbootMetaController(
//    private val descriptorFactory: KraftAdminDescriptorFactory,
//    private val chain: SecurityProviderChain,
//    private val properties: KraftAdminProperties,
//    private val applicationContext: ApplicationContext,
//    private val customActionService: SpringKraftCustomActionService
//) {
//    private val logger = LoggerFactory.getLogger(javaClass)
//
//    @GetMapping("/dashboard")
//    fun getDashboardOverview(): ResponseEntity<KraftDashboardResponse> {
//        // 1. Calculate Entity Counts dynamically
//        val resourceNames = descriptorFactory.getRegisteredResourceNames()
//
//        // sumOf usually returns Int or Long; ensure your factory returns a numeric type
//        val totalEntitiesCount = resourceNames.sumOf { name ->
//            descriptorFactory.getTotalCountForResource(name)
//        }
//
//        // 2. Map Stats Cards
//        val stats = listOf(
//            DashboardStat("Total Managed Records", totalEntitiesCount.toString(), "database"),
//            DashboardStat("Resources Registered", resourceNames.size.toString(), "layers"),
//            DashboardStat("Active Sessions", "1", "users")
//        )
//
//        // 3. Use the dynamic check instead of hardcoded list
//        val libraryFeatures = checkFeatureStatus()
//
//        val response = KraftDashboardResponse(
//            title = properties.title,
//            welcomeMessage = "Welcome to the ${properties.title} command center.",
//            stats = stats,
//            features = libraryFeatures,
//            systemStatus = SystemStatus(
//                environment = "Development", // You can pull this from Spring Profile if needed
//                databaseType = "H2 / R2DBC",
//                totalEntitiesTracked = resourceNames.size
//            )
//        )
//
//        return ResponseEntity.ok(response)
//    }
//
//    /**
//     * Provides the UI Schema (Columns, InputTypes, Labels)
//     * used by the Svelte form to render inputs.
//     */
//    @GetMapping("/resources/descriptors")
//    fun descriptor(): KraftAdminDescriptor = descriptorFactory.create(chain = chain, pConfig = properties)
//
//    /**
//     * List view data.
//     */
//    @GetMapping("/resources/{name}")
//    fun getResourceData(
//        @PathVariable(name = "name") resourceName: String,
//        @RequestParam(defaultValue = "1") page: Int,
//        @RequestParam(defaultValue = "20") size: Int,
//        @RequestParam(required = false) q: String?,
//        @RequestParam(required = false) sortField: String?,
//        @RequestParam(required = false) sortDirection: String?
//    ): ResourceDataResponse {
//        logger.info("Fetching resource: {}, page: {}, sortField: {}", resourceName, page, sortField)
//        try {
//            return descriptorFactory.getResourceData(
//                name = resourceName,
//                page = page,
//                size = size,
//                query = q,
//                sortField = sortField,
//                sortDirection = sortDirection,
//            )
//        }catch (e:Exception){
//            throw e
//        }
//    }
//
//    /**
//     * Fetch a single entity for editing.
//     * The Map returned here must match the Svelte formData structure.
//     */
//    @GetMapping("/resources/{name}/{id}")
//    fun details(@PathVariable name: String, @PathVariable id: String): ResponseEntity<KraftOperationResponse<ResourceRow>> {
//        try {
//            logger.info("fetching resource details for $name with id $id")
//            val data = descriptorFactory.getResourceDetailsData(name, id)
////            return data?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
//            return ResponseEntity.ok(
//                KraftOperationResponse(
//                    success = true,
//                    message = "Resource details returned",
//                    data = data,
//                )
//            )
//        }catch (e: Exception){
//            logger.error("Error fetching resource details for $name with id $id", e.stackTrace)
////            throw e
//            return ResponseEntity.ok(
//                KraftOperationResponse(
//                    success = false,
//                    message = e.message,
//                )
//            )
//        }
//    }
//
//    /**
//     * The main Save/Update entry point.
//     * This triggers the conversion logic (including LocalTime/LocalDate parsing).
//     */
//    @PostMapping("/resources/{name}")
//    fun save(
//        @PathVariable name: String,
//        @RequestBody data: Map<String, Any?>
//    ): ResponseEntity<KraftOperationResponse<Any>> {
//
//        logger.info("Saving resource {}", name)
////        val id = data["id"]?.toString()
////        val hasId = data.containsKey("id") &&
////                data["id"] != null &&
////                data["id"].toString().isNotBlank()
//
//        val id = data["id"]
//
//        val hasId = when (id) {
//            null -> false
//            is Number -> id.toLong() != 0L
//            else -> id.toString().isNotBlank()
//        }
//
//        logger.info("id $id, $hasId")
//        logger.info("data $data")
//
//
//        return try {
//
//            val result = descriptorFactory.validateAndSave(name, data)
//
//            if (result.success) {
//
//                val message =
//                    if (hasId)
//                        "Updated $name successfully."
//                    else
//                        "Saved $name successfully."
//
//                ResponseEntity.ok(
//                    KraftOperationResponse(
//                        success = true,
//                        message = message,
//                        data = result.data,
//                        errors = result.errors
//                    )
//                )
//
//            } else {
//
//                ResponseEntity.unprocessableEntity().body(
//                    KraftOperationResponse(
//                        success = false,
//                        message = "Validation failed.",
//                        errors = result.errors
//                    )
//                )
//
//            }
//
//        } catch (e: PersistenceException) {
//
//            logger.error("Failed to save {}", name, e)
//
//            ResponseEntity.badRequest().body(
//                KraftOperationResponse(
//                    success = false,
//                    message = e.message
//                )
//            )
//
//        } catch (e: Exception) {
//
//            logger.error("Unexpected error", e)
//
//            ResponseEntity.internalServerError().body(
//                KraftOperationResponse(
//                    success = false,
//                    message = "An unexpected error occurred."
//                )
//            )
//        }
//
//    }
//
//// delete resource
//    @DeleteMapping("/resources/{name}/{id}")
//    fun delete(
//        @PathVariable name: String,
//        @PathVariable id: String
//    ): ResponseEntity<KraftOperationResponse<Unit>> {
//
//        return try {
//
//            val result = descriptorFactory.deleteResource(name, id)
//
//            ResponseEntity.ok(result)
//
//        } catch (e: Exception) {
//
//            logger.error("Delete failed", e)
//
//            ResponseEntity.badRequest().body(
//                KraftOperationResponse(
//                    false,
//                    e.message ?: "Delete failed"
//                )
//            )
//        }
//    }
//
//    /**
//     * Real-time lookup for RELATION/MULTI_SELECT types.
//     * Svelte hits this as the user types in a lookup field.
//     */
//    @GetMapping("/resources/{name}/lookup")
//    fun lookup(
//        @PathVariable name: String,
//        @RequestParam(required = false, defaultValue = "") search: String,
//        @RequestParam(required = false) ids: String?  // for label resolution on form load
//    ): ResponseEntity<List<ObjectResponse>> {
//        logger.debug("Lookup: resource=$name search='$search' ids=$ids")
//
//        // ID-based fetch — resolves labels for existing relation values
//        if (!ids.isNullOrBlank()) {
//            val idList = ids.split(",").map { it.trim() }.filter { it.isNotBlank() }
//            val results = descriptorFactory.getLookupDataByIds(name,  idList)
//            return ResponseEntity.ok(results)
//        }
//
//        val results = descriptorFactory.getLookupData(name,  search)
//        return ResponseEntity.ok(results)
//    }
//
//
//    @PostMapping("/resources/{resource}/id/{id}/action/{actionName}")
//    fun handleCustomAction(
//        @PathVariable resource: String,
//        @PathVariable id: String,
//        @PathVariable actionName: String,
//        @RequestBody input: Any?
//    ): KraftActionResponse? {
//        logger.info("Action performed: resource {}, id : {}, action: {}, params: {}", resource, id, actionName, input)
//        val actionResponse = customActionService.execute(resource, id, actionName, input)
//        logger.info("action response: $actionResponse")
//        return actionResponse
//    }
//
//    //     bulk actions controller ie export, print, import etc
//    @GetMapping("/")
//    suspend fun performBulkAction(
//
//    ){
//       logger.info("performing bulk action")
//
//    }
//
//    private fun checkFeatureStatus(): List<LibraryFeature> {
//        val features = mutableListOf<LibraryFeature>()
//
//        // Check 1: Telemetry (Dynamic based on YAML/JSON properties)
//        features.add(LibraryFeature(
//            name = "Telemetry & BI",
//            description = "Streaming system events to ${properties.telemetryConfig.cloudUrl}",
//            status = if (properties.telemetryConfig.enabled) "Active" else "Disabled",
//            unlockCriteria = "Set 'kraftadmin.telemetry-config.enabled: true' in YAML"
//        ))
//
//        // Check 2: Database Auditing (Verified by your infra package)
//        features.add(LibraryFeature(
//            name = "BaseEntity Auditing",
//            description = "Automated tracking of created_at and updated_at fields via bowerzlabs.evry.infra.db.",
//            status = "Active",
//            unlockCriteria = null
//        ))
//
//        // Check 3: Custom Actions (Dynamic discovery)
////        val hasCustomActions = descriptorFactory.getRegisteredResourceNames()
////            .any { provider -> descriptorFactory.getResourceData(provider).resource.customActions.isNotEmpty() }
//
//        features.add(LibraryFeature(
//            name = "Custom Actions",
//            description = "Domain-specific logic triggered via @KraftAdminCustomAction",
////            status = if (hasCustomActions) "Active" else "Pending",
//            status = "Pending",
//            unlockCriteria = "Add @KraftAdminCustomAction to your Domain Entities"
//        ))
//
//        return features
//    }
//
//}


package controller

import actions.KraftActionResponse
import api.responses.KraftOperationResponse
import api.responses.ResourceDataResponse
import api.utils.ObjectResponse
import api.utils.ResourceRow
import com.kraftadmin.api.responses.DashboardStat
import com.kraftadmin.api.responses.KraftDashboardResponse
import com.kraftadmin.api.responses.LibraryFeature
import com.kraftadmin.api.responses.SystemStatus
import security.SecurityProviderChain
import com.kraftadmin.ui_descriptors.KraftAdminDescriptor
import com.kraftadmin.ui_descriptors.KraftAdminDescriptorFactory
import config.KraftAdminProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import events.SpringKraftCustomActionService

@RestController
@RequestMapping("\${kraftadmin.base-path:/admin}/api")
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class KraftAdminSpringbootMetaController(
    private val descriptorFactory: KraftAdminDescriptorFactory,
    private val chain: SecurityProviderChain,
    private val properties: KraftAdminProperties,
    private val applicationContext: ApplicationContext,
    private val customActionService: SpringKraftCustomActionService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/dashboard")
    fun getDashboardOverview(): ResponseEntity<KraftDashboardResponse> {
        val resourceNames = descriptorFactory.getRegisteredResourceNames()

        val totalEntitiesCount = resourceNames.sumOf { name ->
            descriptorFactory.getTotalCountForResource(name)
        }

        val stats = listOf(
            DashboardStat("Total Managed Records", totalEntitiesCount.toString(), "database"),
            DashboardStat("Resources Registered", resourceNames.size.toString(), "layers"),
            DashboardStat("Active Sessions", "1", "users")
        )

        val libraryFeatures = checkFeatureStatus()

        val response = KraftDashboardResponse(
            title = properties.title,
            welcomeMessage = "Welcome to the ${properties.title} command center.",
            stats = stats,
            features = libraryFeatures,
            systemStatus = SystemStatus(
                environment = "Development",
                databaseType = "H2 / R2DBC",
                totalEntitiesTracked = resourceNames.size
            )
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Provides the UI Schema (Columns, InputTypes, Labels)
     * used by the Svelte form to render inputs.
     */
    @GetMapping("/resources/descriptors")
    fun descriptor(): KraftAdminDescriptor = descriptorFactory.create(chain = chain, pConfig = properties)

    /**
     * List view data. Any failure (unknown resource, DB error, etc.)
     * propagates to KraftAdminExceptionHandler — no local try/catch needed.
     */
    @GetMapping("/resources/{name}")
    fun getResourceData(
        @PathVariable(name = "name") resourceName: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) sortField: String?,
        @RequestParam(required = false) sortDirection: String?
    ): ResourceDataResponse {
        logger.info("Fetching resource: {}, page: {}, sortField: {}", resourceName, page, sortField)
        return descriptorFactory.getResourceData(
            name = resourceName,
            page = page,
            size = size,
            query = q,
            sortField = sortField,
            sortDirection = sortDirection,
        )
    }

    /**
     * Fetch a single entity for editing.
     * The Map returned here must match the Svelte formData structure.
     * Failure propagates to KraftAdminExceptionHandler with the correct
     * HTTP status (e.g. 404 for not_found) — never swallowed into a 200.
     */
    @GetMapping("/resources/{name}/{id}")
    fun details(
        @PathVariable name: String,
        @PathVariable id: String
    ): ResponseEntity<KraftOperationResponse<ResourceRow>> {
        logger.info("fetching resource details for $name with id $id")
        val data = descriptorFactory.getResourceDetailsData(name, id)
        return ResponseEntity.ok(
            KraftOperationResponse(
                success = true,
                message = "Resource details returned",
                data = data,
            )
        )
    }

    /**
     * The main Save/Update entry point.
     * Validation failure (result.success == false) is a normal return value,
     * not an exception — that branch stays local. Genuine exceptions (e.g.
     * PersistenceException) propagate to the advice for consistent status/shape.
     */
    @PostMapping("/resources/{name}")
    fun save(
        @PathVariable name: String,
        @RequestBody data: Map<String, Any?>
    ): ResponseEntity<KraftOperationResponse<Any>> {

        logger.info("Saving resource {}", name)

        val id = data["id"]
        val hasId = when (id) {
            null -> false
            is Number -> id.toLong() != 0L
            else -> id.toString().isNotBlank()
        }

        logger.info("id $id, $hasId")
        logger.info("data $data")

        val result = descriptorFactory.validateAndSave(name, data)

        return if (result.success) {
            val message = if (hasId) "Updated $name successfully." else "Saved $name successfully."
            ResponseEntity.ok(
                KraftOperationResponse(
                    success = true,
                    message = message,
                    data = result.data,
                    errors = result.errors
                )
            )
        } else {
            ResponseEntity.unprocessableEntity().body(
                KraftOperationResponse(
                    success = false,
                    message = "Validation failed.",
                    errors = result.errors
                )
            )
        }
    }

    /**
     * Delete resource. Failure propagates to the advice for consistent
     * status/shape (e.g. 409 for foreign_key, 404 for not_found).
     */
    @DeleteMapping("/resources/{name}/{id}")
    fun delete(
        @PathVariable name: String,
        @PathVariable id: String
    ): ResponseEntity<KraftOperationResponse<Unit>> {
        val result = descriptorFactory.deleteResource(name, id)
        return ResponseEntity.ok(result)
    }

    /**
     * Real-time lookup for RELATION/MULTI_SELECT types.
     * Svelte hits this as the user types in a lookup field.
     */
    @GetMapping("/resources/{name}/lookup")
    fun lookup(
        @PathVariable name: String,
        @RequestParam(required = false, defaultValue = "") search: String,
        @RequestParam(required = false) ids: String?
    ): ResponseEntity<List<ObjectResponse>> {
        logger.debug("Lookup: resource=$name search='$search' ids=$ids")

        if (!ids.isNullOrBlank()) {
            val idList = ids.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val results = descriptorFactory.getLookupDataByIds(name, idList)
            return ResponseEntity.ok(results)
        }

        val results = descriptorFactory.getLookupData(name, search)
        return ResponseEntity.ok(results)
    }

    @PostMapping("/resources/{resource}/id/{id}/action/{actionName}")
    fun handleCustomAction(
        @PathVariable resource: String,
        @PathVariable id: String,
        @PathVariable actionName: String,
        @RequestBody input: Any?
    ): KraftActionResponse? {
        logger.info("Action performed: resource {}, id : {}, action: {}, params: {}", resource, id, actionName, input)
        val actionResponse = customActionService.execute(resource, id, actionName, input)
        logger.info("action response: $actionResponse")
        return actionResponse
    }

    @GetMapping("/")
    suspend fun performBulkAction() {
        logger.info("performing bulk action")
    }

    private fun checkFeatureStatus(): List<LibraryFeature> {
        val features = mutableListOf<LibraryFeature>()

        features.add(LibraryFeature(
            name = "Telemetry & BI",
            description = "Streaming system events to ${properties.telemetryConfig.cloudUrl}",
            status = if (properties.telemetryConfig.enabled) "Active" else "Disabled",
            unlockCriteria = "Set 'kraftadmin.telemetry-config.enabled: true' in YAML"
        ))

        features.add(LibraryFeature(
            name = "BaseEntity Auditing",
            description = "Automated tracking of created_at and updated_at fields via bowerzlabs.evry.infra.db.",
            status = "Active",
            unlockCriteria = null
        ))

        features.add(LibraryFeature(
            name = "Custom Actions",
            description = "Domain-specific logic triggered via @KraftAdminCustomAction",
            status = "Pending",
            unlockCriteria = "Add @KraftAdminCustomAction to your Domain Entities"
        ))

        return features
    }

}