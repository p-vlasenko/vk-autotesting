package booker.support

import booker.clients.BookingClient
import booker.config.ApiConfig
import booker.logging.HttpLog
import booker.models.Auth
import io.restassured.response.Response
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Учёт созданных тестом броней и их удаление после теста.
 * Ошибка очистки роняет успешный тест, а к упавшему JUnit добавляет её как suppressed.
 */
class BookingTracker(
    private val client: BookingClient,
    private val auth: () -> Auth,
) : AfterEachCallback {

    private val ids = linkedSetOf<Int>()

    /**
     * Регистрирует бронь, если в ответе есть bookingid — независимо от Content-Type
     * и корректности остальных полей, которые проверяются позже.
     */
    fun track(response: Response): Response = response.also {
        if (it.statusCode in 200..299) {
            runCatching { it.jsonPath().getInt("bookingid") }.getOrNull()?.let(ids::add)
        }
    }

    /** Регистрирует бронь, найденную иначе, чем по ответу на создание (например, поиском по имени). */
    fun track(id: Int) {
        ids += id
    }

    /** Снимает с учёта бронь, удаление которой уже подтвердил сам тест (DELETE → 201, GET → 404). */
    fun untrack(id: Int) {
        ids -= id
    }

    override fun afterEach(context: ExtensionContext) = cleanup()

    /**
     * Удаляет все учтённые брони. Удалённой бронь считается, только если DELETE вернул 201
     * или последующий GET вернул 404: любой другой ответ DELETE (в том числе 405, который стенд
     * отдаёт для уже удалённой брони) сам по себе не доказывает, что брони нет.
     */
    fun cleanup() {
        if (ids.isEmpty()) return
        // Авторизация запрашивается один раз: если /auth недоступен, не повторяем запрос на каждую бронь
        val authResult = runCatching(auth)
        val failures = ids.mapNotNull { id ->
            authResult.mapCatching { deleteAndConfirm(id, it) }
                .getOrElse { "booking $id: ${it.javaClass.simpleName}: ${it.message}" }
        }
        ids.clear()
        if (failures.isNotEmpty()) {
            throw AssertionError(
                "Cleanup failed, bookings may remain on the stand (reproduce data with ${ApiConfig.reproduceArgs}):\n" +
                    failures.joinToString("\n") +
                    "\nHTTP log of cleanup:\n" + HttpLog.drain().joinToString("\n")
            )
        }
    }

    /** null — бронь удалена или её уже нет, иначе описание проблемы. */
    private fun deleteAndConfirm(id: Int, auth: Auth): String? {
        val deleteCode = client.delete(id, auth).statusCode
        if (deleteCode == 201) return null
        val getCode = client.get(id).statusCode
        return if (getCode == 404) null else "booking $id: DELETE returned $deleteCode, GET returned $getCode"
    }
}
