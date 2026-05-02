package com.fogalarm

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WeatherRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repo: WeatherRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repo = WeatherRepository(server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() = server.shutdown()

    private fun fixture(name: String): String =
        javaClass.getResourceAsStream("/com/fogalarm/$name")!!.bufferedReader().readText()

    // --- parseFogEvent (pure, no network) ---

    @Test
    fun `fog code 45 in first hour returns FogEvent`() {
        val event = repo.parseFogEvent(fixture("fog_response.json"))
        assertNotNull(event)
        assertEquals(0, event!!.hourIndex)
        assertEquals("2024-01-01T10:00", event.isoTime)
    }

    @Test
    fun `fog code 48 rime fog in second hour returns FogEvent`() {
        val event = repo.parseFogEvent(fixture("fog_rime_response.json"))
        assertNotNull(event)
        assertEquals(1, event!!.hourIndex)
    }

    @Test
    fun `clear conditions return null`() {
        assertNull(repo.parseFogEvent(fixture("clear_response.json")))
    }

    @Test
    fun `visibility below 1000m triggers fog even without fog code`() {
        val event = repo.parseFogEvent(fixture("low_visibility_response.json"))
        assertNotNull(event)
        assertEquals(2, event!!.hourIndex)
    }

    // --- checkForFog (uses MockWebServer) ---

    @Test
    fun `checkForFog parses server response`() {
        server.enqueue(MockResponse().setBody(fixture("fog_response.json")).setResponseCode(200))
        val event = repo.checkForFog(51.5, -0.1)
        assertNotNull(event)
        assertEquals(0, event!!.hourIndex)
    }

    @Test
    fun `checkForFog throws on non-200`() {
        server.enqueue(MockResponse().setResponseCode(503))
        try {
            repo.checkForFog(51.5, -0.1)
            fail("Expected exception")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("503"))
        }
    }

    @Test
    fun `checkForFog returns null for clear sky`() {
        server.enqueue(MockResponse().setBody(fixture("clear_response.json")).setResponseCode(200))
        assertNull(repo.checkForFog(51.5, -0.1))
    }

    @Test
    fun `request uses correct lat lon query params`() {
        server.enqueue(MockResponse().setBody(fixture("clear_response.json")).setResponseCode(200))
        repo.checkForFog(51.1234, -2.5678)
        val req = server.takeRequest()
        assertTrue(req.path!!.contains("latitude=51.1234"))
        assertTrue(req.path!!.contains("longitude=-2.5678"))
    }
}
