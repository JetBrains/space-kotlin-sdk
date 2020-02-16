package space.jetbrains.api.runtime

import io.ktor.util.date.Month
import platform.Foundation.*

/** @param iso date in ISO8601 format (yyyy-MM-dd) */
actual class SDate(val date: NSDate) : Comparable<SDate> {
    actual constructor(iso: String) : this(NSISO8601DateFormatter().dateFromString(iso)!!)

    /** This date in ISO8601 format (yyyy-MM-dd) */
    actual val iso: String = toString()

    actual override fun equals(other: Any?): Boolean {
        if (other != null && other is NSDate) {
            return date.isEqualToDate(other)
        }
        return false
    }

    actual override fun hashCode(): Int = date.hashCode()

    /** [iso] */
    actual override fun toString(): String = toString()

    actual val year = NSCalendar.currentCalendar.components(NSCalendarUnitYear, date).year.toInt()
    actual val month = NSCalendar.currentCalendar.components(NSCalendarUnitMonth, date).month.toInt()
    actual val dayOfMonth = NSCalendar.currentCalendar.components(NSCalendarUnitDay, date).day.toInt()

    override fun compareTo(other: SDate): Int {
        return date.compare(other.date).toInt()
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
