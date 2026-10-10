package booker.models

/** Способ авторизации для изменяющих запросов. */
sealed interface Auth {
    data object None : Auth
    data class Token(val token: String) : Auth
    data class Basic(val username: String, val password: String) : Auth
}
