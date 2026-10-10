package booker.tests

import booker.config.Specs
import booker.support.AuthKind
import booker.support.KnownBug
import io.restassured.module.kotlin.extensions.Then
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DeleteBookingTest : BaseTest() {

    @ParameterizedTest(name = "DL-1: DELETE ({0}) удаляет бронь")
    @EnumSource(AuthKind::class, names = ["TOKEN", "BASIC"])
    fun deleteRemovesBooking(kind: AuthKind) {
        val id = createBooking().bookingid

        // DELETE → 201 Created, затем GET → 404 Not Found
        deleteBooking(id, auth(kind))
    }

    @ParameterizedTest(name = "DL-2: DELETE ({0}) → 403 Forbidden, бронь остаётся")
    @EnumSource(AuthKind::class, names = ["NONE", "INVALID_TOKEN"])
    fun deleteWithoutValidAuthIsForbidden(kind: AuthKind) {
        val created = createBooking()

        bookingClient.delete(created.bookingid, auth(kind)) Then { spec(Specs.forbidden) }

        assertThat(getBooking(created.bookingid)).isEqualTo(created.booking)
    }

    // Авторизация проверяется раньше существования ресурса
    @Test
    @DisplayName("DL-3: DELETE уже удалённой брони без токена → 403 Forbidden")
    fun deleteMissingBookingWithoutAuthIsForbidden() {
        val id = createDeletedBooking()

        bookingClient.delete(id, auth(AuthKind.NONE)) Then { spec(Specs.forbidden) }
    }

    @Test
    @DisplayName("DL-4: DELETE уже удалённой брони с токеном → 405 [known bug: текущее поведение]")
    fun deleteMissingBookingReturns405() {
        val id = createDeletedBooking()

        bookingClient.delete(id, tokenAuth) Then { spec(Specs.methodNotAllowed) }
    }

    @Test
    @KnownBug("DELETE несуществующей брони с токеном → 405 Method Not Allowed вместо 404 Not Found")
    @DisplayName("DL-4 (bug): DELETE уже удалённой брони с токеном → 404")
    fun deleteMissingBookingReturns404() {
        val id = createDeletedBooking()

        bookingClient.delete(id, tokenAuth) Then { spec(Specs.notFound) }
    }

    private fun createDeletedBooking(): Int {
        val id = createBooking().bookingid
        deleteBooking(id)
        return id
    }
}
