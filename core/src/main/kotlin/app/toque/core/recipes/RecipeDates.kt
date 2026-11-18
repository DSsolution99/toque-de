package app.toque.core.recipes

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dates de publication « AAAA-MM-JJ » (pas de java.time : Android 6). */
object RecipeDates {
    /** Une recette est « nouvelle » pendant ce nombre de jours après sa publication. */
    const val NEW_DAYS = 14

    fun day(ms: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))

    /** Publiée il y a moins de [NEW_DAYS] jours (date au format « AAAA-MM-JJ »). */
    fun isNew(published: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (published.length < 10) return false
        return published.take(10) >= day(nowMs - NEW_DAYS * 86_400_000L)
    }
}
