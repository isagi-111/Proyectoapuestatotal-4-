package com.apuestatotal.model;

import java.time.Instant;

/**
 * Usuario del sistema. Por seguridad NO se guarda el DNI en claro: solo su huella
 * HMAC-SHA256 (para buscar/evitar duplicados) y una versión enmascarada para mostrar.
 *
 * @param id          identificador (0 si aún no se guardó)
 * @param dniHash     huella HMAC del DNI
 * @param dniMascara  DNI parcialmente oculto, por ejemplo {@code 12****78}
 * @param rol         rol del usuario
 * @param creadoEn    fecha de creación
 */
public record Usuario(long id, String dniHash, String dniMascara, Rol rol, Instant creadoEn) {

    /**
     * @param nuevoId identificador asignado por la base de datos
     * @return copia con el identificador indicado
     */
    public Usuario conId(long nuevoId) {
        return new Usuario(nuevoId, dniHash, dniMascara, rol, creadoEn);
    }
}
