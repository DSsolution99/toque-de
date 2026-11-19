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

    /** Noms réservés au réseau local : un site Internet ne peut pas les faire pointer vers le robot. */
    private val LOCAL_SUFFIXES = listOf(".local", ".lan", ".fritz.box", ".home.arpa", ".internal", ".localdomain")

    /** Nom d'hôte sans le port ni les crochets IPv6, en minuscules ; null si absent. */
    fun hostName(host: String?): String? {
        val h = host?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return if (h.startsWith("[")) h.substringBefore(']').removePrefix("[") else h.substringBefore(':')
    }

    /**
     * En-tête « Host » acceptable : l'adresse IP du robot tapée dans le navigateur, « localhost »
     * ou un nom du réseau local (« …fritz.box »). Bloque le « DNS rebinding » : une page Internet
     * qui ferait pointer son propre nom de domaine vers le robot pour piloter l'interface.
     */
    fun isTrustedHost(host: String?): Boolean {
        val h = hostName(host) ?: return false
        if (h == "localhost") return true
        if (Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$").matches(h))
            return h.split('.').all { it.toInt() in 0..255 }
        if (h.contains(':') && Regex("^[0-9a-f:.%a-z]+$").matches(h)) return true
        return LOCAL_SUFFIXES.any { h.endsWith(it) && h.length > it.length }
    }

    /** Une requête qui modifie quelque chose doit venir de la page du robot elle-même (Origin = Host). */
    fun isSameOrigin(origin: String?, host: String?): Boolean {
        val o = origin?.trim()?.takeIf { it.isNotEmpty() && it != "null" } ?: return true
        val h = host?.trim()?.lowercase() ?: return false
        val oHost = o.lowercase().substringAfter("://", "").trimEnd('/')
        return oHost == h || oHost == h.removeSuffix(":80")
    }
}
