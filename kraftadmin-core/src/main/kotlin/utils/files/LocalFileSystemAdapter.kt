package com.kraftadmin.utils.files

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.util.*

class LocalFileSystemAdapter(
    private val uploadDir: String = "uploads/admin",
    private val publicPrefix: String = "/admin/files",
) : AdminStorageProvider {

    private val logger: Logger = LoggerFactory.getLogger(LocalFileSystemAdapter::class.java)

    init {
        // Ensure the directory exists immediately on startup
        logger.info("KraftAdmin: No cloud storage detected. Falling back to local: {}", uploadDir)
        File(uploadDir).mkdirs()
    }

    override fun upload(bytes: ByteArray, fileName: String, context: String): String {
        val extension = fileName.substringAfterLast(".", "bin")
        // Use context namespace within the filename string to mirror cloud structures
        val uniqueName = "$context-${UUID.randomUUID()}.$extension"
        val targetFile = File(uploadDir, uniqueName)

        Files.write(targetFile.toPath(), bytes)

        // Ensure public path format is cleanly normalized
        val sanitizedPrefix = if (publicPrefix.endsWith("/")) publicPrefix else "$publicPrefix/"
        return "$sanitizedPrefix$uniqueName"
    }

    override fun delete(fileUrl: String) {
        try {
            // Extract the filename from the URL route tail end
            // e.g., /admin/files/users-abc-123.jpg -> users-abc-123.jpg
            val fileName = fileUrl.substringAfterLast("/")
            val file = File(uploadDir, fileName)

            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) logger.info("Successfully deleted old local file: {}", fileName)
            }
        } catch (e: Exception) {
            logger.error("Failed to delete local file at $fileUrl", e)
        }
    }

    /**
     * Confirms ownership if the incoming reference matches the local public routing prefix signature.
     */
    override fun contains(fileUrl: String): Boolean {
        val sanitizedPrefix = publicPrefix.trimEnd('/')
        return fileUrl.startsWith(sanitizedPrefix) || fileUrl.contains("/files/")
    }
}