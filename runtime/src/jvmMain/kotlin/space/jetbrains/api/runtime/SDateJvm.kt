package space.jetbrains.api.runtime

import java.time.*
import java.time.temporal.ChronoUnit

actual class SDate(val javaDate: LocalDate) : Comparable<SDate> {
    actual constructor(iso: String) : this(LocalDate.parse(iso))

    actual val iso: String get() = toString()

    actual val year get() = javaDate.year
    actual val month get() = javaDate.month.value
    actual val dayOfMonth get() = javaDate.dayOfMonth

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SDate

        if (javaDate != other.javaDate) return false

        return true
    }

    actual override fun hashCode(): Int = javaDate.hashCode()
    override fun compareTo(other: SDate): Int = javaDate.compareTo(other.javaDate)
    actual override fun toString(): String = javaDate.toString()
}

fun LocalDate.sDate(): SDate = SDate(this)

actual fun SDate.withDay(day: Int): SDate = javaDate.withDayOfMonth(day).sDate()

actual fun SDate.plusDays(days: Long) = javaDate.plusDays(days).sDate()
actual fun SDate.plusMonths(months: Long) = javaDate.plusMonths(months).sDate()
actual fun SDate.plusYears(years: Long) = javaDate.plusYears(years).sDate()

actual fun daysBetween(a: SDate, b: SDate): Long = ChronoUnit.DAYS.between(a.javaDate, b.javaDate)
actual fun monthsBetween(a: SDate, b: SDate): Long = ChronoUnit.MONTHS.between(a.javaDate, b.javaDate)

actual val SDate.weekday: Weekday get() = Weekday.byIsoNumber(javaDate.dayOfWeek.value)

actual fun sDate(year: Int, month: Int, day: Int): SDate = LocalDate.of(year, month, day).sDate()

actual val today: SDate get() = SDate(LocalDate.now())
