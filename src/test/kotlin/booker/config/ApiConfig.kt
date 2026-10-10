package booker.config

import java.time.LocalDate
import java.util.UUID

/**
 * Параметры запуска. Приоритет источников: system property (-D) → переменная окружения → значение по умолчанию.
 */
object ApiConfig {
    val baseUrl: String = read("booker.baseUrl", "BOOKER_BASE_URL", "https://restful-booker.herokuapp.com")
    val connectTimeoutMs: Int = read("booker.connectTimeout", "BOOKER_CONNECT_TIMEOUT", "30000").toInt()
    val readTimeoutMs: Int = read("booker.readTimeout", "BOOKER_READ_TIMEOUT", "15000").toInt()

    // Сколько ждать, пока стенд ответит на /ping перед прогоном: Heroku после простоя «просыпается» долго
    val standWaitMs: Long = read("booker.standWait", "BOOKER_STAND_WAIT", "60000").toLong()

    // Публичные демо-креды из документации API; переопределяются через -D / env
    val username: String = read("booker.username", "BOOKER_USERNAME", "admin")
    val password: String = read("booker.password", "BOOKER_PASSWORD", "password123")

    /** Seed тестовых данных (значения Datafaker). Если не задан — случайный; печатается при падении теста. */
    val seed: Long = read("booker.seed", "BOOKER_SEED", "").toLongOrNull() ?: kotlin.random.Random.nextLong()

    /**
     * Идентификатор запуска — часть уникальных имён. Отделён от seed: при повторе с тем же seed
     * и новым runId данные те же, а имена не пересекаются с бронями прошлого запуска на общем стенде.
     */
    val runId: String = read("booker.runId", "BOOKER_RUN_ID", "").ifEmpty { UUID.randomUUID().toString().take(8) }

    /** День, от которого считаются даты броней. По умолчанию — сегодня. */
    val baseDate: LocalDate = LocalDate.parse(read("booker.baseDate", "BOOKER_BASE_DATE", LocalDate.now().toString()))

    /** Параметры, с которыми повторяются данные упавшего теста. */
    val reproduceArgs: String get() = "-Dbooker.seed=$seed -Dbooker.runId=$runId -Dbooker.baseDate=$baseDate"

    private fun read(property: String, env: String, default: String): String =
        System.getProperty(property)?.takeIf { it.isNotBlank() }
            ?: System.getenv(env)?.takeIf { it.isNotBlank() }
            ?: default
}
