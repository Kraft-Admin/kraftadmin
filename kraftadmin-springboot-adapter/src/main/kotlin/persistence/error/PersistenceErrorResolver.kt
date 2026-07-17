//package persistence.error
//
//interface PersistenceErrorResolver {
//
//    fun resolve(
//        resource: String,
//        exception: Throwable
//    ): PersistenceError
//}

package persistence.error

interface PersistenceErrorResolver {
    fun resolve(resource: String, exception: Throwable): PersistenceErrorDetails
}