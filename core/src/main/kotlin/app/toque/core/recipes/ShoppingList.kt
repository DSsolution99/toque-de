package app.toque.core.recipes

import app.toque.core.json.Json
import app.toque.core.json.bool
import app.toque.core.json.double
import app.toque.core.json.list
import app.toque.core.json.obj
import app.toque.core.json.str

/**
 * Liste de courses sur plusieurs recettes. Un même ingrédient (même identifiant du site, sinon
 * même nom) dans la même unité n'a qu'une ligne : les quantités s'additionnent. Les ingrédients
 * sans quantité (« Salz ») n'ont qu'une ligne, sans total.
 */
class ShoppingList {
    data class Item(
        val key: String,
        val name: String,
        val unit: String = "",
        /** Quantité totale, null si au moins une recette ne donne pas de quantité chiffrée. */
        val amount: Double? = null,
        /** Rayon (« Gemüse »…), vide si inconnu. */
        val aisle: String = "",
        /** Recettes d'où vient la ligne. */
        val recipes: List<String> = emptyList(),
        val checked: Boolean = false,
    ) {
        val display: String
            get() = buildString {
                if (amount != null && amount > 0) append(Quantity.format(amount)).append(' ')
                if (unit.isNotBlank() && amount != null) append(unit).append(' ')
                append(name)
            }
    }

    private val items = ArrayList<Item>()

    val all: List<Item> get() = ArrayList(items)
    val size: Int get() = items.size
    val openCount: Int get() = items.count { !it.checked }

    /** Lignes regroupées par rayon (rayons nommés par ordre alphabétique, puis les autres). */
    fun byAisle(): List<Pair<String, List<Item>>> {
        val groups = LinkedHashMap<String, MutableList<Item>>()
        for (i in items) groups.getOrPut(i.aisle) { ArrayList() } += i
        return groups.entries.sortedWith(compareBy<Map.Entry<String, MutableList<Item>>> { it.key.isEmpty() }
            .thenBy { TextNorm.normalize(it.key) }).map { it.key to it.value.toList() }
    }

    /** Ajoute les ingrédients de [recipe] (quantités × [factor]) ; renvoie le nombre d'ingrédients ajoutés. */
    fun add(recipe: Recipe, factor: Double = 1.0): Int {
        var n = 0
        for (ing in recipe.allIngredients) {
            add(ing, factor, recipe.title)
            n++
        }
        return n
    }

    fun add(ing: Ingredient, factor: Double, recipeTitle: String) {
        val name = ing.name.trim()
        if (name.isEmpty()) return
        val unit = ing.unit.trim()
        val key = keyOf(ing)
        val parsed = Quantity.parse(ing.qty)
        val amount = parsed?.let { (it.high ?: it.low) * factor }
        val index = items.indexOfFirst { it.key == key }
        if (index < 0) {
            items += Item(key, name, unit, amount, ing.aisle.trim(), listOf(recipeTitle))
            return
        }
        val old = items[index]
        val total = if (old.amount != null && amount != null) old.amount + amount else null
        items[index] = old.copy(
            amount = total,
            aisle = old.aisle.ifEmpty { ing.aisle.trim() },
            recipes = (old.recipes + recipeTitle).distinct(),
            // Un ajout rouvre une ligne déjà cochée : il en faut de nouveau.
            checked = false,
        )
    }

    fun toggle(key: String): Boolean {
        val i = items.indexOfFirst { it.key == key }
        if (i < 0) return false
        items[i] = items[i].copy(checked = !items[i].checked)
        return true
    }

    fun remove(key: String): Boolean = items.removeAll { it.key == key }

    fun clearChecked(): Int {
        val before = items.size
        items.removeAll { it.checked }
        return before - items.size
    }

    fun clear() = items.clear()

    fun toJson(): String = Json.write(linkedMapOf("format" to FORMAT, "items" to items.map {
        linkedMapOf("key" to it.key, "name" to it.name, "unit" to it.unit, "amount" to it.amount,
            "aisle" to it.aisle, "recipes" to it.recipes, "checked" to it.checked)
    }))

    companion object {
        const val FORMAT = "toque-courses-1"

        /** Clé d'une ligne : identifiant du site (ou nom sans accents ni pluriel simple) + unité. */
        fun keyOf(ing: Ingredient): String {
            val what = if (ing.ref > 0) "#${ing.ref}" else TextNorm.normalize(ing.name).split(' ')
                .filter { it.isNotEmpty() }.joinToString(" ") { TextNorm.stem(it) }
            return what + "|" + TextNorm.normalize(ing.unit)
        }

        fun fromJson(text: String): ShoppingList {
            val list = ShoppingList()
            val root = try { Json.parse(text).obj() } catch (_: Exception) { return list }
            for (any in root["items"].list()) {
                val m = any.obj()
                val key = m.str("key") ?: continue
                val name = m.str("name")?.trim().orEmpty()
                if (name.isEmpty()) continue
                list.items += Item(
                    key, name, m.str("unit").orEmpty(), m.double("amount"), m.str("aisle").orEmpty(),
                    m["recipes"].list().mapNotNull { it as? String }, m.bool("checked") ?: false,
                )
            }
            return list
        }
    }
}
