package space.jetbrains.api.runtime

/** @param iso date in ISO8601 format (yyyy-MM-dd) */
actual class SDate() : Comparable<SDate> {
    actual constructor(iso: String) : this()

    /** This date in ISO8601 format (yyyy-MM-dd) */
    actual val iso: String = TODO()

    actual override fun equals(other: Any?): Boolean = TODO()
    actual override fun hashCode(): Int = TODO()

    /** [iso] */
    actual override fun toString(): String = TODO()

    actual val year: Int = TODO()
    actual val month: Int = TODO()
    actual val dayOfMonth: Int = TODO()

    override fun compareTo(other: SDate): Int {
        TODO("Not yet implemented")
    }
}



actual fun SDate.withDay(day: Int): SDate = TODO()

actual fun SDate.plusDays(days: Int): SDate = TODO()
actual fun SDate.plusMonths(months: Int): SDate = TODO()
actual fun SDate.plusYears(years: Int): SDate = TODO()

actual fun daysBetween(a: SDate, b: SDate): Int = TODO()
actual fun monthsBetween(a: SDate, b: SDate): Int = TODO()

actual val SDate.weekday: Weekday get() = TODO()

actual fun sDate(year: Int, month: Int, day: Int): SDate = TODO()

actual val sToday: SDate = TODO()
