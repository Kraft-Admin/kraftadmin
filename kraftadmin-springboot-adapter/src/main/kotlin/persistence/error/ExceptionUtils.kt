package persistence.error

object ExceptionUtils {

    inline fun <reified T : Throwable> hasCause(t: Throwable?): Boolean {
        var current = t

        while (current != null) {
            if (current is T) {
                return true
            }

            current = current.cause
        }

        return false
    }

    inline fun <reified T : Throwable> findCause(t: Throwable?): T? {
        var current = t

        while (current != null) {
            if (current is T) {
                return current
            }

            current = current.cause
        }

        return null
    }
}