package com.kraftadmin.utils.files

/**
 * Core storage abstraction layer for KraftAdmin.
 * Defines standard asset lifecycle hooks used by auto-generated administrative resource controllers.
 * * This contract isolates the platform engine from concrete storage platform specifications
 * (e.g., Local File System, AWS S3, Cloudinary), allowing seamless fallback transitions.
 */
interface AdminStorageProvider {

    /**
     * Uploads a raw binary asset payload to the storage engine and returns its fully qualified public URL.
     *
     * @param bytes The raw file content data stream.
     * @param fileName The original file provider including its filename extension (e.g., "avatar.png").
     * @param context A folder organizational namespace hint (e.g., "users", "products", "blog-posts").
     * @return The absolute public HTTP(S) URL or root-relative path pointing to the created asset.
     * @throws RuntimeException If any network transport, authorization, or disk I/O faults occur.
     */
    fun upload(bytes: ByteArray, fileName: String, context: String): String

    /**
     * Replaces an existing storage asset with a new binary payload.
     * Optimizes asset updates by cleaning up the old reference if necessary.
     *
     * @param oldFileUrl The fully qualified public URL of the asset currently stored and slated for replacement.
     * @param bytes The raw file content data stream for the new replacement file.
     * @param fileName The original provider of the replacement file.
     * @param context A folder organizational namespace hint matching the target context.
     * @return The absolute public URL of the newly created replacement asset.
     */
    fun update(oldFileUrl: String?, bytes: ByteArray, fileName: String, context: String): String {
        if (!oldFileUrl.isNullOrBlank() && contains(oldFileUrl)) {
            delete(oldFileUrl)
        }
        return upload(bytes, fileName, context)
    }

    /**
     * Deletes a stored asset permanently from the underlying storage layer using its public URL identifier.
     * If the asset does not exist or has already been cleared, this method fails gracefully without blowing up.
     *
     * @param fileUrl The fully qualified public URL or absolute storage location path of the asset to be deleted.
     */
    fun delete(fileUrl: String)

    /**
     * Utility evaluation check to determine if a given public URL string points to an asset
     * managed or hosted by this specific storage provider engine instance.
     *
     * @param fileUrl The public URL character string to examine.
     * @return `true` if the URL schema matches this provider's domain/directory footprints; `false` otherwise.
     */
    fun contains(fileUrl: String): Boolean
}