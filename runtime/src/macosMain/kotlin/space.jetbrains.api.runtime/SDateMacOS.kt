package space.jetbrains.api.runtime

import kotlinx.cinterop.convert
import platform.Foundation.*

private fun fromISO8601String(date: String): NSDate {
    val dateFormatter = createDateFormatter()
    return dateFormatter.dateFromString(date)!!
}

private fun createDateFormatter(): NSDateFormatter {
    val dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    dateFormatter.timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
    return dateFormatter
}

private fun toISO8601String(date: NSDate): String {
    val dateFormatter = createDateFormatter()
    return dateFormatter.stringFromDate(date)
}

/** @param iso date in ISO8601 format (yyyy-MM-dd) */
actual class SDate(val date: NSDate) : Comparable<SDate> {
    actual constructor(iso: String) : this(fromISO8601String(iso))

    private val dateComponents = NSCalendar.currentCalendar.components(NSCalendarUnitYear + NSCalendarUnitMonth + NSCalendarUnitDay, date)

    /** This date in ISO8601 format (yyyy-MM-dd) */
    actual val iso: String = toISO8601String(date)

    actual override fun equals(other: Any?): Boolean {
        if (other != null && other is SDate) {
            return (other.year == year && other.dayOfMonth == dayOfMonth && other.month == month)
        }
        return false
    }

    actual override fun hashCode(): Int = date.hashCode()

    /** [iso] */
    actual override fun toString(): String = iso

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
