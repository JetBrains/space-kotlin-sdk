@file:Suppress("unused")

package space.jetbrains.api.runtime.moment

@JsName("Date")
external class JsDate {
    fun toDateString(): String
    fun toTimeString(): String
    fun toLocaleString(): String
    fun toLocaleDateString(): String
    fun toLocaleTimeString(): String
    fun valueOf(): Number
    fun getTime(): Number
    fun getFullYear(): Number
    fun getUTCFullYear(): Number
    fun getMonth(): Number
    fun getUTCMonth(): Number
    fun getDate(): Number
    fun getUTCDate(): Number
    fun getDay(): Number
    fun getUTCDay(): Number
    fun getHours(): Number
    fun getUTCHours(): Number
    fun getMinutes(): Number
    fun getUTCMinutes(): Number
    fun getSeconds(): Number
    fun getUTCSeconds(): Number
    fun getMilliseconds(): Number
    fun getUTCMilliseconds(): Number
    fun getTimezoneOffset(): Number
    fun setTime(time: Number): Number
    fun setMilliseconds(ms: Number): Number
    fun setUTCMilliseconds(ms: Number): Number
    fun setSeconds(sec: Number, ms: Number? /* null */): Number
    fun setUTCSeconds(sec: Number, ms: Number? /* null */): Number
    fun setMinutes(min: Number, sec: Number? /* null */, ms: Number? /* null */): Number
    fun setUTCMinutes(min: Number, sec: Number? /* null */, ms: Number? /* null */): Number
    fun setHours(hours: Number, min: Number? /* null */, sec: Number? /* null */, ms: Number? /* null */): Number
    fun setUTCHours(hours: Number, min: Number? /* null */, sec: Number? /* null */, ms: Number? /* null */): Number
    fun setDate(date: Number): Number
    fun setUTCDate(date: Number): Number
    fun setMonth(month: Number, date: Number? /* null */): Number
    fun setUTCMonth(month: Number, date: Number? /* null */): Number
    fun setFullYear(year: Number, month: Number? /* null */, date: Number? /* null */): Number
    fun setUTCFullYear(year: Number, month: Number? /* null */, date: Number? /* null */): Number
    fun toUTCString(): String
    fun toISOString(): String
    fun toJSON(key: Any? /* null */): String
}

enum class Months {
    JAN, FEB, MAR, APR,
    MAY, JUN, JUL, AUG,
    SEP, OCT, NOV, DEC
}

data class CalDate(val date: Int, val month: Months, val year: Int) {
    constructor(date: Int, index: Int, year: Int) : this(date, Months.values()[index], year)
    constructor(jsDate: JsDate) : this(jsDate.getDate().toInt(), jsDate.getMonth().toInt(), jsDate.getFullYear().toInt())
    constructor(mom: Moment = moment()) : this(mom.date().toInt(), mom.month().toInt(), mom.year().toInt())
    constructor(timestamp: Number) : this(moment(timestamp))

    init {
        if (!toMoment().isValid()) {
            throw IllegalArgumentException("Invalid date")
        }
    }

    override fun toString() = "$date ${month.name.toLowerCase()} $year"
    fun toMoment() = moment("${this}Z") // use UTC for serialization
    fun toJsDate() = toMoment().toDate()
}
