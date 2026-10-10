package booker.logging

import booker.config.ApiConfig
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Печатает запросы и ответы, если тест упал. Срабатывает до @AfterEach,
 * поэтому в лог не попадают запросы очистки.
 */
class LogOnFailureExtension : BeforeEachCallback, AfterTestExecutionCallback {

    override fun beforeEach(context: ExtensionContext) = HttpLog.clear()

    override fun afterTestExecution(context: ExtensionContext) {
        val exchanges = HttpLog.drain()
        if (context.executionException.isPresent) {
            println("HTTP log for failed test ${context.displayName} (reproduce data with ${ApiConfig.reproduceArgs}):")
            exchanges.forEach(::println)
        }
    }
}
