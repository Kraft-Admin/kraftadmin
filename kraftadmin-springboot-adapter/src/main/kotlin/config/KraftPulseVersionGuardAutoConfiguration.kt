package config

import org.springframework.boot.SpringBootVersion
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import jakarta.annotation.PostConstruct
import org.springframework.core.Ordered

@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
class KraftPulseVersionGuardAutoConfiguration {

    @PostConstruct
    fun checkSupportedVersion() {
        val version = SpringBootVersion.getVersion() ?: return // can't determine, don't block

        val majorVersion = version.substringBefore(".").toIntOrNull() ?: return

        if (majorVersion >= 4) {
            throw IllegalStateException(
                """
                KraftPulse/KraftAdmin: Unsupported Spring Boot version detected: $version
                
                This version of the library supports Spring Boot 3.x only (3.2.0+).
                Spring Boot 4.x introduces breaking changes (Jakarta EE 11, Framework 7)
                that are not yet supported.
                
                Please either:
                  1. Pin your application to Spring Boot 3.x, or
                  2. Check for a newer KraftPulse/KraftAdmin release with Boot 4.x support.
                """.trimIndent()
            )
        }
    }
}