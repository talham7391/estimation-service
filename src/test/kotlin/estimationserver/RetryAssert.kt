package estimationserver

import kotlinx.coroutines.delay

suspend inline fun <T : Any> retryUntilNotNull (delay: Long, times: Int, func: () -> T?) : T? {
    for (i in 0 until times) {
        delay(delay)
        val x = func()
        if (x != null) {
            return x
        }
        delay(delay)
    }
    return null
}