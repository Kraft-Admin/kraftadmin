package annotations;

import com.kraftadmin.annotations.ToolbarProfile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.FIELD}) // TYPE_USE covers Kotlin's TYPE target
public @interface RichTextConfig {
    ToolbarProfile toolbarProfile() default ToolbarProfile.MINIMAL;
    String placeholder() default "";
}