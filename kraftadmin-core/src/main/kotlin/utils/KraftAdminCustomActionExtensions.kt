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

//fun KraftAdminCustomAction.toDescriptor(): KraftActionDescriptor {
//    return KraftActionDescriptor(
//        entityClass = this.entityClass.takeIf { it != Nothing::class }?.simpleName,
//        name = this.name,
//        label = this.label,
//        icon = this.icon,
//        variant = this.variant,
//        target = this.target,
//        bulk = this.bulk,
//        requiresSelection = this.requiresSelection,
//        confirmMessage = this.confirmMessage.takeIf { it.isNotBlank() },
//        hideAfterExecution = this.hideAfterExecution,
//        refresh = this.refresh,
//        order = this.order,
//        permission = this.permission.takeIf { it.isNotBlank() },
//        group = this.group.takeIf { it.isNotBlank() }
//    )
//}