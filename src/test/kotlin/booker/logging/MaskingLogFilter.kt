package booker.logging

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.restassured.filter.Filter
import io.restassured.filter.FilterContext
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification

/**
 * Запоминает каждый HTTP-обмен текущего теста в [HttpLog] с маскировкой секретов.
 * Печатается только при падении теста (см. [LogOnFailureExtension]).
 */
class MaskingLogFilter : Filter {

    override fun filter(
        request: FilterableRequestSpecification,
        responseSpec: FilterableResponseSpecification,
        ctx: FilterContext,
    ): Response {
        // Запрос форматируется до отправки, чтобы попасть в лог и при транспортной ошибке
        val requestLog = buildString {
            appendLine("--> ${request.method} ${request.uri}")
            request.headers.forEach { appendLine("    ${it.name}: ${maskHeader(it.name, it.value)}") }
            request.cookies.forEach { appendLine("    Cookie: ${it.name}=***") }
            request.getBody<Any?>()?.let { appendLine("    ${maskBody(it.toString())}") }
        }
        val response = try {
            ctx.next(request, responseSpec)
        } catch (e: Exception) {
            HttpLog.add("$requestLog<-- NO RESPONSE: ${e.javaClass.name}: ${e.message}")
            throw e
        }
        HttpLog.add(
            buildString {
                append(requestLog)
                appendLine("<-- ${response.statusLine}")
                response.headers.forEach { appendLine("    ${it.name}: ${maskHeader(it.name, it.value)}") }
                append("    ${maskBody(response.asString())}")
            }
        )
        return response
    }

    companion object {
        private val sensitiveHeaders = setOf("authorization", "cookie", "set-cookie")
        private val sensitiveFields = setOf("token", "password")
        private val mapper = ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)

        fun maskHeader(name: String, value: String): String =
            if (name.lowercase() in sensitiveHeaders) "***" else value

        /**
         * JSON разбирается и секретные поля заменяются на любой глубине. Тело, которое не разбирается
         * как JSON, но упоминает секретное поле, не выводится вовсе.
         */
        fun maskBody(body: String): String {
            if (body.isBlank()) return body
            val tree = try {
                mapper.readTree(body)
            } catch (_: JacksonException) {
                null
            }
            return when {
                tree != null -> mapper.writeValueAsString(mask(tree))
                sensitiveFields.any { body.contains(it, ignoreCase = true) } ->
                    "<body hidden: not valid JSON and may contain secrets>"
                else -> body
            }
        }

        private fun mask(node: JsonNode): JsonNode {
            when (node) {
                is ObjectNode -> node.fieldNames().asSequence().toList().forEach { name ->
                    if (name.lowercase() in sensitiveFields) node.put(name, "***") else mask(node.get(name))
                }
                else -> node.forEach { mask(it) }
            }
            return node
        }
    }
}
