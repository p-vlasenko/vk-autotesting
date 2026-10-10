package booker.data

import booker.config.ApiConfig
import booker.models.Booking
import booker.models.BookingDates
import net.datafaker.Faker
import java.time.LocalDate
import java.util.Random

/**
 * Валидные тестовые брони. Экземпляр создаётся на каждый тест со своим seed (см. BaseTest).
 * Все значения, включая суффиксы имён и даты, определяются тройкой seed + runId + baseDate
 * ([ApiConfig.reproduceArgs]), поэтому данные теста повторяются точно.
 * В тесте через copy() меняются только важные для сценария поля.
 */
class BookingBuilder(seed: Long) {
    private val faker = Faker(Random(seed))

    fun valid(): Booking {
        val checkin = date(faker.number().numberBetween(1L, 60L))
        val checkout = checkin.plusDays(faker.number().numberBetween(1L, 14L))
        return Booking(
            firstname = uniqueName(faker.name().firstName()),
            lastname = uniqueName(faker.name().lastName()),
            totalprice = faker.number().numberBetween(50, 2000),
            depositpaid = faker.bool().bool(),
            bookingdates = BookingDates(checkin.toString(), checkout.toString()),
            additionalneeds = faker.options().option("Breakfast", "Lunch", "Dinner", "Late checkout"),
        )
    }

    /**
     * Уникальная строка на общем стенде: читаемая основа + runId + суффикс из seed.
     * runId разводит запуски, суффикс — имена внутри теста.
     */
    fun uniqueName(base: String): String = "$base-${ApiConfig.runId}-${faker.regexify("[a-z0-9]{6}")}"

    /** Дата через [days] дней от базовой ([ApiConfig.baseDate]). */
    fun date(days: Long): LocalDate = ApiConfig.baseDate.plusDays(days)
}
