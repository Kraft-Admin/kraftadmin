package persistence.jpa.delete

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import com.kraftadmin.utils.files.AdminStorageProvider
import jakarta.persistence.CascadeType
import jakarta.persistence.Embedded
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.util.Collections
import java.util.IdentityHashMap

class FileCleanupService(
    private val adminStorageProvider: AdminStorageProvider
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val fileInputs = setOf(
        FormInputType.IMAGE,
        FormInputType.VIDEO,
        FormInputType.AUDIO,
        FormInputType.FILE,
        FormInputType.DOCUMENT
    )

    fun cleanupFiles(instance: Any?) {
        if (instance == null) return

        val visited =
            Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())

        cleanup(instance, visited)
    }

    private fun cleanup(
        instance: Any,
        visited: MutableSet<Any>
    ) {

        if (!visited.add(instance)) {
            return
        }

        var type: Class<*>? = instance.javaClass

        while (type != null && type != Any::class.java) {

            type.declaredFields.forEach { field ->

                runCatching {

                    field.isAccessible = true

                    val value = field.get(instance)
                        ?: return@runCatching

                    val adminField =
                        field.getAnnotation(KraftAdminField::class.java)

                    if (
                        adminField != null &&
                        adminField.inputType in fileInputs
                    ) {
                        deleteFieldValue(value)
                        return@runCatching
                    }

                    when {

                        field.isAnnotationPresent(Embedded::class.java) ->
                            recurse(value, visited)

                        field.shouldCascadeDelete() ->
                            recurse(value, visited)
                    }

                }.onFailure {

                    logger.debug(
                        "Ignoring cleanup failure for {}.{}",
//                        type.simpleName,
                        field.name,
                        it
                    )

                }

            }

            type = type.superclass
        }
    }

    private fun recurse(
        value: Any,
        visited: MutableSet<Any>
    ) {

        when (value) {

            is Collection<*> ->
                value.filterNotNull()
                    .forEach { cleanup(it, visited) }

            is Array<*> ->
                value.filterNotNull()
                    .forEach { cleanup(it, visited) }

            else ->
                cleanup(value, visited)
        }
    }

    private fun deleteFieldValue(value: Any) {

        when (value) {

            is String ->
                deleteIfManaged(value)

            is Collection<*> ->
                value.filterIsInstance<String>()
                    .forEach(::deleteIfManaged)

            is Array<*> ->
                value.filterIsInstance<String>()
                    .forEach(::deleteIfManaged)
        }
    }

    private fun Field.shouldCascadeDelete(): Boolean {

        val cascades = when {

            isAnnotationPresent(OneToOne::class.java) ->
                getAnnotation(OneToOne::class.java).cascade

            isAnnotationPresent(OneToMany::class.java) ->
                getAnnotation(OneToMany::class.java).cascade

            isAnnotationPresent(ManyToOne::class.java) ->
                getAnnotation(ManyToOne::class.java).cascade

            isAnnotationPresent(ManyToMany::class.java) ->
                getAnnotation(ManyToMany::class.java).cascade

            else ->
                return false
        }

        return CascadeType.ALL in cascades ||
                CascadeType.REMOVE in cascades
    }

    private fun deleteIfManaged(url: String) {

        if (!adminStorageProvider.contains(url)) {
            return
        }

        runCatching {
            adminStorageProvider.delete(url)
        }
            .onSuccess {
                logger.debug("Deleted uploaded file {}", url)
            }
            .onFailure {
                logger.warn(
                    "Failed deleting uploaded file {}",
                    url,
                    it
                )
            }
    }
}