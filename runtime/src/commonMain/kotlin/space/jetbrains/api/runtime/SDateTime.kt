package space.jetbrains.api.runtime

/** @param timestamp milliseconds from 1970-01-01T00:00:00Z */
expect class SDateTime(timestamp: Long) : Comparable<SDateTime> {
    /** This date time in ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZZ) */
    val iso: String

    /** [iso] */
    override fun toString(): String

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

data class STimeZone(val id: String)

expect fun SDateTime.withZone(zone: STimeZone): SDateTime

expect fun SDateTime.plusDays(days: Long): SDateTime
expect fun SDateTime.plusMonths(months: Long): SDateTime
expect fun SDateTime.plusYears(years: Long): SDateTime
expect fun SDateTime.plusMinutes(minutes: Long): SDateTime
expect fun SDateTime.plusSeconds(seconds: Long): SDateTime

expect fun SDate.toDateTimeAtStartOfDay(zone: STimeZone): SDateTime
expect fun SDateTime.toDate(): SDate

/** Milliseconds from 1970-01-01T00:00:00Z */
expect val SDateTime.timestamp: Long

expect fun daysBetween(a: SDateTime, b: SDateTime): Long
expect fun monthsBetween(a: SDateTime, b: SDateTime): Long
expect fun yearsBetween(a: SDateTime, b: SDateTime): Long

expect val now: SDateTime

fun SDate.isSame(other: SDate) = year == other.year && month == other.month && dayOfMonth == other.dayOfMonth
fun SDate.isToday() = isSame(today)
fun SDate.isTomorrow() = isSame(tomorrow)

fun SDateTime.minutesDifferenceAbs(other: SDateTime): Int {
    val first = timestamp / 1000 / 60
    val second = other.timestamp / 1000 / 60
    return kotlin.math.abs((second - first)).toInt()
}

fun SDateTime.minutesDifference(other: SDateTime): Int {
    val first = timestamp / 1000 / 60
    val second = other.timestamp / 1000 / 60
    return (second - first).toInt()
}

expect fun sDateTime(iso: String): SDateTime
expect fun sDateTime(year: Int, month: Int, day: Int, hours: Int, minutes: Int, timezone: STimeZone): SDateTime


expect val SDateTime.second: Int
expect val SDateTime.minute: Int
expect val SDateTime.minuteOfDay: Int
expect val SDateTime.hour: Int
expect val SDateTime.dayOfMonth: Int
expect val SDateTime.month: Int
expect val SDateTime.year: Int
expect val SDateTime.weekday: Weekday
expect val clientTimeZone: STimeZone
