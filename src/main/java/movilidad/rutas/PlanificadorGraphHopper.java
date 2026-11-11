package movilidad.rutas;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.graphhopper.GHRequest;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.DefaultSnapFilter;
import com.graphhopper.util.PMap;

import movilidad.modelo.TipoVehiculo;

public final class PlanificadorGraphHopper
        implements PlanificadorRutas {

    private final GraphHopper motor;

    public PlanificadorGraphHopper(GraphHopper motor) {
        this.motor = Objects.requireNonNull(motor);
    }

    @Override
    public RutaCalculada calcular(
            PuntoRuta origen,
            PuntoRuta destino,
            TipoVehiculo tipoVehiculo) {

        String nombrePerfil = perfilPara(tipoVehiculo);
        Objects.requireNonNull(origen);
        Objects.requireNonNull(destino);

        GHRequest pedido = new GHRequest(
            origen.latitud(),
            origen.longitud(),
            destino.latitud(),
            destino.longitud()
        );

        pedido.setProfile(nombrePerfil);
        pedido.setLocale(Locale.forLanguageTag("es-AR"));

        var respuesta = motor.route(pedido);

        if (respuesta.hasErrors()) {
            throw new IllegalArgumentException(
                "No se pudo calcular el recorrido: "
                    + respuesta.getErrors()
            );
        }

        var camino = respuesta.getBest();
        var geometria = camino.getPoints();
        var puntos = new ArrayList<PuntoRuta>();

        for (int i = 0; i < geometria.size(); i++) {
            puntos.add(new PuntoRuta(
                geometria.getLat(i),
                geometria.getLon(i)
            ));
        }

        return new RutaCalculada(
            camino.getDistance(),
            camino.getTime(),
            puntos
        );
    }

    @Override
    public Optional<PuntoRuta> ajustarACalle(
            PuntoRuta punto,
            TipoVehiculo tipoVehiculo) {

        String nombrePerfil = perfilPara(tipoVehiculo);
        Objects.requireNonNull(punto);

        var perfil = motor.getProfile(nombrePerfil);

        var filtro = new DefaultSnapFilter(
            motor.createWeighting(perfil, new PMap()),
            motor.getEncodingManager()
                .getBooleanEncodedValue(nombrePerfil + "_subnetwork")
        );

        var ajuste = motor.getLocationIndex().findClosest(
            punto.latitud(),
            punto.longitud(),
            filtro
        );

        if (!ajuste.isValid()
                || ajuste.getQueryDistance() > 100) {
            return Optional.empty();
        }

        var coordenadas = ajuste.getSnappedPoint();

        return Optional.of(new PuntoRuta(
            coordenadas.lat,
            coordenadas.lon
        ));
    }

    private String perfilPara(TipoVehiculo tipoVehiculo) {
        return switch (tipoVehiculo) {
            case AUTO -> "car";
            case MOTO -> "motorcycle";
        };
    }

    @Override
    public void close() {
        motor.close();
    }
}
