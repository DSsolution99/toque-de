package app.toque.ui.screens

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import app.toque.core.i18n.tr
import app.toque.ui.MainActivity
import app.toque.ui.Screen
import app.toque.ui.Ui

/**
 * Liste des catégories (les plus fournies d'abord). Toucher une catégorie ouvre la liste des
 * recettes déjà filtrée ; le bouton du robot fait défiler.
 */
class CategoriesScreen(a: MainActivity) : Screen(a) {
    override val title = tr("Catégories")

    private var items: List<Pair<String, Int>> = emptyList()
    private lateinit var grid: GridView
    private lateinit var empty: TextView
    private val adapter = Adapter()
    private var unsub: (() -> Unit)? = null

    override fun build(): View {
        val root = android.widget.FrameLayout(c)
        root.setPadding(Ui.u(24), Ui.u(10), Ui.u(24), 0)
        grid = GridView(c).apply {
            numColumns = 4
            horizontalSpacing = Ui.u(14)
            verticalSpacing = Ui.u(14)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            selector = android.graphics.drawable.ColorDrawable(0)
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, 0, 0, Ui.u(20))
            clipToPadding = false
            adapter = this@CategoriesScreen.adapter
            setOnItemClickListener { _, _, pos, _ ->
                val cat = items.getOrNull(pos)?.first ?: return@setOnItemClickListener
                a.push(RecipesScreen(a, category = cat))
            }
        }
        root.addView(grid, Ui.flp())
        empty = Ui.text(c, tr("Aucune catégorie pour l'instant : synchronisez d'abord les recettes Lidl."), 21f, Ui.TEXT2)
            .apply { gravity = Gravity.CENTER }
        root.addView(empty, Ui.flp(Ui.WRAP, Ui.WRAP, Gravity.CENTER))
        return root
    }

    private fun refresh() {
        items = a.app.recipes.index.categoryCounts
        adapter.notifyDataSetChanged()
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onShow() {
        refresh()
        unsub = a.app.recipes.onChange { refresh() }
    }

    override fun onHide() {
        unsub?.invoke()
    }

    override fun onKnob(delta: Int): Boolean {
        grid.smoothScrollBy(delta * Ui.u(240), 150)
        return true
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val holder = (convertView?.tag as? Holder) ?: Holder(c)
            val (name, n) = items[position]
            holder.name.text = name
            holder.count.text = if (n == 1) tr("1 recette") else tr("%d recettes", n)
            holder.root.background = Ui.gradient(Ui.placeholderColors(name), 22f)
            return holder.root
        }
    }

    private class Holder(c: Context) {
        val root: LinearLayout = Ui.vertical(c, Gravity.BOTTOM)
        val name: TextView = Ui.ellipsize(Ui.text(c, "", 21f, Ui.TEXT, Ui.MEDIUM), 2)
        val count: TextView = Ui.text(c, "", 16f, Ui.TEXT2)

        init {
            root.tag = this
            root.setPadding(Ui.u(18), Ui.u(14), Ui.u(14), Ui.u(16))
            root.layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, Ui.u(140))
            root.addView(name)
            root.addView(count, Ui.lp(t = 4))
        }
    }
}
