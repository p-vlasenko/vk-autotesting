package booker.clients

import booker.config.Specs
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response

class HealthClient {

    fun ping(): Response =
        Given {
            spec(Specs.request)
        } When {
            get("/ping")
        }
}
