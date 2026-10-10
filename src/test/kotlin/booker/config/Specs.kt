package booker.config

import booker.logging.MaskingLogFilter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.type.LogicalType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.restassured.builder.RequestSpecBuilder
import io.restassured.builder.ResponseSpecBuilder
import io.restassured.config.HttpClientConfig
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification
import org.hamcrest.Matchers.equalTo

object Specs {

    val objectMapper = jacksonObjectMapper()
        // Отсутствующие (null) поля не отправляются — так строятся запросы без обязательных полей
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        // Структуру ответа проверяет JSON Schema, DTO не падают на новых полях
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        // Типы строгие: "777" не становится Int, "true" и 1 — Boolean, 1.5 — Int, 123 — String
        .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false)
        .apply {
            coercionConfigDefaults().setCoercion(CoercionInputShape.String, CoercionAction.Fail)
            coercionConfigFor(LogicalType.Textual).apply {
                setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
            }
            coercionConfigFor(LogicalType.Boolean).setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
        }

    private val config: RestAssuredConfig = RestAssuredConfig.config()
        .httpClient(
            HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", ApiConfig.connectTimeoutMs)
                .setParam("http.socket.timeout", ApiConfig.readTimeoutMs)
        )
        .objectMapperConfig(
            ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory { _, _ -> objectMapper }
        )

    /** Базовая спецификация запроса: адрес, таймауты, JSON, логирование с маскировкой. */
    val request: RequestSpecification = RequestSpecBuilder()
        .setBaseUri(ApiConfig.baseUrl)
        .setConfig(config)
        .setContentType(ContentType.JSON)
        // Строкой, а не ContentType.JSON: список из нескольких MIME-типов API отклоняет с 418
        .setAccept("application/json")
        .addFilter(MaskingLogFilter())
        .build()

    /** Успешный JSON-ответ: 200 + application/json. */
    val okJson: ResponseSpecification = ResponseSpecBuilder()
        .expectStatusCode(200)
        .expectContentType(ContentType.JSON)
        .build()

    /** Текстовый ответ API (ошибки и 201 Created): точные статус, Content-Type и тело. */
    fun textResponse(status: Int, body: String): ResponseSpecification = ResponseSpecBuilder()
        .expectStatusCode(status)
        .expectContentType(ContentType.TEXT)
        .expectBody(equalTo(body))
        .build()

    val created = textResponse(201, "Created")
    val badRequest = textResponse(400, "Bad Request")
    val forbidden = textResponse(403, "Forbidden")
    val notFound = textResponse(404, "Not Found")
    val methodNotAllowed = textResponse(405, "Method Not Allowed")
    val teapot = textResponse(418, "I'm a Teapot")
    val serverError = textResponse(500, "Internal Server Error")
}
