package movilidad.rutas;

import java.util.Optional;

import movilidad.modelo.TipoVehiculo;

/**
 * Contrato para servicios de cálculo de trayectos viales y geocodificación sobre calles.
 * Permite desacoplar el motor específico (GraphHopper, OSRM, etc.) del resto del sistema.
 */
public interface PlanificadorRutas extends AutoCloseable {

    /**
     * Calcula la ruta óptima entre dos coordenadas viales según el tipo de vehículo.
     *
     * @param origen       punto de partida
     * @param destino      punto de llegada
     * @param tipoVehiculo perfil del rodado (AUTO o MOTO)
     * @return objeto {@link RutaCalculada} con distancia, tiempo y geometría de la polilínea
     */
    RutaCalculada calcular(
        PuntoRuta origen,
        PuntoRuta destino,
        TipoVehiculo tipoVehiculo
    );

    /**
     * Ajusta una coordenada arbitraria hacia el nodo o calzada transitable más cercano.
     *
     * @param punto        coordenada geográfica original
     * @param tipoVehiculo perfil del rodado para evaluar accesibilidad
     * @return {@link Optional} con el punto sobre calzada si se encontró, o vacío
     */
    Optional<PuntoRuta> ajustarACalle(
        PuntoRuta punto,
        TipoVehiculo tipoVehiculo
    );

    @Override
    void close();
}