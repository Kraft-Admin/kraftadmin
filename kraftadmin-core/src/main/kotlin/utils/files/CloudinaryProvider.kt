package com.kraftadmin.utils.files

import org.slf4j.LoggerFactory
import java.util.Map

/**
 * A zero-compile-dependency Cloudinary storage adapter implementation.
 * Uses pure Java reflection pipelines to bridge KraftAdmin with the parent application's
 * loaded 'com.cloudinary.Cloudinary' library classes, avoiding mandatory binary dependency bloat.
 *
 * @property cloudinary The raw companion proxy object representing the parent app's Cloudinary instance.
 */
class CloudinaryProvider(
    private val cloudinary: Any
) : AdminStorageProvider {

    private val logger = LoggerFactory.getLogger(CloudinaryProvider::class.java)

    /**
     * Uploads an asset payload directly to Cloudinary.
     * Invokes `cloudinary.uploader().upload(bytes, params)` underneath the hood via reflection.
     */
    override fun upload(bytes: ByteArray, fileName: String, context: String): String {
        try {
            // Get the internal Uploader instance: cloudinary.uploader()
            val uploader = cloudinary.javaClass.getMethod("uploader").invoke(cloudinary)

            // Build payload parameters mapping to Cloudinary API targets
            val params = Map.of(
                "folder", "admin/$context",
                "resource_type", "auto"
            )

            // Invoke uploader.upload(bytes, params) reflectively
            val uploadMethod = uploader.javaClass.getMethod(
                "upload",
                Any::class.java, // Cloudinary uses 'Object' in the SDK for the first param
                Map::class.java
            )

            val uploadResult = uploadMethod.invoke(uploader, bytes, params) as Map<*, *>

            // Extract the return path target
            return uploadResult["secure_url"]?.toString()
                ?: uploadResult["url"]?.toString()
                ?: throw RuntimeException("Cloudinary response payload missing required URL properties")
        } catch (e: Exception) {
            logger.error("Cloudinary runtime upload failed via reflection layer", e)
            throw RuntimeException("Cloudinary upload execution failed", e)
        }
    }

    /**
     * Permanently deletes a remote Cloudinary asset after resolving its public identifier.
     * Invokes `cloudinary.uploader().destroy(publicId, options)` reflectively.
     */
    override fun delete(fileUrl: String) {
        // Ex: https://res.cloudinary.com/demo/image/upload/v123456/admin/avatar/abc-123.jpg
        // Extracts: admin/avatar/abc-123 (matching folders + public id prefix)
        val cleanPath = fileUrl.substringAfter("/upload/").substringAfter("/")
        val publicId = cleanPath.substringBeforeLast(".")

        try {
            val uploader = cloudinary.javaClass.getMethod("uploader").invoke(cloudinary)

            // Invoke uploader.destroy(publicId, Map.of()) reflectively
            val destroyMethod = uploader.javaClass.getMethod(
                "destroy",
                String::class.java,
                Map::class.java
            )
            destroyMethod.invoke(uploader, publicId, Map.of<Any, Any>())
            logger.info("Successfully initiated remote removal for Cloudinary Public ID: {}", publicId)
        } catch (e: Exception) {
            logger.error("Cloudinary asset removal failure via reflective destruction line", e)
        }
    }

    /**
     * Checks if the file path is a valid Cloudinary resource host URL.
     */
    override fun contains(fileUrl: String): Boolean {
        return fileUrl.contains("res.cloudinary.com", ignoreCase = true)
    }
}