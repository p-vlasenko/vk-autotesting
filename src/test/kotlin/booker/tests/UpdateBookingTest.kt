package booker.tests

import booker.config.Specs
import booker.models.Booking
import booker.models.BookingDates
import booker.support.AuthKind
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Then
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class UpdateBookingTest : BaseTest() {

    @ParameterizedTest(name = "UP-1: PUT ({0}) обновляет все поля")
    @EnumSource(AuthKind::class, names = ["TOKEN", "BASIC"])
    fun putUpdatesBooking(kind: AuthKind) {
        val created = createBooking()
        val id = created.bookingid
        val updated = changedEveryField(created.booking)

        val response = bookingClient.update(id, updated, auth(kind)) Then {
            spec(Specs.okJson)
        } Extract {
            `as`(Booking::class.java)
        }

        assertThat(response).isEqualTo(updated)
        assertThat(getBooking(id)).isEqualTo(updated)
    }

    @ParameterizedTest(name = "UP-2: PATCH ({0}) меняет только переданные поля")
    @EnumSource(AuthKind::class, names = ["TOKEN", "BASIC"])
    fun patchUpdatesOnlyGivenFields(kind: AuthKind) {
        val created = createBooking()
        val newName = data.uniqueName("Patched")
        val newPrice = created.booking.totalprice!! + 1
        val expected = created.booking.copy(firstname = newName, totalprice = newPrice)

        val response = bookingClient.patch(
            created.bookingid, mapOf("firstname" to newName, "totalprice" to newPrice), auth(kind)
        ) Then {
            spec(Specs.okJson)
        } Extract {
            `as`(Booking::class.java)
        }

        assertThat(response).isEqualTo(expected)
        assertThat(getBooking(created.bookingid)).isEqualTo(expected)
    }

    // Особенность API: без прав — 403, а не 401
    @ParameterizedTest(name = "UP-3: {0} ({1}) → 403 Forbidden, данные не изменились")
    @MethodSource("forbiddenUpdates")
    fun updateWithoutValidAuthIsForbidden(method: String, kind: AuthKind) {
        val created = createBooking()

        val response = when (method) {
            "PUT" -> bookingClient.update(created.bookingid, data.valid(), auth(kind))
            else -> bookingClient.patch(created.bookingid, mapOf("firstname" to "Hacker"), auth(kind))
        }

        response Then { spec(Specs.forbidden) }
        assertThat(getBooking(created.bookingid)).isEqualTo(created.booking)
    }

    /** Новая бронь, у которой каждое поле гарантированно отличается от [original]. */
    private fun changedEveryField(original: Booking): Booking {
        val dates = original.bookingdates!!
        return data.valid().copy(
            totalprice = original.totalprice!! + 1,
            depositpaid = !original.depositpaid!!,
            bookingdates = BookingDates(
                LocalDate.parse(dates.checkin).plusDays(1).toString(),
                LocalDate.parse(dates.checkout).plusDays(2).toString(),
            ),
            additionalneeds = "${original.additionalneeds} + extra towels",
        ).also { updated ->
            // Имена уникальны и так; проверка защищает от правок выше, которые оставят поле прежним
            listOf(
                Booking::firstname, Booking::lastname, Booking::totalprice,
                Booking::depositpaid, Booking::bookingdates, Booking::additionalneeds,
            ).forEach { assertThat(it.get(updated)).describedAs(it.name).isNotEqualTo(it.get(original)) }
        }
    }

    companion object {
        @JvmStatic
        fun forbiddenUpdates(): List<Arguments> =
            listOf("PUT", "PATCH").flatMap { method ->
                listOf(AuthKind.NONE, AuthKind.INVALID_TOKEN).map { Arguments.of(method, it) }
            }
    }
}
