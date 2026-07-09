package persistence.jpa.delete

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import com.kraftadmin.utils.files.AdminStorageProvider
import org.slf4j.LoggerFactory

class FileCleanupService(private val adminStorageProvider: AdminStorageProvider) {
    private val logger = LoggerFactory.getLogger(FileCleanupService::class.java)

    private val FILE_INPUTS = setOf(
        FormInputType.IMAGE, FormInputType.VIDEO, FormInputType.AUDIO,
        FormInputType.FILE, FormInputType.DOCUMENT
    )

    fun cleanupFiles(instance: Any?) {
        if (instance == null) return

        var type: Class<*>? = instance.javaClass

        while (type != null && type != Any::class.java) {

            type.declaredFields.forEach { field ->
                field.isAccessible = true

                val value = runCatching {
                    field.get(instance)
                }.getOrNull() ?: return@forEach

                logger.info("Field: {} value={}", field.name, value)

                val annotation = field.getAnnotation(KraftAdminField::class.java)

                logger.info(
                    "Field {} annotation={}",
                    field.name,
                    annotation?.inputType
                )

                if (annotation != null) {

                    if (annotation.inputType !in FILE_INPUTS) {
                        return@forEach
                    }

                    deleteFieldValue(value)
                    return@forEach
                }

                // Legacy fallback
                cleanupUnknownValue(value)
            }

            type = type.superclass
        }
    }

    private fun deleteFieldValue(value: Any?) {
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

    private fun cleanupUnknownValue(value: Any?) {
        when (value) {

            null,
            is Number,
            is Boolean,
            is Enum<*>,
            is CharSequence,
            is Map<*, *> -> return

            is Collection<*> -> {
                value.filterIsInstance<String>()
                    .filter(::looksLikeManagedFile)
                    .forEach(::deleteIfManaged)
            }

            is Array<*> -> {
                value.filterIsInstance<String>()
                    .filter(::looksLikeManagedFile)
                    .forEach(::deleteIfManaged)
            }

            is String -> {
                if (looksLikeManagedFile(value)) {
                    deleteIfManaged(value)
                }
            }

            else -> {
                val pkg = value.javaClass.`package`?.name ?: ""
                if (!pkg.startsWith("java.") && !pkg.startsWith("kotlin.")) {
                    cleanupFiles(value)
                }
            }
        }
    }

    private fun looksLikeManagedFile(url: String): Boolean {

        if (!adminStorageProvider.contains(url)) {
            return false
        }

        val extension = url
            .substringAfterLast('.', "")
            .substringBefore('?')
            .lowercase()

        return extension in setOf(
            "jpg","jpeg","png","gif","webp","svg",
            "mp4","mov","webm",
            "mp3","wav","ogg","aac","flac",
            "pdf","doc","docx","xls","xlsx","ppt","pptx",
            "txt","csv",
            "zip","rar","gz"
        )
    }

    private fun deleteIfManaged(url: String) {
        if (!adminStorageProvider.contains(url)) {
            return
        }

        try {
            adminStorageProvider.delete(url)
            logger.debug("Deleted uploaded file {}", url)
        } catch (e: Exception) {
            logger.warn("Failed deleting file {}", url, e)
        }
    }

}