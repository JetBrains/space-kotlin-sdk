package space.jetbrains.api.runtime

import kotlinx.cinterop.convert
import platform.Foundation.*
import platform.darwin.NSInteger


/** @param timestamp milliseconds from 1970-01-01T00:00:00Z */
actual class SDateTime(val dateTime: NSDate) : Comparable<SDateTime> {
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

    private fun getComponentsFromDateTime(): DateTimeComponents {
        val components = NSCalendar.currentCalendar.components(NSCalendarUnitYear + NSCalendarUnitMonth + NSCalendarUnitDay + NSCalendarUnitHour +
                NSCalendarUnitMinute + NSCalendarUnitSecond + NSCalendarUnitTimeZone, dateTime)
        // TODO - fix day of month
        return DateTimeComponents(components.year, components.month, components.day, components.hour, components.minute, components.second, components.timeZone, components.minute + (components.hour * 60), 1)
    }

    val dateTimeComponents = getComponentsFromDateTime()

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
    val components = NSDateComponents()
    components.year = dateTimeComponents.year
    components.month = dateTimeComponents.month
    components.day = dateTimeComponents.day
    components.hour = hours.convert()
    components.minute = minutes.convert()
    components.second = seconds.convert()
    // TODO - mills...
    return SDateTime(NSCalendar.currentCalendar.dateFromComponents(components)!!)
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

data class DateTimeComponents(val year: NSInteger,
                              val month: NSInteger,
                              val day: NSInteger,
                              val hour: NSInteger,
                              val minute: NSInteger,
                              val second: NSInteger,
                              val timezone: NSTimeZone?,
                              val minuteOfDay: NSInteger,
                              val dayOfMonth: NSInteger)


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
    get() = dateTimeComponents.second.convert()
actual val SDateTime.minute: Int
    get() = dateTimeComponents.minute.convert()
actual val SDateTime.minuteOfDay: Int
    get() = dateTimeComponents.minuteOfDay.convert()
actual val SDateTime.hour: Int
    get() = dateTimeComponents.hour.convert()
actual val SDateTime.dayOfMonth: Int
    get() = TODO("dayOfMonth")
actual val SDateTime.month: Int
    get() = dateTimeComponents.month.convert()
actual val SDateTime.year: Int
    get() = dateTimeComponents.year.convert()
actual val clientTimeZone: STimeZone
    get() = TODO("clientTimeZone")

actual val SDateTime.weekday: Weekday get() = Weekday.byIsoNumber(NSCalendar.currentCalendar.component(NSCalendarUnitWeekday, dateTime).convert())
