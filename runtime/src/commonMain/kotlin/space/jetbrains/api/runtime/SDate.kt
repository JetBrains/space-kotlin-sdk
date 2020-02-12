package space.jetbrains.api.runtime

/** @param iso date in ISO8601 format (yyyy-MM-dd) */
expect class SDate(iso: String) {
    /** This date in ISO8601 format (yyyy-MM-dd) */
    val iso: String

    /** [iso] */
    override fun toString(): String
}
