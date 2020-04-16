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

expect fun SDateTime.plusDays(days: Int): SDateTime
expect fun SDateTime.plusMonths(months: Int): SDateTime
expect fun SDateTime.plusYears(years: Int): SDateTime
expect fun SDateTime.plusMinutes(minutes: Int): SDateTime
expect fun SDateTime.plusSeconds(seconds: Int): SDateTime

expect fun SDate.toDateTimeAtStartOfDay(zone: STimeZone): SDateTime
expect fun SDate.toDateTimeAtStartOfDay(): SDateTime
expect fun SDateTime.withTime(hours: Int, minutes: Int, seconds: Int, mills: Int): SDateTime
expect fun SDateTime.toDate(): SDate

/** Milliseconds from 1970-01-01T00:00:00Z */
expect val SDateTime.timestamp: Long

expect fun daysBetween(a: SDateTime, b: SDateTime): Int
expect fun monthsBetween(a: SDateTime, b: SDateTime): Int
expect fun yearsBetween(a: SDateTime, b: SDateTime): Int

expect fun STimeZone.offsetOnTime(time: SDateTime): Int

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
