package movilidad.rutas;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import movilidad.modelo.TipoVehiculo;

/**
 * Gestiona y cachea en tiempo constante O(1) la ubicación geográfica de los conductores.
 *
 * <p>Para fines pedagógicos y de simulación, si el chofer no cuenta con GPS físico, genera
 * determinísticamente una coordenada verosímil dentro de la red vial de Puerto Madryn
 * mediante la semilla del correo y ajusta el punto a la calzada transitable más cercana.
 */
public final class UbicacionesConductores {

    private final Map<String, PuntoRuta> ubicaciones =
        new HashMap<>();

    /**
     * Obtiene o genera la ubicación del conductor para el tipo de vehículo especificado.
     *
     * @param correo       correo identificador del conductor
     * @param tipoVehiculo perfil del rodado (AUTO o MOTO)
     * @param planificador planificador de rutas para encastrar la coordenada sobre la calle
     * @return punto de ubicación geográfica verificado sobre calzada
     */
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