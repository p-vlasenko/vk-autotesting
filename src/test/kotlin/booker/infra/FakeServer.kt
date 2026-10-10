package booker.infra

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

/** Локальный HTTP-сервер для тестов инфраструктуры: отвечает по заданной функции и запоминает запросы. */
class FakeServer(private val respond: (method: String, path: String) -> Pair<Int, String>) : AutoCloseable {
    val requests = mutableListOf<String>()

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            val method = exchange.requestMethod
            val path = exchange.requestURI.path
            requests += "$method $path"
            val (status, body) = respond(method, path)
            val bytes = body.toByteArray()
            exchange.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
            exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }

    val url = "http://127.0.0.1:${server.address.port}"

    override fun close() = server.stop(0)
}
