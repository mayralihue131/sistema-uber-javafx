package movilidad.rutas;

import java.util.Optional;

import movilidad.modelo.TipoVehiculo;

public interface PlanificadorRutas extends AutoCloseable {

    RutaCalculada calcular(
        PuntoRuta origen,
        PuntoRuta destino,
        TipoVehiculo tipoVehiculo
    );

    Optional<PuntoRuta> ajustarACalle(
        PuntoRuta punto,
        TipoVehiculo tipoVehiculo
    );

    @Override
    void close();
}

//efine las operaciones para calcular una ruta, 
//ajustar un punto a una calle y liberar los recursos al cerrar. 
//Después las implementaremos con GraphHopper.