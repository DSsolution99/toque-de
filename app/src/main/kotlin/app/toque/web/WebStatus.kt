package app.toque.web

import app.toque.core.i18n.tr
import app.toque.core.machine.JobState
import app.toque.core.machine.Snapshot

/** État du robot pour le téléphone (lecture seule). */
object WebStatus {
    fun of(s: Snapshot): Map<String, Any?> = linkedMapOf(
        "ready" to s.ready,
        "busy" to s.busy,
        "state" to when {
            s.hasFault -> tr("Erreur")
            s.state == JobState.PAUSED -> tr("En pause")
            s.running -> tr("En cours")
            s.ready -> tr("Prête")
            else -> tr("Base non connectée")
        },
        "fault" to s.faultMessage,
        "program" to s.jobLabel,
        "remainingMs" to s.remainingMs,
        "totalMs" to s.totalMs,
        "phase" to s.phaseIndex + 1,
        "phases" to s.phaseCount,
        "tempC" to if (s.hasSample) s.actualTempC else null,
        "targetC" to s.targetTempC,
        "speed" to s.speedLevel,
        "lidOpen" to s.lidOpen,
        "weightG" to s.weightG,
    )
}
