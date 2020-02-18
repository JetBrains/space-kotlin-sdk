package space.jetbrains.api.runtime

import kotlinx.cinterop.convert
import platform.Foundation.*


/** @param timestamp milliseconds from 1970-01-01T00:00:00Z */
actual class SDateTime(val dateTime: NSDate)  : Comparable<SDateTime> {
    actual constructor(timestamp: Long) : this(NSDate.dateWithTimeIntervalSince1970(timestamp.toDouble()))

    /** This date time in ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZZ) */
    actual val iso: String get() = dateTime.toString()

    /** [iso] */
    actual override fun toString() = iso

    actual override fun equals(other: Any?): Boolean {
        if (other != null && other is NSDate) {
            return dateTime.isEqualToDate(other)
        }
        return false
    }

    actual override fun hashCode() = dateTime.hashCode()

    override fun compareTo(other: SDateTime) = dateTime.compare(other.dateTime).toInt()


}

actual fun SDateTime.withZone(zone: STimeZone): SDateTime {
    TODO("Not yet implemented")
}

actual fun SDateTime.plusDays(days: Int) = SDateTime(NSCalendar.currentCalendar.dateByAddingUnit(NSCalendarUnitDay, days.convert(), dateTime, NSCalendarOptions.MIN_VALUE)!!)

actual fun SDateTime.plusMonths(months: Int) = SDateTime(NSCalendar.currentCalendar.dateByAddingUnit(NSCalendarUnitDay, months.convert(), dateTime, NSCalendarOptions.MIN_VALUE)!!)


actual fun SDateTime.plusYears(years: Int) = SDateTime(NSCalendar.currentCalendar.dateByAddingUnit(NSCalendarUnitDay, years.convert(), dateTime, NSCalendarOptions.MIN_VALUE)!!)


actual fun SDateTime.plusMinutes(minutes: Int) = SDateTime(NSCalendar.currentCalendar.dateByAddingUnit(NSCalendarUnitDay, minutes.convert(), dateTime, NSCalendarOptions.MIN_VALUE)!!)


actual fun SDateTime.plusSeconds(seconds: Int) = SDateTime(NSCalendar.currentCalendar.dateByAddingUnit(NSCalendarUnitDay, seconds.convert(), dateTime, NSCalendarOptions.MIN_VALUE)!!)


actual fun SDate.toDateTimeAtStartOfDay(zone: STimeZone): SDateTime {
    TODO("Not yet implemented")
}

actual fun SDate.toDateTimeAtStartOfDay(): SDateTime {
    TODO("Not yet implemented")
}

actual fun SDateTime.withTime(hours: Int, minutes: Int, seconds: Int, mills: Int): SDateTime {
    TODO("Not yet implemented")
}

actual fun SDateTime.toDate(): SDate {
    TODO("Not yet implemented")
}

/** Milliseconds from 1970-01-01T00:00:00Z */
actual val SDateTime.timestamp: Long
    get() = TODO("Not yet implemented")

actual fun daysBetween(a: SDateTime, b: SDateTime): Int {
    TODO("Not yet implemented")
}

actual fun monthsBetween(a: SDateTime, b: SDateTime): Int {
    TODO("Not yet implemented")
}

actual fun yearsBetween(a: SDateTime, b: SDateTime): Int {
    TODO("Not yet implemented")
}

actual fun STimeZone.offsetOnTime(time: SDateTime): Int {
    TODO("Not yet implemented")
}

actual val sNow: SDateTime
    get() = TODO("Not yet implemented")

actual fun sDateTime(iso: String): SDateTime {
    TODO("Not yet implemented")
}

actual fun sDateTime(year: Int, month: Int, day: Int, hours: Int, minutes: Int, timezone: STimeZone): SDateTime {
    val components = NSDateComponents()
    components.year = year.convert()
    components.month = month.convert()
    components.day = day.convert()
    components.hour = hours.convert()
    components.minute = minutes.convert()
    components.timeZone = NSTimeZone.create(timezone.id)
    return SDateTime(NSCalendar.currentCalendar.dateFromComponents(components)!!)
}

actual val SDateTime.second: Int
    get() = TODO("Not yet implemented")
actual val SDateTime.minute: Int
    get() = TODO("Not yet implemented")
actual val SDateTime.minuteOfDay: Int
    get() = TODO("Not yet implemented")
actual val SDateTime.hour: Int
    get() = TODO("Not yet implemented")
actual val SDateTime.dayOfMonth: Int
    get() = TODO("Not yet implemented")
actual val SDateTime.month: Int
    get() = TODO("Not yet implemented")
actual val SDateTime.year: Int
    get() = TODO("Not yet implemented")
actual val clientTimeZone: STimeZone
    get() = TODO("Not yet implemented")

actual val SDateTime.weekday: Weekday get() = Weekday.byIsoNumber(NSCalendar.currentCalendar.component(NSCalendarUnitWeekday, dateTime).convert())
