package com.kraftadmin.ui_descriptors

import security.AdminUserDTO
import config.FeatureConfig
import config.LocaleConfig
import config.PaginationConfig

data class KraftAdminDescriptor(
    val basePath: String,
    val title: String,
    val version: String,
    val environment: EnvironmentDescriptor, // Changed from String to Object
    val currentUser: AdminUserDTO? = null,
    val resources: List<ResourceDescriptor>
)

data class EnvironmentDescriptor(
    val name: String,
    val authMode: String,
    val showLogout: Boolean,
    val version: String,
    val theme: ThemeDescriptor,   // Colors and Dark Mode
    val features: FeatureConfig,  // Global UI Toggles
    val pagination: PaginationConfig, // Page size limits
    val locale: LocaleConfig      // Language/Timezone
)

data class ThemeDescriptor(
    val primaryColor: String,
    val darkMode: Boolean,
    val logoUrl: String?
)
