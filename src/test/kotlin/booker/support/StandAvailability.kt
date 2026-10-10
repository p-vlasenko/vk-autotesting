package booker.support

import booker.clients.HealthClient
import booker.config.ApiConfig
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.opentest4j.TestAbortedException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Один раз за прогон ждёт ответа GET /ping (polling до [ApiConfig.standWaitMs]). Если стенд так и не ответил,
 * первый тестовый класс падает с понятной причиной, остальные прерываются (aborted) без ожидания таймаутов
 * в каждом тесте.
 */
class StandAvailability : BeforeAllCallback {

    private class Status(val error: Throwable?) {
        val reported = AtomicBoolean(false)
    }

    override fun beforeAll(context: ExtensionContext) {
        val status = context.root.getStore(NAMESPACE)
            .getOrComputeIfAbsent("status", { Status(waitForStand()) }, Status::class.java)
        val error = status.error ?: return
        val message = "Stand ${ApiConfig.baseUrl} is unavailable after ${ApiConfig.standWaitMs} ms: " +
            "${error.javaClass.simpleName}: ${error.message}"
        if (status.reported.compareAndSet(false, true)) throw AssertionError(message, error)
        throw TestAbortedException("$message (see the first failed test class)")
    }

    /**
     * Опрашивает /ping, пока не получит 201 или не истечёт срок. Срок проверяется между попытками,
     * поэтому последняя попытка может закончиться позже на величину таймаутов соединения и чтения.
     * Возвращает ошибку последней попытки или null, если стенд ответил.
     */
    private fun waitForStand(): Throwable? {
        val deadline = System.nanoTime() + ApiConfig.standWaitMs * 1_000_000
        while (true) {
            val error = ping() ?: return null
            if (System.nanoTime() + POLL_INTERVAL_MS * 1_000_000 > deadline) return error
            Thread.sleep(POLL_INTERVAL_MS)
        }
    }

    private fun ping(): Throwable? = runCatching {
        val code = HealthClient().ping().statusCode
        check(code == 201) { "GET /ping returned $code" }
    }.exceptionOrNull()

    companion object {
        private val NAMESPACE = ExtensionContext.Namespace.create(StandAvailability::class.java)
        private const val POLL_INTERVAL_MS = 3_000L
    }
}
