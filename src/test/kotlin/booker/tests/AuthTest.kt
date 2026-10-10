package booker.tests

import booker.config.ApiConfig
import booker.config.Specs
import io.restassured.module.kotlin.extensions.Then
import org.hamcrest.Matchers.blankOrNullString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AuthTest : BaseTest() {

    @Test
    @DisplayName("AU-1: POST /auth с верными кредами возвращает токен")
    fun validCredentialsReturnToken() {
        authClient.auth(ApiConfig.username, ApiConfig.password) Then {
            spec(Specs.okJson)
            body("token", instanceOf<String>(String::class.java))
            body("token", not<String>(blankOrNullString()))
            body("reason", nullValue())
        }
    }

    // Особенность API: неверные креды → 200 + reason, а не 401
    @ParameterizedTest(name = "AU-2: {0}")
    @MethodSource("invalidCredentials")
    fun invalidCredentialsAreRejected(case: String, username: String, password: String) {
        authClient.auth(username, password) Then {
            spec(Specs.okJson)
            body("reason", equalTo("Bad credentials"))
            body("token", nullValue())
        }
    }

    companion object {
        @JvmStatic
        fun invalidCredentials() = listOf(
            Arguments.of("неверный пароль", ApiConfig.username, "wrong-password"),
            Arguments.of("неверный логин", "no-such-user", ApiConfig.password),
            Arguments.of("пустые логин и пароль", "", ""),
        )
    }
}
