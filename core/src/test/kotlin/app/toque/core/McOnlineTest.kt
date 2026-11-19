package app.toque.core

import app.toque.core.recipes.McOnline
import app.toque.core.recipes.RecipeCodec
import app.toque.core.recipes.StepProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Conversion des recettes de l'API monsieur-cuisine.com (données d'essai inventées, même structure). */
class McOnlineTest {
    private fun fixture(name: String) = javaClass.getResourceAsStream("/fixtures/$name")!!.bufferedReader().use { it.readText() }

    @Test fun page() {
        val p = McOnline.parsePage(fixture("mc-online-page.json"))
        assertEquals(3, p.total)
        assertEquals(1, p.totalPages)
        assertEquals(listOf(900001L, 900002L, 900003L), p.entries.map { it.apiId })
        assertEquals("2026-04-19 09:00:02", p.entries[0].updated)
        assertEquals("", p.entries[2].updated)
        // Auteur et note, aussi dans la liste (pour garder les notes à jour sans tout retélécharger).
        assertEquals("Chef d'essai", p.entries[0].author)
        assertEquals(4.4, p.entries[0].rating, 0.001)
        assertEquals(500, p.entries[0].ratingCount)
        assertEquals(3.5, p.entries[1].rating, 0.001)
        assertEquals(0, p.entries[2].ratingCount)
        assertEquals("2026-04-19 09:00:02|4.40|500|Chef d'essai|0.00", p.entries[0].stamp)
        assertEquals("2026-04-19 09:00:02", McOnline.updatedOf(p.entries[0].stamp))
        assertEquals("2025-01-02 10:00:00", McOnline.updatedOf("2025-01-02 10:00:00"))
    }

    @Test fun recipe() {
        val r = McOnline.parseRecipe(fixture("mc-online-recipe.json"))!!
        assertEquals("web-900001", r.id)
        assertEquals("Soupe d'essai", r.title)
        assertEquals("Soupes", r.category)
        assertEquals(listOf("Végétarien"), r.tags)
        assertEquals(4, r.servings)
        assertEquals(15, r.prepMinutes)
        assertEquals(40, r.totalMinutes)
        assertEquals(2, r.difficulty)
        assertEquals("https://cdn.example.test/uploads/2021/11/123essai.jpg", r.image)
        assertEquals("Astuce : servir chaud.", r.notes)
        assertEquals("Chef d'essai", r.author)
        assertEquals(4.4, r.rating, 0.001)
        assertEquals(500, r.ratingCount)
        // Le format de stockage garde l'auteur et la note.
        val back = RecipeCodec.decode(RecipeCodec.encode(r))!!
        assertEquals(r.author, back.author)
        assertEquals(r.rating, back.rating, 0.001)
        assertEquals(r.ratingCount, back.ratingCount)

        // Ingrédients : sans groupe d'abord (dans l'ordre), puis les groupes ; sans nom = ignoré.
        assertEquals(listOf("", "POUR LA GARNITURE"), r.ingredients.map { it.title })
        assertEquals(listOf("1 oignon", "200 g carottes"), r.ingredients[0].items.map { it.display })
        assertEquals("10 g persil", r.ingredients[1].items.single().display)

        val s = r.steps
        assertEquals(9, s.size)
        // Le réglage « Mode » est rattaché à l'étape qui décrit le geste.
        assertEquals("Mettre l'oignon dans le bol mixeur.", s[0].text)
        assertEquals(StepProgram.MANUAL, s[0].machine!!.program)
        assertEquals(5, s[0].machine!!.seconds)
        assertEquals(5, s[0].machine!!.speed)
        assertTrue(s[0].needsReview)
        // Balance.
        assertEquals(200, s[1].weigh!!.grams)
        assertNull(s[1].machine)
        // Réglage après une pesée : étape à part, avec chauffe et sens inverse.
        with(s[2].machine!!) {
            assertEquals(100, tempC); assertEquals(1, speed); assertEquals(1200, seconds); assertTrue(reverse)
        }
        assertEquals(StepProgram.TURBO, s[3].machine!!.program)
        assertEquals(2, s[3].machine!!.pulses)
        assertEquals(StepProgram.STEAM, s[4].machine!!.program)
        assertEquals(600, s[4].machine!!.seconds)
        assertEquals(StepProgram.TOAST, s[5].machine!!.program)
        assertEquals(StepProgram.KNEAD, s[6].machine!!.program)
        // Programme absent du Monsieur Cuisine connect : étape manuelle, signalée.
        assertNull(s[7].machine)
        assertTrue(s[7].text.startsWith("Cuire sous vide.\n"))
        assertEquals("Servir.", s[8].text)

        // Tous les réglages importés passent les règles de sécurité.
        for (step in s) step.machine?.let { assertTrue("${it.summary} : ${it.violations()}", it.valid) }
    }

    @Test fun reviews() {
        val p = McOnline.parseReviews(fixture("mc-online-reviews.json"))
        assertEquals(13, p.total)
        assertEquals(5, p.totalPages)
        // Les avis signalés ne sont pas affichés.
        assertEquals(2, p.reviews.size)
        with(p.reviews[0]) {
            assertEquals("Cuisinier d'essai", author); assertEquals(5, rating); assertEquals("2025-12-02", date)
            assertEquals("Très bonne recette, toute la famille a aimé.", message); assertEquals("", reply)
        }
        assertEquals("Bonjour,\nMerci, nous corrigeons la recette.\n\nMeilleures salutations", p.reviews[1].reply)
        assertEquals(900001L, McOnline.apiIdOf("web-900001"))
        assertNull(McOnline.apiIdOf("mc-26946"))
        assertTrue(McOnline.reviewsUrl(900001, 2).endsWith("/api/v1/recipe-reviews/recipes/900001/reviews?page=2&size=10"))
        assertEquals(36, McOnline.reviewHeaders("fr-FR")["X-Request-Id"]!!.length)
        val updated = McOnline.withScore(McOnline.parseRecipe(fixture("mc-online-recipe.json"))!!, McOnline.Entry(900001, "", "", "", 4.6, 510))
        assertEquals(4.6, updated.rating, 0.001); assertEquals(510, updated.ratingCount); assertEquals("Chef d'essai", updated.author)
    }

    @Test fun unusable() {
        assertNull(McOnline.parseRecipe("""{"data":{"recipe":{"id":1,"name":"  "}}}"""))
        assertNull(McOnline.parseRecipe("""{"code":404,"message":"Recipe not found"}"""))
        assertNotNull(McOnline.parseRecipe("""{"data":{"recipe":{"id":"2","name":"Vide"}}}"""))
    }

    @Test fun headersAndUrls() {
        assertEquals("web", McOnline.headers("fr-FR")["device-type"])
        assertEquals("fr-FR", McOnline.languageFor("fr"))
        assertEquals("en-US", McOnline.languageFor("en"))
        assertTrue(McOnline.listUrl(2).contains("/page/2/size/100?"))
        assertTrue(McOnline.listUrl(1).contains("MC2.0"))
    }

    @Test fun whiskInGermanAndFrenchRecipes() {
        assertTrue(McOnline.mentionsWhisk("Den Rühraufsatz in den Mixbehälter einsetzen."))
        assertTrue(McOnline.mentionsWhisk("Insérer le fouet."))
        assertTrue(McOnline.mentionsWhisk("Insert the whisk."))
        assertTrue(!McOnline.mentionsWhisk("Die Zwiebeln in den Mixbehälter geben."))
    }

    private fun entry(id: Long, name: String, source: String = "official", vararg devices: String, ratings: Int = 0) =
        McOnline.Entry(id, "", name, ratingCount = ratings, source = source, devices = devices.toList())

    @Test fun listAsksForEveryModelAndSource() {
        val url = McOnline.listUrl(1)
        for (d in McOnline.DEVICES) assertTrue(url.contains("filters%5BdeviceTypes%5D%5B%5D=$d"))
        assertTrue(!url.contains("source"))
    }

    @Test fun sameTitleWithWordsInAnotherOrder() {
        assertEquals(McOnline.titleKey("Gefüllte Paprika mit Djuvec-Reis"), McOnline.titleKey("Paprika mit Djuvec-Reis, gefüllte"))
        assertEquals(McOnline.titleKey("Möhrenbrei, der allererste"), McOnline.titleKey("Der allererste Möhrenbrei "))
        assertTrue(McOnline.titleKey("Kuchen mit Zucker") != McOnline.titleKey("Kuchen ohne Zucker"))
    }

    @Test fun oneRecipePerTitle() {
        val list = listOf(
            entry(1, "Datteldip mit Curry", "official", "MC1.1", ratings = 28),
            entry(2, "Datteldip mit Curry", "official", "MC0.5", ratings = 138),
            entry(3, "Datteldip mit Curry", "official", "MC2.0", "MC3.0", ratings = 18724),
            entry(4, "Eierlikör", "member", "MC3.0", ratings = 9999),
            entry(5, "Eierlikör", "official", "MC3.0", ratings = 5),
            entry(6, "Pizzateig", "member", "MC3.0", ratings = 412),
            entry(7, "Pizzateig", "member", "MC3.0", ratings = 9850),
            entry(8, "Kräuterbutter", "official", "MC9.9"),
            entry(9, "Tiramisu", "official", "MC1.1"),
        )
        val kept = McOnline.dedupe(list).map { it.apiId }
        // Connect vor den anderen Modellen, offiziell vor Community, sonst die meisten Bewertungen;
        // unbekannte Modelle fallen weg, ein Rezept nur für ein altes Modell bleibt.
        assertEquals(listOf(3L, 5L, 7L, 9L), kept)
        // Bei Gleichstand bleibt das Rezept, das schon im Katalog ist (Favoriten bleiben erhalten).
        assertEquals(listOf(3L, 5L, 6L, 9L), McOnline.dedupe(list, present = setOf(6L)).map { it.apiId })
    }

    @Test fun pageReadsSourceAndModels() {
        val json = """{"data":{"total":1,"totalPage":1,"recipes":[
            {"id":10,"name":"Bolognese","source":"member","deviceTypes":["MC3.0"]}]}}"""
        val e = McOnline.parsePage(json).entries.single()
        assertEquals("member", e.source)
        assertEquals(listOf("MC3.0"), e.devices)
        // Ohne Angaben: offiziell, wie bisher.
        assertEquals("official", McOnline.parsePage(fixture("mc-online-page.json")).entries[0].source)
    }

    @Test fun textOnlyRecipeOfAnOlderModel() {
        val json = """{"data":{"recipe":{"id":11,"name":"Dip","source":"member","deviceTypes":["MC1.1"],
            "servingSizes":[{"id":1,"amount":4,"servingUnit":"Portionen",
              "steps":[{"order":0,"name":"Mode","description":"","deviceSetting":null}],
              "instruction":"<p>Datteln 6 Sekunden/Stufe 10 zerkleinern.</p><p>Frischkäse zugeben, 15 Sekunden/Stufe 4 verrühren.</p><p>TIPPS: Kalt servieren.</p>"}]}}}"""
        val r = McOnline.parseRecipe(json)!!
        assertEquals(listOf("Datteln 6 Sekunden/Stufe 10 zerkleinern.", "Frischkäse zugeben, 15 Sekunden/Stufe 4 verrühren."), r.steps.map { it.text })
        assertTrue(r.steps.all { it.machine == null })
        assertTrue(r.notes.contains("TIPPS: Kalt servieren."))
        assertTrue(r.notes.contains("autre modèle"))
        assertTrue(r.tags.contains("Communauté"))
    }

    @Test fun photosFallBackToLidlMirrors() {
        val path = "/media-service-upload/images/20250218101256/1000131846_thumbnail.jpg"
        val urls = McOnline.photoUrls("https://mc-web-user-content-cdn.tecpal.com$path")
        assertEquals(3, urls.size)
        assertEquals("https://mc-web-user-content-cdn.tecpal.com$path", urls[0])
        assertEquals("https://mc-web-content-cdn.tecpal.com$path", urls[1])
        assertTrue(urls.all { it.endsWith(path) })
        assertEquals(listOf("https://example.com/a.jpg"), McOnline.photoUrls("https://example.com/a.jpg"))
    }
}
