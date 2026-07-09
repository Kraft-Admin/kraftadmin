package persistence.jpa.mapper

import api.utils.ResourceRow
import jakarta.persistence.EntityManager
import java.util.Optional

class EntityMapper(entityManager: EntityManager) {
    fun toEntity(data: Map<String, Any?>) {}

    fun toRow(entity: Any) : ResourceRow {
        return ResourceRow(
            id = TODO(),
            values = TODO(),
            metadata = TODO()
        )
    }
}