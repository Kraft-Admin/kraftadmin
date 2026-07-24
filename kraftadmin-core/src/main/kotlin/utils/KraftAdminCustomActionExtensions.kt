package com.kraftadmin.utils

import com.kraftadmin.annotations.KraftAdminCustomAction
import com.kraftadmin.ui_descriptors.KraftActionDescriptor

fun KraftAdminCustomAction.toDescriptor(): KraftActionDescriptor {
    return KraftActionDescriptor(
        entityClass = entityClass
            .takeIf { it != Nothing::class }
            ?.simpleName,

        name = name,
        label = label,
        icon = icon,
        variant = variant,
        target = target,
        bulk = bulk,
        requiresSelection = requiresSelection,

        confirmMessage = confirmMessage
            .takeIf { it.isNotBlank() },

        hideAfterExecution = hideAfterExecution,
        refresh = refresh,
        order = order,

        permission = permission
            .takeIf { it.isNotBlank() },

        group = group
            .takeIf { it.isNotBlank() }
    )
}