package com.apuestatotal.soporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj controlable para probar expiraciones y ventanas de tiempo sin esperar. */
public final class RelojMutable extends Clock {

    private Instant ahora = Instant.parse("2026-10-25T12:00:00Z");

    public void avanzar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
