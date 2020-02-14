@file:Suppress("unused")

package space.jetbrains.api.runtime.moment

import kotlin.js.*

/*
 * From https://momentjs.com/timezone/docs/#/use-it/node-js/
 *
 * moment-timezone module is just a wrapper for moment library
 * which internally does the following simplified trick:
 *
 * ```
 * var moment = require('moment')
 * var tz = { /* timezone code */ }
 * moment.tz = tz
 * return moment
 * ```
 */
@JsModule("moment-timezone")
external fun moment(inp: Any? = definedExternally,
                    format: Any? = definedExternally,
                    strict: Boolean? = definedExternally): Moment

@JsModule("moment-timezone")
external fun moment(inp: Any?,
                    format: Any? = definedExternally /* null */,
                    language: String? = definedExternally /* null */,
                    strict: Boolean? = definedExternally /* null */): Moment = definedExternally

@JsModule("moment-timezone")
external object MomentObject {
    val tz: MomentTimezone
    fun parseZone(s: String): Moment
}

@JsModule("moment-timezone")
external object MomentTimezoneObject

val MomentObjectHackDce = MomentObject
val MomentTimezoneHackDce = MomentTimezoneObject

external interface Locale {
    fun calendar(key: Any? /* "sameElse" */ = definedExternally /* null */, m: Moment? = definedExternally /* null */, now: Moment? = definedExternally /* null */): String
    fun calendar(key: String? = definedExternally /* null */, m: Moment? = definedExternally /* null */, now: Moment? = definedExternally /* null */): String
    fun longDateFormat(key: dynamic /* Any /* "LTS" */ | Any /* "LT" */ | Any /* "L" */ | Any /* "LL" */ | Any /* "LLL" */ | Any /* "LLLL" */ | Any /* "lts" */ | Any /* "lt" */ | Any /* "l" */ | Any /* "ll" */ | Any /* "lll" */ | Any /* "llll" */ */): String
    fun invalidDate(): String
    fun ordinal(n: Number): String
    fun preparse(inp: String): String
    fun postformat(inp: String): String
    fun relativeTime(n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean): String
    fun pastFuture(diff: Number, absRelTime: String): String
    fun set(config: Any)
    fun months(): Array<String>
    fun months(m: Moment, format: String? = definedExternally /* null */): String
    fun monthsShort(): Array<String>
    fun monthsShort(m: Moment, format: String? = definedExternally /* null */): String
    fun monthsParse(monthName: String, format: String, strict: Boolean): Number
    fun week(m: Moment): Number
    fun firstDayOfYear(): Number
    fun firstDayOfWeek(): Number
    fun weekdays(): Array<String>
    fun weekdays(m: Moment, format: String? = definedExternally /* null */): String
    fun weekdaysMin(): Array<String>
    fun weekdaysMin(m: Moment): String
    fun weekdaysShort(): Array<String>
    fun weekdaysShort(m: Moment): String
    fun weekdaysParse(weekdayName: String, format: String, strict: Boolean): Number
    fun weekdaysRegex(strict: Boolean): RegExp
    fun weekdaysShortRegex(strict: Boolean): RegExp
    fun weekdaysMinRegex(strict: Boolean): RegExp
    fun isPM(input: String): Boolean
    fun meridiem(hour: Number, minute: Number, isLower: Boolean): String
}

external interface StandaloneFormatSpec {
    var format: Array<String>
    var standalone: Array<String>
    var isFormat: RegExp? get() = definedExternally; set(value) = definedExternally
}

external interface WeekSpec {
    var dow: Number
    var doy: Number
}

external interface CalendarSpec {
    var sameDay: dynamic /* String | (m: Moment? /*= null*/, now: Moment? /*= null*/) -> String */ get() = definedExternally; set(value) = definedExternally
    var nextDay: dynamic /* String | (m: Moment? /*= null*/, now: Moment? /*= null*/) -> String */ get() = definedExternally; set(value) = definedExternally
    var lastDay: dynamic /* String | (m: Moment? /*= null*/, now: Moment? /*= null*/) -> String */ get() = definedExternally; set(value) = definedExternally
    var nextWeek: dynamic /* String | (m: Moment? /*= null*/, now: Moment? /*= null*/) -> String */ get() = definedExternally; set(value) = definedExternally
    var lastWeek: dynamic /* String | (m: Moment? /*= null*/, now: Moment? /*= null*/) -> String */ get() = definedExternally; set(value) = definedExternally
    var sameElse: dynamic /* String | (m: Moment? /*= null*/, now: Moment? /*= null*/) -> String */ get() = definedExternally; set(value) = definedExternally
}

//
//operator fun CalendarSpec.set(x: String, value: Unit) {
//}
//
//operator fun CalendarSpec.set(x: String, value: (m: Moment? /*= null*/, now: Moment? /*= null*/) -> String) {
//}
//
//operator fun CalendarSpec.set(x: String, value: String) {
//}
//
//operator fun CalendarSpec.get(x: String): dynamic /* String | (m: Moment? /*= null*/, now: Moment? /*= null*/) -> String | Unit */ {
//}

external interface RelativeTimeSpec {
    var future: dynamic /* String | (relTime: String) -> String */
    var past: dynamic /* String | (relTime: String) -> String */
    var s: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
    var m: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
    var mm: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
    var h: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
    var hh: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
    var d: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
    var dd: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
    var M: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
    var MM: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
    var y: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
    var yy: dynamic /* String | (n: Number, withoutSuffix: Boolean, key: dynamic /* Any /* "s" */ | Any /* "m" */ | Any /* "mm" */ | Any /* "h" */ | Any /* "hh" */ | Any /* "d" */ | Any /* "dd" */ | Any /* "M" */ | Any /* "MM" */ | Any /* "y" */ | Any /* "yy" */ */, isFuture: Boolean) -> String */
}

external interface LongDateFormatSpec {
    var LTS: String
    var LT: String
    var L: String
    var LL: String
    var LLL: String
    var LLLL: String
    var lts: String? get() = definedExternally; set(value) = definedExternally
    var lt: String? get() = definedExternally; set(value) = definedExternally
    var l: String? get() = definedExternally; set(value) = definedExternally
    var ll: String? get() = definedExternally; set(value) = definedExternally
    var lll: String? get() = definedExternally; set(value) = definedExternally
    var llll: String? get() = definedExternally; set(value) = definedExternally
}

external interface LocaleSpecification {
    var months: dynamic /* Array<String> | StandaloneFormatSpec | (momentToFormat: Moment, format: String? /*= null*/) -> String */ get() = definedExternally; set(value) = definedExternally
    var monthsShort: dynamic /* Array<String> | StandaloneFormatSpec | (momentToFormat: Moment, format: String? /*= null*/) -> String */ get() = definedExternally; set(value) = definedExternally
    var weekdays: dynamic /* Array<String> | StandaloneFormatSpec | (momentToFormat: Moment, format: String? /*= null*/) -> String */ get() = definedExternally; set(value) = definedExternally
    var weekdaysShort: dynamic /* Array<String> | StandaloneFormatSpec | (momentToFormat: Moment) -> String */ get() = definedExternally; set(value) = definedExternally
    var weekdaysMin: dynamic /* Array<String> | StandaloneFormatSpec | (momentToFormat: Moment) -> String */ get() = definedExternally; set(value) = definedExternally
    var meridiemParse: RegExp? get() = definedExternally; set(value) = definedExternally
    var meridiem: ((hour: Number, minute: Number, isLower: Boolean) -> String)? get() = definedExternally; set(value) = definedExternally
    var isPM: ((input: String) -> Boolean)? get() = definedExternally; set(value) = definedExternally
    var longDateFormat: LongDateFormatSpec? get() = definedExternally; set(value) = definedExternally
    var calendar: CalendarSpec? get() = definedExternally; set(value) = definedExternally
    var relativeTime: RelativeTimeSpec? get() = definedExternally; set(value) = definedExternally
    var invalidDate: String? get() = definedExternally; set(value) = definedExternally
    var ordinal: ((n: Number) -> String)? get() = definedExternally; set(value) = definedExternally
    var ordinalParse: RegExp? get() = definedExternally; set(value) = definedExternally
    var week: WeekSpec? get() = definedExternally; set(value) = definedExternally
}

/*
@nativeSetter
operator fun LocaleSpecification.set(x: String, value: Any) {
}

@nativeGetter
operator fun LocaleSpecification.get(x: String): Any? {
}
*/

external interface MomentObjectOutput {
    var years: Number
    var months: Number
    var date: Number
    var hours: Number
    var minutes: Number
    var seconds: Number
    var milliseconds: Number
    var tz: MomentTimezone?
}

external interface Duration {
    fun humanize(withSuffix: Boolean? = definedExternally /* null */): String
    fun abs(): Duration
    fun milliseconds(): Number
    fun asMilliseconds(): Number
    fun seconds(): Number
    fun asSeconds(): Number
    fun minutes(): Number
    fun asMinutes(): Number
    fun hours(): Number
    fun asHours(): Number
    fun days(): Number
    fun asDays(): Number
    fun weeks(): Number
    fun asWeeks(): Number
    fun months(): Number
    fun asMonths(): Number
    fun years(): Number
    fun asYears(): Number
    fun locale(): String
    fun localeData(): Locale
    fun toISOString(): String
    fun toJSON(): String
    fun lang(): Locale
    fun toIsoString(): String
}

external interface MomentRelativeTime {
    var future: Any
    var past: Any
    var s: Any
    var m: Any
    var mm: Any
    var h: Any
    var hh: Any
    var d: Any
    var dd: Any
    var M: Any
    var MM: Any
    var y: Any
    var yy: Any
}

external interface MomentLongDateFormat {
    var L: String
    var LL: String
    var LLL: String
    var LLLL: String
    var LT: String
    var LTS: String
    var l: String? get() = definedExternally; set(value) = definedExternally
    var ll: String? get() = definedExternally; set(value) = definedExternally
    var lll: String? get() = definedExternally; set(value) = definedExternally
    var llll: String? get() = definedExternally; set(value) = definedExternally
    var lt: String? get() = definedExternally; set(value) = definedExternally
    var lts: String? get() = definedExternally; set(value) = definedExternally
}

external interface MomentParsingFlags {
    var empty: Boolean
    var unusedTokens: Array<String>
    var unusedInput: Array<String>
    var overflow: Number
    var charsLeftOver: Number
    var nullInput: Boolean
    var invalidMonth: dynamic /* String | Unit */
    var invalidFormat: Boolean
    var userInvalidated: Boolean
    var iso: Boolean
    var parsedDateParts: Array<Any>
    var meridiem: dynamic /* String | Unit */
}

external interface MomentParsingFlagsOpt {
    var empty: Boolean? get() = definedExternally; set(value) = definedExternally
    var unusedTokens: Array<String>? get() = definedExternally; set(value) = definedExternally
    var unusedInput: Array<String>? get() = definedExternally; set(value) = definedExternally
    var overflow: Number? get() = definedExternally; set(value) = definedExternally
    var charsLeftOver: Number? get() = definedExternally; set(value) = definedExternally
    var nullInput: Boolean? get() = definedExternally; set(value) = definedExternally
    var invalidMonth: String? get() = definedExternally; set(value) = definedExternally
    var invalidFormat: Boolean? get() = definedExternally; set(value) = definedExternally
    var userInvalidated: Boolean? get() = definedExternally; set(value) = definedExternally
    var iso: Boolean? get() = definedExternally; set(value) = definedExternally
    var parsedDateParts: Array<Any>? get() = definedExternally; set(value) = definedExternally
    var meridiem: String? get() = definedExternally; set(value) = definedExternally
}

external interface MomentBuiltinFormat {
    var __momentBuiltinFormatBrand: Any
}

external interface MomentInputObject {
    var years: Number? get() = definedExternally; set(value) = definedExternally
    var year: Number? get() = definedExternally; set(value) = definedExternally
    var y: Number? get() = definedExternally; set(value) = definedExternally
    var months: Number? get() = definedExternally; set(value) = definedExternally
    var month: Number? get() = definedExternally; set(value) = definedExternally
    var M: Number? get() = definedExternally; set(value) = definedExternally
    var days: Number? get() = definedExternally; set(value) = definedExternally
    var day: Number? get() = definedExternally; set(value) = definedExternally
    var d: Number? get() = definedExternally; set(value) = definedExternally
    var dates: Number? get() = definedExternally; set(value) = definedExternally
    var date: Number? get() = definedExternally; set(value) = definedExternally
    var D: Number? get() = definedExternally; set(value) = definedExternally
    var hours: Number? get() = definedExternally; set(value) = definedExternally
    var hour: Number? get() = definedExternally; set(value) = definedExternally
    var h: Number? get() = definedExternally; set(value) = definedExternally
    var minutes: Number? get() = definedExternally; set(value) = definedExternally
    var minute: Number? get() = definedExternally; set(value) = definedExternally
    var m: Number? get() = definedExternally; set(value) = definedExternally
    var seconds: Number? get() = definedExternally; set(value) = definedExternally
    var second: Number? get() = definedExternally; set(value) = definedExternally
    var s: Number? get() = definedExternally; set(value) = definedExternally
    var milliseconds: Number? get() = definedExternally; set(value) = definedExternally
    var millisecond: Number? get() = definedExternally; set(value) = definedExternally
    var ms: Number? get() = definedExternally; set(value) = definedExternally
}

external interface DurationInputObject : MomentInputObject {
    var quarters: Number? get() = definedExternally; set(value) = definedExternally
    var quarter: Number? get() = definedExternally; set(value) = definedExternally
    var Q: Number? get() = definedExternally; set(value) = definedExternally
}

external interface MomentSetObject : MomentInputObject {
    var weekYears: Number? get() = definedExternally; set(value) = definedExternally
    var weekYear: Number? get() = definedExternally; set(value) = definedExternally
    var gg: Number? get() = definedExternally; set(value) = definedExternally
    var isoWeekYears: Number? get() = definedExternally; set(value) = definedExternally
    var isoWeekYear: Number? get() = definedExternally; set(value) = definedExternally
    var GG: Number? get() = definedExternally; set(value) = definedExternally
    var quarters: Number? get() = definedExternally; set(value) = definedExternally
    var quarter: Number? get() = definedExternally; set(value) = definedExternally
    var Q: Number? get() = definedExternally; set(value) = definedExternally
    var weeks: Number? get() = definedExternally; set(value) = definedExternally
    var week: Number? get() = definedExternally; set(value) = definedExternally
    var w: Number? get() = definedExternally; set(value) = definedExternally
    var isoWeeks: Number? get() = definedExternally; set(value) = definedExternally
    var isoWeek: Number? get() = definedExternally; set(value) = definedExternally
    var W: Number? get() = definedExternally; set(value) = definedExternally
    var dayOfYears: Number? get() = definedExternally; set(value) = definedExternally
    var dayOfYear: Number? get() = definedExternally; set(value) = definedExternally
    var DDD: Number? get() = definedExternally; set(value) = definedExternally
    var weekdays: Number? get() = definedExternally; set(value) = definedExternally
    var weekday: Number? get() = definedExternally; set(value) = definedExternally
    var e: Number? get() = definedExternally; set(value) = definedExternally
    var isoWeekdays: Number? get() = definedExternally; set(value) = definedExternally
    var isoWeekday: Number? get() = definedExternally; set(value) = definedExternally
    var E: Number? get() = definedExternally; set(value) = definedExternally
}

external interface FromTo {
    var from: Any
    var to: Any
}

external interface MomentCreationData {
    var input: String
    var format: String
    var locale: Locale
    var isUTC: Boolean
    var strict: Boolean
}

external interface TimezoneInstance {
    var name: String? get() = definedExternally; set(value) = definedExternally
    var abbrs: Array<dynamic /* String */>? get() = definedExternally; set(value) = definedExternally
    var untils: Array<dynamic /* Number */>? get() = definedExternally; set(value) = definedExternally
    var offsets: Array<dynamic /* Number */>? get() = definedExternally; set(value) = definedExternally

    fun abbr(timestamp: Number = definedExternally): String
    fun offset(timestamp: Number = definedExternally): Number
    fun utcOffset(timestamp: Number = definedExternally): Number
    fun parse(timestamp: Number = definedExternally): Number
}

external interface MomentTimezone {
    fun guess(ignoreCache: Boolean? = definedExternally): String
    fun setDefault(timezone: String)
    fun defaultTime()
    fun names() : Array<String>
    fun zone(id: String) : TimezoneInstance
    fun moment(value: String? = definedExternally, timezoneId: String? = definedExternally): Moment
}

external interface Moment {
    fun format(format: String? = definedExternally /* null */): String
    fun startOf(unitOfTime: Any): Moment
    fun endOf(unitOfTime: Any): Moment
    fun add(amount: Duration? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun add(amount: Number? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun add(amount: String? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun add(amount: FromTo? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun add(amount: DurationInputObject? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun add(amount: Unit? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun subtract(amount: Duration? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun subtract(amount: Number? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun subtract(amount: String? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun subtract(amount: FromTo? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun subtract(amount: DurationInputObject? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun subtract(amount: Unit? = definedExternally /* null */, unit: String? = definedExternally /* null */): Moment
    fun calendar(time: Moment? = definedExternally /* null */, formats: CalendarSpec? = definedExternally /* null */): String
    fun calendar(time: JsDate? = definedExternally /* null */, formats: CalendarSpec? = definedExternally /* null */): String
    fun calendar(time: String? = definedExternally /* null */, formats: CalendarSpec? = definedExternally /* null */): String
    fun calendar(time: Number? = definedExternally /* null */, formats: CalendarSpec? = definedExternally /* null */): String
    fun calendar(time: Array<dynamic /* Number | String */>? = definedExternally /* null */, formats: CalendarSpec? = definedExternally /* null */): String
    fun calendar(time: MomentInputObject? = definedExternally /* null */, formats: CalendarSpec? = definedExternally /* null */): String
    fun calendar(time: Unit? = definedExternally /* null */, formats: CalendarSpec? = definedExternally /* null */): String
    fun clone(): Moment
    fun valueOf(): Number
    fun local(keepLocalTime: Boolean? = definedExternally /* null */): Moment
    fun isLocal(): Boolean
    fun utc(keepLocalTime: Boolean? = definedExternally /* null */): Moment
    fun isUTC(): Boolean
    fun isUtc(): Boolean
    fun parseZone(): Moment
    fun isValid(): Boolean
    fun invalidAt(): Number
    fun hasAlignedHourOffset(other: Moment? = definedExternally /* null */): Boolean
    fun hasAlignedHourOffset(other: JsDate? = definedExternally /* null */): Boolean
    fun hasAlignedHourOffset(other: String? = definedExternally /* null */): Boolean
    fun hasAlignedHourOffset(other: Number? = definedExternally /* null */): Boolean
    fun hasAlignedHourOffset(other: Array<dynamic /* Number | String */>? = definedExternally /* null */): Boolean
    fun hasAlignedHourOffset(other: MomentInputObject? = definedExternally /* null */): Boolean
    fun hasAlignedHourOffset(other: Unit? = definedExternally /* null */): Boolean
    fun creationData(): MomentCreationData
    fun parsingFlags(): MomentParsingFlags
    fun year(y: Number): Moment
    fun year(): Number
    fun years(y: Number): Moment
    fun years(): Number
    fun quarter(): Number
    fun quarter(q: Number): Moment
    fun quarters(): Number
    fun quarters(q: Number): Moment
    fun month(M: Number): Moment
    fun month(M: String): Moment
    fun month(): Number
    fun months(M: Number): Moment
    fun months(M: String): Moment
    fun months(): Number
    fun day(d: Number): Moment
    fun day(d: String): Moment
    fun day(): Number
    fun days(d: Number): Moment
    fun days(d: String): Moment
    fun days(): Number
    fun date(d: Number): Moment
    fun date(): Number
    fun dates(d: Number): Moment
    fun dates(): Number
    fun hour(h: Number): Moment
    fun hour(): Number
    fun hours(h: Number): Moment
    fun hours(): Number
    fun minute(m: Number): Moment
    fun minute(): Number
    fun minutes(m: Number): Moment
    fun minutes(): Number
    fun second(s: Number): Moment
    fun second(): Number
    fun seconds(s: Number): Moment
    fun seconds(): Number
    fun millisecond(ms: Number): Moment
    fun millisecond(): Number
    fun milliseconds(ms: Number): Moment
    fun milliseconds(): Number
    fun weekday(): Number
    fun weekday(d: Number): Moment
    fun isoWeekday(): Number
    fun isoWeekday(d: Number): Moment
    fun isoWeekday(d: String): Moment
    fun weekYear(): Number
    fun weekYear(d: Number): Moment
    fun isoWeekYear(): Number
    fun isoWeekYear(d: Number): Moment
    fun week(): Number
    fun week(d: Number): Moment
    fun weeks(): Number
    fun weeks(d: Number): Moment
    fun isoWeek(): Number
    fun isoWeek(d: Number): Moment
    fun isoWeeks(): Number
    fun isoWeeks(d: Number): Moment
    fun weeksInYear(): Number
    fun isoWeeksInYear(): Number
    fun dayOfYear(): Number
    fun dayOfYear(d: Number): Moment
    fun from(inp: Moment, suffix: Boolean? = definedExternally /* null */): String
    fun from(inp: JsDate, suffix: Boolean? = definedExternally /* null */): String
    fun from(inp: String, suffix: Boolean? = definedExternally /* null */): String
    fun from(inp: Number, suffix: Boolean? = definedExternally /* null */): String
    fun from(inp: Array<dynamic /* Number | String */>, suffix: Boolean? = definedExternally /* null */): String
    fun from(inp: MomentInputObject, suffix: Boolean? = definedExternally /* null */): String
    fun from(inp: Unit, suffix: Boolean? = definedExternally /* null */): String
    fun to(inp: Moment, suffix: Boolean? = definedExternally /* null */): String
    fun to(inp: JsDate, suffix: Boolean? = definedExternally /* null */): String
    fun to(inp: String, suffix: Boolean? = definedExternally /* null */): String
    fun to(inp: Number, suffix: Boolean? = definedExternally /* null */): String
    fun to(inp: Array<dynamic /* Number | String */>, suffix: Boolean? = definedExternally /* null */): String
    fun to(inp: MomentInputObject, suffix: Boolean? = definedExternally /* null */): String
    fun to(inp: Unit, suffix: Boolean? = definedExternally /* null */): String
    fun fromNow(withoutSuffix: Boolean? = definedExternally /* null */): String
    fun toNow(withoutPrefix: Boolean? = definedExternally /* null */): String
    fun diff(b: Moment, unitOfTime: Any? = definedExternally /* null */, precise: Boolean? = definedExternally /* null */): Number
    fun diff(b: JsDate, unitOfTime: Any? = definedExternally /* null */, precise: Boolean? = definedExternally /* null */): Number
    fun diff(b: String, unitOfTime: Any? = definedExternally /* null */, precise: Boolean? = definedExternally /* null */): Number
    fun diff(b: Number, unitOfTime: Any? = definedExternally /* null */, precise: Boolean? = definedExternally /* null */): Number
    fun diff(b: Array<dynamic /* Number | String */>, unitOfTime: Any? = definedExternally /* null */, precise: Boolean? = definedExternally /* null */): Number
    fun diff(b: MomentInputObject, unitOfTime: Any? = definedExternally /* null */, precise: Boolean? = definedExternally /* null */): Number
    fun diff(b: Unit, unitOfTime: Any? = definedExternally /* null */, precise: Boolean? = definedExternally /* null */): Number
    fun toArray(): Array<Number>
    fun toDate(): JsDate
    fun toISOString(): String
    fun inspect(): String
    fun toJSON(): String
    fun unix(): Number
    fun isLeapYear(): Boolean
    fun zone(): Number
    fun zone(b: Number): Moment
    fun zone(b: String): Moment
    fun utcOffset(): Number
    fun utcOffset(b: Number, keepLocalTime: Boolean? = definedExternally /* null */): Moment
    fun utcOffset(b: String, keepLocalTime: Boolean? = definedExternally /* null */): Moment
    fun isUTCOffset(): Boolean
    fun daysInMonth(): Number
    fun isDST(): Boolean
    fun zoneAbbr(): String
    fun zoneName(): String
    fun isBefore(inp: Moment? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isBefore(inp: JsDate? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isBefore(inp: String? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isBefore(inp: Number? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isBefore(inp: Array<dynamic /* Number | String */>? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isBefore(inp: MomentInputObject? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isBefore(inp: Unit? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isAfter(inp: Moment? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isAfter(inp: JsDate? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isAfter(inp: String? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isAfter(inp: Number? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isAfter(inp: Array<dynamic /* Number | String */>? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isAfter(inp: MomentInputObject? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isAfter(inp: Unit? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSame(inp: Moment? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSame(inp: JsDate? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSame(inp: String? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSame(inp: Number? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSame(inp: Array<dynamic /* Number | String */>? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSame(inp: MomentInputObject? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSame(inp: Unit? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrAfter(inp: Moment? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrAfter(inp: JsDate? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrAfter(inp: String? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrAfter(inp: Number? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrAfter(inp: Array<dynamic /* Number | String */>? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrAfter(inp: MomentInputObject? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrAfter(inp: Unit? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrBefore(inp: Moment? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrBefore(inp: JsDate? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrBefore(inp: String? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrBefore(inp: Number? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrBefore(inp: Array<dynamic /* Number | String */>? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrBefore(inp: MomentInputObject? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isSameOrBefore(inp: Unit? = definedExternally /* null */, granularity: Any? = definedExternally /* null */): Boolean
    fun isBetween(a: Moment, b: dynamic /* Moment | Date | String | Number | Array<dynamic /* Number | String */> | MomentInputObject | Unit */, granularity: Any? = definedExternally /* null */, inclusivity: dynamic /* Any? /* "()" */ | Any? /* "[)" */ | Any? /* "(]" */ | Any? /* "[]" */ */ = definedExternally /* null */): Boolean
    fun isBetween(a: JsDate, b: dynamic /* Moment | Date | String | Number | Array<dynamic /* Number | String */> | MomentInputObject | Unit */, granularity: Any? = definedExternally /* null */, inclusivity: dynamic /* Any? /* "()" */ | Any? /* "[)" */ | Any? /* "(]" */ | Any? /* "[]" */ */ = definedExternally /* null */): Boolean
    fun isBetween(a: String, b: dynamic /* Moment | Date | String | Number | Array<dynamic /* Number | String */> | MomentInputObject | Unit */, granularity: Any? = definedExternally /* null */, inclusivity: dynamic /* Any? /* "()" */ | Any? /* "[)" */ | Any? /* "(]" */ | Any? /* "[]" */ */ = definedExternally /* null */): Boolean
    fun isBetween(a: Number, b: dynamic /* Moment | Date | String | Number | Array<dynamic /* Number | String */> | MomentInputObject | Unit */, granularity: Any? = definedExternally /* null */, inclusivity: dynamic /* Any? /* "()" */ | Any? /* "[)" */ | Any? /* "(]" */ | Any? /* "[]" */ */ = definedExternally /* null */): Boolean
    fun isBetween(a: Array<dynamic /* Number | String */>, b: dynamic /* Moment | Date | String | Number | Array<dynamic /* Number | String */> | MomentInputObject | Unit */, granularity: Any? = definedExternally /* null */, inclusivity: dynamic /* Any? /* "()" */ | Any? /* "[)" */ | Any? /* "(]" */ | Any? /* "[]" */ */ = definedExternally /* null */): Boolean
    fun isBetween(a: MomentInputObject, b: dynamic /* Moment | Date | String | Number | Array<dynamic /* Number | String */> | MomentInputObject | Unit */, granularity: Any? = definedExternally /* null */, inclusivity: dynamic /* Any? /* "()" */ | Any? /* "[)" */ | Any? /* "(]" */ | Any? /* "[]" */ */ = definedExternally /* null */): Boolean
    fun isBetween(a: Unit, b: dynamic /* Moment | Date | String | Number | Array<dynamic /* Number | String */> | MomentInputObject | Unit */, granularity: Any? = definedExternally /* null */, inclusivity: dynamic /* Any? /* "()" */ | Any? /* "[)" */ | Any? /* "(]" */ | Any? /* "[]" */ */ = definedExternally /* null */): Boolean
    fun lang(language: String): Moment
    fun lang(language: Moment): Moment
    fun lang(language: Duration): Moment
    fun lang(language: Array<String>): Moment
    fun lang(): Locale
    fun locale(): String
    fun locale(locale: String): Moment
    fun locale(locale: Moment): Moment
    fun locale(locale: Duration): Moment
    fun locale(locale: Array<String>): Moment
    fun localeData(): Locale
    fun isDSTShifted(): Boolean
    fun max(inp: Moment? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: JsDate? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: String? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: Number? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: Array<dynamic /* Number | String */>? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: MomentInputObject? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: Unit? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: Moment? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: JsDate? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: String? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: Number? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: Array<dynamic /* Number | String */>? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: MomentInputObject? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun max(inp: Unit? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: Moment? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: JsDate? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: String? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: Number? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: Array<dynamic /* Number | String */>? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: MomentInputObject? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: Unit? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: Moment? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: JsDate? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: String? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: Number? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: Array<dynamic /* Number | String */>? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: MomentInputObject? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun min(inp: Unit? = definedExternally /* null */, format: dynamic /* String? | MomentBuiltinFormat? | Array<dynamic /* String | MomentBuiltinFormat */>? */ = definedExternally /* null */, language: String? = definedExternally /* null */, strict: Boolean? = definedExternally /* null */): Moment
    fun get(unit: String): Number
    fun set(unit: String, value: Number): Moment
    fun set(objectLiteral: MomentSetObject): Moment
    fun toObject(): MomentObjectOutput
    fun tz(str: String): Moment
}

fun Moment.validate(): Moment = if (!this.isValid()) error("Invalid date") else this
