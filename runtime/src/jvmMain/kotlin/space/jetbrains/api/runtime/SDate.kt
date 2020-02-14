package space.jetbrains.api.runtime

import org.joda.time.*

actual class SDate(val joda: LocalDate) : Comparable<SDate> {
    actual constructor(iso: String) : this(LocalDate.parse(iso))

    actual val iso: String get() = toString()

    actual val year get() = joda.year
    actual val month get() = joda.monthOfYear
    actual val dayOfMonth get() = joda.dayOfMonth

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SDate

        if (joda != other.joda) return false

        return true
    }

    actual override fun hashCode(): Int = joda.hashCode()
    override fun compareTo(other: SDate): Int = joda.compareTo(other.joda)
    actual override fun toString(): String = joda.toString()
}

fun LocalDate.sDate(): SDate = SDate(this)

actual fun SDate.withDay(day: Int): SDate = joda.withDayOfMonth(day).sDate()

actual fun SDate.plusDays(days: Int) = joda.plusDays(days).sDate()
actual fun SDate.plusMonths(months: Int) = joda.plusMonths(months).sDate()
actual fun SDate.plusYears(years: Int) = joda.plusYears(years).sDate()

actual fun daysBetween(a: SDate, b: SDate) = Days.daysBetween(a.joda, b.joda).days
actual fun monthsBetween(a: SDate, b: SDate) = Months.monthsBetween(a.joda, b.joda).months

actual val SDate.weekday: Weekday get() = Weekday.byIsoNumber(joda.dayOfWeek)

actual fun sDate(year: Int, month: Int, day: Int): SDate = LocalDate(year, month, day).sDate()

actual val sToday: SDate get() = SDate(LocalDate.now())
