package booker.support

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Тест известного дефекта API: проверяет правильное поведение и сейчас падает.
 * Исключён из `./gradlew test`, запускается `./gradlew knownBugs`.
 * Текущее (ошибочное) поведение фиксирует парный тест в основном наборе.
 * Транспортные ошибки прерывают тест (aborted), а не роняют его — см. [AbortOnTransportError].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("known-bug")
@ExtendWith(AbortOnTransportError::class)
annotation class KnownBug(val description: String)
