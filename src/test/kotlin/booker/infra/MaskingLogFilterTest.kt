package booker.infra

import booker.logging.MaskingLogFilter.Companion.maskBody
import booker.logging.MaskingLogFilter.Companion.maskHeader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Инфраструктура: маскировка секретов в HTTP-логе")
class MaskingLogFilterTest {

    @Test
    @DisplayName("пароль с экранированной кавычкой скрыт целиком")
    fun passwordWithEscapedQuote() {
        val masked = maskBody("""{"username":"admin","password":"p\"visible-suffix"}""")

        assertThat(masked).doesNotContain("visible").contains("\"password\":\"***\"").contains("admin")
    }

    @Test
    @DisplayName("token маскируется на любой глубине и без учёта регистра")
    fun nestedToken() {
        val masked = maskBody("""{"data":[{"Token":"abc123"}],"token":"def456"}""")

        assertThat(masked).doesNotContain("abc123").doesNotContain("def456")
    }

    @Test
    @DisplayName("не-JSON с упоминанием секрета не выводится, обычный текст выводится")
    fun nonJsonBody() {
        assertThat(maskBody("""password=s3cr3t-value&user=admin""")).doesNotContain("s3cr3t")
        assertThat(maskBody("""{"password": "s3cr3t-value" trailing""")).doesNotContain("s3cr3t")
        assertThat(maskBody("Not Found")).isEqualTo("Not Found")
    }

    @Test
    @DisplayName("Authorization, Cookie и Set-Cookie маскируются")
    fun sensitiveHeaders() {
        assertThat(listOf("Authorization", "cookie", "Set-Cookie").map { maskHeader(it, "secret") })
            .containsOnly("***")
        assertThat(maskHeader("Content-Type", "application/json")).isEqualTo("application/json")
    }
}
