package app.toque.ui

import app.toque.core.machine.RootShell
import java.util.concurrent.Executors

/**
 * Hintergrundlicht direkt über den Treiber (root) : une luminosité de fenêtre à 0 est relevée par
 * Android au minimum autorisé, l'écran reste donc éclairé. Pour la veille de Toque, on écrit 0 dans
 * le fichier du rétroéclairage, puis on remet la valeur d'avant au réveil.
 */
object Backlight {
    private val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "toque-ecran") }
    private const val FIND = "for f in /sys/class/leds/lcd-backlight/brightness /sys/class/backlight/*/brightness; " +
        "do [ -w \$f ] && { echo \$f; break; }; done"
    @Volatile private var saved: String? = null

    fun off() = exec.execute {
        val r = RootShell.run("F=\$($FIND); [ -n \"\$F\" ] && cat \$F && echo 0 > \$F", 5_000)
        val v = r.out.trim().lines().firstOrNull()?.trim()
        if (!v.isNullOrEmpty() && v != "0") saved = v
    }

    fun on() = exec.execute {
        val v = saved ?: RootShell.run("settings get system screen_brightness", 5_000).out.trim().takeIf { it.toIntOrNull() != null } ?: "128"
        RootShell.run("F=\$($FIND); [ -n \"\$F\" ] && echo $v > \$F", 5_000)
    }
}
