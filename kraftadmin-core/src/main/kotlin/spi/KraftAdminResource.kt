package spi

import api.responses.KraftOperationResponse
import api.responses.PagedResponse
import api.utils.ResourceRow
import com.kraftadmin.spi.KraftAdminColumn
import com.kraftadmin.ui_descriptors.KraftActionDescriptor
import com.kraftadmin.ui_descriptors.ResourceDescriptor
import kotlin.reflect.KClass

interface KraftAdminResource<T : Any> {

    val name: String
    val label: String

    // Metadata properties from @KraftAdminResource
    val group: String
    val icon: String
    val isHidden: Boolean
    val isSearchable: Boolean
    val defaultSort: String
    val isReadOnly: Boolean
    val pageSize: Int
    val permissionScope: String
    val searchableColumns: List<String>
    val sortableColumns: List<String>
    val isExportable: Boolean
    val customActions: List<KraftActionDescriptor>
    val entityClass: KClass<T>
    val columns: List<KraftAdminColumn>
    fun getIdentifier(entity: T): Any

    var dataProvider: KraftDataProvider<T>?

    /**
     * Fetch all rows for this resource
     * Returns a list of maps where key = column name, value = field value
     */
    fun getAllRows(
        page: Int,
        size: Int,
        query: String?,
        columns: List<KraftAdminColumn>,
        sortField: String?,
        sortDirection: String?
    ): PagedResponse<ResourceRow> =
        dataProvider?.fetchAll(page, size, query, columns, sortField, sortDirection) ?: PagedResponse(emptyList(), 0, 0, 0, 0)

    fun getById(id: String) = dataProvider?.fetchById(id, columns)

    fun save(name: String, data: Map<String, Any?>) = dataProvider?.save(name = name, data = data)

    fun delete(id: String): KraftOperationResponse<Unit>? = dataProvider?.delete(id)

    fun countAll(name: String): Long? = dataProvider?.countAll(name)

    fun toDescriptor(): ResourceDescriptor =
        ResourceDescriptor(
            name = name,
            label = label,
            totalCount = countAll(name) ?: 0L,
            customActions = customActions.toList(),
            columns = columns.map { it.toDescriptor() },
            data = PagedResponse(emptyList(), 0, 0, pageSize, 0),
            group = group,
            icon = icon,
            hidden = isHidden,
            searchable = isSearchable,
            defaultSort = defaultSort,
            readOnly = isReadOnly,
            pageSize = pageSize,
            permissionScope = permissionScope,
            exportable = isExportable,
            searchableFields = searchableColumns,
            sortableFields = sortableColumns,
        )

}
