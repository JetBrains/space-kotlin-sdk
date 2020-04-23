package space.jetbrains.api.runtime

/** @param iso date in ISO8601 format (yyyy-MM-dd) */
expect class SDate(iso: String) : Comparable<SDate> {
    /** This date in ISO8601 format (yyyy-MM-dd) */
    val iso: String

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int

    /** [iso] */
    override fun toString(): String

    val year: Int
    val month: Int
    val dayOfMonth: Int
}

enum class Weekday(val isoNumber: Int, val title: String) {
    SUNDAY(7, "Sunday"),
    MONDAY(1, "Monday"),
    TUESDAY(2, "Tuesday"),
    WEDNESDAY(3, "Wednesday"),
    THURSDAY(4, "Thursday"),
    FRIDAY(5, "Friday"),
    SATURDAY(6, "Saturday");

    companion object {
        private val weekdaysByNumber = values().associateBy { it.isoNumber }

        fun byIsoNumber(isoNumber: Int) = weekdaysByNumber[isoNumber]
            ?: throw NoSuchElementException("Weekday with ISO number $isoNumber doesn't exist. Possible values are 1..7")
    }
}

expect fun SDate.withDay(day: Int): SDate

expect fun SDate.plusDays(days: Long): SDate
expect fun SDate.plusMonths(months: Long): SDate
expect fun SDate.plusYears(years: Long): SDate

expect fun daysBetween(a: SDate, b: SDate): Long
expect fun monthsBetween(a: SDate, b: SDate): Long

expect val SDate.weekday: Weekday

expect fun sDate(year: Int, month: Int, day: Int): SDate

expect val today: SDate
val tomorrow get() = today.plusDays(1)
