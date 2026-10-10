package booker.tests

import booker.config.Specs
import booker.models.Booking
import booker.support.KnownBug
import io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Then
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class GetBookingTest : BaseTest() {

    @Test
    @DisplayName("GT-1: GET /booking/{id} — ответ соответствует JSON Schema и отправленным данным")
    fun getBookingMatchesSchemaAndData() {
        val expected = data.valid()
        val created = createBooking(expected)

        val booking = bookingClient.get(created.bookingid) Then {
            spec(Specs.okJson)
            body(matchesJsonSchemaInClasspath("schemas/booking.json"))
        } Extract {
            `as`(Booking::class.java)
        }

        assertThat(booking).isEqualTo(expected)
    }

    @Test
    @DisplayName("GT-2: GET удалённой брони → 404 Not Found")
    fun getDeletedBookingReturns404() {
        val id = createBooking().bookingid

        // deleteBooking проверяет и сам 404 на GET
        deleteBooking(id)
    }

    @Test
    @DisplayName("GT-3: GET с нечисловым id → 404 Not Found")
    fun getNonNumericIdReturns404() {
        bookingClient.get("abc") Then { spec(Specs.notFound) }
    }

    @Test
    @DisplayName("GT-4: GET /booking — список содержит созданную бронь")
    fun listContainsCreatedBooking() {
        val id = createBooking().bookingid

        assertThat(listIds()).contains(id)
    }

    @Test
    @DisplayName("GT-5: фильтр по firstname и lastname возвращает ровно свою бронь")
    fun filterByNameReturnsOnlyOwnBooking() {
        val created = createBooking()

        assertThat(listIds(nameFilter(created.booking))).containsExactly(created.bookingid)
    }

    // Особенность API: неподдерживаемый Accept → 418 I'm a Teapot
    @Test
    @DisplayName("GT-6: GET с Accept: text/plain → 418 I'm a Teapot")
    fun getWithUnsupportedAcceptReturns418() {
        val id = createBooking().bookingid

        bookingClient.get(id, accept = "text/plain") Then { spec(Specs.teapot) }
    }

    @Test
    @DisplayName("GT-7: фильтр по несуществующему имени → пустой список")
    fun filterByUnknownNameReturnsEmptyList() {
        assertThat(listIds(mapOf("firstname" to data.uniqueName("Nobody")))).isEmpty()
    }

    // Фильтр по имени изолирует проверку от чужих броней на стенде
    @ParameterizedTest(name = "GT-8: {0}")
    @MethodSource("dateFilterCases")
    fun filterByDate(case: DateFilterCase) {
        val created = createBooking()

        val ids = listIds(nameFilter(created.booking) + case.filter(created.booking))

        if (case.actual) assertThat(ids).containsExactly(created.bookingid) else assertThat(ids).isEmpty()
    }

    @KnownBug("Фильтры checkin/checkout не соответствуют документации (\"greater than or equal\")")
    @ParameterizedTest(name = "GT-8 (bug): {0}")
    @MethodSource("dateFilterBugCases")
    fun filterByDateAsDocumented(case: DateFilterCase) {
        val created = createBooking()

        val ids = listIds(nameFilter(created.booking) + case.filter(created.booking))

        if (case.documented) assertThat(ids).containsExactly(created.bookingid) else assertThat(ids).isEmpty()
    }

    private fun nameFilter(booking: Booking) =
        mapOf("firstname" to booking.firstname!!, "lastname" to booking.lastname!!)

    /**
     * Фильтр по дате: [field] = дата брони + [offsetDays].
     * [documented] — найдётся ли бронь по документации (checkin/checkout >= даты фильтра),
     * [actual] — фактическое поведение стенда.
     */
    data class DateFilterCase(
        val title: String,
        val field: String,
        val offsetDays: Long,
        val documented: Boolean,
        val actual: Boolean,
    ) {
        fun filter(booking: Booking): Map<String, String> {
            val dates = booking.bookingdates!!
            val base = if (field == "checkin") dates.checkin else dates.checkout
            return mapOf(field to LocalDate.parse(base).plusDays(offsetDays).toString())
        }

        val isBug get() = actual != documented

        override fun toString() = title
    }

    companion object {
        private val cases = listOf(
            DateFilterCase("checkin за день до заезда", "checkin", -1, documented = true, actual = true),
            DateFilterCase("checkin = день заезда", "checkin", 0, documented = true, actual = false),
            DateFilterCase("checkin на день позже заезда", "checkin", 1, documented = false, actual = false),
            DateFilterCase("checkout за день до выезда", "checkout", -1, documented = true, actual = false),
            DateFilterCase("checkout = день выезда", "checkout", 0, documented = true, actual = true),
            DateFilterCase("checkout на день позже выезда", "checkout", 1, documented = false, actual = true),
        )

        private fun found(value: Boolean) = if (value) "бронь найдена" else "бронь не найдена"

        @JvmStatic
        fun dateFilterCases(): List<Named<DateFilterCase>> = cases.map {
            Named.of("$it → ${found(it.actual)}" + if (it.isBug) " [known bug: текущее поведение]" else "", it)
        }

        @JvmStatic
        fun dateFilterBugCases(): List<Named<DateFilterCase>> = cases.filter { it.isBug }
            .map { Named.of("$it → ${found(it.documented)} (по документации)", it) }
    }
}
