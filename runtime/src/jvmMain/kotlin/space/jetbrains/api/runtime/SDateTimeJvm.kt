package space.jetbrains.api.runtime

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.*
import java.util.TimeZone

actual class SDateTime(val javaDateTime: ZonedDateTime) : Comparable<SDateTime> {
    actual constructor(timestamp: Long) : this(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC))

    actual val iso: String get() = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(javaDateTime)

    actual override fun toString(): String = iso

    override fun compareTo(other: SDateTime) = javaDateTime.compareTo(other.javaDateTime)

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return javaDateTime == (other as SDateTime).javaDateTime
    }

    actual override fun hashCode(): Int = javaDateTime.hashCode()
}

fun ZonedDateTime.sDateTime(): SDateTime = SDateTime(this)
fun STimeZone.javaZoneId(): ZoneId = ZoneId.of(id)

actual fun SDateTime.withZone(zone: STimeZone): SDateTime = javaDateTime.withZoneSameInstant(ZoneId.of(zone.id)).sDateTime()

actual fun SDateTime.plusDays(days: Long): SDateTime = javaDateTime.plusDays(days).sDateTime()
actual fun SDateTime.plusMonths(months: Long): SDateTime = javaDateTime.plusMonths(months).sDateTime()
actual fun SDateTime.plusYears(years: Long): SDateTime = javaDateTime.plusYears(years).sDateTime()
actual fun SDateTime.plusMinutes(minutes: Long): SDateTime = javaDateTime.plusMinutes(minutes).sDateTime()
actual fun SDateTime.plusSeconds(seconds: Long): SDateTime = javaDateTime.plusSeconds(seconds).sDateTime()
actual fun SDate.toDateTimeAtStartOfDay(zone: STimeZone): SDateTime = javaDate.atStartOfDay(zone.javaZoneId()).sDateTime()

actual fun SDateTime.toDate(): SDate = javaDateTime.toLocalDate().sDate()

actual val SDateTime.timestamp: Long get() = javaDateTime.toInstant().toEpochMilli()

actual fun daysBetween(a: SDateTime, b: SDateTime): Long = ChronoUnit.DAYS.between(a.javaDateTime, b.javaDateTime)
actual fun monthsBetween(a: SDateTime, b: SDateTime): Long = ChronoUnit.MONTHS.between(a.javaDateTime, b.javaDateTime)
actual fun yearsBetween(a: SDateTime, b: SDateTime): Long = ChronoUnit.YEARS.between(a.javaDateTime, b.javaDateTime)

actual val now: SDateTime get() = SDateTime(ZonedDateTime.now())

actual fun sDateTime(iso: String): SDateTime = ZonedDateTime.parse(iso).sDateTime()

actual fun sDateTime(year: Int, month: Int, day: Int, hours: Int, minutes: Int, timezone: STimeZone): SDateTime {
    return ZonedDateTime.of(year, month, day, hours, minutes, 0, 0, timezone.javaZoneId()).sDateTime()
}

actual val SDateTime.second: Int get() = javaDateTime.second
actual val SDateTime.minute: Int get() = javaDateTime.minute
actual val SDateTime.minuteOfDay: Int get() = javaDateTime.get(ChronoField.MINUTE_OF_DAY)
actual val SDateTime.hour: Int get() = javaDateTime.hour
actual val SDateTime.dayOfMonth: Int get() = javaDateTime.dayOfMonth
actual val SDateTime.month: Int get() = javaDateTime.monthValue
actual val SDateTime.year: Int get() = javaDateTime.year
actual val SDateTime.weekday: Weekday get() = Weekday.byIsoNumber(javaDateTime.dayOfWeek.value)

actual val clientTimeZone: STimeZone get() = STimeZone(TimeZone.getDefault().id)
