package com.kraftadmin.annotations

import com.kraftadmin.enums.FormInputType
import com.kraftadmin.ui_descriptors.FileConfigDescriptor

object FileConfigDefaults {
    fun getDefaultsFor(type: FormInputType): FileConfigDescriptor {
        return when (type) {
            FormInputType.IMAGE -> FileConfigDescriptor(
                allowedExtensions = listOf(
                    FileExtension.JPG.value, FileExtension.JPEG.value,
                    FileExtension.PNG.value, FileExtension.WEBP.value, FileExtension.SVG.value
                ),
                allowedMimeTypes = listOf(
                    MimeType.IMAGE_JPEG.value, MimeType.IMAGE_PNG.value,
                    MimeType.IMAGE_WEBP.value, MimeType.IMAGE_SVG.value
                )
            )
            FormInputType.VIDEO -> FileConfigDescriptor(
                allowedExtensions = listOf(
                    FileExtension.MP4.value, FileExtension.WEBM.value, FileExtension.MOV.value
                ),
                allowedMimeTypes = listOf(
                    MimeType.VIDEO_MP4.value, MimeType.VIDEO_WEBM.value
                )
            )
            FormInputType.AUDIO -> FileConfigDescriptor(
                allowedExtensions = listOf(
                    FileExtension.MP3.value, FileExtension.WAV.value,
                    FileExtension.OGG.value, FileExtension.AAC.value, FileExtension.FLAC.value
                ),
                allowedMimeTypes = listOf(
                    MimeType.AUDIO_MPEG.value, MimeType.AUDIO_OGG.value,
                    MimeType.AUDIO_AAC.value, MimeType.AUDIO_FLAC.value
                )
            )
            FormInputType.DOCUMENT -> FileConfigDescriptor(
                allowedExtensions = listOf(
                    FileExtension.PDF.value, FileExtension.DOCX.value, FileExtension.DOC.value,
                    FileExtension.XLSX.value, FileExtension.XLS.value, FileExtension.TXT.value,
                    FileExtension.CSV.value, FileExtension.PPTX.value
                ),
                allowedMimeTypes = listOf(
                    MimeType.PDF.value, MimeType.MS_WORD.value, MimeType.MS_WORD_X.value,
                    MimeType.MS_EXCEL.value, MimeType.MS_EXCEL_X.value, MimeType.TEXT.value,
                    MimeType.CSV.value, MimeType.MS_POWERPOINT_X.value
                )
            )
            FormInputType.FILE -> FileConfigDescriptor(
                // General purpose file upload (includes archives)
                allowedExtensions = listOf(
                    FileExtension.PDF.value, FileExtension.ZIP.value, FileExtension.RAR.value
                ),
                allowedMimeTypes = listOf(
                    MimeType.PDF.value, MimeType.ZIP.value
                )
            )
            else -> FileConfigDescriptor()
        }
    }
}
