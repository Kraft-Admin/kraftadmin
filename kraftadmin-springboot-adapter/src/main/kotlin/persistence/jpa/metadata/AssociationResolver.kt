package persistence.jpa.metadata

import jakarta.persistence.*
import org.slf4j.LoggerFactory
import persistence.jpa.util.HibernateUtil
import persistence.jpa.util.HibernateUtil.unproxy
import java.lang.reflect.Field
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
        val real = unproxy(entity) ?: return null

        val field = findIdField(real.javaClass) ?: return null

        field.isAccessible = true

        return field.get(real)
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


    fun getIdPropertyName(type: KClass<*>): String {
        return findIdField(type.java)?.name
            ?: throw IllegalStateException(
                "No @Id field found for ${type.simpleName}"
            )
    }

    private fun findIdField(type: Class<*>): Field? {
        var current: Class<*>? = type

        while (current != null && current != Any::class.java) {
            current.declaredFields.firstOrNull {
                it.isAnnotationPresent(Id::class.java) ||
                        it.isAnnotationPresent(org.springframework.data.annotation.Id::class.java)
            }?.let { return it }

            current = current.superclass
        }

        return null
    }


}