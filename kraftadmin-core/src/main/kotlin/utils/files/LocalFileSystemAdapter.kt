//package com.kraftadmin.utils.files
//
//import com.kraftadmin.logging.KraftAdminLogging
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import java.io.File
//import java.nio.file.Files
//import java.util.*
//
//class LocalFileSystemAdapter(
//    private val uploadDir: String = "uploads/admin",
//    private val publicPrefix: String = "/admin/files",
//) : AdminStorageProvider {
//
//    private val logger = KraftAdminLogging.logger(javaClass)
//
//
//    init {
//        // Ensure the directory exists immediately on startup
//        logger.info("KraftAdmin: No cloud storage detected. Falling back to local: {}", uploadDir)
//        File(uploadDir).mkdirs()
//    }
//
//    override fun upload(bytes: ByteArray, fileName: String, context: String, baseUrl: String): String {
//        val extension = fileName.substringAfterLast(".", "bin")
//        // Use context namespace within the filename string to mirror cloud structures
//        val uniqueName = "$context-${UUID.randomUUID()}.$extension"
//        val targetFile = File(uploadDir, uniqueName)
//
//        Files.write(targetFile.toPath(), bytes)
//
//        // Ensure public path format is cleanly normalized
//        val sanitizedPrefix = if (publicPrefix.endsWith("/")) publicPrefix else "$publicPrefix/"
//        return "$sanitizedPrefix$uniqueName"
//    }
//
//    override fun delete(fileUrl: String) {
//        try {
//            // Extract the filename from the URL route tail end
//            // e.g., /admin/files/users-abc-123.jpg -> users-abc-123.jpg
//            val fileName = fileUrl.substringAfterLast("/")
//            val file = File(uploadDir, fileName)
//
//            if (file.exists()) {
//                val deleted = file.delete()
//                if (deleted) logger.info("Successfully deleted old local file: {}", fileName)
//            }
//        } catch (e: Exception) {
//            logger.error("Failed to delete local file at $fileUrl", e)
//        }
//    }
//
//    /**
//     * Confirms ownership if the incoming reference matches the local public routing prefix signature.
//     */
//    override fun contains(fileUrl: String): Boolean {
//        val sanitizedPrefix = publicPrefix.trimEnd('/')
//        return fileUrl.startsWith(sanitizedPrefix) || fileUrl.contains("/files/")
//    }
//}


package com.kraftadmin.utils.files

import com.kraftadmin.logging.KraftAdminLogging
import java.io.File
import java.nio.file.Files
import java.util.*

class LocalFileSystemAdapter(
    private val uploadDir: String = "uploads/admin",
    private val publicPrefix: String = "/admin/files",
) : AdminStorageProvider {

    private val logger = KraftAdminLogging.logger(javaClass)

    init {
        File(uploadDir).mkdirs()
    }

    override fun upload(bytes: ByteArray, fileName: String, context: String, baseUrl: String): String {
        val extension = fileName.substringAfterLast(".", "bin")
        val uniqueName = "$context-${UUID.randomUUID()}.$extension"
        Files.write(File(uploadDir, uniqueName).toPath(), bytes)

        val relativePath = "/${publicPrefix.trim('/')}/$uniqueName"

        // If baseUrl is provided (e.g., http://localhost:8080), prepend it
        return if (baseUrl.isNotBlank()) "${baseUrl.trimEnd('/')}$relativePath" else relativePath
    }

    override fun delete(fileUrl: String) {
        try {
            // Remove the domain/base if it's there, then extract the filename
            val path = if (fileUrl.contains("://")) {
                // Strip the domain part: "http://localhost:8080/admin/files/uuid.jpg" -> "/admin/files/uuid.jpg"
                "/" + fileUrl.substringAfter("://").substringAfter("/", "")
            } else {
                fileUrl
            }

            val fileName = path.substringAfterLast("/")
            val file = File(uploadDir, fileName)

            if (file.exists() && file.delete()) {
                logger.info("Successfully deleted local file: {}", fileName)
            }
        } catch (e: Exception) {
            logger.error("Failed to delete local file at $fileUrl", e)
        }
    }

    override fun contains(fileUrl: String): Boolean {
        return fileUrl.contains(publicPrefix.trim('/'))
    }
}