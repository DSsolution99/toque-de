package app.toque.core

import app.toque.core.recipes.Ingredient
import app.toque.core.recipes.IngredientGroup
import app.toque.core.recipes.Nutrition
import app.toque.core.recipes.Recipe
import app.toque.core.recipes.RecipeFilter
import app.toque.core.recipes.RecipeSort
import app.toque.core.recipes.SearchIndex
import app.toque.core.recipes.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Katalog in realistischer Größe (3.000 Rezepte mit Zutaten und Schritten, großer Wortschatz).
 * Die Grenzen gelten für den GitHub-Rechner; das Tablet im Gerät ist etwa 10- bis 20-mal langsamer.
 * Die gemessenen Zeiten stehen als „PERF“ im Testbericht.
 */
class PerformanceTest {
    private val rnd = Random(42)
    private val syllables = listOf("ka", "tof", "fel", "sup", "pe", "nu", "del", "to", "ma", "ten", "hähn", "chen", "cur", "ry",
        "lin", "sen", "zwie", "bel", "kno", "blauch", "pa", "pri", "ka", "rei", "spi", "nat", "brok", "ko", "li", "kür", "bis",
        "schnit", "zel", "gu", "lasch", "auf", "lauf", "sa", "lat", "ku", "chen", "mehl", "zu", "cker", "but", "ter", "sah", "ne")

    private fun word(): String {
        val n = 2 + rnd.nextInt(3)
        return (0 until n).joinToString("") { syllables[rnd.nextInt(syllables.size)] }
    }

    private fun sentence(words: Int) = (0 until words).joinToString(" ") { word() }

    private val cats = listOf("Hauptgerichte", "Vegetarisch", "Vegan", "Backen", "Desserts", "Suppen & Eintöpfe", "Snacks", "Beilagen")

    private fun catalog(n: Int) = (0 until n).map { i ->
        Recipe(
            id = "web-$i", title = sentence(2 + rnd.nextInt(3)).replaceFirstChar { it.uppercaseChar() },
            category = cats[i % cats.size], categories = listOf(cats[i % cats.size], cats[(i * 7) % cats.size]),
            tags = listOf(word(), word()), totalMinutes = 10 + rnd.nextInt(120),
            nutrition = Nutrition(100 + rnd.nextInt(900)), servings = 4, servingUnit = "Portionen",
            popularity = rnd.nextDouble() * 10, published = "2026-0${1 + rnd.nextInt(9)}-1${rnd.nextInt(9)}",
            rating = 3 + rnd.nextDouble() * 2, ratingCount = rnd.nextInt(2000),
            ingredients = listOf(IngredientGroup("", (0 until 10).map { Ingredient("${10 * (1 + rnd.nextInt(30))}", "g", word()) })),
            steps = (0 until 12).map { Step(sentence(10 + rnd.nextInt(10))) },
        )
    }

    private fun ms(block: () -> Unit): Double {
        val t0 = System.nanoTime()
        block()
        return (System.nanoTime() - t0) / 1e6
    }

    @Test fun realisticCatalog() {
        val recipes = catalog(3000)
        // Aufwärmen der JVM (auf dem Gerät gibt es kein JIT-Aufwärmen in diesem Sinn, aber die
        // erste Messung auf dem Testrechner wäre sonst verfälscht).
        SearchIndex(recipes.take(300))

        var index = SearchIndex(emptyList())
        val build = ms { index = SearchIndex.Builder().apply { recipes.forEach { add(it) } }.build() }
        val probe = recipes[1234].title
        val queries = listOf(probe, probe.lowercase().dropLast(1), "kartofel", "nudel tomaten", "sup", "hähnchen curry")
        val search = queries.map { q -> ms { repeat(20) { index.search(q) } } / 20 }
        val filtered = ms { repeat(20) { index.search("", RecipeFilter(category = "Vegan", maxKcal = 400, sort = RecipeSort.LIGHT)) } } / 20
        val sorted = ms { repeat(20) { index.search("", RecipeFilter(sort = RecipeSort.POPULAR)) } } / 20
        val ingredients = ms { repeat(20) { index.withIngredients(listOf("kartofel", "zwiebel")) } } / 20
        var favs = index
        val fav = ms { repeat(20) { i -> favs = favs.withFavorites(setOf("web-$i")) } } / 20

        println("PERF Aufbau 3000 Rezepte: ${"%.0f".format(build)} ms")
        println("PERF Suche je Anfrage: ${search.joinToString(" / ") { "%.1f".format(it) }} ms")
        println("PERF Filter Vegan+400 kcal+Wenigste kcal: ${"%.1f".format(filtered)} ms; Sortierung Beliebt: ${"%.1f".format(sorted)} ms")
        println("PERF Zutatensuche: ${"%.1f".format(ingredients)} ms; Favorit umschalten: ${"%.2f".format(fav)} ms")

        // Treffer stimmen noch.
        assertEquals("web-1234", index.search(probe).first().entry.id)
        assertTrue(favs.entries.single { it.id == "web-19" }.favorite)
        assertEquals(1, favs.entries.count { it.favorite })
        // Großzügige Grenzen für den Testrechner.
        assertTrue("Aufbau zu langsam: $build ms", build < 5000)
        assertTrue("Suche zu langsam: $search", search.all { it < 60 })
        assertTrue("Filter zu langsam: $filtered", filtered < 30)
        assertTrue("Favorit zu langsam: $fav", fav < 20)
    }
}
