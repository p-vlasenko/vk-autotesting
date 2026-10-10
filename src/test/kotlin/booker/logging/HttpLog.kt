package booker.logging

/** HTTP-обмены текущего теста. Тесты запускаются последовательно, ThreadLocal — на всякий случай. */
object HttpLog {
    private val entries = ThreadLocal.withInitial { mutableListOf<String>() }

    fun add(entry: String) {
        entries.get().add(entry)
    }

    fun clear() = entries.get().clear()

    fun drain(): List<String> = entries.get().toList().also { clear() }
}
