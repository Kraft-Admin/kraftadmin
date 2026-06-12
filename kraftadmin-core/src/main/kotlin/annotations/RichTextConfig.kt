package com.kraftadmin.annotations

/**
 * Defines the formatting configurations available for rich text components.
 * This enum maps directly to frontend text editor module layouts (such as Quill or TinyMCE).
 *
 * @property displayName The human-readable label used in management dashboards and developer settings.
 * @property description A clear explanation of the target use case and visual footprint of the profile.
 * @property toolbarConfig A collection of toolbar button groupings. Each entry defines a logical row/block of actions.
 */
enum class ToolbarProfile(
    val displayName: String,
    val description: String,
    val toolbarConfig: List<List<Any>>
) {
    /**
     * Compact layout intended for small inline text boxes, user remarks, or simple comments
     * where complex media embedding or structural formatting is unnecessary.
     */
    MINIMAL(
        displayName = "Minimal Layout",
        description = "Provides basic character adjustments (bold, italic) and quick formatting clears. Best for inline text entry.",
        toolbarConfig = listOf(
            listOf("bold", "italic"),
            listOf("clean")
        )
    ),

    /**
     * Standard balance of structure and typography layout. Ideal for typical blog summaries,
     * product descriptions, and standard document articles.
     */
    STANDARD(
        displayName = "Standard Content Creator",
        description = "Adds header styles, blockquotes, hyperlinking, and list arrangements. Suited for primary content fields.",
        toolbarConfig = listOf(
            listOf("bold", "italic", "underline", "strike"),
            listOf(listOf("header" to 1), listOf("header" to 2), "blockquote"),
            listOf(mapOf("list" to "ordered"), mapOf("list" to "bullet")),
            listOf("link", "clean")
        )
    ),

    /**
     * Comprehensive rich-text feature engine. Includes multi-level structural elements,
     * fine-grain alignment rules, multi-media insertion, colors, and layout modifiers.
     */
    FULL(
        displayName = "Full Enterprise Publisher",
        description = "Unlocks advanced layout modules including nested script accents, code blocks, font alignment rules, and image/video injections.",
        toolbarConfig = listOf(
            listOf(mapOf("font" to emptyList<String>()), mapOf("size" to emptyList<String>())),
            listOf("bold", "italic", "underline", "strike"),
            listOf(mapOf("color" to emptyList<String>()), mapOf("background" to emptyList<String>())),
            listOf(mapOf("script" to "sub"), mapOf("script" to "super")),
            listOf(listOf("header" to 1), listOf("header" to 2), "blockquote", "code-block"),
            listOf(mapOf("list" to "ordered"), mapOf("list" to "bullet"), mapOf("indent" to "-1"), mapOf("indent" to "+1")),
            listOf(mapOf("direction" to "rtl"), mapOf("align" to emptyList<String>())),
            listOf("link", "image", "video"),
            listOf("clean")
        )
    );
}

/**
 * Nested configuration layout specifically targeting fields marked as WYSIWYG rich text inputs.
 * Allows you to tune the tool strip presence and fallback text behaviors during view rendering steps.
 *
 * Usage example:
 * ```kotlin
 * @KraftAdminField(
 * inputType = FormInputType.WYSIWYG,
 * wysiwygConfig = RichTextConfig(
 * toolbarProfile = ToolbarProfile.STANDARD,
 * placeholder = "Compose an extensive, formatted article introduction..."
 * )
 * )
 * val articleBody: String
 * ```
 *
 * @property toolbarProfile The predefined operational toolbar profile determining which formatting controls are displayed to the user.
 * @property placeholder The informational ghost string shown when the input surface remains empty. If blank, falls back to the parent component default text.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class RichTextConfig(
    val toolbarProfile: ToolbarProfile = ToolbarProfile.MINIMAL,
    val placeholder: String = ""
)