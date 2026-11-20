package app.toque.core.recipes

import app.toque.core.i18n.tr
import app.toque.core.json.Json
import app.toque.core.json.bool
import app.toque.core.json.double
import app.toque.core.json.int
import app.toque.core.json.list
import app.toque.core.json.obj
import app.toque.core.json.str
import app.toque.core.machine.Accessory
import app.toque.core.machine.JobKind
import app.toque.core.machine.JobSpec
import app.toque.core.machine.Programs
import app.toque.core.machine.SafetyPolicy
import app.toque.core.machine.Setting
import app.toque.core.machine.Violation
import app.toque.core.protocol.Direction
import app.toque.core.protocol.Mcc
import app.toque.core.protocol.Operation

/** Recettes Lidl importées (site monsieur-cuisine.com ou appli d'origine) ; USER : réservé aux recettes perso. */
enum class RecipeSource { IMPORTED, USER }

data class Ingredient(
    val qty: String = "",
    val unit: String = "",
    val name: String,
    val note: String = "",
    /** Identifiant de l'ingrédient sur le site (0 : inconnu) : sert à additionner la liste de courses. */
    val ref: Long = 0,
    /** Rayon du magasin d'après le site (« Gemüse », « Milchprodukte »…), vide si inconnu. */
    val aisle: String = "",
) {
    /** Le même ingrédient avec les quantités multipliées par [factor] (voir [Quantity]). */
    fun scaled(factor: Double): Ingredient = if (factor == 1.0) this else copy(qty = Quantity.scale(qty, factor))

    val display: String
        get() = buildString {
            if (qty.isNotBlank()) append(qty.trim()).append(' ')
            if (unit.isNotBlank()) append(unit.trim()).append(' ')
            append(name.trim())
            if (note.isNotBlank()) append(", ").append(note.trim())
        }
}

data class IngredientGroup(val title: String = "", val items: List<Ingredient>)

enum class StepProgram(val key: String, val kind: JobKind) {
    MANUAL("manuel", JobKind.MANUAL),
    KNEAD("petrir", JobKind.KNEAD),
    STEAM("vapeur", JobKind.STEAM),
    TOAST("rissoler", JobKind.TOAST),
    TURBO("turbo", JobKind.TURBO);

    companion object {
        fun of(key: String?): StepProgram? = values().firstOrNull { it.key == key?.trim()?.lowercase() }
    }
}

/** Réglage machine d'une étape, en unités humaines (secondes, °C). */
data class StepMachine(
    val program: StepProgram = StepProgram.MANUAL,
    val seconds: Int = 0,
    val speed: Int = 0,
    val tempC: Int = 0,
    val reverse: Boolean = false,
    val accessory: Accessory = Accessory.BLADE,
    /** Turbo : nombre d'impulsions conseillées. */
    val pulses: Int = 0,
) {
    val temperatureLevel: Int get() = Mcc.levelAtMost(tempC)

    val summary: String
        get() = when (program) {
            StepProgram.TURBO -> if (pulses > 0) "Turbo × $pulses" else "Turbo"
            StepProgram.KNEAD -> JobKind.KNEAD.label + " " + SafetyPolicy.formatDuration(seconds * 1000L)
            StepProgram.STEAM -> JobKind.STEAM.label + " " + SafetyPolicy.formatDuration(seconds * 1000L)
            StepProgram.TOAST -> JobKind.TOAST.label + " " + SafetyPolicy.formatDuration(seconds * 1000L)
            StepProgram.MANUAL -> buildString {
                append(SafetyPolicy.formatDuration(seconds * 1000L))
                if (tempC > 0) append(" · ").append(Mcc.celsiusForLevel(temperatureLevel)).append(" °C")
                append(" · ").append(tr("vitesse %d", speed))
                if (reverse) append(" · ").append(tr("inverse"))
                if (accessory == Accessory.WHISK) append(" · ").append(tr("fouet"))
            }
        }

    /** Travail exécutable par la machine (sauf Turbo, piloté à l'appui). */
    fun toJob(label: String, hotBlendConfirmed: Boolean = false): JobSpec? {
        val ms = seconds * 1000L
        return when (program) {
            StepProgram.MANUAL -> Programs.manual(
                speed, temperatureLevel,
                if (reverse) Direction.COUNTER_CLOCKWISE else Direction.CLOCKWISE,
                ms, accessory, hotBlendConfirmed, label,
            )
            StepProgram.KNEAD -> Programs.knead(ms).copy(label = label)
            StepProgram.STEAM -> Programs.steam(ms).copy(label = label)
            StepProgram.TOAST -> Programs.toast(ms).copy(label = label)
            StepProgram.TURBO -> null
        }
    }

    /** Problèmes de sécurité indépendants de l'état de la machine. */
    fun violations(): List<Violation> {
        if (program == StepProgram.TURBO) {
            val v = ArrayList<Violation>()
            if (accessory == Accessory.WHISK) v += Violation("accessory_turbo", tr("Pas de Turbo avec le fouet"))
            return v
        }
        val v = ArrayList<Violation>()
        if (tempC != 0 && Mcc.levelForCelsius(tempC) < 0)
            v += Violation("temp_table", tr("Température %d °C absente de la machine", tempC))
        val job = toJob("vérification") ?: return v
        v += SafetyPolicy.checkJob(job, null)
        return v
    }

    val valid: Boolean get() = violations().isEmpty()
}

data class Weigh(val label: String, val grams: Int)

data class Step(
    val text: String,
    val machine: StepMachine? = null,
    val weigh: Weigh? = null,
    /** Réglage importé non vérifié : l'utilisateur doit le confirmer avant d'exécuter. */
    val needsReview: Boolean = false,
)

data class Recipe(
    val id: String,
    val title: String,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val servings: Int = 0,
    val prepMinutes: Int = 0,
    val totalMinutes: Int = 0,
    val difficulty: Int = 1,
    val ingredients: List<IngredientGroup> = emptyList(),
    val steps: List<Step> = emptyList(),
    val source: RecipeSource = RecipeSource.IMPORTED,
    val image: String = "",
    val favorite: Boolean = false,
    val notes: String = "",
    /** Qui a publié la recette (vide si inconnu). */
    val author: String = "",
    /** Note moyenne sur 5 et nombre de notes sur monsieur-cuisine.com (0 : aucune). */
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    /** Toutes les catégories du site, dans l'ordre (la première est aussi [category]). */
    val categories: List<String> = emptyList(),
    /** Unité des portions (« Portionen », « Stück »…), vide si inconnue. */
    val servingUnit: String = "",
    /** Valeurs nutritionnelles pour une portion, null si inconnues. */
    val nutrition: Nutrition? = null,
    /** Popularité sur le site (sans unité, plus grand = plus populaire) ; 0 si inconnue. */
    val popularity: Double = 0.0,
    /** Date de publication sur le site (« AAAA-MM-JJ »), vide si inconnue. */
    val published: String = "",
    /** Page de la recette sur le site, vide si inconnue. */
    val url: String = "",
    /** Modèles Monsieur Cuisine pour lesquels le site publie la recette (« MC2.0 » : Connect). */
    val devices: List<String> = emptyList(),
) {
    val machineSteps: Int get() = steps.count { it.machine != null }
    val allIngredients: List<Ingredient> get() = ingredients.flatMap { it.items }
    /** Catégories pour les filtres : [categories], ou à défaut [category]. */
    val allCategories: List<String> get() = categories.ifEmpty { listOfNotNull(category.takeIf { it.isNotBlank() }) }
}

/** Valeurs nutritionnelles d'une portion. */
data class Nutrition(val kcal: Int, val carbsG: Double = 0.0, val fatG: Double = 0.0, val proteinG: Double = 0.0)

/** Format de fichier « toque-recettes-1 » (JSON lisible et éditable). */
object RecipeCodec {
    const val FORMAT = "toque-recettes-1"

    fun toMap(r: Recipe): Map<String, Any?> = linkedMapOf(
        "id" to r.id,
        "title" to r.title,
        "category" to r.category,
        "tags" to r.tags,
        "servings" to r.servings,
        "prepMinutes" to r.prepMinutes,
        "totalMinutes" to r.totalMinutes,
        "difficulty" to r.difficulty,
        "source" to r.source.name.lowercase(),
        "image" to r.image,
        "favorite" to r.favorite,
        "notes" to r.notes,
        "author" to r.author,
        "rating" to r.rating,
        "ratingCount" to r.ratingCount,
        "categories" to r.categories,
        "servingUnit" to r.servingUnit,
        "nutrition" to r.nutrition?.let {
            linkedMapOf("kcal" to it.kcal, "carbsG" to it.carbsG, "fatG" to it.fatG, "proteinG" to it.proteinG)
        },
        "popularity" to r.popularity,
        "published" to r.published,
        "url" to r.url,
        "devices" to r.devices,
        "ingredients" to r.ingredients.map { g ->
            linkedMapOf(
                "group" to g.title,
                "items" to g.items.map {
                    val m = linkedMapOf<String, Any?>("qty" to it.qty, "unit" to it.unit, "name" to it.name, "note" to it.note)
                    if (it.ref != 0L) m["ref"] = it.ref
                    if (it.aisle.isNotEmpty()) m["aisle"] = it.aisle
                    m
                },
            )
        },
        "steps" to r.steps.map { s ->
            val m = linkedMapOf<String, Any?>("text" to s.text)
            s.machine?.let { mc ->
                m["machine"] = linkedMapOf(
                    "program" to mc.program.key, "seconds" to mc.seconds, "speed" to mc.speed,
                    "tempC" to mc.tempC, "reverse" to mc.reverse,
                    "accessory" to mc.accessory.name.lowercase(), "pulses" to mc.pulses,
                )
            }
            s.weigh?.let { w -> m["weigh"] = linkedMapOf("label" to w.label, "grams" to w.grams) }
            if (s.needsReview) m["needsReview"] = true
            m
        },
    )

    fun fromMap(m: Map<String, Any?>, fallbackSource: RecipeSource = RecipeSource.IMPORTED): Recipe? {
        val title = m.str("title")?.trim().orEmpty()
        if (title.isEmpty()) return null
        val id = m.str("id")?.trim().takeUnless { it.isNullOrEmpty() } ?: slug(title)
        val source = RecipeSource.values().firstOrNull { it.name.equals(m.str("source"), true) } ?: fallbackSource
        val groups = m["ingredients"].list().mapNotNull { gAny ->
            val g = gAny.obj()
            val items = g["items"].list().mapNotNull { iAny ->
                when (iAny) {
                    is String -> if (iAny.isBlank()) null else Ingredient(name = iAny.trim())
                    else -> {
                        val i = iAny.obj()
                        val name = i.str("name")?.trim().orEmpty()
                        if (name.isEmpty()) null
                        else Ingredient(i.str("qty").orEmpty(), i.str("unit").orEmpty(), name, i.str("note").orEmpty(),
                            ref = (i["ref"] as? Number)?.toLong() ?: 0L, aisle = i.str("aisle").orEmpty())
                    }
                }
            }
            if (items.isEmpty()) null else IngredientGroup(g.str("group").orEmpty(), items)
        }
        val steps = m["steps"].list().mapNotNull { sAny ->
            val s = sAny.obj()
            val text = s.str("text")?.trim().orEmpty()
            val mc = s["machine"]?.obj()?.let { parseMachine(it) }
            val w = s["weigh"]?.obj()?.let { wm ->
                val g = wm.int("grams") ?: 0
                if (g > 0) Weigh(wm.str("label").orEmpty(), g) else null
            }
            if (text.isEmpty() && mc == null) null else Step(text, mc, w, s.bool("needsReview") ?: false)
        }
        return Recipe(
            id = id,
            title = title,
            category = m.str("category").orEmpty(),
            tags = m["tags"].list().mapNotNull { (it as? String)?.trim()?.takeIf { t -> t.isNotEmpty() } },
            servings = m.int("servings") ?: 0,
            prepMinutes = m.int("prepMinutes") ?: 0,
            totalMinutes = m.int("totalMinutes") ?: 0,
            difficulty = (m.int("difficulty") ?: 1).coerceIn(1, 3),
            ingredients = groups,
            steps = steps,
            source = source,
            image = m.str("image").orEmpty(),
            favorite = m.bool("favorite") ?: false,
            notes = m.str("notes").orEmpty(),
            author = m.str("author")?.trim().orEmpty(),
            rating = (m.double("rating") ?: 0.0).coerceIn(0.0, 5.0),
            ratingCount = (m.int("ratingCount") ?: 0).coerceAtLeast(0),
            categories = m["categories"].list().mapNotNull { (it as? String)?.trim()?.takeIf { t -> t.isNotEmpty() } },
            servingUnit = m.str("servingUnit")?.trim().orEmpty(),
            nutrition = m["nutrition"]?.obj()?.let { n ->
                val kcal = n.int("kcal") ?: return@let null
                Nutrition(kcal, n.double("carbsG") ?: 0.0, n.double("fatG") ?: 0.0, n.double("proteinG") ?: 0.0)
            },
            popularity = (m.double("popularity") ?: 0.0).coerceAtLeast(0.0),
            published = m.str("published")?.trim().orEmpty(),
            url = m.str("url")?.trim().orEmpty(),
            devices = m["devices"].list().mapNotNull { (it as? String)?.trim()?.takeIf { d -> d.isNotEmpty() } },
        )
    }

    private fun parseMachine(m: Map<String, Any?>): StepMachine? {
        val program = StepProgram.of(m.str("program")) ?: return null
        val acc = Accessory.values().firstOrNull { it.name.equals(m.str("accessory"), true) } ?: Accessory.BLADE
        return StepMachine(
            program = program,
            seconds = (m.int("seconds") ?: 0).coerceAtLeast(0),
            speed = m.int("speed") ?: 0,
            tempC = m.int("tempC") ?: 0,
            reverse = m.bool("reverse") ?: false,
            accessory = acc,
            pulses = (m.int("pulses") ?: 0).coerceAtLeast(0),
        )
    }

    fun encode(r: Recipe, pretty: Boolean = false): String = Json.write(toMap(r), pretty)

    fun decode(text: String, fallbackSource: RecipeSource = RecipeSource.IMPORTED): Recipe? =
        fromMap(Json.parse(text).obj(), fallbackSource)

    fun encodeFile(recipes: List<Recipe>): String =
        Json.write(linkedMapOf("format" to FORMAT, "recipes" to recipes.map { toMap(it) }), pretty = true)

    /** Lit un fichier de recettes ; les entrées illisibles sont ignorées et comptées. */
    fun decodeFile(text: String, fallbackSource: RecipeSource): Pair<List<Recipe>, Int> {
        val root = Json.parse(text)
        val entries = when (root) {
            is List<*> -> root
            is Map<*, *> -> root.obj()["recipes"].list()
            else -> emptyList()
        }
        var bad = 0
        val out = ArrayList<Recipe>(entries.size)
        for (e in entries) {
            val r = try {
                fromMap(e.obj(), fallbackSource)
            } catch (_: Exception) {
                null
            }
            if (r == null) bad++ else out += r
        }
        return out to bad
    }

    fun slug(text: String): String {
        val base = TextNorm.normalize(text).replace(' ', '-').trim('-')
        return if (base.isEmpty()) "recette-" + System.currentTimeMillis() else base.take(48)
    }
}

/** Exécute un réglage d'étape à la main (utile pour le mode manuel pré-rempli). */
fun StepMachine.toSetting(): Setting = Setting(
    when (program) {
        StepProgram.MANUAL -> Operation.STANDARD
        StepProgram.KNEAD -> Operation.KNEAD
        StepProgram.STEAM -> Operation.STEAM
        StepProgram.TOAST -> Operation.TOAST
        StepProgram.TURBO -> Operation.TURBO
    },
    speed, temperatureLevel, if (reverse) Direction.COUNTER_CLOCKWISE else Direction.CLOCKWISE,
)
