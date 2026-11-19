package app.toque

import android.content.Context
import android.os.Build
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Android 6 (le Monsieur Cuisine Connect) ne connaît pas les racines de Let's Encrypt (ISRG Root
 * X1/X2, ajoutées dans Android 7.1.1). Or GitHub sert ses fichiers de version (et d'autres sites)
 * avec ces certificats : sans elles, « Trust anchor for certification path not found ».
 *
 * On ajoute ces deux racines publiques (assets/certs, empreintes vérifiées) à celles du système
 * pour toutes les connexions HTTPS de l'appli. Rien n'est assoupli : chaîne, dates et nom d'hôte
 * restent vérifiés comme d'habitude.
 */
object ExtraRoots {
    private val FILES = listOf("certs/isrg-root-x1.pem", "certs/isrg-root-x2.pem")

    fun install(context: Context, log: Logbook) {
        if (Build.VERSION.SDK_INT >= 25) return
        try {
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(null as KeyStore?)
            val system = tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
            val store = KeyStore.getInstance(KeyStore.getDefaultType())
            store.load(null, null)
            system.acceptedIssuers.forEachIndexed { i, cert -> store.setCertificateEntry("system-$i", cert) }
            val cf = CertificateFactory.getInstance("X.509")
            for (name in FILES) {
                val cert = context.assets.open(name).use { cf.generateCertificate(it) } as X509Certificate
                store.setCertificateEntry(name, cert)
            }
            val all = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            all.init(store)
            val ssl = SSLContext.getInstance("TLS")
            ssl.init(null, all.trustManagers, null)
            HttpsURLConnection.setDefaultSSLSocketFactory(ssl.socketFactory)
        } catch (e: Exception) {
            log.write("réseau", "racines Let's Encrypt non ajoutées : $e")
        }
    }
}
