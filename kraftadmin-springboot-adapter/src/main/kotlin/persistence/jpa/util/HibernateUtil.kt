package persistence.jpa.util

import org.hibernate.proxy.HibernateProxy

object HibernateUtil {

    /**
     * Unwraps a Hibernate proxy to its real underlying entity.
     * Safe to call on non-proxy objects and nulls.
     */
    fun unproxy(value: Any?): Any? {
        if (value == null) return null
        return if (value is HibernateProxy) {
            value.hibernateLazyInitializer.implementation
        } else {
            value
        }
    }
}