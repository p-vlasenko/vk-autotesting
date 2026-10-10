package booker.infra

import booker.clients.BookingClient
import booker.models.Auth
import booker.support.BookingTracker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Инфраструктура: BookingTracker на поддельном сервере")
class BookingTrackerTest {

    private fun tracker(server: FakeServer) = BookingTracker(BookingClient(server.url)) { Auth.Token("t") }

    @Test
    @DisplayName("ID регистрируется из 2xx-ответа даже с text/plain; ответ 4xx не регистрируется")
    fun tracksIdFromAnySuccessfulResponse() {
        FakeServer { method, _ ->
            when (method) {
                "POST" -> 200 to """{"bookingid":7}"""
                else -> 201 to "Created"
            }
        }.use { server ->
            val tracker = tracker(server)
            val client = BookingClient(server.url)

            tracker.track(client.createRaw("{}"))
            tracker.cleanup()

            assertThat(server.requests).containsExactly("POST /booking", "DELETE /booking/7")
        }
        FakeServer { _, _ -> 400 to """{"bookingid":8}""" }.use { server ->
            val tracker = tracker(server)

            tracker.track(BookingClient(server.url).createRaw("{}"))
            tracker.cleanup()

            assertThat(server.requests).containsExactly("POST /booking")
        }
    }

    @Test
    @DisplayName("405 на DELETE при существующей брони — ошибка очистки")
    fun methodNotAllowedWithExistingBookingFails() {
        FakeServer { method, _ -> if (method == "DELETE") 405 to "Method Not Allowed" else 200 to "{}" }.use { server ->
            val tracker = tracker(server).apply { track(1) }

            assertThatThrownBy { tracker.cleanup() }
                .isInstanceOf(AssertionError::class.java)
                .hasMessageContaining("booking 1: DELETE returned 405, GET returned 200")
        }
    }

    @Test
    @DisplayName("405 на DELETE и 404 на GET — брони нет, очистка успешна")
    fun methodNotAllowedWithMissingBookingPasses() {
        FakeServer { method, _ -> if (method == "DELETE") 405 to "Method Not Allowed" else 404 to "Not Found" }.use { server ->
            val tracker = tracker(server).apply { track(1) }

            tracker.cleanup()

            assertThat(server.requests).containsExactly("DELETE /booking/1", "GET /booking/1")
        }
    }

    @Test
    @DisplayName("ошибка одной брони не мешает удалить остальные; untrack снимает бронь с учёта")
    fun continuesAfterFailureAndRespectsUntrack() {
        FakeServer { method, path ->
            when {
                method == "DELETE" && path.endsWith("/1") -> 403 to "Forbidden"
                method == "DELETE" -> 201 to "Created"
                else -> 200 to "{}"
            }
        }.use { server ->
            val tracker = tracker(server).apply { track(1); track(2); track(3); untrack(3) }

            assertThatThrownBy { tracker.cleanup() }.hasMessageContaining("booking 1: DELETE returned 403")
            assertThat(server.requests).contains("DELETE /booking/2").doesNotContain("DELETE /booking/3")
        }
    }
}
