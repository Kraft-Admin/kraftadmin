package discovery.descriptors.column.jpa

import com.kraftadmin.annotations.FileConfig
import com.kraftadmin.annotations.FileConfigDefaults
import com.kraftadmin.enums.FormInputType
import com.kraftadmin.ui_descriptors.FileConfigDescriptor

/**
 * Resolves file upload configuration for FILE/IMAGE/VIDEO fields.
 *
 * If no @FileConfig annotation is present, sensible defaults are used.
 */
class JpaFileResolver {

    fun resolve(
        type: FormInputType,
        annotation: FileConfig?
    ): FileConfigDescriptor? {

        if (
            type != FormInputType.FILE &&
            type != FormInputType.IMAGE &&
            type != FormInputType.VIDEO &&
            type != FormInputType.AUDIO &&
            type != FormInputType.DOCUMENT
        ) {
            return null
        }

        val defaults = FileConfigDefaults.getDefaultsFor(type)

        // No annotation -> return defaults
        if (annotation == null) {
            return defaults
        }

        // Merge annotation values with defaults
        return FileConfigDescriptor(

            multiple = annotation.multiple,

            maxFiles = annotation.maxFiles,

            allowedExtensions =
                annotation.allowedExtensions
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.value }
                    ?: defaults.allowedExtensions,

            maxSizeBytes =
                annotation.maxSizeBytes
                    .takeIf { it > 0 }
                    ?: defaults.maxSizeBytes,

            minSizeBytes = annotation.minSizeBytes,

            allowedMimeTypes =
                annotation.allowedMimeTypes
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.value }
                    ?: defaults.allowedMimeTypes
        )
    }
}