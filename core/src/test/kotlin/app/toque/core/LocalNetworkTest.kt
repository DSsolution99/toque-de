package app.toque.core

import app.toque.core.net.LocalNetwork
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class LocalNetworkTest {
    private fun ip(bytes: List<Int>): InetAddress = InetAddress.getByAddress(bytes.map { it.toByte() }.toByteArray())
    private fun mapped(a: Int, b: Int, c: Int, d: Int) = ip(List(10) { 0 } + listOf(0xFF, 0xFF, a, b, c, d))

    @Test fun homeNetworkIsAllowed() {
        assertTrue(LocalNetwork.isLocal(ip(listOf(192, 168, 178, 42))))
        assertTrue(LocalNetwork.isLocal(ip(listOf(10, 0, 0, 5))))
        assertTrue(LocalNetwork.isLocal(ip(listOf(172, 16, 1, 1))))
        assertTrue(LocalNetwork.isLocal(ip(listOf(127, 0, 0, 1))))
        // IPv4 über einen IPv6-Socket (::ffff:192.168.178.42).
        assertTrue(LocalNetwork.isLocal(mapped(192, 168, 178, 42)))
        // IPv6: lokal eindeutig (fd…) und Link-lokal (fe80…).
        assertTrue(LocalNetwork.isLocal(ip(listOf(0xFD, 0x12) + List(14) { 1 })))
        assertTrue(LocalNetwork.isLocal(ip(listOf(0xFE, 0x80) + List(14) { 0 } .mapIndexed { i, v -> if (i == 13) 1 else v })))
    }

    @Test fun internetIsRefused() {
        assertFalse(LocalNetwork.isLocal(ip(listOf(8, 8, 8, 8))))
        assertFalse(LocalNetwork.isLocal(mapped(8, 8, 8, 8)))
        assertFalse(LocalNetwork.isLocal(ip(listOf(0x20, 0x01, 0x0d, 0xb8) + List(12) { 1 })))
        assertFalse(LocalNetwork.isLocal(null))
    }

    @Test fun onlyTheRobotsOwnAddressMayBeUsedAsHost() {
        assertTrue(LocalNetwork.isTrustedHost("192.168.178.36:8080"))
        assertTrue(LocalNetwork.isTrustedHost("localhost:8080"))
        assertTrue(LocalNetwork.isTrustedHost("[fe80::1]:8080"))
        assertTrue(LocalNetwork.isTrustedHost("android-5d4d.fritz.box:8080"))
        assertTrue(LocalNetwork.isTrustedHost("toque.local"))
        // DNS rebinding: ein fremder Domainname, der auf das Gerät zeigt.
        assertFalse(LocalNetwork.isTrustedHost("evil.example.com:8080"))
        assertFalse(LocalNetwork.isTrustedHost("192.168.178.36.evil.com"))
        assertFalse(LocalNetwork.isTrustedHost("fritz.box.evil.com"))
        assertFalse(LocalNetwork.isTrustedHost("999.1.1.1"))
        assertFalse(LocalNetwork.isTrustedHost(""))
        assertFalse(LocalNetwork.isTrustedHost(null))
    }

    @Test fun changesMustComeFromTheRobotsOwnPage() {
        assertTrue(LocalNetwork.isSameOrigin(null, "192.168.178.36:8080"))
        assertTrue(LocalNetwork.isSameOrigin("http://192.168.178.36:8080", "192.168.178.36:8080"))
        assertFalse(LocalNetwork.isSameOrigin("http://evil.example.com", "192.168.178.36:8080"))
        assertFalse(LocalNetwork.isSameOrigin("http://192.168.178.36:8080", null))
    }
}
