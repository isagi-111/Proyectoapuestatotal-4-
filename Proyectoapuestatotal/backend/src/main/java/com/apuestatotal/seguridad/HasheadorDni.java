package com.apuestatotal.seguridad;

/** Protege el DNI: nunca se almacena en claro. */
public interface HasheadorDni {

    /**
     * @param dni DNI ya validado
     * @return huella irreversible (hex) usada para buscar y evitar duplicados
     */
    String hash(String dni);

    /**
     * @param dni DNI ya validado
     * @return versión parcialmente oculta, por ejemplo {@code 12****78}
     */
    String mascara(String dni);
}
