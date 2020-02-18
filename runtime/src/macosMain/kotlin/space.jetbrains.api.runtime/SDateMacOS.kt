package space.jetbrains.api.runtime

import kotlinx.cinterop.convert
import platform.Foundation.*

/** @param iso date in ISO8601 format (yyyy-MM-dd) */
actual class SDate(val date: NSDate) : Comparable<SDate> {
    actual constructor(iso: String) : this(NSISO8601DateFormatter().dateFromString(iso)!!)

    private val dateComponents = NSCalendar.currentCalendar.components(NSCalendarUnitYear + NSCalendarUnitMonth + NSCalendarUnitDay, date)

    /** This date in ISO8601 format (yyyy-MM-dd) */
    actual val iso: String = date.toString()

    actual override fun equals(other: Any?): Boolean {
        if (other != null && other is NSDate) {
            return date.isEqualToDate(other)
        }
        return false
    }

    actual override fun hashCode(): Int = date.hashCode()

    /** [iso] */
    actual override fun toString(): String = toString()

    actual val year = dateComponents.year.toInt()
    actual val month = dateComponents.month.toInt()
    actual val dayOfMonth = dateComponents.day.toInt()

    override fun compareTo(other: SDate): Int {
        return date.compare(other.date).toInt()
    }


}


actual fun SDate.withDay(day: Int) = sDate(year, month, day)

actual fun SDate.plusDays(days: Int) = SDate(NSCalendar.currentCalendar.dateByAddingUnit(NSCalendarUnitDay, days.convert(), date, NSCalendarOptions.MIN_VALUE)!!)
actual fun SDate.plusMonths(months: Int) = SDate(NSCalendar.currentCalendar.dateByAddingUnit(NSCalendarUnitMonth, months.convert(), date, NSCalendarOptions.MIN_VALUE)!!)
actual fun SDate.plusYears(years: Int) = SDate(NSCalendar.currentCalendar.dateByAddingUnit(NSCalendarUnitYear, years.convert(), date, NSCalendarOptions.MIN_VALUE)!!)

fun dateDifference(a: SDate, b: SDate, unit: NSCalendarUnit): Int {
    val date1 = NSCalendar.currentCalendar.startOfDayForDate(a.date)
    val date2 = NSCalendar.currentCalendar.startOfDayForDate(b.date)
    val components = NSCalendar.currentCalendar.components(unit, date1, date2, NSCalendarOptions.MIN_VALUE)
    return components.day.convert()

}

actual fun daysBetween(a: SDate, b: SDate) = dateDifference(a, b, NSCalendarUnitDay)
actual fun monthsBetween(a: SDate, b: SDate) = dateDifference(a, b, NSCalendarUnitMonth)

actual val SDate.weekday: Weekday get() = Weekday.byIsoNumber(NSCalendar.currentCalendar.component(NSCalendarUnitWeekday, date).convert())

actual fun sDate(year: Int, month: Int, day: Int): SDate  {
    val components = NSDateComponents()
    components.year = year.convert()
    components.month = month.convert()
    components.day = day.convert()
    return SDate(NSCalendar.currentCalendar.dateFromComponents(components)!!)
}

actual val sToday: SDate = SDate(NSDate())
