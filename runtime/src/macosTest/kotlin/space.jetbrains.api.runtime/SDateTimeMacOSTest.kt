package space.jetbrains.api.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class SDateTimeMacOSTest {

    @Test
    fun `time from NSDate returns valid time`() {
        val time = SDateTime(createNSDate(2020, 1, 1, 12, 3, 1))
        assertEquals(12, time.hour)
        assertEquals(3, time.minute)
        assertEquals(1, time.second)
    }

    @Test
    fun `time from long returns valid time`() {
        val time = SDateTime(4043434343L)
        assertEquals(1, time.hour)
        assertEquals(12, time.minute)
        assertEquals(23, time.second)
    }

    @Test
    fun `plusDays should add days to the given date`() {
        val date = SDateTime(createNSDate(2020, 3, 29, 21, 0, 0))
        val newDate = date.plusDays(3)
        assertEquals(2020, newDate.year)
        assertEquals(4, newDate.month)
        assertEquals(1, newDate.dayOfMonth)
    }


    @Test
    fun `plusMonths should add months to the given date`() {
        val date = SDateTime(createNSDate(2020, 3, 29, 21, 0, 0))
        val newDate = date.plusMonths(3)
        assertEquals(2020, newDate.year)
        assertEquals(6, newDate.month)
        assertEquals(29, newDate.dayOfMonth)
    }

    @Test
    fun `plusYears should add years to the given date`() {
        val date = SDateTime(createNSDate(2020, 3, 29, 21, 0, 0))
        val newDate = date.plusYears(3)
        assertEquals(2023, newDate.year)
        assertEquals(3, newDate.month)
        assertEquals(29, newDate.dayOfMonth)
    }

    @Test
    fun `plusMinutes should add minutes to the given date`() {
        val date = SDateTime(createNSDate(2020, 3, 29, 21, 0, 0))
        val newDate = date.plusMinutes(3)
        assertEquals(3, newDate.minute)
    }

    @Test
    fun `plusSeconds should add seconds to the given date`() {
        val date = SDateTime(createNSDate(2020, 3, 29, 21, 0, 0))
        val newDate = date.plusSeconds(3)
        assertEquals(3, newDate.second)
    }
}