package com.reza.khodro45monitor
import org.junit.Assert.*
import org.junit.Test
class FilterUrlTest {
 @Test fun `accepts all official listing URL forms`() { listOf("https://dealer.khodro45.com/cars", "https://dealer.khodro45.com/cars/", "https://dealer.khodro45.com/cars?ordering=suggested", "https://dealer.khodro45.com/cars?price__lte=%221000000000%22&ordering=suggested").forEach { assertNotNull(FilterUrl.import(it)) } }
 @Test fun `preserves encoded and unknown query parameters`() { val result=FilterUrl.import("https://dealer.khodro45.com/cars?price__lte=%221000000000%22&future=x&ordering=suggested")!!; assertEquals("price__lte=%221000000000%22&future=x",result.query); assertEquals("suggested",result.ordering) }
 @Test fun `rejects a non khodro url`()=assertNull(FilterUrl.import("https://example.com/cars?brand=x"))
 @Test fun `rejects unsafe schemes`() { assertNull(FilterUrl.import("javascript:alert(1)")); assertNull(FilterUrl.import("file:///etc/test")) }
 @Test fun `normalizes Arabic letters`()=assertEquals("دنا یک",Persian.normalize(" دنا يـك "))
 @Test fun `formats price in Persian`()=assertTrue(Persian.toman(980000000).contains("تومان"))
}
