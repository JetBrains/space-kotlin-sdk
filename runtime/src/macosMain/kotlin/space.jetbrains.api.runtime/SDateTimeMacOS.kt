package space.jetbrains.api.runtime


/** @param timestamp milliseconds from 1970-01-01T00:00:00Z */
actual class SDateTime actual constructor(timestamp: Long) : Comparable<SDateTime> {
    /** This date time in ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZZ) */
    actual val iso: String
        get() = TODO("Not yet implemented")

    /** [iso] */
    actual override fun toString(): String {
        TODO("Not yet implemented")
    }

    actual override fun equals(other: Any?): Boolean {
        TODO("Not yet implemented")
    }

    actual override fun hashCode(): Int {
        TODO("Not yet implemented")
    }

    override fun compareTo(other: SDateTime): Int {
        TODO("Not yet implemented")
    }

}

actual fun SDateTime.withZone(zone: STimeZone): SDateTime {
    TODO("Not yet implemented")
}

actual fun SDateTime.plusDays(days: Int): SDateTime {
    TODO("Not yet implemented")
}

actual fun SDateTime.plusMonths(months: Int): SDateTime {
    TODO("Not yet implemented")
}

actual fun SDateTime.plusYears(years: Int): SDateTime {
    TODO("Not yet implemented")
}

actual fun SDateTime.plusMinutes(minutes: Int): SDateTime {
    TODO("Not yet implemented")
}

actual fun SDateTime.plusSeconds(seconds: Int): SDateTime {
    TODO("Not yet implemented")
}

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
    TODO("Not yet implemented")
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

actual val SDateTime.weekday: Weekday get() = TODO()
