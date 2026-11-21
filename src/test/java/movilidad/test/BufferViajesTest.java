package movilidad.test;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import movilidad.logica.MovilidadDigital;
import movilidad.modelo.*;
import movilidad.simulacion.*;

public class BufferViajesTest {

    private Viaje crearViajePrueba(String emailCliente) {
        Usuario cliente = new Usuario("Cliente Test", "123456", emailCliente);
        Servicio servicio = new Servicio("Estandar", 2000, 200, 100, TipoVehiculo.AUTO, CategoriaVehiculo.ESTANDAR, TipoServicio.PASAJEROS);
        Viaje v = new Viaje(cliente, new Ubicacion(-42.76, -65.03), new Ubicacion(-42.78, -65.04), servicio);
        v.solicitar(LocalDateTime.now());
        return v;
    }

    @Test
    public void testAgregarYTomarViaje() throws InterruptedException {
        BufferViajes buffer = new BufferViajes(3);

        Viaje v1 = crearViajePrueba("cliente1@test.com");
        buffer.agregarViaje(v1);

        Viaje tomado = buffer.tomarViaje();
        assertNotNull(tomado);
        assertEquals(v1.getId(), tomado.getId());
        assertEquals(EstadoViaje.SOLICITADO, tomado.estadoActual());
        assertEquals("Estandar", tomado.getServicio().getNombre());
    }

    @Test
    public void testCapacidadYPendientes() throws InterruptedException {
        BufferViajes buffer = new BufferViajes(2);
        assertEquals(2, buffer.getCapacidadMaxima());
        assertEquals(0, buffer.getCantidadPendientes());

        Viaje v1 = crearViajePrueba("c1@test.com");
        Viaje v2 = crearViajePrueba("c2@test.com");

        buffer.agregarViaje(v1);
        buffer.agregarViaje(v2);
        assertEquals(2, buffer.getCantidadPendientes());

        Viaje tomado1 = buffer.tomarViaje();
        assertEquals(v1.getId(), tomado1.getId());
        assertEquals(1, buffer.getCantidadPendientes());

        Viaje tomado2 = buffer.tomarViaje();
        assertEquals(v2.getId(), tomado2.getId());
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
    public void testCicloVidaCompletoConcurrente() throws Exception {
        MovilidadDigital sistema = new MovilidadDigital();
        Servicio servicio = new Servicio("Estandar", 2000, 200, 100, TipoVehiculo.AUTO, CategoriaVehiculo.ESTANDAR, TipoServicio.PASAJEROS);
        sistema.agregarServicio(servicio);

        Usuario chofer = new Usuario("Chofer Test", "111", "chofer@test.com");
        Vehiculo vehiculo = new Vehiculo("TEST01", "Auto Test", 4, TipoVehiculo.AUTO, CategoriaVehiculo.ESTANDAR, TipoServicio.PASAJEROS);
        sistema.agregarUsuario(chofer);
        sistema.registrarConductor(chofer, "B1", vehiculo);
        sistema.definirCategoriaAceptada(chofer, CategoriaVehiculo.ESTANDAR);
        sistema.cambiarRolActivo(chofer, RolUsuario.CONDUCTOR);
        sistema.ponerDisponible(chofer);

        Usuario cliente = new Usuario("Cliente Test", "222", "cliente@test.com");
        sistema.agregarUsuario(cliente);

        BufferViajes buffer = new BufferViajes(2);
        ConductorConsumidor consumidor = new ConductorConsumidor(sistema, chofer, buffer);
        Thread hiloChofer = new Thread(consumidor);
        hiloChofer.start();

        Viaje viaje = sistema.solicitarViaje(cliente, servicio, new Ubicacion(-42.76, -65.03), new Ubicacion(-42.78, -65.04), LocalDateTime.now());
        buffer.agregarViaje(viaje);

        // Esperar a que el chofer lo procese completamente
        int intentos = 0;
        while (viaje.estadoActual() != EstadoViaje.FINALIZADO && intentos < 30) {
            Thread.sleep(100);
            intentos++;
        }

        buffer.detener();
        hiloChofer.join(1000);

        assertEquals(EstadoViaje.FINALIZADO, viaje.estadoActual());
        assertEquals(chofer, viaje.getConductor());
        assertEquals(vehiculo, viaje.getVehiculo());
        assertEquals(4, viaje.getRegistroViaje().size()); // SOLICITADO, ACEPTADO, INICIADO, FINALIZADO
        assertEquals(EstadoConductor.FUERA_DE_SERVICIO, chofer.getConductor().getEstadoConductor());
    }

    @Test
    public void testSimulacionHoraPicoAltaDemanda() {
        // Escenario de "hora pico" utilizando el modelo real de dominio:
        // Productor genera viajes reales en MovilidadDigital, encolando en BufferViajes(3)
        // y 1 conductor concurrente atiende, transiciona y finaliza los viajes en el modelo.
        SimuladorUber simuladorHoraPico = new SimuladorUber(
            3,   // Duración: 3 segundos
            1,   // 1 conductor atendiendo viajes
            10,  // 10 viajes generados rápidamente
            3,   // Capacidad reducida de espera
            60   // Cadencia rápida de pedidos
        );

        assertDoesNotThrow(() -> simuladorHoraPico.iniciarSimulacion());
    }
}
