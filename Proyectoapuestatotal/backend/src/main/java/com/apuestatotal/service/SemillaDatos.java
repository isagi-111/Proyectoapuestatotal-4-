package com.apuestatotal.service;

import com.apuestatotal.dao.PartidoDao;
import com.apuestatotal.model.EstadoPartido;
import com.apuestatotal.model.Partido;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Carga los partidos de ejemplo de la portada si la tabla está vacía. */
public final class SemillaDatos {

    private static final String LIGA = "Clasificatorias Mundial";

    private SemillaDatos() {
    }

    /**
     * @param partidos DAO de partidos
     * @param reloj    fuente de tiempo
     */
    public static void cargarSiVacio(PartidoDao partidos, Clock reloj) {
        if (!partidos.listar().isEmpty()) {
            return;
        }
        Instant base = reloj.instant().plus(Duration.ofHours(24));
        agregar(partidos, "Turquía", "Francia", base, EstadoPartido.PROGRAMADO, "2.51", "3.19", "2.44");
        agregar(partidos, "Italia", "Bélgica", base, EstadoPartido.PROGRAMADO, "2.31", "3.18", "3.65");
        agregar(partidos, "Hungría", "Ucrania", reloj.instant().minus(Duration.ofMinutes(50)),
                EstadoPartido.EN_VIVO, "2.97", "3.38", "2.17");
        agregar(partidos, "Suecia", "Rumania", base, EstadoPartido.PROGRAMADO, "2.03", "3.44", "3.83");
    }

    private static void agregar(PartidoDao dao, String local, String visita, Instant inicio, EstadoPartido estado,
                                String cl, String ce, String cv) {
        dao.guardar(new Partido(0, LIGA, local, visita, inicio, estado,
                new BigDecimal(cl), new BigDecimal(ce), new BigDecimal(cv), null, null));
    }
}
