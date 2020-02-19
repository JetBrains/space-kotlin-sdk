package space.jetbrains.api.runtime

import kotlinx.cinterop.convert
import platform.Foundation.*
import kotlin.test.*


class SDateMacOSTest {

    @Test
    fun `date from ISO string returns valid date`() {
        val date = SDate("2005-06-27T21:00:00Z")
        assertEquals(2005, date.year)
        assertEquals(6, date.month)
        assertEquals(27, date.dayOfMonth)
    }


    @Test
    fun `toString returns correct date`() {
        val date = SDate(createNSDate(2005, 6, 27, 9, 10, 20))
        assertEquals("2005-06-27T07:10:20Z", date.toString())
    }

    @Test
    fun `two equal dates should return true on comparison of equality`() {
        val date1 = SDate("2005-06-27T21:00:00Z")
        val date2 = SDate("2005-06-27T21:00:00Z")
        assertTrue(date1.equals(date2))
    }

    @Test
    fun `withDay should set a specific day for the given date`() {
        val date = SDate(createNSDate(2020, 2, 18, 21, 0, 0))
        val newDate = date.withDay(20)
        assertEquals(2020, newDate.year)
        assertEquals(2, newDate.month)
        assertEquals(20, newDate.dayOfMonth)
    }

    @Test
    fun `plusDays should add days to the given date`() {
        val date = SDate(createNSDate(2020, 3, 29, 21, 0, 0))
        val newDate = date.plusDays(3)
        assertEquals(2020, newDate.year)
        assertEquals(4, newDate.month)
        assertEquals(1, newDate.dayOfMonth)
    }

    @Test
    fun `plusMonths should add months to the given date`() {
        val date = SDate(createNSDate(2020, 3, 29, 21, 0, 0))
        val newDate = date.plusMonths(3)
        assertEquals(2020, newDate.year)
        assertEquals(6, newDate.month)
        assertEquals(29, newDate.dayOfMonth)
    }

    @Test
    fun `plusYears should add years to the given date`() {
        val date = SDate(createNSDate(2020, 3, 29, 21, 0, 0))
        val newDate = date.plusYears(3)
        assertEquals(2023, newDate.year)
        assertEquals(3, newDate.month)
        assertEquals(29, newDate.dayOfMonth)
    }

    @Test
    fun `daysBetween should return number of days between two dates`() {
        val date1 = SDate(createNSDate(2020, 3, 29, 21, 0, 0))
        val date2 = SDate(createNSDate(2020, 4, 5, 21, 0, 0))
        assertEquals(7, daysBetween(date1, date2))
    }

    @Test
    fun `monthsBetween should return number of months between two dates`() {
        val date1 = SDate(createNSDate(2020, 3, 29, 21, 0, 0))
        val date2 = SDate(createNSDate(2020, 6, 31, 21, 0, 0))
        assertEquals(3, monthsBetween(date1, date2))
    }

    @Test
    fun `weekDay should return day of week`() {
        val date = SDate(createNSDate(2020, 3, 29, 21, 0, 0))
        assertEquals(Weekday.MONDAY, date.weekday)
    }


}
