package app.toque.web

import android.content.Intent
import app.toque.ToqueApp
import app.toque.core.i18n.I18n
import app.toque.core.json.Json
import app.toque.core.json.int
import app.toque.core.json.obj
import app.toque.core.json.str
import app.toque.core.net.LocalNetwork
import app.toque.core.recipes.RecipeFilter
import app.toque.core.recipes.RecipeImport
import app.toque.core.recipes.RecipeSource
import app.toque.core.recipes.RecipeSort
import app.toque.core.recipes.WebViews
import app.toque.ui.MainActivity
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.util.concurrent.Executors

/**
 * Interface web du robot pour le téléphone, dans le réseau de la maison : chercher et lire les
 * recettes, les ouvrir sur l'écran du robot, gérer les favoris et la liste de courses.
 *
 * Sécurité : rien ne démarre le moteur ni la chauffe ; une recette ne s'ouvre pas pendant un
 * programme. Seules les adresses du réseau local sont servies, et toute modification exige
 * l'en-tête « X-Toque » (une page web étrangère ne peut pas l'envoyer sans autorisation).
 * Les recettes importées ([RecipeImport]) ont des réglages machine à confirmer sur le robot.
 */
class WebServer(private val app: ToqueApp) {
    @Volatile private var socket: ServerSocket? = null
    private val pool = Executors.newFixedThreadPool(3)

    val running: Boolean get() = socket?.isClosed == false

    @Synchronized fun start() {
        if (running) return
        val s = try {
            ServerSocket(PORT, 16)
        } catch (e: Exception) {
            app.log.write("web", "port $PORT indisponible : $e")
            return
        }
        socket = s
        app.log.write("web", "interface web sur le port $PORT")
        Thread({
            while (!s.isClosed) {
                val client = try { s.accept() } catch (_: SocketException) { break } catch (_: Exception) { continue }
                try {
                    pool.execute { handle(client) }
                } catch (_: Exception) {
                    client.close()
                }
            }
        }, "toque-web").apply { isDaemon = true }.start()
    }

    @Synchronized fun stop() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    // ------------------------------------------------------------------ HTTP

    private class Request(val method: String, val path: String, val query: Map<String, String>,
                          val headers: Map<String, String>, val body: ByteArray)

    private class Response(val status: Int, val type: String, val body: ByteArray, val cache: Boolean = false)

    private fun handle(client: Socket) {
        client.use { sock ->
            try {
                sock.soTimeout = 15_000
                if (!isLocal(sock.inetAddress)) {
                    write(sock.getOutputStream(), text(403, "forbidden"))
                    return
                }
                val req = read(BufferedInputStream(sock.getInputStream())) ?: return
                // Une page Internet qui ferait pointer son nom de domaine vers le robot (DNS rebinding)
                // n'envoie pas l'adresse du robot dans « Host » : refusée avant toute réponse.
                if (!LocalNetwork.isTrustedHost(req.headers["host"])) {
                    write(sock.getOutputStream(), text(403, "forbidden host"))
                    return
                }
                if (req.method == "POST" && !LocalNetwork.isSameOrigin(req.headers["origin"], req.headers["host"])) {
                    write(sock.getOutputStream(), text(403, "forbidden origin"))
                    return
                }
                val res = try {
                    route(req)
                } catch (e: Exception) {
                    app.log.write("web", "${req.method} ${req.path} : $e")
                    json(500, mapOf("error" to (e.message ?: e.toString())))
                }
                write(sock.getOutputStream(), res)
            } catch (_: Exception) {
            }
        }
    }

    private fun read(input: InputStream): Request? {
        val head = ByteArrayOutputStream()
        var last4 = 0
        while (true) {
            val b = input.read()
            if (b < 0) return null
            head.write(b)
            last4 = (last4 shl 8) or b
            if (last4 == 0x0D0A0D0A) break
            if (head.size() > 16 * 1024) return null
        }
        val lines = String(head.toByteArray(), Charsets.ISO_8859_1).split("\r\n")
        val first = lines.first().split(' ')
        if (first.size < 2) return null
        val headers = HashMap<String, String>()
        for (l in lines.drop(1)) {
            val i = l.indexOf(':')
            if (i > 0) headers[l.substring(0, i).trim().lowercase()] = l.substring(i + 1).trim()
        }
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        if (length > MAX_BODY) return null
        val body = ByteArray(length)
        var n = 0
        while (n < length) {
            val r = input.read(body, n, length - n)
            if (r < 0) return null
            n += r
        }
        val target = first[1]
        val path = URLDecoder.decode(target.substringBefore('?'), "UTF-8")
        val query = HashMap<String, String>()
        if (target.contains('?')) for (part in target.substringAfter('?').split('&')) {
            if (part.isEmpty()) continue
            query[URLDecoder.decode(part.substringBefore('='), "UTF-8")] = URLDecoder.decode(part.substringAfter('=', ""), "UTF-8")
        }
        return Request(first[0].uppercase(), path, query, headers, body)
    }

    private fun write(out: OutputStream, r: Response) {
        val reason = when (r.status) { 200 -> "OK"; 400 -> "Bad Request"; 404 -> "Not Found"; 403 -> "Forbidden"; 409 -> "Conflict"; else -> "Status" }
        val head = StringBuilder()
            .append("HTTP/1.1 ").append(r.status).append(' ').append(reason).append("\r\n")
            .append("Content-Type: ").append(r.type).append("\r\n")
            .append("Content-Length: ").append(r.body.size).append("\r\n")
            .append("Cache-Control: ").append(if (r.cache) "max-age=86400" else "no-store").append("\r\n")
            .append("X-Content-Type-Options: nosniff\r\n")
            .append("Connection: close\r\n\r\n")
        out.write(head.toString().toByteArray(Charsets.ISO_8859_1))
        out.write(r.body)
        out.flush()
    }

    private fun json(status: Int, v: Any?) = Response(status, "application/json; charset=utf-8", Json.write(v).toByteArray(Charsets.UTF_8))
    private fun ok(v: Any?) = json(200, v)
    private fun text(status: Int, s: String) = Response(status, "text/plain; charset=utf-8", s.toByteArray(Charsets.UTF_8))

    // ------------------------------------------------------------------ routes

    private fun route(req: Request): Response {
        val write = req.method == "POST"
        if (write && req.headers["x-toque"] != "1") return text(403, "missing X-Toque header")
        if (!write && req.method != "GET") return text(405, "method not allowed")
        if (write && req.path == "/api/import") return importRecipes(req.body)
        val body: Map<String, Any?> = if (write && req.body.isNotEmpty())
            (try { Json.parse(String(req.body, Charsets.UTF_8)) } catch (_: Exception) { null }).obj() else emptyMap()
        return when {
            req.path == "/" || req.path == "/index.html" -> asset("web/index.html", "text/html; charset=utf-8")
            req.path == "/app.js" -> asset("web/app.js", "application/javascript; charset=utf-8")
            req.path == "/app.css" -> asset("web/app.css", "text/css; charset=utf-8")
            req.path.startsWith("/img/") -> image(req.path.removePrefix("/img/"))
            req.path == "/api/texts" -> ok(linkedMapOf("language" to I18n.code, "texts" to WebTexts.all()))
            req.path == "/api/status" -> ok(WebStatus.of(app.machine.snapshot))
            req.path == "/api/categories" -> ok(app.recipes.index.categories)
            req.path == "/api/filters" -> ok(linkedMapOf("kcal" to RecipeFilter.KCAL_LEVELS, "sorts" to RecipeSort.values().map { it.name }))
            req.path == "/api/recipes" -> recipes(req.query)
            req.path == "/api/prompt" -> asset("web/prompt.txt", "text/plain; charset=utf-8")
            req.path == "/api/recipe" -> recipe(req.query["id"].orEmpty(), req.query["servings"]?.toIntOrNull() ?: 0)
            req.path == "/api/shopping" && !write -> shopping()
            !write -> text(404, "not found")
            req.path == "/api/open" -> open(body.str("id").orEmpty(), body.int("servings") ?: 0)
            req.path == "/api/favorite" -> {
                val id = body.str("id").orEmpty()
                if (app.recipes.get(id) == null) return text(404, "unknown recipe")
                app.recipes.setFavorite(id, body["on"] == true)
                ok(mapOf("favorite" to (body["on"] == true)))
            }
            req.path == "/api/delete" -> {
                val id = body.str("id").orEmpty()
                if (!RecipeImport.isOwn(id)) return text(403, "only own recipes can be deleted")
                if (!app.recipes.deleteOwn(id)) return text(404, "unknown recipe")
                ok(mapOf("deleted" to id))
            }
            req.path == "/api/shopping/add" -> {
                val r = app.recipes.get(body.str("id").orEmpty()) ?: return text(404, "unknown recipe")
                val servings = body.int("servings") ?: 0
                val factor = if (servings > 0 && r.servings > 0) servings.toDouble() / r.servings else 1.0
                app.shopping.add(r, factor)
                shopping()
            }
            req.path == "/api/shopping/toggle" -> { app.shopping.toggle(body.str("key").orEmpty()); shopping() }
            req.path == "/api/shopping/remove" -> { app.shopping.remove(body.str("key").orEmpty()); shopping() }
            req.path == "/api/shopping/clear-checked" -> { app.shopping.clearChecked(); shopping() }
            req.path == "/api/shopping/clear" -> { app.shopping.clear(); shopping() }
            else -> text(404, "not found")
        }
    }

    private fun asset(name: String, type: String): Response = try {
        Response(200, type, app.assets.open(name).use { it.readBytes() })
    } catch (_: Exception) {
        text(404, "not found")
    }

    private fun hasImage(url: String): Boolean = app.recipes.imageFile(url) != null

    private fun image(name: String): Response {
        if (name.isEmpty() || name.contains('/') || name.contains("..")) return text(404, "not found")
        val f: File = app.recipes.imageFile(name) ?: return text(404, "not found")
        return Response(200, "image/jpeg", f.readBytes(), cache = true)
    }

    private fun recipes(q: Map<String, String>): Response {
        val sort = RecipeSort.values().firstOrNull { it.name.equals(q["sort"], true) } ?: RecipeSort.RELEVANCE
        val filter = RecipeFilter(
            category = q["category"].orEmpty(),
            favoritesOnly = q["favorites"] == "1",
            maxMinutes = q["minutes"]?.toIntOrNull() ?: 0,
            maxKcal = q["kcal"]?.toIntOrNull() ?: 0,
            source = if (q["mine"] == "1") RecipeSource.USER else null,
            sort = sort,
        )
        val query = q["q"].orEmpty()
        val index = app.recipes.index
        val hits = if (query.contains(',')) index.withIngredients(query.split(','), filter) else index.search(query, filter, limit = 5000)
        val offset = (q["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
        val limit = (q["limit"]?.toIntOrNull() ?: 40).coerceIn(1, 100)
        val page = hits.drop(offset).take(limit).map { WebViews.summary(it.entry, app.recipes.isFavorite(it.entry.id), ::hasImage) }
        return ok(linkedMapOf("total" to hits.size, "offset" to offset, "items" to page))
    }

    private fun recipe(id: String, servings: Int): Response {
        val r = app.recipes.get(id) ?: return text(404, "unknown recipe")
        return ok(WebViews.detail(r, servings, app.recipes.isFavorite(id), ::hasImage))
    }

    private fun shopping(): Response =
        ok(app.shopping.read { WebViews.shopping(it, app.shopping.version) })

    /**
     * Range des recettes personnelles (format « toque-recettes-1 »). Refusé pendant un programme :
     * la reconstruction de l'index ne doit pas ralentir la tablette en pleine cuisson.
     */
    private fun importRecipes(raw: ByteArray): Response {
        if (app.machine.snapshot.busy) return json(409, mapOf("error" to "busy"))
        val res = RecipeImport.parse(String(raw, Charsets.UTF_8))
        if (res.recipes.isNotEmpty()) {
            app.recipes.upsertAll(res.recipes)
            app.log.write("web", "${res.recipes.size} recette(s) perso importée(s)")
        }
        return json(if (res.recipes.isEmpty()) 400 else 200, linkedMapOf(
            "imported" to res.recipes.map { linkedMapOf("id" to it.id, "title" to it.title) },
            "warnings" to res.warnings,
            "errors" to res.errors,
        ))
    }

    /** Ouvre la recette sur l'écran du robot, sauf pendant un programme ou dans l'appli d'origine. */
    private fun open(id: String, servings: Int): Response {
        if (app.recipes.get(id) == null) return text(404, "unknown recipe")
        if (app.machine.snapshot.busy) return json(409, mapOf("error" to "busy"))
        if (app.prefs.vendorSession) return json(409, mapOf("error" to "vendor"))
        val intent = Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_OPEN_RECIPE, id)
            .putExtra(MainActivity.EXTRA_SERVINGS, servings)
        app.startActivity(intent)
        return ok(mapOf("opened" to true))
    }

    companion object {
        const val PORT = 8080
        /** Corps de requête maximal (un envoi de recettes reste bien en dessous). */
        const val MAX_BODY = 512 * 1024

        /** Adresse du réseau de la maison (voir [LocalNetwork]). */
        fun isLocal(a: InetAddress?): Boolean = LocalNetwork.isLocal(a)

        /** Adresses IPv4 du robot dans le réseau local, pour les afficher dans les réglages. */
        fun localAddresses(): List<String> = try {
            NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filter { it is Inet4Address && it.isSiteLocalAddress }
                .map { it.hostAddress }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
