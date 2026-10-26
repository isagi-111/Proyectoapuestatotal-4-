package com.apuestatotal.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Huella del DNI con HMAC-SHA256 y un "pepper" secreto que vive fuera de la base de datos.
 * Un atacante que robe la tabla no puede recuperar los DNI por fuerza bruta sin el pepper
 * (un SHA-256 simple sería trivial de revertir con solo 10^8 combinaciones).
 */
public final class HasheadorDniHmac implements HasheadorDni {

    private static final String ALGORITMO = "HmacSHA256";

    private final byte[] pepper;

    /**
     * @param pepper secreto del servidor (mínimo 16 bytes)
     */
    public HasheadorDniHmac(byte[] pepper) {
        if (pepper == null || pepper.length < 16) {
            throw new IllegalArgumentException("El pepper debe tener al menos 16 bytes");
        }
        this.pepper = pepper.clone();
    }

    @Override
    public String hash(String dni) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(pepper, ALGORITMO));
            return HexFormat.of().formatHex(mac.doFinal(dni.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 no disponible", e);
        }
    }

    @Override
    public String mascara(String dni) {
        return dni.substring(0, 2) + "****" + dni.substring(dni.length() - 2);
    }
}
