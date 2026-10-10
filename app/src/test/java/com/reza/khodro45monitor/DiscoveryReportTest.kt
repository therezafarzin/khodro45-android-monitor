package com.reza.khodro45monitor
import org.junit.Assert.*
import org.junit.Test
class DiscoveryReportTest {
 @Test fun `redacts nested secrets`() { val out=DiscoveryRedactor.redact(org.json.JSONObject("{\"token\":\"x\",\"nested\":{\"email\":\"a@b\",\"ok\":1}}")) as org.json.JSONObject; assertEquals("[REDACTED]",out.getString("token"));assertEquals("[REDACTED]",out.getJSONObject("nested").getString("email")) }
 @Test fun `serialized report has no sensitive values`() { val report=DiscoveryReportFactory.json(listOf("{\"password\":\"hidden\",\"payload\":{\"session\":\"secret\"}}")); assertFalse(report.contains("hidden"));assertFalse(report.contains("secret"));assertTrue(report.contains("[REDACTED]")) }
}
