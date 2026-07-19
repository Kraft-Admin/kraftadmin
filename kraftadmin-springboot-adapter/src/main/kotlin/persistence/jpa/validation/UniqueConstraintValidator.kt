package persistence.jpa.validation

import persistence.error.PersistenceErrorDetails

import jakarta.persistence.Column
import jakarta.persistence.EntityManager
import jakarta.persistence.Table
import persistence.error.PersistenceException
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Validates @Column(unique = true) before Hibernate reaches flush().
 *
 * Table-level unique constraints can be added later.
 */
class UniqueConstraintValidator : PersistenceValidator {

    override fun validate(context: ValidationContext<*>) {

        val entity = context.entity
        val entityClass = context.entityClass
        val entityManager = context.entityManager


        entityClass.memberProperties
            .filterIsInstance<KProperty1<Any, *>>()
            .forEach { property ->
                property.isAccessible = true
                val column = property.findAnnotation<Column>() ?: return@forEach

                if (!column.unique) {
                    return@forEach
                }

                val value = property.get(entity) ?: return@forEach

                // Ignore null values.

                validateUnique(
                    entityManager,
                    context,
                    property,
                    value
                )
            }

        // Reserved for @Table(uniqueConstraints = ...)
        entityClass.findAnnotation<Table>()

    }

    private fun validateUnique(
        entityManager: EntityManager,
        context: ValidationContext<*>,
//        property: String,
        property: KProperty1<Any, *>,
        value: Any
    ) {

        val isString = property.returnType.classifier == String::class
        val entityName = context.metadata.entityName

        val jpql = buildString {
            append("select count(e) from ")
            append(entityName)
            append(" e where ")

            if (isString) {
                append("lower(e.")
                append(property.name)
                append(") = lower(:value)")
            } else {
                append("e.")
                append(property.name)
                append(" = :value")
            }

            if (context.isUpdate && context.entityId != null) {
                append(" and e.")
                append(context.metadata.idField)
                append(" <> :id")
            }
        }

        val query = entityManager.createQuery(jpql, java.lang.Long::class.java)
            .setParameter("value", value)

        if (context.isUpdate && context.entityId != null) {
            query.setParameter(context.metadata.idField, context.entityId)
        }

        val count = query.singleResult

        if (count > 0L) {
            throw PersistenceException(
                PersistenceErrorDetails(
                    code = "duplicate_value",
                    message = "'${property.name}' must be unique."
                )
            )
        }
    }
}