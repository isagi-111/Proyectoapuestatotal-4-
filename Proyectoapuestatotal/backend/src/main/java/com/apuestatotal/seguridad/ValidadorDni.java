package com.apuestatotal.seguridad;

import com.apuestatotal.excepcion.ValidacionException;

/** Validación de entrada del DNI (lista blanca: exactamente 8 dígitos ASCII). */
public final class ValidadorDni {

    private ValidadorDni() {
    }

    /**
     * Valida y normaliza el DNI.
     *
     * @param dni texto recibido del cliente
     * @return el DNI sin espacios en los extremos
     * @throws ValidacionException si no son exactamente 8 dígitos
     */
    public static String validar(String dni) {
        if (dni == null) {
            throw new ValidacionException("El DNI es obligatorio.");
        }
        String limpio = dni.trim();
        if (limpio.length() != 8) {
            throw new ValidacionException("El DNI debe tener 8 dígitos.");
        }
        for (int i = 0; i < limpio.length(); i++) {
            char c = limpio.charAt(i);
            if (c < '0' || c > '9') {
                throw new ValidacionException("El DNI solo puede contener números.");
            }
        }
        return limpio;
    }
}
