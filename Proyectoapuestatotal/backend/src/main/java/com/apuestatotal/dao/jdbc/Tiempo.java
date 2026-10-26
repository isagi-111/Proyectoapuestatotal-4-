package com.apuestatotal.dao.jdbc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Conversión entre {@link Instant} y el tipo que entiende JDBC. */
final class Tiempo {

    private Tiempo() {
    }

    static OffsetDateTime aSql(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    static Instant deSql(OffsetDateTime valor) {
        return valor.toInstant();
    }
}
