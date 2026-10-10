package booker.tests

import booker.config.Specs
import io.restassured.module.kotlin.extensions.Then
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class HealthCheckTest : BaseTest() {

    @Test
    @DisplayName("HC-1: GET /ping — стенд доступен (201 Created)")
    fun pingReturnsCreated() {
        healthClient.ping() Then {
            spec(Specs.created)
        }
    }
}
