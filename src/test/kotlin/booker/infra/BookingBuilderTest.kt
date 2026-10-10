package booker.infra

import booker.config.ApiConfig
import booker.data.BookingBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Инфраструктура: воспроизводимость тестовых данных")
class BookingBuilderTest {

    @Test
    @DisplayName("один seed — те же брони и имена, включая суффиксы; другой seed — другие")
    fun sameSeedGivesSameData() {
        val first = BookingBuilder(42).let { listOf(it.valid(), it.valid()) to it.uniqueName("x") }
        val second = BookingBuilder(42).let { listOf(it.valid(), it.valid()) to it.uniqueName("x") }

        assertThat(second).isEqualTo(first)
        assertThat(BookingBuilder(43).valid()).isNotEqualTo(first.first[0])
        assertThat(first.second).startsWith("x-${ApiConfig.runId}-")
    }

    @Test
    @DisplayName("даты считаются от booker.baseDate")
    fun datesFromBaseDate() {
        val dates = BookingBuilder(42).valid().bookingdates!!

        assertThat(dates.checkin).isGreaterThan(ApiConfig.baseDate.toString())
        assertThat(BookingBuilder(42).date(10)).isEqualTo(ApiConfig.baseDate.plusDays(10))
    }
}
