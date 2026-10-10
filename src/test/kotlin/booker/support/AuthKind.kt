package booker.support

/** Способ авторизации для параметризованных тестов, где токена ещё нет. */
enum class AuthKind(private val title: String) {
    NONE("без авторизации"),
    TOKEN("токен в Cookie"),
    BASIC("Basic auth"),
    INVALID_TOKEN("невалидный токен");

    override fun toString() = title
}
