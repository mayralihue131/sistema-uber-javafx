package movilidad.rutas;

import java.util.Objects;

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

//guarda la ruta del conductor hasta el pasajero y
//la del pasajero hasta su destino. Después las dibujaremos en el mapa.