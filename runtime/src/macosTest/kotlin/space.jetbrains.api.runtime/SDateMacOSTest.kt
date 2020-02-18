package space.jetbrains.api.runtime

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
        val components = NSDateComponents()
        components.year = 2005
        components.month = 6
        components.day = 27
        components.hour = 9
        components.minute = 10
        components.second = 20
        components.setTimeZone(NSTimeZone.localTimeZone)
        val date = SDate(NSCalendar.currentCalendar.dateFromComponents(components)!!)
        assertEquals("2005-06-27T07:10:20Z", date.toString())
    }

    @Test
    fun `two equal dates should return true on comparison of equality`() {
        val date1 = SDate("2005-06-27T21:00:00Z")
        val date2 = SDate("2005-06-27T21:00:00Z")
        assertTrue(date1.equals(date2))
    }
}
