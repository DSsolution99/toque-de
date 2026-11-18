package app.toque.recipes

import android.content.Context
import android.os.Handler
import android.os.Looper
import app.toque.core.recipes.Recipe
import app.toque.core.recipes.ShoppingList
import java.io.File

/**
 * Liste de courses enregistrée sur le robot (fichier JSON), partagée entre l'écran du robot et
 * l'interface web du téléphone. Toutes les opérations sont synchronisées.
 */
class ShoppingRepo(context: Context) {
    private val file = File(context.filesDir, "einkaufsliste.json")
    private val main = Handler(Looper.getMainLooper())
    private val listeners = ArrayList<() -> Unit>()
    private val list: ShoppingList = try {
        if (file.exists()) ShoppingList.fromJson(file.readText()) else ShoppingList()
    } catch (_: Exception) {
        ShoppingList()
    }

    /** Numéro de version, augmenté à chaque changement (le téléphone s'en sert pour se rafraîchir). */
    @Volatile var version = 0L
        private set

    fun onChange(l: () -> Unit): () -> Unit {
        synchronized(listeners) { listeners += l }
        return { synchronized(listeners) { listeners -= l } }
    }

    fun <T> read(block: (ShoppingList) -> T): T = synchronized(list) { block(list) }

    private fun <T> write(block: (ShoppingList) -> T): T {
        val result = synchronized(list) {
            val r = block(list)
            version++
            try {
                val tmp = File(file.path + ".part")
                tmp.writeText(list.toJson())
                tmp.renameTo(file)
            } catch (_: Exception) {
            }
            r
        }
        val copy = synchronized(listeners) { ArrayList(listeners) }
        main.post { for (l in copy) l() }
        return result
    }

    fun add(recipe: Recipe, factor: Double = 1.0): Int = write { it.add(recipe, factor) }
    fun toggle(key: String): Boolean = write { it.toggle(key) }
    fun remove(key: String): Boolean = write { it.remove(key) }
    fun clearChecked(): Int = write { it.clearChecked() }
    fun clear() = write { it.clear() }
}
