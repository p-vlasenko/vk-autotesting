package booker.clients

import booker.config.ApiConfig
import booker.config.Specs
import booker.models.Auth
import booker.models.Booking
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification

/**
 * HTTP-обёртки над /booking. Только запросы, без проверок.
 * [baseUrl] переопределяется в локальных тестах инфраструктуры (поддельный сервер).
 */
class BookingClient(private val baseUrl: String = ApiConfig.baseUrl) {

    fun create(booking: Booking): Response =
        Given {
            spec(Specs.request)
            baseUri(baseUrl)
            body(booking)
        } When {
            post("/booking")
        }

    /** Отправка произвольного тела — для невалидного JSON. */
    fun createRaw(body: String): Response =
        Given {
            spec(Specs.request)
            baseUri(baseUrl)
            body(body)
        } When {
            post("/booking")
        }

    fun get(id: Any, accept: String? = null): Response =
        Given {
            spec(Specs.request)
            baseUri(baseUrl)
            accept?.let { accept(it) } ?: this
        } When {
            get("/booking/{id}", id)
        }

    fun list(filters: Map<String, String> = emptyMap()): Response =
        Given {
            spec(Specs.request)
            baseUri(baseUrl)
            queryParams(filters)
        } When {
            get("/booking")
        }

    fun update(id: Int, booking: Booking, auth: Auth): Response =
        Given {
            spec(Specs.request)
            baseUri(baseUrl)
            withAuth(auth)
            body(booking)
        } When {
            put("/booking/{id}", id)
        }

    fun patch(id: Int, fields: Map<String, Any>, auth: Auth): Response =
        Given {
            spec(Specs.request)
            baseUri(baseUrl)
            withAuth(auth)
            body(fields)
        } When {
            patch("/booking/{id}", id)
        }

    fun delete(id: Int, auth: Auth): Response =
        Given {
            spec(Specs.request)
            baseUri(baseUrl)
            withAuth(auth)
        } When {
            delete("/booking/{id}", id)
        }

    private fun RequestSpecification.withAuth(auth: Auth): RequestSpecification = when (auth) {
        Auth.None -> this
        is Auth.Token -> cookie("token", auth.token)
        is Auth.Basic -> auth().preemptive().basic(auth.username, auth.password)
    }
}
