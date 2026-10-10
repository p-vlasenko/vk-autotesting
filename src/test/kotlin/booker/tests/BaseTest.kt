package booker.tests

import booker.clients.AuthClient
import booker.clients.BookingClient
import booker.clients.HealthClient
import booker.config.ApiConfig
import booker.config.Specs
import booker.data.BookingBuilder
import booker.logging.LogOnFailureExtension
import booker.models.Auth
import booker.models.Booking
import booker.models.CreateBookingResponse
import booker.support.AuthKind
import booker.support.BookingTracker
import booker.support.StandAvailability
import io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Then
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension

@ExtendWith(StandAvailability::class, LogOnFailureExtension::class)
abstract class BaseTest {
    protected val authClient = AuthClient()
    protected val bookingClient = BookingClient()
    protected val healthClient = HealthClient()

    /** Токен запрашивается один раз на тест и только если нужен. */
    protected val token: String by lazy { authClient.token() }
    protected val tokenAuth: Auth get() = Auth.Token(token)
    protected val basicAuth: Auth get() = Auth.Basic(ApiConfig.username, ApiConfig.password)

    /** Созданные тестом брони удаляются после теста. */
    @JvmField
    @RegisterExtension
    val bookings = BookingTracker(bookingClient) { tokenAuth }

    /** Генератор данных теста: seed = booker.seed + имя теста, данные воспроизводятся (см. [ApiConfig.reproduceArgs]). */
    protected lateinit var data: BookingBuilder

    @BeforeEach
    fun initTestData(testInfo: TestInfo) {
        val testId = "${javaClass.name}#${testInfo.testMethod.map { it.name }.orElse("")}#${testInfo.displayName}"
        data = BookingBuilder(ApiConfig.seed xor testId.hashCode().toLong())
    }

    /** Создаёт бронь (precondition). ID регистрируется для очистки до проверок ответа. */
    protected fun createBooking(booking: Booking = data.valid()): CreateBookingResponse =
        bookings.track(bookingClient.create(booking)) Then {
            spec(Specs.okJson)
        } Extract {
            `as`(CreateBookingResponse::class.java)
        }

    protected fun getBooking(id: Int): Booking =
        bookingClient.get(id) Then {
            spec(Specs.okJson)
        } Extract {
            `as`(Booking::class.java)
        }

    /** ID броней из GET /booking с фильтрами. ID извлекаются после проверки формы [{bookingid}]. */
    protected fun listIds(filters: Map<String, String> = emptyMap()): List<Int> =
        bookingClient.list(filters) Then {
            spec(Specs.okJson)
            body(matchesJsonSchemaInClasspath("schemas/booking-list.json"))
        } Extract {
            jsonPath().getList("bookingid", Int::class.javaObjectType)
        }

    /**
     * Брони с такими именами не существует. Найденные брони сначала регистрируются для очистки:
     * API мог сохранить бронь, даже вернув ошибку.
     */
    protected fun assertNotCreated(booking: Booking) {
        val filters = buildMap {
            booking.firstname?.let { put("firstname", it) }
            booking.lastname?.let { put("lastname", it) }
        }
        require(filters.isNotEmpty()) { "Booking has no names to search by" }
        val ids = listIds(filters)
        ids.forEach(bookings::track)
        assertThat(ids).describedAs("bookings saved despite the error").isEmpty()
    }

    /** Удаляет бронь и подтверждает удаление через GET; после этого очистка её не трогает. */
    protected fun deleteBooking(id: Int, auth: Auth = tokenAuth) {
        bookingClient.delete(id, auth) Then { spec(Specs.created) }
        bookingClient.get(id) Then { spec(Specs.notFound) }
        bookings.untrack(id)
    }

    /** Способ авторизации по имени — для параметризованных тестов, где токена ещё нет. */
    protected fun auth(kind: AuthKind): Auth = when (kind) {
        AuthKind.NONE -> Auth.None
        AuthKind.TOKEN -> tokenAuth
        AuthKind.BASIC -> basicAuth
        AuthKind.INVALID_TOKEN -> Auth.Token("invalid-token")
    }
}
