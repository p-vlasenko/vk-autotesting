package booker.support

import org.apache.http.NoHttpResponseException
import org.apache.http.conn.ConnectTimeoutException
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import org.opentest4j.TestAbortedException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Превращает транспортную ошибку (DNS, таймаут, обрыв соединения) в aborted.
 * Для known-bug тестов падение должно означать «API ответил неправильно», а не «стенд не ответил».
 *
 * Ловит только сетевые исключения, а не любой IOException: ошибки разбора ответа Jackson
 * (JacksonException) тоже IOException, но означают, что стенд ответил, и должны ронять тест.
 * Ошибки очистки (BookingTracker) сюда не попадают: обработчик видит только исключения из тела теста.
 */
class AbortOnTransportError : TestExecutionExceptionHandler {
    override fun handleTestExecutionException(context: ExtensionContext, throwable: Throwable) {
        val transportError = generateSequence(throwable) { it.cause }.firstOrNull(::isTransportError)
            ?: throw throwable
        throw TestAbortedException("Stand did not respond: ${transportError.javaClass.simpleName}: ${transportError.message}", throwable)
    }

    companion object {
        // ConnectException и HttpHostConnectException — подклассы SocketException
        private val transportErrors = listOf(
            SocketException::class,
            SocketTimeoutException::class,
            UnknownHostException::class,
            SSLException::class,
            NoHttpResponseException::class,
            ConnectTimeoutException::class,
        )

        private fun isTransportError(error: Throwable): Boolean = transportErrors.any { it.isInstance(error) }
    }
}
