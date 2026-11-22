package app.toque.ui

import app.toque.core.machine.RootShell
import java.util.concurrent.Executors

/**
 * Veille de l'écran du robot (root).
 *
 * [androidSleep] met Android en veille comme la touche Marche/Arrêt : le bouton du robot le rallume
 * ensuite par son pilote. On n'écrit plus dans le fichier du rétroéclairage : un 0 écrit à la main
 * empêchait le bouton de rallumer l'écran (voir MainActivity). [repair] remet seulement un
 * rétroéclairage resté à 0 par une ancienne version.
 */
object Backlight {
    private val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "toque-ecran") }
    private const val LED = "/sys/class/leds/lcd-backlight/brightness"

    /*
     * Touche Marche/Arrêt injectée sur le clavier du robot : « sendevent » est immédiat, « input
     * keyevent 26 » met plusieurs secondes à démarrer sur ce processeur (secours seulement).
     */
    private const val POWER = "D=; for e in /dev/input/event*; do " +
        "n=\$(cat /sys/class/input/\${e##*/}/device/name 2>/dev/null); [ \"\$n\" = mtk-kpd ] && D=\$e; done; " +
        "if [ -n \"\$D\" ]; then sendevent \$D 1 116 1; sendevent \$D 0 0 0; sendevent \$D 1 116 0; sendevent \$D 0 0 0; " +
        "else input keyevent 26; fi"

    fun androidSleep() = exec.execute { RootShell.run(POWER, 10_000) }

    fun repair() = exec.execute {
        RootShell.run("[ -w $LED ] && [ \"\$(cat $LED)\" = 0 ] && " +
            "{ v=\$(settings get system screen_brightness); case \$v in ''|*[!0-9]*|0) v=128;; esac; echo \$v > $LED; }", 10_000)
    }
}
