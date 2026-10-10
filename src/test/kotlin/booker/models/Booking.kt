package booker.models

/** Поля nullable, чтобы можно было отправить бронь без обязательного поля (null не сериализуется). */
data class Booking(
    val firstname: String? = null,
    val lastname: String? = null,
    val totalprice: Int? = null,
    val depositpaid: Boolean? = null,
    val bookingdates: BookingDates? = null,
    val additionalneeds: String? = null,
)

data class BookingDates(
    val checkin: String,
    val checkout: String,
)

data class CreateBookingResponse(
    val bookingid: Int,
    val booking: Booking,
)

data class AuthRequest(
    val username: String,
    val password: String,
)

data class AuthResponse(
    val token: String? = null,
    val reason: String? = null,
)
