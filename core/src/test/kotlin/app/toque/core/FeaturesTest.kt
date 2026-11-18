package app.toque.core

import app.toque.core.recipes.Ingredient
import app.toque.core.recipes.IngredientGroup
import app.toque.core.recipes.McOnline
import app.toque.core.recipes.Nutrition
import app.toque.core.recipes.Quantity
import app.toque.core.recipes.Recipe
import app.toque.core.recipes.RecipeCodec
import app.toque.core.recipes.RecipeDates
import app.toque.core.recipes.RecipeFilter
import app.toque.core.recipes.RecipeSort
import app.toque.core.recipes.SearchIndex
import app.toque.core.recipes.ShoppingList
import app.toque.core.recipes.WebViews
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nährwerte, Kategorien, Sortierung, Portionen und Einkaufsliste. */
class FeaturesTest {

    // ------------------------------------------------------------------ Mengen

    @Test fun quantitiesAreReadInEveryUsualForm() {
        assertEquals(100.0, Quantity.parse("100")!!.low, 1e-9)
        assertEquals(0.5, Quantity.parse("0.5")!!.low, 1e-9)
        assertEquals(1.5, Quantity.parse("1,5")!!.low, 1e-9)
        assertEquals(0.5, Quantity.parse("1/2")!!.low, 1e-9)
        assertEquals(1.5, Quantity.parse("1 ½")!!.low, 1e-9)
        assertEquals(1.5, Quantity.parse("1½")!!.low, 1e-9)
        with(Quantity.parse("2-3")!!) { assertEquals(2.0, low, 1e-9); assertEquals(3.0, high!!, 1e-9) }
        assertNull(Quantity.parse("etwas"))
        assertNull(Quantity.parse(""))
    }

    @Test fun scaledQuantitiesStayReadable() {
        assertEquals("200", Quantity.scale("100", 2.0))
        assertEquals("½", Quantity.scale("1", 0.5))
        assertEquals("1½", Quantity.scale("1", 1.5))
        assertEquals("⅔", Quantity.scale("1", 2.0 / 3))
        assertEquals("4–6", Quantity.scale("2-3", 2.0))
        assertEquals("13", Quantity.scale("10", 4.0 / 3))
        assertEquals("etwas", Quantity.scale("etwas", 2.0))
        assertEquals("100", Quantity.scale("100", 1.0))
        assertEquals("2 g Salz", Ingredient("1", "g", "Salz").scaled(2.0).display)
    }

    // ------------------------------------------------------------------ Lidl-Daten

    @Test fun nutritionCategoriesAndIdsAreRead() {
        val json = """{"data":{"recipe":{"id":12,"name":"Nussplätzchen","popularity":3.1465,
            "publishedDate":"2021-11-13 12:36:48","url":"https://www.monsieur-cuisine.com/de/recipe/nussplaetzchen",
            "categories":[{"name":"Backen"},{"name":"Vegetarisch"}],
            "nutrients":[{"name":"Calories","amountFloat":49,"unit":"kcal"},{"name":"Carbohydrate","amountFloat":4.5,"unit":"g"},
                         {"name":"Fat","amountFloat":3,"unit":"g"},{"name":"Protein","amountFloat":1.2,"unit":"g"}],
            "servingSizes":[{"id":1,"amount":80,"servingUnit":"Stück",
              "ingredients":[{"name":"Haselnusskerne","amount":"100","unit":"g","systemIngredientId":21372,
                              "ingredientCategory":{"name":"Nüsse"}}],
              "steps":[{"order":0,"description":"Backen."}]}]}}}"""
        val r = McOnline.parseRecipe(json)!!
        assertEquals(Nutrition(49, 4.5, 3.0, 1.2), r.nutrition)
        assertEquals(listOf("Backen", "Vegetarisch"), r.categories)
        assertEquals("Stück", r.servingUnit)
        assertEquals(3.1465, r.popularity, 1e-9)
        assertEquals("2021-11-13", r.published)
        assertEquals("https://www.monsieur-cuisine.com/de/recipe/nussplaetzchen", r.url)
        with(r.allIngredients.single()) { assertEquals(21372L, ref); assertEquals("Nüsse", aisle) }
        // Alles bleibt beim Speichern erhalten.
        val back = RecipeCodec.decode(RecipeCodec.encode(r))!!
        assertEquals(r.nutrition, back.nutrition)
        assertEquals(r.categories, back.categories)
        assertEquals(r.servingUnit, back.servingUnit)
        assertEquals(r.published, back.published)
        assertEquals(r.url, back.url)
        assertEquals(r.popularity, back.popularity, 1e-9)
        assertEquals(r.allIngredients, back.allIngredients)
        // Ohne Kalorien keine Nährwerte.
        assertNull(McOnline.parseRecipe("""{"data":{"recipe":{"id":13,"name":"X","nutrients":[]}}}""")!!.nutrition)
    }

    // ------------------------------------------------------------------ Filter und Sortierung

    private fun recipe(id: String, title: String, vararg cats: String, kcal: Int = 0, pop: Double = 0.0,
                       published: String = "", rating: Double = 0.0, ratings: Int = 0) = Recipe(
        id = id, title = title, category = cats.firstOrNull().orEmpty(), categories = cats.toList(),
        nutrition = if (kcal > 0) Nutrition(kcal) else null, popularity = pop, published = published,
        rating = rating, ratingCount = ratings,
    )

    private val index = SearchIndex(listOf(
        recipe("a", "Linsensuppe", "Suppen & Eintöpfe", "Vegan", kcal = 320, pop = 2.0, published = "2026-11-09", rating = 4.0, ratings = 50),
        recipe("b", "Gulasch", "Hauptgerichte", kcal = 650, pop = 9.0, published = "2024-01-01", rating = 4.8, ratings = 900),
        recipe("c", "Gemüsecurry", "Hauptgerichte", "Vegetarisch", "Vegan", kcal = 480, pop = 5.0, published = "2025-06-01", rating = 5.0, ratings = 2),
        recipe("d", "Brot", "Backen", pop = 1.0, published = "2023-03-03"),
    ))

    @Test fun categoryFilterLooksAtEveryCategory() {
        assertEquals(setOf("a", "c"), index.search("", RecipeFilter(category = "Vegan")).map { it.entry.id }.toSet())
        assertEquals(listOf("c"), index.search("", RecipeFilter(category = "Vegetarisch")).map { it.entry.id })
        // Die häufigsten Kategorien zuerst.
        assertEquals(listOf("Hauptgerichte", "Vegan"), index.categories.take(2))
    }

    @Test fun kcalFilterKeepsOnlyKnownValuesUnderTheLimit() {
        assertEquals(setOf("a", "c"), index.search("", RecipeFilter(maxKcal = 500)).map { it.entry.id }.toSet())
    }

    @Test fun kcalCountsOnlyRealPortions() {
        val cookies = recipe("e", "Nussplätzchen", "Backen", kcal = 49).copy(servings = 80, servingUnit = "Stück")
        val soup = recipe("f", "Kürbissuppe", "Suppen & Eintöpfe", kcal = 210).copy(servings = 4, servingUnit = "Portionen")
        val idx = SearchIndex(listOf(cookies, soup))
        // 49 kcal pro Plätzchen ist keine Mahlzeit: weder Filter noch Sortierung zählen es.
        assertEquals(listOf("f"), idx.search("", RecipeFilter(maxKcal = 300)).map { it.entry.id })
        assertEquals(listOf("f", "e"), idx.search("", RecipeFilter(sort = RecipeSort.LIGHT)).map { it.entry.id })
        assertTrue(RecipeFilter.isPortionUnit("Portionen"))
        assertTrue(RecipeFilter.isPortionUnit(""))
        assertFalse(RecipeFilter.isPortionUnit("Gläser"))
        assertEquals(listOf(200, 300, 400, 500, 600, 800), RecipeFilter.KCAL_LEVELS)
    }

    @Test fun sortOrders() {
        assertEquals(listOf("b", "c", "a", "d"), index.search("", RecipeFilter(sort = RecipeSort.POPULAR)).map { it.entry.id })
        assertEquals(listOf("a", "c", "b", "d"), index.search("", RecipeFilter(sort = RecipeSort.NEWEST)).map { it.entry.id })
        // Mit nur 2 Bewertungen zählt die 5,0 nicht vor 4,8 bei 900 Bewertungen.
        assertEquals("b", index.search("", RecipeFilter(sort = RecipeSort.RATING)).first().entry.id)
        // Sortierung auch mit Suchbegriff.
        assertEquals(listOf("b", "c"), index.search("", RecipeFilter(category = "Hauptgerichte", sort = RecipeSort.POPULAR)).map { it.entry.id })
    }

    @Test fun newRecipes() {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse("2026-11-18")!!.time
        assertTrue(RecipeDates.isNew("2026-11-09", now))
        assertTrue(RecipeDates.isNew("2026-11-09 08:00:00", now))
        assertFalse(RecipeDates.isNew("2026-10-01", now))
        assertFalse(RecipeDates.isNew("", now))
    }

    // ------------------------------------------------------------------ Einkaufsliste

    private fun withIngredients(title: String, vararg items: Ingredient) =
        Recipe(id = title, title = title, ingredients = listOf(IngredientGroup("", items.toList())))

    @Test fun shoppingListAddsUpTheSameIngredient() {
        val list = ShoppingList()
        list.add(withIngredients("Suppe", Ingredient("1", "", "Zwiebel", ref = 7, aisle = "Gemüse"),
            Ingredient("200", "g", "Karotten", ref = 8, aisle = "Gemüse"), Ingredient("", "", "Salz")))
        list.add(withIngredients("Curry", Ingredient("2", "", "Zwiebeln", ref = 7, aisle = "Gemüse"),
            Ingredient("1", "kg", "Karotten", ref = 8), Ingredient("", "", "Salz")), 2.0)
        val items = list.all.associateBy { it.key }
        // Gleiche ID und Einheit: zusammengezählt (1 + 2×2), Name der ersten Zeile.
        with(items.getValue(ShoppingList.keyOf(Ingredient("", "", "x", ref = 7)))) {
            assertEquals(5.0, amount!!, 1e-9); assertEquals("5 Zwiebel", display); assertEquals(listOf("Suppe", "Curry"), recipes)
        }
        // Andere Einheit: eigene Zeile.
        assertEquals(2, list.all.count { it.name == "Karotten" })
        // Ohne Menge: eine Zeile, ohne Summe.
        with(list.all.single { it.name == "Salz" }) { assertNull(amount); assertEquals("Salz", display) }
        // Nach Warengruppe, „ohne Gruppe“ zuletzt.
        assertEquals(listOf("Gemüse", ""), list.byAisle().map { it.first })
    }

    @Test fun shoppingListChecksAndSurvivesSaving() {
        val list = ShoppingList()
        list.add(withIngredients("Suppe", Ingredient("1", "", "Zwiebel", ref = 7), Ingredient("2", "EL", "Öl")))
        val key = list.all.first().key
        assertTrue(list.toggle(key))
        assertEquals(1, list.openCount)
        val back = ShoppingList.fromJson(list.toJson())
        assertEquals(list.all, back.all)
        // Nochmal hinzufügen öffnet die abgehakte Zeile wieder.
        list.add(withIngredients("Suppe", Ingredient("1", "", "Zwiebel", ref = 7)))
        assertEquals(2, list.openCount)
        list.toggle(key)
        assertEquals(1, list.clearChecked())
        assertEquals(1, list.size)
        assertEquals(0, ShoppingList.fromJson("kaputt").size)
    }

    // ------------------------------------------------------------------ Handy

    @Test fun phoneGetsScaledRecipeAndLocalImages() {
        val r = Recipe(id = "web-1", title = "Suppe", servings = 4, servingUnit = "Portionen",
            image = "https://cdn.example.test/uploads/2021/suppe.jpg?w=1",
            ingredients = listOf(IngredientGroup("", listOf(Ingredient("200", "g", "Linsen"), Ingredient("", "", "Salz")))))
        val d = WebViews.detail(r, 8, favorite = true) { true }
        assertEquals(8, d["wanted"]); assertEquals(true, d["scaled"]); assertEquals(true, d["favorite"])
        @Suppress("UNCHECKED_CAST")
        val items = (d["ingredients"] as List<Map<String, Any?>>).single()["items"] as List<String>
        assertEquals(listOf("400 g Linsen", "Salz"), items)
        assertEquals("/img/suppe.jpg", d["image"])
        // Ohne Foto im Speicher kein Bild-Link; ohne Portionsangabe keine Umrechnung.
        assertEquals("", WebViews.detail(r, 0, false) { false }["image"])
        assertEquals(false, WebViews.detail(r.copy(servings = 0), 8, false) { true }["scaled"])
        assertEquals("/img/a%20b.jpg", WebViews.imagePath("https://x.test/a b.jpg") { true })
    }

    @Test fun phoneShoppingList() {
        val list = ShoppingList()
        list.add(withIngredients("Suppe", Ingredient("1", "", "Zwiebel", ref = 7, aisle = "Gemüse")))
        val v = WebViews.shopping(list, 3)
        assertEquals(3L, v["version"]); assertEquals(1, v["open"])
        @Suppress("UNCHECKED_CAST")
        val group = (v["groups"] as List<Map<String, Any?>>).single()
        assertEquals("Gemüse", group["aisle"])
    }
}
