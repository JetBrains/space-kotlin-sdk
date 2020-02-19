package space.jetbrains.api.runtime

import kotlinx.cinterop.convert
import platform.Foundation.*

fun createNSDate(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): NSDate {
    val components = NSDateComponents()
    components.year = year.convert()
    components.month = month.convert()
    components.day = day.convert()
    components.hour = hour.convert()
    components.minute = minute.convert()
    components.second = second.convert()
    components.setTimeZone(NSTimeZone.localTimeZone)
    return NSCalendar.currentCalendar.dateFromComponents(components)!!
}
