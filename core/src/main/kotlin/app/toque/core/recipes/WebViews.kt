package app.toque.core.recipes

import app.toque.core.i18n.tr
import java.net.URLEncoder

/**
 * Données envoyées à l'interface web du téléphone (objets prêts pour [app.toque.core.json.Json.write]).
 * Les images passent par le robot (« /img/<fichier> ») : le téléphone n'a pas besoin d'Internet.
 */
object WebViews {
    /** Chemin web d'une image de recette, vide si elle n'est pas en cache ([hasImage]). */
    fun imagePath(url: String, hasImage: (String) -> Boolean): String {
        if (url.isBlank() || !hasImage(url)) return ""
        val name = url.substringAfterLast('/').substringBefore('?')
        return "/img/" + URLEncoder.encode(name, "UTF-8").replace("+", "%20")
    }

    fun summary(e: SearchIndex.Entry, favorite: Boolean, hasImage: (String) -> Boolean): Map<String, Any?> = linkedMapOf(
        "id" to e.id,
        "title" to e.title,
        "image" to imagePath(e.image, hasImage),
        "minutes" to e.totalMinutes,
        "kcal" to e.kcal,
        "rating" to e.rating,
        "ratingCount" to e.ratingCount,
        "category" to e.category,
        "isNew" to RecipeDates.isNew(e.published),
        "favorite" to favorite,
        "own" to (e.source == RecipeSource.USER),
    )

    /** Fiche complète pour [servings] portions (0 : celles de la recette). */
    fun detail(r: Recipe, servings: Int, favorite: Boolean, hasImage: (String) -> Boolean): Map<String, Any?> {
        val wanted = if (servings > 0 && r.servings > 0) servings else r.servings
        val factor = if (r.servings > 0 && wanted > 0) wanted.toDouble() / r.servings else 1.0
        return linkedMapOf(
            "id" to r.id,
            "title" to r.title,
            "image" to imagePath(r.image, hasImage),
            "minutes" to r.totalMinutes,
            "prepMinutes" to r.prepMinutes,
            "difficulty" to r.difficulty,
            "author" to r.author,
            "rating" to r.rating,
            "ratingCount" to r.ratingCount,
            "categories" to r.allCategories,
            "servings" to r.servings,
            "wanted" to wanted,
            "servingUnit" to r.servingUnit,
            "scaled" to (factor != 1.0),
            "nutrition" to r.nutrition?.let {
                linkedMapOf("kcal" to it.kcal, "carbsG" to it.carbsG, "fatG" to it.fatG, "proteinG" to it.proteinG)
            },
            "url" to r.url,
            "favorite" to favorite,
            "own" to (r.source == RecipeSource.USER),
            "notes" to r.notes,
            "ingredients" to r.ingredients.map { g ->
                linkedMapOf("group" to g.title, "items" to g.items.map { it.scaled(factor).display })
            },
            "steps" to r.steps.map { s ->
                linkedMapOf(
                    "text" to s.text,
                    "machine" to s.machine?.summary,
                    "valid" to (s.machine?.valid ?: true),
                    "weigh" to s.weigh?.grams,
                    "review" to (s.machine != null && s.needsReview),
                )
            },
        )
    }

    fun shopping(list: ShoppingList, version: Long): Map<String, Any?> = linkedMapOf(
        "version" to version,
        "open" to list.openCount,
        "groups" to list.byAisle().map { (aisle, items) ->
            linkedMapOf(
                "aisle" to aisle.ifEmpty { tr("Autres") },
                "items" to items.map {
                    linkedMapOf("key" to it.key, "text" to it.display, "recipes" to it.recipes, "checked" to it.checked)
                },
            )
        },
    )
}
