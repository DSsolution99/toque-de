package app.toque.ui.screens

import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import app.toque.core.i18n.tr
import app.toque.ui.Icon
import app.toque.ui.MainActivity
import app.toque.ui.Screen
import app.toque.ui.Ui

/** Liste de courses : une ligne par ingrédient (quantités additionnées), par rayon ; toucher = coché. */
class ShoppingScreen(a: MainActivity) : Screen(a) {
    override val title = tr("Liste de courses")
    private lateinit var list: LinearLayout
    private lateinit var scroller: ScrollView
    private var unsub: (() -> Unit)? = null

    override fun build(): View {
        val root = Ui.vertical(c)
        root.setPadding(Ui.u(24), Ui.u(6), Ui.u(24), Ui.u(18))
        val bar = Ui.horizontal(c)
        bar.addView(Ui.text(c, tr("Ajoutez des ingrédients depuis une recette (bouton +) ou depuis le téléphone."), 16f, Ui.TEXT2),
            LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        bar.addView(Ui.button(c, tr("Retirer les cochés"), Ui.Style.SECONDARY, Icon.Kind.CHECK, height = 54, textSize = 17f) {
            a.app.shopping.clearChecked()
        }, Ui.lp(Ui.WRAP, Ui.WRAP, l = 12))
        bar.addView(Ui.button(c, tr("Tout effacer"), Ui.Style.GHOST, Icon.Kind.CLOSE, height = 54, textSize = 17f) {
            a.confirm(tr("Tout effacer"), tr("Vider toute la liste de courses ?"), tr("Tout effacer")) { a.app.shopping.clear() }
        }, Ui.lp(Ui.WRAP, Ui.WRAP, l = 12))
        root.addView(bar)
        list = Ui.vertical(c)
        scroller = Ui.scroll(c, list)
        root.addView(scroller, LinearLayout.LayoutParams(Ui.MATCH, 0, 1f).apply { topMargin = Ui.u(12) })
        return root
    }

    private fun render() {
        list.removeAllViews()
        val groups = a.app.shopping.read { it.byAisle() }
        if (groups.isEmpty()) {
            list.addView(Ui.text(c, tr("La liste de courses est vide."), 21f, Ui.TEXT2).apply { gravity = Gravity.CENTER },
                Ui.lp(t = 60))
            return
        }
        for ((aisle, items) in groups) {
            list.addView(Ui.text(c, aisle.ifEmpty { tr("Autres") }.uppercase(), 15f, Ui.TEXT2, Ui.MEDIUM)
                .apply { letterSpacing = 0.08f }, Ui.lp(t = 14, b = 4))
            for (item in items) {
                val row = Ui.horizontal(c)
                row.setPadding(Ui.u(16), Ui.u(12), Ui.u(16), Ui.u(12))
                row.background = Ui.pressable(Ui.round(Ui.SURFACE, 18f), 18f)
                row.addView(Icon(c, if (item.checked) Icon.Kind.CHECK else Icon.Kind.PLUS, if (item.checked) Ui.ACCENT else Ui.TEXT3),
                    LinearLayout.LayoutParams(Ui.u(26), Ui.u(26)).apply { rightMargin = Ui.u(14) })
                val col = Ui.vertical(c)
                val name = Ui.text(c, item.display, 19f, if (item.checked) Ui.TEXT3 else Ui.TEXT, Ui.MEDIUM)
                if (item.checked) name.paintFlags = name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                col.addView(name)
                col.addView(Ui.ellipsize(Ui.text(c, item.recipes.joinToString(" · "), 14f, Ui.TEXT3)), Ui.lp(t = 2))
                row.addView(col, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
                row.isClickable = true
                row.setOnClickListener { a.app.shopping.toggle(item.key) }
                list.addView(row, Ui.lp(t = 8))
            }
        }
    }

    override fun onShow() {
        render()
        unsub = a.app.shopping.onChange { render() }
    }

    override fun onHide() {
        unsub?.invoke()
    }

    override fun onKnob(delta: Int): Boolean {
        scroller.smoothScrollBy(0, delta * Ui.u(160))
        return true
    }
}
