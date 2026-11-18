package app.toque.core.net

import java.net.InetAddress

/** Adresses du réseau de la maison : seules celles-ci peuvent utiliser l'interface web du robot. */
object LocalNetwork {
    /**
     * Adresse du réseau local (ou de l'appareil lui-même). Une connexion IPv4 peut arriver sous la
     * forme « ::ffff:192.168.x.y » (socket IPv6) : on regarde alors l'adresse IPv4.
     */
    fun isLocal(a: InetAddress?): Boolean {
        if (a == null) return false
        val b = a.address
        if (b.size == 16 && (0 until 10).all { b[it].toInt() == 0 } && b[10].toInt() == -1 && b[11].toInt() == -1) {
            return isLocal(InetAddress.getByAddress(b.copyOfRange(12, 16)))
        }
        // Adresses IPv6 locales uniques (fc00::/7), en plus du lien local et de l'ancien « site local ».
        val uniqueLocal = b.size == 16 && (b[0].toInt() and 0xFE) == 0xFC
        return a.isLoopbackAddress || a.isSiteLocalAddress || a.isLinkLocalAddress || uniqueLocal
    }
}
