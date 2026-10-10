package booker.tests

import booker.config.Specs
import booker.models.Booking
import booker.models.BookingDates
import booker.models.CreateBookingResponse
import booker.support.KnownBug
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Then
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class CreateBookingTest : BaseTest() {

    private fun createAndExtract(booking: Booking): CreateBookingResponse =
        bookings.track(bookingClient.create(booking)) Then {
            spec(Specs.okJson)
        } Extract {
            `as`(CreateBookingResponse::class.java)
        }

    @Test
    @DisplayName("CR-1: валидная бронь создаётся и читается по ID")
    fun createValidBooking() {
        val booking = data.valid()

        val created = createAndExtract(booking)

        assertThat(created.bookingid).isPositive()
        assertThat(created.booking).isEqualTo(booking)
        assertThat(getBooking(created.bookingid)).isEqualTo(booking)
    }

    @Test
    @DisplayName("CR-2: бронь без необязательного additionalneeds")
    fun createWithoutAdditionalNeeds() {
        val booking = data.valid().copy(additionalneeds = null)

        val created = createAndExtract(booking)

        assertThat(created.booking).isEqualTo(booking)
        assertThat(getBooking(created.bookingid).additionalneeds).isNull()
    }

    @Test
    @DisplayName("CR-3a: граница — totalprice = 0")
    fun createWithZeroPrice() {
        val booking = data.valid().copy(totalprice = 0)

        val created = createAndExtract(booking)

        assertThat(getBooking(created.bookingid).totalprice).isZero()
    }

    @Test
    @DisplayName("CR-3b: граница — однодневная бронь, checkin == checkout")
    fun createSameDayBooking() {
        val day = data.date(10).toString()
        val booking = data.valid().copy(bookingdates = BookingDates(day, day))

        val created = createAndExtract(booking)

        assertThat(getBooking(created.bookingid).bookingdates).isEqualTo(BookingDates(day, day))
    }

    // Ограничение длины не документировано; на стенде такие строки принимаются (200)
    @Test
    @DisplayName("CR-4: длинные строки (300 символов), кириллица и спецсимволы сохраняются без искажений")
    fun createWithLongAndUnicodeStrings() {
        val booking = data.valid().copy(
            firstname = data.uniqueName("Иван-Ø'&<>\""),
            lastname = data.uniqueName("x".repeat(300)),
            additionalneeds = "Завтрак 🍳",
        )

        val created = createAndExtract(booking)

        assertThat(created.booking).isEqualTo(booking)
        assertThat(getBooking(created.bookingid)).isEqualTo(booking)
    }

    @ParameterizedTest(name = "CR-5: без обязательного поля {0} → 500, бронь не создаётся [known bug: текущее поведение]")
    @MethodSource("missingRequiredField")
    fun createWithoutRequiredFieldIsRejected(field: String, strip: (Booking) -> Booking) {
        val booking = strip(data.valid())

        bookings.track(bookingClient.create(booking)) Then {
            spec(Specs.serverError)
        }

        assertNotCreated(booking)
    }

    @KnownBug("Отсутствие обязательного поля → 500 Internal Server Error вместо 400 Bad Request")
    @ParameterizedTest(name = "CR-5 (bug): без обязательного поля {0} → 400")
    @MethodSource("missingRequiredField")
    fun createWithoutRequiredFieldReturns400(field: String, strip: (Booking) -> Booking) {
        val booking = strip(data.valid())

        bookings.track(bookingClient.create(booking)) Then {
            spec(Specs.badRequest)
        }

        assertNotCreated(booking)
    }

    @Test
    @DisplayName("CR-6a: синтаксически невалидный JSON → 400 Bad Request")
    fun createWithMalformedJson() {
        bookings.track(bookingClient.createRaw("""{"firstname": """)) Then {
            spec(Specs.badRequest)
        }
    }

    @Test
    @DisplayName("CR-6b: пустой объект {} → 500 [known bug: текущее поведение]")
    fun createWithEmptyObjectIsRejected() {
        bookings.track(bookingClient.createRaw("{}")) Then {
            spec(Specs.serverError)
        }
    }

    @Test
    @KnownBug("Пустой объект {} → 500 Internal Server Error вместо 400 Bad Request")
    @DisplayName("CR-6b (bug): пустой объект {} → 400")
    // Побочный эффект здесь проверить нельзя: в теле нет имён, по которым искать бронь
    fun createWithEmptyObjectReturns400() {
        bookings.track(bookingClient.createRaw("{}")) Then {
            spec(Specs.badRequest)
        }
    }

    @Test
    @DisplayName("CR-7a: отрицательная totalprice принимается [known bug: текущее поведение]")
    fun createWithNegativePriceIsAccepted() {
        val booking = data.valid().copy(totalprice = -100)

        val created = createAndExtract(booking)

        assertThat(created.booking).isEqualTo(booking)
        assertThat(getBooking(created.bookingid).totalprice).isEqualTo(-100)
    }

    @Test
    @KnownBug("Отрицательная totalprice принимается (200), ожидается 400")
    @DisplayName("CR-7a (bug): отрицательная totalprice → 400")
    fun createWithNegativePrice() {
        val booking = data.valid().copy(totalprice = -100)

        bookings.track(bookingClient.create(booking)) Then {
            spec(Specs.badRequest)
        }

        assertNotCreated(booking)
    }

    @Test
    @DisplayName("CR-7b: checkout раньше checkin принимается [known bug: текущее поведение]")
    fun createWithCheckoutBeforeCheckinIsAccepted() {
        val booking = data.valid().copy(bookingdates = reversedDates())

        val created = createAndExtract(booking)

        assertThat(created.booking).isEqualTo(booking)
        assertThat(getBooking(created.bookingid).bookingdates).isEqualTo(booking.bookingdates)
    }

    @Test
    @KnownBug("checkout раньше checkin принимается (200), ожидается 400")
    @DisplayName("CR-7b (bug): checkout раньше checkin → 400")
    fun createWithCheckoutBeforeCheckin() {
        val booking = data.valid().copy(bookingdates = reversedDates())

        bookings.track(bookingClient.create(booking)) Then {
            spec(Specs.badRequest)
        }

        assertNotCreated(booking)
    }

    private fun reversedDates() = BookingDates(data.date(10).toString(), data.date(7).toString())

    companion object {
        @JvmStatic
        fun missingRequiredField(): List<Arguments> = listOf(
            Arguments.of("firstname", { b: Booking -> b.copy(firstname = null) }),
            Arguments.of("lastname", { b: Booking -> b.copy(lastname = null) }),
            Arguments.of("totalprice", { b: Booking -> b.copy(totalprice = null) }),
            Arguments.of("depositpaid", { b: Booking -> b.copy(depositpaid = null) }),
            Arguments.of("bookingdates", { b: Booking -> b.copy(bookingdates = null) }),
        )
    }
}
