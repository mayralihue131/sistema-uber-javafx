package movilidad.rutas;

import java.util.Objects;

/**
 * Agrupa los dos segmentos viales que componen un viaje completo en la plataforma:
 * el trayecto del conductor hacia el punto de recogida (origen) y el trayecto
 * desde el origen hacia el destino final.
 *
 * @param conductorAOrigen ruta desde la posición del chofer hasta el punto de encuentro
 * @param origenADestino   ruta del viaje contratado hacia el destino
 */
public record RutasViaje(
        RutaCalculada conductorAOrigen,
        RutaCalculada origenADestino) {

    public RutasViaje {
        Objects.requireNonNull(
            conductorAOrigen,
            "Falta la ruta del conductor al origen."
        );

        Objects.requireNonNull(
            origenADestino,
            "Falta la ruta del origen al destino."
        );
    }
}