package app.toque.core

import app.toque.core.machine.MachineLoop
import app.toque.core.machine.Transport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/** Neuverbindung der Motorbasis: eine langsam geöffnete Verbindung darf nicht sofort wieder schließen. */
class LoopReconnectTest {
    @Test fun slowOpenIsNotClosedAsSilent() {
        val clock = FakeClock()
        val log = CopyOnWriteArrayList<String>()
        // Öffnen dauert wie auf dem Gerät rund 8 s (Root-Prüfung, Anschlusssuche).
        val transport = object : Transport {
            override val name = "langsam"
            override fun open() { clock.now += 8_000 }
            override fun write(frame: ByteArray) {}
            override fun read(buffer: ByteArray): Int {
                try { Thread.sleep(60_000) } catch (_: InterruptedException) {}
                return 0
            }
            override fun close() {}
        }
        val loop = MachineLoop({ transport }, clock, periodMs = 10, log = { log += it })
        loop.start()
        try {
            Thread.sleep(400)
            assertTrue(log.toString(), log.any { it.startsWith("liaison ouverte") })
            assertFalse(log.toString(), log.any { it.startsWith("liaison fermée") })
            // Bleibt die Basis danach wirklich 5 s stumm, wird neu verbunden.
            clock.now += 6_000
            Thread.sleep(300)
            assertTrue(log.toString(), log.any { it.startsWith("liaison fermée") })
        } finally {
            loop.shutdown()
        }
    }
}
