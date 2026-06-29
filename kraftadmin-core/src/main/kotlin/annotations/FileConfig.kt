package com.kraftadmin.annotations

/**
 * Configuration for fields of type [FormInputType.FILE] or [FormInputType.IMAGE].
 */
@Target(AnnotationTarget.TYPE) // Or AnnotationTarget.ANNOTATION_CLASS
@Retention(AnnotationRetention.RUNTIME)
annotation class FileConfig(
    val multiple: Boolean = false,
    val maxFiles: Int = 1,
    val allowedExtensions: Array<FileExtension> = [],
    val minSizeBytes: Long = 0,
    val maxSizeBytes: Long = 10 * 1024 * 1024,
    val allowedMimeTypes: Array<MimeType> = []
)

enum class FileExtension(val value: String) {
    // Documents
    PDF("pdf"), DOCX("docx"), DOC("doc"), TXT("txt"), CSV("csv"),
    // Images
    JPG("jpg"), JPEG("jpeg"), PNG("png"), GIF("gif"), WEBP("webp"), SVG("svg"),
    // Video/Audio
    MP4("mp4"), WEBM("webm"), MOV("mov"), MP3("mp3"), WAV("wav"),
    // Audio
    OGG("ogg"), AAC("aac"), FLAC("flac"),
    // Documents
    XLSX("xlsx"), XLS("xls"), PPTX("pptx"), PPT("ppt"),
    // Archives
    ZIP("zip"), RAR("rar"), GZ("gz");
}

enum class MimeType(val value: String) {
    // Documents
    PDF("application/pdf"),
    MS_WORD("application/msword"),
    MS_WORD_X("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    CSV("text/csv"),
    TEXT("text/plain"),
    // Images
    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png"),
    IMAGE_GIF("image/gif"),
    IMAGE_WEBP("image/webp"),
    IMAGE_SVG("image/svg+xml"),
    // Video/Audio
    VIDEO_MP4("video/mp4"),
    VIDEO_WEBM("video/webm"),
    AUDIO_MPEG("audio/mpeg"),
    // Audio
    AUDIO_OGG("audio/ogg"),
    AUDIO_AAC("audio/aac"),
    AUDIO_FLAC("audio/flac"),
    // Documents
    MS_EXCEL("application/vnd.ms-excel"),
    MS_EXCEL_X("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    MS_POWERPOINT("application/vnd.ms-powerpoint"),
    MS_POWERPOINT_X("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    // Archives
    ZIP("application/zip");
}