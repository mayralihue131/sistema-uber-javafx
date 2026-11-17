package movilidad.rutas;

/**
 * Objeto de valor inmutable (Record) que representa una coordenada geográfica en la superficie terrestre.
 * Valida los rangos estándar de latitud ([-90, 90]) y longitud ([-180, 180]).
 *
 * @param latitud  componente de latitud en grados decimales
 * @param longitud componente de longitud en grados decimales
 */
public record PuntoRuta(double latitud, double longitud) {

    public PuntoRuta {
        if (!Double.isFinite(latitud)
                || !Double.isFinite(longitud)
                || latitud < -90 || latitud > 90
                || longitud < -180 || longitud > 180) {
            throw new IllegalArgumentException(
                "Las coordenadas no son válidas."
            );
        }
    }
}