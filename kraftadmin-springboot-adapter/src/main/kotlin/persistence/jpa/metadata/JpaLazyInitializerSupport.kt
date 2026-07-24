package persistence.jpa.metadata

/**
 * JPA-only concern: forcing initialization of lazy Hibernate proxies/collections
 * while the persistence session is still open. Not part of the generic
 * KraftEntityMetadata contract because Mongo/R2DBC have no equivalent notion
 * of a lazy-loading session.
 */
interface JpaLazyInitializationSupport {
    fun ensureLobsInitialized(entity: Any)
    fun ensureSingleRelationsInitialized(entity: Any)
}