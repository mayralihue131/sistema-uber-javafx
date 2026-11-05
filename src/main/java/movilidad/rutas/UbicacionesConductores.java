package movilidad.rutas;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import movilidad.modelo.TipoVehiculo;

public final class UbicacionesConductores {

    private final Map<String, PuntoRuta> ubicaciones =
        new HashMap<>();

    public synchronized PuntoRuta obtener(
            String correo,
            TipoVehiculo tipoVehiculo,
            PlanificadorRutas planificador) {

        Objects.requireNonNull(correo);
        Objects.requireNonNull(tipoVehiculo);
        Objects.requireNonNull(planificador);

        String clave = correo.trim().toLowerCase(Locale.ROOT)
            + "|" + tipoVehiculo.name();

        PuntoRuta existente = ubicaciones.get(clave);

        if (existente != null) {
            return existente;
        }

        Random aleatorio = new Random(clave.hashCode());

        for (int intento = 0; intento < 200; intento++) {
            double latitud = -42.810
                + aleatorio.nextDouble() * 0.050;

            double longitud = -65.090
                + aleatorio.nextDouble() * 0.060;

            PuntoRuta candidato = new PuntoRuta(
                latitud, longitud
            );

            var puntoEnCalle = planificador.ajustarACalle(
                candidato, tipoVehiculo
            );

            if (puntoEnCalle.isPresent()) {
                PuntoRuta posicion = puntoEnCalle.get();

                ubicaciones.put(clave, posicion);

                return posicion;
            }
        }

        throw new IllegalStateException(
            "No se pudo ubicar al conductor en una calle."
        );
    }
}