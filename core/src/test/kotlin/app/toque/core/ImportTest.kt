package app.toque.core

import app.toque.core.recipes.RecipeImport
import app.toque.core.recipes.RecipeSource
import app.toque.core.recipes.SearchIndex
import app.toque.core.recipes.RecipeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Eigene Rezepte, die per Weboberfläche (Handy oder Claude) ankommen. */
class ImportTest {
    private val soup = """
        {"title": "Kürbissuppe", "servings": 4, "totalMinutes": 35, "category": "Suppen",
         "nutrition": {"kcal": 210},
         "ingredients": [{"group": "", "items": [{"qty": "800", "unit": "g", "name": "Hokkaido-Kürbis"}, "Salz"]}],
         "steps": [
           {"text": "Kürbis zerkleinern.", "machine": {"program": "manuel", "seconds": 5, "speed": 5}},
           {"text": "Garen.", "machine": {"program": "manuel", "seconds": 1200, "speed": 1, "tempC": 100}},
           {"text": "Abschmecken."}
         ]}
    """.trimIndent()

    @Test fun singleRecipeBecomesOwnRecipeWithReviewedSteps() {
        val res = RecipeImport.parse(soup)
        assertTrue(res.errors.toString(), res.errors.isEmpty())
        val r = res.recipes.single()
        assertTrue(r.id, RecipeImport.isOwn(r.id))
        assertEquals(RecipeSource.USER, r.source)
        assertEquals(2, r.allIngredients.size)
        assertEquals(210, r.nutrition!!.kcal)
        // Jeder Geräte-Schritt muss am Gerät bestätigt werden, reine Text-Schritte nicht.
        assertTrue(r.steps[0].needsReview)
        assertTrue(r.steps[1].needsReview)
        assertFalse(r.steps[2].needsReview)
        assertTrue(res.warnings.isEmpty())
    }

    @Test fun listsAndFilesAreAcceptedAndIdsStayUnique() {
        assertEquals(2, RecipeImport.parse("[$soup, $soup]").recipes.map { it.id }.toSet().size)
        assertEquals(1, RecipeImport.parse("""{"format": "toque-recettes-1", "recipes": [$soup]}""").recipes.size)
    }

    @Test fun ownIdsCannotOverwriteLidlRecipes() {
        val r = RecipeImport.parse("""{"id": "web-12345", "title": "X", "steps": [{"text": "a"}]}""").recipes.single()
        assertTrue(r.id.startsWith("perso-"))
        val again = RecipeImport.parse("""{"id": "${r.id}", "title": "X", "steps": [{"text": "a"}]}""").recipes.single()
        assertEquals(r.id, again.id)
    }

    @Test fun unsafeSettingsAreReportedAndImagesAndHttpLinksDropped() {
        val res = RecipeImport.parse("""{"title": "Sahne", "image": "http://x/y.jpg", "url": "http://x",
            "steps": [{"text": "Schlagen", "machine": {"program": "manuel", "seconds": 60, "speed": 8, "accessory": "whisk"}}]}""")
        val r = res.recipes.single()
        assertEquals(1, res.warnings.size)
        assertFalse(r.steps[0].machine!!.valid)
        assertTrue(r.steps[0].needsReview)
        assertEquals("", r.image)
        assertEquals("", r.url)
    }

    @Test fun brokenInputGivesErrorsNotCrashes() {
        assertFalse(RecipeImport.parse("kein json").errors.isEmpty())
        assertFalse(RecipeImport.parse("""{"steps": []}""").errors.isEmpty())
        assertFalse(RecipeImport.parse("""{"title": "Leer"}""").errors.isEmpty())
        assertFalse(RecipeImport.parse("[]").errors.isEmpty())
        val many = (1..51).joinToString(",", "[", "]") { """{"title": "R$it", "steps": [{"text": "a"}]}""" }
        assertFalse(RecipeImport.parse(many).errors.isEmpty())
    }

    @Test fun ownRecipesCanBeFiltered() {
        val own = RecipeImport.parse(soup).recipes
        val index = SearchIndex(own)
        assertEquals(1, index.search("", RecipeFilter(source = RecipeSource.USER)).size)
        assertEquals(0, index.search("", RecipeFilter(source = RecipeSource.IMPORTED)).size)
    }

    @Test fun assistantAnswersWithFencesAreAccepted() {
        val answer = "Hier ist dein Rezept:\n```json\n$soup\n```\nGuten Appetit!"
        assertEquals(1, RecipeImport.parse(answer).recipes.size)
    }

    /** Das Beispiel in der Anleitung für Claude muss selbst ein gültiges Rezept ohne Warnungen sein. */
    @Test fun promptExampleIsAValidRecipe() {
        val prompt = File("../app/src/main/assets/web/prompt.txt").readText()
        val example = prompt.substring(prompt.indexOf("\n{") + 1, prompt.indexOf("\n}\n") + 2)
            .replace("\"https://… (source page, https only, optional)\"", "\"\"")
        val res = RecipeImport.parse(example)
        assertTrue(res.errors.toString(), res.errors.isEmpty())
        assertTrue(res.warnings.toString(), res.warnings.isEmpty())
        val r = res.recipes.single()
        assertEquals(5, r.steps.size)
        assertEquals(800, r.steps[2].weigh!!.grams)
    }
}
