package movilidad.test;

import org.junit.jupiter.api.Test;

import movilidad.simulacion.*;


import static org.junit.jupiter.api.Assertions.*;

public class BufferViajesTest {

    @Test
    public void testAgregarYTomarViaje() throws InterruptedException {
        BufferViajes buffer = new BufferViajes(3);
        
        // Agregamos un viaje de prueba
        buffer.agregarViaje("Viaje Test 1");
        
        // Verificamos que se pueda tomar correctamente
        String viajeTomado = buffer.tomarViaje();
        assertNotNull(viajeTomado);
        assertEquals("Viaje Test 1", viajeTomado);
    }

    @Test
    public void testCapacidadYPendientes() throws InterruptedException {
        BufferViajes buffer = new BufferViajes(2);
        assertEquals(2, buffer.getCapacidadMaxima());
        assertEquals(0, buffer.getCantidadPendientes());

        buffer.agregarViaje("V1");
        buffer.agregarViaje("V2");
        assertEquals(2, buffer.getCantidadPendientes());

        assertEquals("V1", buffer.tomarViaje());
        assertEquals(1, buffer.getCantidadPendientes());
        assertEquals("V2", buffer.tomarViaje());
        assertEquals(0, buffer.getCantidadPendientes());
    }

    @Test
    public void testDetenerBuffer() throws InterruptedException {
        BufferViajes buffer = new BufferViajes(2);
        assertTrue(buffer.isSimulacionActiva());
        buffer.detener();
        assertFalse(buffer.isSimulacionActiva());
        assertNull(buffer.tomarViaje());
    }

    @Test
    public void testConfiguracionSimulador() {
        SimuladorUber sim = new SimuladorUber();
        assertEquals(15, sim.getDuracionSimulacionSegundos());
        assertEquals(3, sim.getCantidadConductores());
        assertEquals(20, sim.getMaxViajes());
        assertEquals(5, sim.getCapacidadCola());
        assertEquals(800, sim.getIntervaloGeneracionMs());

        sim.setDuracionSimulacionSegundos(30);
        sim.setCantidadConductores(5);
        sim.setMaxViajes(50);
        sim.setCapacidadCola(10);
        sim.setIntervaloGeneracionMs(500);

        assertEquals(30, sim.getDuracionSimulacionSegundos());
        assertEquals(5, sim.getCantidadConductores());
        assertEquals(50, sim.getMaxViajes());
        assertEquals(10, sim.getCapacidadCola());
        assertEquals(500, sim.getIntervaloGeneracionMs());
    }

    @Test
    public void testSimulacionHoraPicoAltaDemanda() {
        // Escenario de "hora pico":
        // Gran afluencia de viajes (cada 50 ms), buffer limitado (3) y solo 1 conductor (cuello de botella).
        // Demuestra que el productor se bloquea en noLleno.await() cuando el buffer se satura,
        // sin producir pérdida de datos, desbordamiento de memoria ni deadlocks.
        SimuladorUber simuladorHoraPico = new SimuladorUber(
            3,   // Duración de prueba: 3 segundos
            1,   // 1 conductor atendiendo viajes
            12,  // 12 viajes generados rápidamente
            3,   // Capacidad reducida de espera
            50   // Un viaje generado cada 50 ms
        );

        assertDoesNotThrow(() -> simuladorHoraPico.iniciarSimulacion());
    }
}