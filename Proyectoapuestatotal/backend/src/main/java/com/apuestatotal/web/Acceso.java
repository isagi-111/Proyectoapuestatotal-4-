package com.apuestatotal.web;

/** Nivel de acceso que exige una ruta. */
public enum Acceso {
    /** Cualquiera puede llamarla. */
    PUBLICO,
    /** Requiere sesión iniciada. */
    CLIENTE,
    /** Requiere sesión de administrador. */
    ADMIN
}
