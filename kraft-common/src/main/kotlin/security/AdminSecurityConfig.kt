package com.kraftadmin.security

/**
 * Top-level configuration for the admin security system.
 *
 * requiredRoles has NO default. Every consuming application defines its own
 * role model — there is no universally-correct guess for "which role means
 * admin access here." An app using this library MUST explicitly configure
 * kraftadmin.security.required-roles (or supply AdminSecurityConfig directly)
 * to whichever role(s) represent staff/admin access in ITS OWN security setup.
 * Falling back to a baked-in default here would silently grant admin access
 * based on a guess about the parent app's role naming — exactly the class of
 * bug this library must never introduce.
 */
data class AdminSecurityConfig(
    val basicAuth: BasicAuthConfig = BasicAuthConfig(),
    val customProvider: AdminSecurityProvider? = null,
    val sessionConfig: SessionConfig = SessionConfig(),
    /**
     * Supplied by adapter modules (e.g. spring-boot-adapter).
     * Called only when [frameworkSecurityActiveCheck] returns true.
     */
    val frameworkAdapterFactory: (() -> AdminSecurityProvider)? = null,
    /**
     * Optional override for framework detection logic.
     * Defaults to classpath marker scanning in [security.SecurityProviderResolver].
     */
    val frameworkSecurityActiveCheck: (() -> Boolean)? = null,

    /**
     * Roles permitted to access /admin/ when no per-route override exists
     * in [protectedRoutes]. Deliberately narrow by default — admin access
     * should be opt-in per role, never a broad set of general app roles.
     * A consuming application integrating with an existing Spring Security
     * setup (e.g. a TALENT/USER-role main app) MUST explicitly configure
     * this to whichever role(s) actually represent admin/staff access —
     * never widen this default to include general-purpose application roles.
     */

    val requiredRoles: List<String> = emptyList(),

    /**
     * Per-route-prefix role overrides (e.g. "/api/settings/" -> {"ROLE_SUPERUSER"}).
     * Longest matching prefix wins; falls back to [requiredRoles] when no
     * entry matches. See AdminSecurityFilter.resolveRouteRoles.
     */
    val protectedRoutes: Map<String, Set<String>> = emptyMap(),


    )