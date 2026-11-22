package movilidad.rutas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import movilidad.modelo.TipoVehiculo;

/**
 * Pruebas unitarias para la configuración y alternancia dinámica de ciudades
 * (Puerto Madryn, Trelew, Rawson y Gaiman) mediante {@link FabricaGraphHopper} y mapa.properties.
 */
class CiudadMapaTest {

    @BeforeEach
    void setUp() {
        FabricaGraphHopper.reiniciar();
    }

    @AfterEach
    void tearDown() {
        FabricaGraphHopper.reiniciar();
    }

    @Test
    void testCiudadesDisponiblesYPredeterminada() {
        List<String> ciudades = FabricaGraphHopper.getCiudadesDisponibles();
        assertNotNull(ciudades);
        assertTrue(ciudades.contains("Puerto Madryn"));
        assertTrue(ciudades.contains("Trelew"));
        assertTrue(ciudades.contains("Rawson"));
        assertTrue(ciudades.contains("Gaiman"));

        // Por defecto lee mapa.properties (Puerto Madryn)
        assertEquals("Puerto Madryn", FabricaGraphHopper.getCiudadActiva());
    }

    @Test
    void testCambioDinamicoACiudadTrelew() {
        FabricaGraphHopper.setCiudadActiva("Trelew");
        assertEquals("Trelew", FabricaGraphHopper.getCiudadActiva());

        PuntoRuta centro = FabricaGraphHopper.centroMapa();
        assertEquals(-43.251, centro.latitud(), 0.01);
        assertEquals(-65.309, centro.longitud(), 0.01);

        // Coordenada dentro de Trelew
        assertTrue(FabricaGraphHopper.puntoAdmitido(-43.251, -65.309));
        // Coordenada de Puerto Madryn debe estar fuera de los límites de Trelew
        assertFalse(FabricaGraphHopper.puntoAdmitido(-42.769, -65.038));

        double[] zona = FabricaGraphHopper.zonaConductores();
        assertEquals(4, zona.length);
        assertTrue(zona[0] < zona[2]);
        assertTrue(zona[1] < zona[3]);
    }

    @Test
    void testCambioDinamicoACiudadRawson() {
        FabricaGraphHopper.setCiudadActiva("Rawson");
        assertEquals("Rawson", FabricaGraphHopper.getCiudadActiva());

        PuntoRuta centro = FabricaGraphHopper.centroMapa();
        assertEquals(-43.300, centro.latitud(), 0.01);
        assertEquals(-65.102, centro.longitud(), 0.01);

        assertTrue(FabricaGraphHopper.puntoAdmitido(-43.300, -65.102));
        assertFalse(FabricaGraphHopper.puntoAdmitido(-43.251, -65.309)); // Trelew fuera de Rawson
    }

    @Test
    void testCambioDinamicoACiudadGaiman() {
        FabricaGraphHopper.setCiudadActiva("Gaiman");
        assertEquals("Gaiman", FabricaGraphHopper.getCiudadActiva());

        PuntoRuta centro = FabricaGraphHopper.centroMapa();
        assertEquals(-43.288, centro.latitud(), 0.01);
        assertEquals(-65.492, centro.longitud(), 0.01);

        assertTrue(FabricaGraphHopper.puntoAdmitido(-43.288, -65.492));
        assertFalse(FabricaGraphHopper.puntoAdmitido(-42.769, -65.038)); // Madryn fuera de Gaiman
    }

    @Test
    void testAlternarEntreCiudades() {
        FabricaGraphHopper.setCiudadActiva("Puerto Madryn");
        assertEquals("Puerto Madryn", FabricaGraphHopper.getCiudadActiva());
        assertTrue(FabricaGraphHopper.puntoAdmitido(-42.769, -65.038));

        FabricaGraphHopper.setCiudadActiva("Rawson");
        assertEquals("Rawson", FabricaGraphHopper.getCiudadActiva());
        assertTrue(FabricaGraphHopper.puntoAdmitido(-43.300, -65.102));

        FabricaGraphHopper.setCiudadActiva("Gaiman");
        assertEquals("Gaiman", FabricaGraphHopper.getCiudadActiva());
        assertTrue(FabricaGraphHopper.puntoAdmitido(-43.288, -65.492));

        FabricaGraphHopper.setCiudadActiva("Trelew");
        assertEquals("Trelew", FabricaGraphHopper.getCiudadActiva());
        assertTrue(FabricaGraphHopper.puntoAdmitido(-43.251, -65.309));
    }

    @Test
    void testRuteoGraphHopperTrelew() {
        FabricaGraphHopper.setCiudadActiva("Trelew");

        try (PlanificadorRutas planificador = new PlanificadorGraphHopper(FabricaGraphHopper.crear())) {
            assertNotNull(planificador);

            // Plaza Independencia -> Museo Paleontológico MEF
            PuntoRuta origen = new PuntoRuta(-43.251, -65.309);
            PuntoRuta destino = new PuntoRuta(-43.253, -65.310);

            var ajusteOrigen = planificador.ajustarACalle(origen, TipoVehiculo.AUTO);
            var ajusteDestino = planificador.ajustarACalle(destino, TipoVehiculo.AUTO);

            assertTrue(ajusteOrigen.isPresent(), "El origen en Trelew debe ajustar a calle");
            assertTrue(ajusteDestino.isPresent(), "El destino en Trelew debe ajustar a calle");

            RutaCalculada ruta = planificador.calcular(
                    ajusteOrigen.get(), ajusteDestino.get(), TipoVehiculo.AUTO);

            assertNotNull(ruta);
            assertTrue(ruta.distanciaMetros() > 0, "La distancia debe ser positiva");
            assertTrue(ruta.duracionMilisegundos() > 0, "El tiempo de viaje debe ser positivo");
            assertFalse(ruta.puntos().isEmpty(), "La ruta debe contener puntos de geometría");
        }
    }

    @Test
    void testRuteoGraphHopperRawson() {
        FabricaGraphHopper.setCiudadActiva("Rawson");

        try (PlanificadorRutas planificador = new PlanificadorGraphHopper(FabricaGraphHopper.crear())) {
            assertNotNull(planificador);

            // Plaza Guillermo Rawson -> Terminal de Rawson
            PuntoRuta origen = new PuntoRuta(-43.300, -65.102);
            PuntoRuta destino = new PuntoRuta(-43.304, -65.099);

            var ajusteOrigen = planificador.ajustarACalle(origen, TipoVehiculo.AUTO);
            var ajusteDestino = planificador.ajustarACalle(destino, TipoVehiculo.AUTO);

            assertTrue(ajusteOrigen.isPresent(), "El origen en Rawson debe ajustar a calle");
            assertTrue(ajusteDestino.isPresent(), "El destino en Rawson debe ajustar a calle");

            RutaCalculada ruta = planificador.calcular(
                    ajusteOrigen.get(), ajusteDestino.get(), TipoVehiculo.AUTO);

            assertNotNull(ruta);
            assertTrue(ruta.distanciaMetros() > 0);
            assertTrue(ruta.duracionMilisegundos() > 0);
        }
    }
}
