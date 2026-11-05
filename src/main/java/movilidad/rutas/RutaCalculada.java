package movilidad.rutas;

import java.util.List;

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

// guarda la distancia, el tiempo y los puntos del recorrido.