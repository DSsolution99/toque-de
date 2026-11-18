package app.toque.core.recipes

import app.toque.core.i18n.I18n
import java.text.DecimalFormatSymbols

/**
 * Quantités des ingrédients telles qu'écrites dans les recettes : « 100 », « 0.5 », « 1,5 »,
 * « 1/2 », « 1 ½ », « 2-3 »… Sert à recalculer les portions et à additionner la liste de courses.
 * Un texte non numérique (« etwas », « 1 Prise ») n'est pas modifié.
 */
object Quantity {
    /** Une quantité lue : [low], et [high] pour une fourchette (« 2-3 »). */
    data class Amount(val low: Double, val high: Double? = null) {
        fun times(f: Double) = Amount(low * f, high?.let { it * f })
    }

    private val UNICODE = mapOf('½' to 0.5, '¼' to 0.25, '¾' to 0.75, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3, '⅛' to 0.125)
    private val RANGE = Regex("^(.+?)\\s*[-–]\\s*(.+)$")

    fun parse(text: String): Amount? {
        val t = text.trim()
        if (t.isEmpty()) return null
        RANGE.matchEntire(t)?.let { m ->
            val a = number(m.groupValues[1]) ?: return null
            val b = number(m.groupValues[2]) ?: return null
            return if (b > a) Amount(a, b) else Amount(a)
        }
        return number(t)?.let { Amount(it) }
    }

    /** Un nombre : entier, décimal (point ou virgule), fraction, fraction unicode, ou entier + fraction. */
    private fun number(text: String): Double? {
        var t = text.trim()
        if (t.isEmpty()) return null
        var total = 0.0
        val last = t.last()
        UNICODE[last]?.let { frac ->
            t = t.dropLast(1).trim()
            total += frac
            if (t.isEmpty()) return total
        }
        val parts = t.split(Regex("\\s+"))
        if (parts.size > 2) return null
        for (p in parts) {
            val v = if (p.contains('/')) {
                val (n, d) = p.split('/', limit = 2).map { it.trim().replace(',', '.').toDoubleOrNull() ?: return null }
                if (d == 0.0) return null
                n / d
            } else p.replace(',', '.').toDoubleOrNull() ?: return null
            if (v < 0 || v.isNaN() || v.isInfinite()) return null
            total += v
        }
        return total
    }

    /** [qty] multipliée par [factor], au même format lisible ; inchangée si elle n'est pas un nombre. */
    fun scale(qty: String, factor: Double): String {
        if (factor == 1.0) return qty
        val a = parse(qty) ?: return qty
        return format(a.times(factor))
    }

    fun format(a: Amount): String = if (a.high == null) format(a.low) else format(a.low) + "–" + format(a.high)

    private val NICE = listOf(0.25 to "¼", 1.0 / 3 to "⅓", 0.5 to "½", 2.0 / 3 to "⅔", 0.75 to "¾")

    /** Nombre arrondi lisiblement : entier à partir de 10, sinon fraction courante ou une décimale. */
    fun format(v: Double): String {
        if (v <= 0.0) return "0"
        if (v >= 10) return Math.round(v).toString()
        val whole = Math.floor(v)
        val frac = v - whole
        if (frac < 0.04) return whole.toLong().toString()
        if (frac > 0.96) return (whole.toLong() + 1).toString()
        for ((value, glyph) in NICE) {
            if (Math.abs(frac - value) < 0.04) return if (whole == 0.0) glyph else "${whole.toLong()}$glyph"
        }
        val rounded = Math.round(v * 10) / 10.0
        val sep = try { DecimalFormatSymbols.getInstance(I18n.locale).decimalSeparator } catch (_: Exception) { '.' }
        val s = if (rounded == Math.floor(rounded)) rounded.toLong().toString() else rounded.toString()
        return s.replace('.', sep)
    }
}
