package cc.kesardev.scparchive

import cc.kesardev.scparchive.data.ProxyConfig
import cc.kesardev.scparchive.data.ProxyEndpoint
import cc.kesardev.scparchive.data.ProxyKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ProxySettingsTest {

    @Test
    fun defaultConfigIsBlankAndDisabled() {
        val config = ProxyConfig()

        assertFalse(config.enabled)
        assertEquals("", config.address)
        assertEquals(ProxyKind.HTTP, config.kind)
    }
    @Test
    fun parsesHttpsAddressAsHttpProxy() {
        val endpoint = ProxyEndpoint.parse("https://proxy.kesar.cc:20170", ProxyKind.HTTP)

        assertEquals("proxy.kesar.cc", endpoint?.host)
        assertEquals(20170, endpoint?.port)
        assertEquals(ProxyKind.HTTP, endpoint?.kind)
    }

    @Test
    fun parsesSocksAddressAndDefaultPort() {
        val explicit = ProxyEndpoint.parse("socks5://127.0.0.1:1080", ProxyKind.HTTP)
        val defaultPort = ProxyEndpoint.parse("socks://127.0.0.1", ProxyKind.HTTP)

        assertEquals(ProxyKind.SOCKS, explicit?.kind)
        assertEquals(1080, explicit?.port)
        assertEquals(ProxyKind.SOCKS, defaultPort?.kind)
        assertEquals(1080, defaultPort?.port)
    }

    @Test
    fun acceptsBareHostPortUsingSelectedKind() {
        val endpoint = ProxyEndpoint.parse("127.0.0.1:3128", ProxyKind.HTTP)

        assertEquals("127.0.0.1", endpoint?.host)
        assertEquals(3128, endpoint?.port)
        assertEquals(ProxyKind.HTTP, endpoint?.kind)
    }

    @Test
    fun rejectsBlankAddress() {
        assertNull(ProxyEndpoint.parse("   ", ProxyKind.HTTP))
    }
}