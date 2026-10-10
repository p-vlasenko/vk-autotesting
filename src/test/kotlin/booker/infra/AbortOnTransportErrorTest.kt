package booker.infra

import booker.support.AbortOnTransportError
import com.fasterxml.jackson.core.JsonParseException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.opentest4j.TestAbortedException
import java.lang.reflect.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@DisplayName("Инфраструктура: AbortOnTransportError")
class AbortOnTransportErrorTest {

    private val handler = AbortOnTransportError()

    // Обработчик контекст не использует
    private val context = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ExtensionContext::class.java)) { _, _, _ ->
        throw UnsupportedOperationException()
    } as ExtensionContext

    @Test
    @DisplayName("сетевая ошибка, в том числе в цепочке причин, превращается в aborted")
    fun transportErrorAborts() {
        listOf(UnknownHostException("stand"), RuntimeException(SocketTimeoutException("Read timed out"))).forEach { error ->
            assertThatThrownBy { handler.handleTestExecutionException(context, error) }
                .isInstanceOf(TestAbortedException::class.java)
        }
    }

    @Test
    @DisplayName("ошибка разбора ответа и AssertionError пробрасываются как есть")
    fun otherErrorsAreRethrown() {
        listOf(JsonParseException(null, "bad json"), AssertionError("expected 400")).forEach { error ->
            assertThatThrownBy { handler.handleTestExecutionException(context, error) }.isSameAs(error)
        }
    }
}
