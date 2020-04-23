package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.moment.*
import kotlin.js.Date

actual class SDateTime(val moment: Moment) : Comparable<SDateTime> {
    actual constructor(timestamp: Long) : this(moment(timestamp.toDouble()))

    actual val iso: String get() = moment.toISOString()
    actual override fun toString(): String = iso

    actual override fun hashCode(): Int = timestamp.toInt()

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false
        return timestamp == (other as SDateTime).timestamp
    }

    override operator fun compareTo(other: SDateTime): Int = when {
        moment == other.moment -> 0
        moment.isAfter(other.moment) -> 1
        else -> -1
    }
}

fun Moment.sDateTime(): SDateTime = SDateTime(this)

actual fun SDateTime.withZone(zone: STimeZone): SDateTime = SDateTime(moment.clone().tz(zone.id))

actual fun SDateTime.plusDays(days: Long): SDateTime = moment.clone().add(days.toDouble(), "days").sDateTime()
actual fun SDateTime.plusMonths(months: Long): SDateTime = moment.clone().add(months.toDouble(), "months").sDateTime()
actual fun SDateTime.plusYears(years: Long): SDateTime = moment.clone().add(years.toDouble(), "years").sDateTime()
actual fun SDateTime.plusMinutes(minutes: Long): SDateTime = moment.clone().add(minutes.toDouble(), "minutes").sDateTime()
actual fun SDateTime.plusSeconds(seconds: Long): SDateTime = moment.clone().add(seconds.toDouble(), "seconds").sDateTime()

actual fun SDate.toDateTimeAtStartOfDay(zone: STimeZone): SDateTime = moment.clone().tz(zone.id).startOf("day").sDateTime()

actual fun SDateTime.toDate(): SDate = moment.clone().sDate()

actual val SDateTime.timestamp: Long get() = moment.valueOf().toLong()

actual fun daysBetween(a: SDateTime, b: SDateTime): Long = b.moment.diff(a.moment, "days").toLong()
actual fun monthsBetween(a: SDateTime, b: SDateTime): Long = b.moment.diff(a.moment, "months").toLong()
actual fun yearsBetween(a: SDateTime, b: SDateTime): Long = b.moment.diff(a.moment, "years").toLong()

actual val now: SDateTime get() = SDateTime(moment())

actual fun sDateTime(iso: String): SDateTime = SDateTime(moment(iso).local())

actual fun sDateTime(year: Int, month: Int, day: Int, hours: Int, minutes: Int, timezone: STimeZone): SDateTime {
    return moment(Date(year, month, day, hours, minutes)).tz(timezone.id).sDateTime()
}

actual val SDateTime.second: Int get() = moment.second().toInt()
actual val SDateTime.minute: Int get() = moment.minute().toInt()
actual val SDateTime.minuteOfDay: Int get() = moment.minute().toInt() + moment.hour().toInt() * 60
actual val SDateTime.hour: Int get() = moment.hour().toInt()
actual val SDateTime.dayOfMonth: Int get() = moment.date().toInt()
actual val SDateTime.month: Int get() = moment.month().toInt()
actual val SDateTime.year: Int get() = moment.year().toInt()
actual val SDateTime.weekday: Weekday get() = Weekday.byIsoNumber(moment.isoWeekday().toInt())

actual val clientTimeZone: STimeZone get() = STimeZone(MomentObject.tz.guess())
