package org.eu.suisei.proxyreset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProxyEndpointTest {
    @Test fun acceptsUsableAddressesAndNormalizesInput() {
        assertEquals(ProxyEndpoint("127.0.0.1", 2080), ProxyEndpoint.parse(" 127.0.0.1:2080 "))
        assertEquals(ProxyEndpoint("proxy.example", 65535), ProxyEndpoint.parse("Proxy.Example:65535"))
        assertEquals(ProxyEndpoint("localhost", 1), ProxyEndpoint.parse("localhost:1"))
    }

    @Test fun rejectsAddressesThatWouldBeSavedButNotApplied() {
        listOf("", ":0", "bad:port", "127.0.0.1", "127.0.0.1:0", "127.0.0.1:65536",
            "127.0.0.1:-1", "127.0.0.1:+80", "999.1.1.1:80", "127.0.1:80",
            "http://127.0.0.1:80", "a b:80", "a..b:80", "-proxy:80", "[::1]:80",
            "127.0.0.1:80/path", "user@host:80").forEach {
            assertNull("Should reject $it", ProxyEndpoint.parse(it))
        }
    }
}
