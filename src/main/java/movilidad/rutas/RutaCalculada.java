package movilidad.rutas;

import java.util.List;

/**
 * Resultado inmutable del cálculo de un trayecto por calzada.
 *
 * @param distanciaMetros      longitud física de la ruta en metros
 * @param duracionMilisegundos tiempo estimado de conducción en milisegundos
 * @param puntos               secuencia ordenada de coordenadas (polilínea) que conforman el recorrido
 */
public record RutaCalculada(
        double distanciaMetros,
        long duracionMilisegundos,
        List<PuntoRuta> puntos) {

    public RutaCalculada {
        if (!Double.isFinite(distanciaMetros)
                || distanciaMetros < 0
                || duracionMilisegundos < 0) {
            throw new IllegalArgumentException(
                "La distancia y la duración deben ser válidas."
            );
        }

        puntos = List.copyOf(puntos);

        if (puntos.size() < 2) {
            throw new IllegalArgumentException(
                "La ruta debe tener al menos dos puntos."
            );
        }
    }
}