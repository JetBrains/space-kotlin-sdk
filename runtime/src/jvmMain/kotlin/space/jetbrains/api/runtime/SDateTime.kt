package space.jetbrains.api.runtime

import org.joda.time.*
import org.joda.time.format.ISODateTimeFormat
import java.util.TimeZone
import java.util.concurrent.TimeUnit.MILLISECONDS

actual class SDateTime(val joda: DateTime) : Comparable<SDateTime> {
    actual constructor(timestamp: Long) : this(DateTime(timestamp))

    actual val iso: String get() = joda.toString()

    actual override fun toString(): String = iso

    override fun compareTo(other: SDateTime) = joda.compareTo(other.joda)

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return joda == (other as SDateTime).joda
    }

    actual override fun hashCode(): Int = joda.hashCode()
}

fun DateTime.sDateTime(): SDateTime = SDateTime(this)
fun STimeZone.joda(): DateTimeZone = DateTimeZone.forID(id)

actual fun SDateTime.withZone(zone: STimeZone): SDateTime = joda.withZone(DateTimeZone.forID(zone.id)).sDateTime()

actual fun SDateTime.plusDays(days: Int): SDateTime = joda.plusDays(days).sDateTime()
actual fun SDateTime.plusMonths(months: Int): SDateTime = joda.plusMonths(months).sDateTime()
actual fun SDateTime.plusYears(years: Int): SDateTime = joda.plusYears(years).sDateTime()
actual fun SDateTime.plusMinutes(minutes: Int): SDateTime = joda.plusMinutes(minutes).sDateTime()
actual fun SDateTime.plusSeconds(seconds: Int): SDateTime = joda.plusSeconds(seconds).sDateTime()
actual fun SDate.toDateTimeAtStartOfDay(zone: STimeZone): SDateTime = joda.toDateTimeAtStartOfDay(zone.joda()).sDateTime()
actual fun SDate.toDateTimeAtStartOfDay(): SDateTime = joda.toDateTimeAtStartOfDay().sDateTime()
actual fun SDateTime.withTime(hours: Int, minutes: Int, seconds: Int, mills: Int): SDateTime =
    joda.withTime(hours, minutes, seconds, mills).sDateTime()

actual fun SDateTime.toDate(): SDate = joda.toLocalDate().sDate()

actual val SDateTime.timestamp: Long get() = joda.millis

actual fun daysBetween(a: SDateTime, b: SDateTime): Int = Days.daysBetween(a.joda, b.joda).days
actual fun monthsBetween(a: SDateTime, b: SDateTime): Int = Months.monthsBetween(a.joda, b.joda).months
actual fun yearsBetween(a: SDateTime, b: SDateTime): Int = Years.yearsBetween(a.joda, b.joda).years

actual fun STimeZone.offsetOnTime(time: SDateTime): Int = time.joda.withZone(DateTimeZone.forID(id)).let {
    MILLISECONDS.toMinutes(it.zone.getOffset(it.millis).toLong()).toInt()
}

actual val sNow: SDateTime get() = SDateTime(DateTime.now())

actual fun sDateTime(iso: String): SDateTime = DateTime.parse(iso, ISODateTimeFormat.dateTimeParser()).sDateTime()

actual fun sDateTime(year: Int, month: Int, day: Int, hours: Int, minutes: Int, timezone: STimeZone): SDateTime {
    return DateTime(year, month, day, hours, minutes, timezone.joda()).sDateTime()
}

actual val SDateTime.second: Int get() = joda.secondOfMinute
actual val SDateTime.minute: Int get() = joda.minuteOfHour
actual val SDateTime.minuteOfDay: Int get() = joda.minuteOfDay
actual val SDateTime.hour: Int get() = joda.hourOfDay
actual val SDateTime.dayOfMonth: Int get() = joda.dayOfMonth
actual val SDateTime.month: Int get() = joda.monthOfYear
actual val SDateTime.year: Int get() = joda.year
actual val SDateTime.weekday: Weekday get() = Weekday.byIsoNumber(joda.dayOfWeek)

actual val clientTimeZone: STimeZone get() = STimeZone(TimeZone.getDefault().id)
