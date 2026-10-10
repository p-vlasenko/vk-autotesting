package booker.clients

import booker.config.ApiConfig
import booker.config.Specs
import booker.models.AuthRequest
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response

class AuthClient {

    fun auth(username: String, password: String): Response =
        Given {
            spec(Specs.request)
            body(AuthRequest(username, password))
        } When {
            post("/auth")
        }

    /** Токен для демо-аккаунта из [ApiConfig]. */
    fun token(): String =
        auth(ApiConfig.username, ApiConfig.password) Then {
            spec(Specs.okJson)
        } Extract {
            path<String>("token") ?: error("POST /auth did not return a token")
        }
}
