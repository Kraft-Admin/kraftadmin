//package com.kraftadmin.ui_descriptors
//
//import api.responses.PagedResponse
//import api.utils.ResourceRow
//
//data class ResourceDescriptor(
//    val name: String,
//    val label: String,
//    val totalCount: Long = 0,
//    val customActions: List<KraftActionDescriptor> = emptyList(),
//    val columns: List<ColumnDescriptor>,
//    val data: PagedResponse<ResourceRow> = PagedResponse(emptyList(), 0, 0, 20, 0)
//)

package com.kraftadmin.ui_descriptors

import com.kraftadmin.annotations.KraftAdminResource
import api.responses.PagedResponse
import api.utils.ResourceRow

data class ResourceDescriptor(
    val name: String,
    val label: String,
    val group: String,
    val icon: String,
    val hidden: Boolean,
    val searchable: Boolean,
    val defaultSort: String,
    val readOnly: Boolean,
    val pageSize: Int,
    val permissionScope: String,
    val exportable: Boolean,
    val totalCount: Long = 0,
    val customActions: List<KraftActionDescriptor> = emptyList(),
    val columns: List<ColumnDescriptor>,
    val data: PagedResponse<ResourceRow>
) {
    companion object {
        /**
         * Factory method to create a descriptor from an annotated class.
         */
        fun from(clazz: Class<*>, annotation: KraftAdminResource, columns: List<ColumnDescriptor>): ResourceDescriptor {
            return ResourceDescriptor(
                name = clazz.simpleName,
                label = annotation.label.ifBlank { clazz.simpleName },
                group = annotation.group,
                icon = annotation.icon,
                hidden = annotation.hidden,
                searchable = annotation.searchable,
                defaultSort = annotation.defaultSort,
                readOnly = annotation.readOnly,
                pageSize = annotation.pageSize,
                permissionScope = annotation.permissionScope,
                exportable = annotation.exportable,
                columns = columns,
                data = PagedResponse(emptyList(), 0, 0, annotation.pageSize, 0)
            )
        }
    }
}
