package persistence.jpa.metadata

import jakarta.persistence.*
import org.slf4j.LoggerFactory
import persistence.jpa.util.HibernateUtil
import persistence.jpa.util.HibernateUtil.unproxy
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Resolves JPA associations — extracts IDs and display labels
 * from related entities without triggering N+1s.
 */
object AssociationResolver {

    private val logger = LoggerFactory.getLogger(AssociationResolver::class.java)

    fun extractId(entity: Any): Any? {
        val clean = unproxy(entity) ?: return null
        var currentClass: Class<*>? = clean.javaClass

        while (currentClass != null && currentClass != Any::class.java) {
            // Find by annotation or name
            val idField = currentClass.declaredFields.find {
                it.isAnnotationPresent(Id::class.java) || it.name == "id"
            }

            if (idField != null) {
                return try {
                    idField.isAccessible = true
                    idField.get(clean)
                } catch (e: Exception) {
                    null
                }
            }
            // Move up the hierarchy to find ID in BaseEntity
            currentClass = currentClass.superclass
        }

        return null
    }

    fun resolveDisplayLabel(entity: Any): String? {
        val real = HibernateUtil.unproxy(entity) ?: return null
        return try {
            val candidates = real::class.memberProperties.filter { p ->
                val javaField = p.javaField ?: return@filter false
                if (javaField.isAnnotationPresent(Transient::class.java) ||
                    javaField.isAnnotationPresent(jakarta.persistence.Transient::class.java)
                ) return@filter false

                val name = p.name.lowercase()
                if (name == "id" || name.endsWith("id") || name.contains("password")) return@filter false

                val classifier = p.returnType.classifier
                if (classifier !is KClass<*>) return@filter false

                PropertyResolver.isSimpleType(classifier)
            }

            val best = candidates.find { it.name in listOf("name", "title", "label", "email") }
                ?: candidates.firstOrNull()

            best?.let {
                it.isAccessible = true
                it.getter.call(real)?.toString()
            }
        } catch (e: Exception) {
            logger.warn("Could not resolve label for ${real::class.simpleName}: ${e.message}")
            null
        }
    }

}