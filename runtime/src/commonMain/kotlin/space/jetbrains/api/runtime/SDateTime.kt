package space.jetbrains.api.runtime

/** @param timestamp milliseconds from 1970-01-01T00:00:00Z */
expect class SDateTime(timestamp: Long) {
    /** Milliseconds from 1970-01-01T00:00:00Z */
    val timestamp: Long

    /** This date time in ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZZ) */
    val iso: String

    /** [iso] */
    override fun toString(): String
}
