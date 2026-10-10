package booker.infra

import booker.config.Specs
import booker.models.Booking
import com.fasterxml.jackson.core.JacksonException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Инфраструктура: DTO не приводят типы молча")
class StrictMappingTest {

    @ParameterizedTest(name = "{0} не десериализуется")
    @ValueSource(strings = [
        """{"totalprice":"777"}""",
        """{"totalprice":777.5}""",
        """{"depositpaid":"true"}""",
        """{"depositpaid":1}""",
        """{"firstname":123}""",
        """{"firstname":true}""",
    ])
    fun wrongTypeIsRejected(json: String) {
        assertThatThrownBy { Specs.objectMapper.readValue(json, Booking::class.java) }
            .isInstanceOf(JacksonException::class.java)
    }

    @Test
    @DisplayName("корректные типы и неизвестные поля принимаются")
    fun correctTypesAreAccepted() {
        val booking = Specs.objectMapper.readValue(
            """{"firstname":"A","totalprice":777,"depositpaid":true,"extra":1}""", Booking::class.java
        )

        assertThat(booking).isEqualTo(Booking(firstname = "A", totalprice = 777, depositpaid = true))
    }
}
