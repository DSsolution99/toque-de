package app.toque.core.recipes

import app.toque.core.i18n.tr
import app.toque.core.json.Json

/**
 * Recettes personnelles envoyées au robot (par le téléphone ou un assistant comme Claude), au
 * format « toque-recettes-1 » : une recette seule, une liste, ou un fichier { format, recipes }.
 *
 * Le robot ne fait que vérifier et ranger : pas d'analyse de texte libre sur la tablette.
 * Sécurité : chaque réglage machine importé doit être confirmé sur le robot avant de tourner
 * ([Step.needsReview]) ; les réglages hors limites sont signalés et restent bloqués.
 */
object RecipeImport {
    /** Préfixe des recettes personnelles : la synchro Lidl ne les supprime jamais. */
    const val ID_PREFIX = "perso-"
    const val MAX_RECIPES = 50
    private const val MAX_TEXT = 4000
    private const val MAX_SHORT = 200
    private const val MAX_ITEMS = 120

    fun isOwn(id: String): Boolean = id.startsWith(ID_PREFIX)

    data class Result(val recipes: List<Recipe>, val warnings: List<String>, val errors: List<String>)

    /** Le JSON seul, sans le texte ni les balises ``` qu'un assistant met parfois autour. */
    fun extractJson(text: String): String {
        val start = text.indexOfFirst { it == '{' || it == '[' }
        val end = text.indexOfLast { it == '}' || it == ']' }
        return if (start in 0 until end) text.substring(start, end + 1) else text.trim()
    }

    fun parse(text: String): Result {
        val root = try {
            Json.parse(extractJson(text))
        } catch (e: Exception) {
            return Result(emptyList(), emptyList(), listOf(tr("JSON illisible : %s", e.message ?: e.toString())))
        }
        val entries: List<*> = when (root) {
            is List<*> -> root
            is Map<*, *> -> if (root.containsKey("recipes")) root["recipes"] as? List<*> ?: emptyList<Any?>() else listOf(root)
            else -> emptyList<Any?>()
        }
        val errors = ArrayList<String>()
        val warnings = ArrayList<String>()
        if (entries.isEmpty()) errors += tr("Aucune recette trouvée")
        if (entries.size > MAX_RECIPES) errors += tr("%d recettes au plus par envoi", MAX_RECIPES)
        if (errors.isNotEmpty()) return Result(emptyList(), warnings, errors)

        val out = ArrayList<Recipe>()
        for ((i, any) in entries.withIndex()) {
            @Suppress("UNCHECKED_CAST")
            val m = any as? Map<String, Any?>
            val r = try {
                m?.let { RecipeCodec.fromMap(it, RecipeSource.USER) }
            } catch (_: Exception) {
                null
            }
            if (r == null) {
                errors += tr("Recette %d : titre manquant ou format inconnu", i + 1)
                continue
            }
            if (r.ingredients.isEmpty() && r.steps.isEmpty()) {
                errors += tr("« %s » : ni ingrédients ni étapes", r.title)
                continue
            }
            val clean = sanitize(r)
            for ((n, s) in clean.steps.withIndex()) {
                val v = s.machine?.violations().orEmpty()
                if (v.isNotEmpty()) warnings += tr("« %s », étape %d : %s", clean.title, n + 1, v.joinToString(" ; ") { it.message })
            }
            // Deux recettes du même envoi avec le même titre : la seconde garde son propre identifiant.
            var id = clean.id
            var k = 2
            while (out.any { it.id == id }) id = clean.id.take(56) + "-" + k++
            out += clean.copy(id = id)
        }
        return Result(out, warnings, errors)
    }

    /** Recette prête à ranger : identifiant personnel, textes bornés, réglages machine à confirmer. */
    fun sanitize(r: Recipe): Recipe {
        val base = RecipeCodec.slug(r.id.removePrefix(ID_PREFIX).ifBlank { r.title })
        return r.copy(
            id = ID_PREFIX + base,
            title = r.title.trim().take(MAX_SHORT),
            category = r.category.trim().take(MAX_SHORT),
            tags = r.tags.take(20).map { it.take(60) },
            servings = r.servings.coerceIn(0, 200),
            prepMinutes = r.prepMinutes.coerceIn(0, 6000),
            totalMinutes = r.totalMinutes.coerceIn(0, 6000),
            source = RecipeSource.USER,
            // Les photos viennent du cache du robot : pas de téléchargement pour une recette perso.
            image = "",
            favorite = false,
            notes = r.notes.take(MAX_TEXT),
            author = r.author.take(MAX_SHORT),
            rating = 0.0,
            ratingCount = 0,
            categories = r.categories.take(10).map { it.take(60) },
            servingUnit = r.servingUnit.take(40),
            popularity = 0.0,
            url = r.url.takeIf { it.startsWith("https://") && it.length <= 500 }.orEmpty(),
            ingredients = r.ingredients.take(20).map { g ->
                g.copy(title = g.title.take(MAX_SHORT), items = g.items.take(MAX_ITEMS).map {
                    it.copy(qty = it.qty.take(20), unit = it.unit.take(30), name = it.name.take(MAX_SHORT),
                        note = it.note.take(MAX_SHORT), ref = 0, aisle = it.aisle.take(60))
                })
            },
            steps = r.steps.take(MAX_ITEMS).map { s ->
                s.copy(text = s.text.take(MAX_TEXT), needsReview = s.needsReview || s.machine != null)
            },
        )
    }
}
