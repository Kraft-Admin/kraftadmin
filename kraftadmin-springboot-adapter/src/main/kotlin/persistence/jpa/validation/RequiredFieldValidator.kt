package persistence.jpa.validation

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import persistence.error.PersistenceErrorDetails
import persistence.error.PersistenceException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class RequiredFieldValidator : PersistenceValidator {

    private data class RequiredField(
        val field: Field,
        val label: String,
        val checkBlank: Boolean
    )

    override fun validate(context: ValidationContext<*>) {
        val fields = ValidatorMetadataCache.get(javaClass, context.entityClass) {
            resolveRequiredFields(context.entityClass)
        }

        for (rf in fields) {
            rf.field.isAccessible = true
            val value = rf.field.get(context.entity)
            val missing = value == null || (rf.checkBlank && value is String && value.isBlank())

            if (missing) {
                throw PersistenceException(
                    PersistenceErrorDetails("required_field", "'${rf.label}' is required.")
                )
            }
        }
    }

    private fun resolveRequiredFields(entityClass: KClass<*>): List<RequiredField> {
        val result = mutableListOf<RequiredField>()

        entityClass.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach

            // 1. Exclude static fields
            if (Modifier.isStatic(field.modifiers)) return@forEach

            // 2. Exclude auto-managed JPA/Hibernate fields
            if (field.isAnnotationPresent(Id::class.java) ||
                field.isAnnotationPresent(GeneratedValue::class.java) ||
                field.isAnnotationPresent(Version::class.java) ||
                field.isAnnotationPresent(CreationTimestamp::class.java) ||
                field.isAnnotationPresent(UpdateTimestamp::class.java)
            ) return@forEach

            // 3. Exclude relations/embedded
            if (field.isAnnotationPresent(ManyToOne::class.java) ||
                field.isAnnotationPresent(OneToOne::class.java) ||
                field.isAnnotationPresent(OneToMany::class.java) ||
                field.isAnnotationPresent(ManyToMany::class.java) ||
                field.isAnnotationPresent(ElementCollection::class.java) ||
                field.isAnnotationPresent(Embedded::class.java)
            ) return@forEach

            // 4. STRICTLY check for @Column(nullable = false)
            val column = field.getAnnotation(Column::class.java)

            // Only proceed if @Column exists and explicitly sets nullable to false
            if (column != null && !column.nullable) {
                result += RequiredField(
                    field = field,
                    label = humanize(field.name),
                    checkBlank = field.type == String::class.java
                )
            }
        }
        return result
    }
}