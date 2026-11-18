package app.toque.web

import app.toque.core.i18n.tr
import app.toque.core.i18n.trKey

/** Textes de l'interface web, traduits comme ceux du robot (clé courte → texte français). */
object WebTexts {
    private val KEYS = linkedMapOf(
        "recipes" to trKey("Recettes"),
        "shopping" to trKey("Liste de courses"),
        "search" to trKey("Recette, ingrédient… (ex. : poulet, curry)"),
        "all" to trKey("Toutes"),
        "favorites" to trKey("Favoris"),
        "minutes30" to trKey("30 min max"),
        "kcal500" to trKey("%d kcal max"),
        "sortRelevance" to trKey("Pertinence"),
        "sortPopular" to trKey("Populaires"),
        "sortNewest" to trKey("Nouveautés"),
        "sortRating" to trKey("Mieux notées"),
        "category" to trKey("Catégorie"),
        "isNew" to trKey("Nouveau"),
        "count" to trKey("%d recettes"),
        "more" to trKey("Afficher plus"),
        "none" to trKey("Aucune recette ne correspond.\nAstuce : séparez plusieurs ingrédients par des virgules."),
        "ingredients" to trKey("Ingrédients"),
        "steps" to trKey("Étapes"),
        "servings" to trKey("%d pers."),
        "perServing" to trKey("par portion"),
        "perOf" to trKey("pour 1 sur %d %s"),
        "nutrition" to trKey("%d kcal · glucides %s g · lipides %s g · protéines %s g"),
        "scaled" to trKey("Quantités recalculées. Les temps, pesées et réglages des étapes restent ceux de la recette pour %d : adaptez-les si besoin."),
        "openOnDevice" to trKey("Ouvrir sur le robot"),
        "opened" to trKey("Recette ouverte sur le robot"),
        "busy" to trKey("Le robot est occupé : terminez d'abord le programme en cours"),
        "vendor" to trKey("L'appli d'origine est ouverte sur le robot"),
        "addShopping" to trKey("Sur la liste de courses"),
        "added" to trKey("%d ingrédients ajoutés à la liste de courses"),
        "favorite" to trKey("Favori"),
        "website" to trKey("Sur monsieur-cuisine.com"),
        "weigh" to trKey("Peser %d g"),
        "easy" to trKey("Facile"),
        "medium" to trKey("Moyen"),
        "hard" to trKey("Difficile"),
        "by" to trKey("Par %s"),
        "official" to trKey("Monsieur Cuisine (recette officielle)"),
        "emptyList" to trKey("La liste de courses est vide."),
        "removeChecked" to trKey("Retirer les cochés"),
        "clearAll" to trKey("Tout effacer"),
        "clearConfirm" to trKey("Vider toute la liste de courses ?"),
        "left" to trKey("%s · reste %s"),
        "offline" to trKey("Robot injoignable"),
        "back" to trKey("Revenir"),
        "loading" to trKey("Chargement…"),
        "notes" to trKey("Remarques"),
    )

    fun all(): Map<String, String> = KEYS.mapValues { tr(it.value) }
}
