package space.jetbrains.api.runtime

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneForSecondsFromGMT

internal fun fromISO8601String(date: String): NSDate {
    val dateFormatter = createDateFormatter()
    return dateFormatter.dateFromString(date)!!
}

internal fun createDateFormatter(): NSDateFormatter {
    val dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    dateFormatter.timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
    return dateFormatter
}

internal fun toISO8601String(date: NSDate): String {
    val dateFormatter = createDateFormatter()
    return dateFormatter.stringFromDate(date)
}

