package movilidad.test;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import movilidad.logica.MovilidadDigital;
import movilidad.modelo.*;
import movilidad.simulacion.*;
import movilidad.aplicacion.AplicacionConsultas;
import movilidad.dao.txt.CargaParametros;

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
    public void testCargarConductoresDisponiblesPrimero() {
        MovilidadDigital sistema = new MovilidadDigital();
        Servicio s = new Servicio("Estandar", 2000, 200, 100, TipoVehiculo.AUTO, CategoriaVehiculo.ESTANDAR, TipoServicio.PASAJEROS);
        sistema.agregarServicio(s);

        // Crear chofer inicial
        Usuario c1 = new Usuario("Chofer Base", "+549112233", "choferbase@test.com");
        Vehiculo v1 = new Vehiculo("BASE01", "Cruze", 4, TipoVehiculo.AUTO, CategoriaVehiculo.ESTANDAR, TipoServicio.PASAJEROS);
        sistema.agregarUsuario(c1);
        sistema.registrarConductor(c1, "B1", v1);

        // Simulador configurado para 3 conductores
        SimuladorUber sim = new SimuladorUber(sistema, 5, 3, 10, 5, 200);

        // Cargar conductores disponibles primero
        java.util.List<Usuario> disponibles = sim.cargarConductoresDisponibles();

        // Debe haber cargado exactamente los 3 conductores requeridos
        assertEquals(3, disponibles.size());
        assertEquals(3, sim.getConductoresCargados().size());

        // Cada conductor debe estar en rol CONDUCTOR, estado DISPONIBLE y con vehículo activo
        for (Usuario cond : disponibles) {
            assertEquals(RolUsuario.CONDUCTOR, cond.getRolActivo());
            assertNotNull(cond.getConductor());
            assertEquals(EstadoConductor.DISPONIBLE, cond.getConductor().getEstadoConductor());
            assertNotNull(cond.getConductor().getVehiculoActivo());
        }
    }

    @Test
    public void testSolicitarMotoApareceEnPerfilConductor() {
        MovilidadDigital sistema = new MovilidadDigital();
        Servicio moto = new Servicio("Mototaxi", 1000, 100, 50, TipoVehiculo.MOTO, CategoriaVehiculo.ESTANDAR, TipoServicio.PASAJEROS);
        sistema.agregarServicio(moto);

        Usuario choferMoto = new Usuario("Chofer Moto", "111", "moto@test.com");
        Vehiculo vehiculoMoto = new Vehiculo("MOTO01", "Honda Wave", 1, TipoVehiculo.MOTO, CategoriaVehiculo.ESTANDAR, TipoServicio.PASAJEROS);
        sistema.agregarUsuario(choferMoto);
        sistema.registrarConductor(choferMoto, "A1", vehiculoMoto);
        sistema.ponerDisponible(choferMoto);

        Usuario cliente = new Usuario("Cliente", "222", "cliente@test.com");
        sistema.agregarUsuario(cliente);

        Viaje viaje = sistema.solicitarViaje(cliente, moto, new Ubicacion(-42.76, -65.03), new Ubicacion(-42.78, -65.04), LocalDateTime.now());

        java.util.List<Viaje> solicitudes = sistema.obtenerSolicitudesDisponibles(choferMoto);
        assertFalse(solicitudes.isEmpty(), "La solicitud de moto debe aparecer para el conductor de moto");
    }

    @Test
    public void testLimpiarTestRowsEnBD() {
        try {
            java.sql.Connection con = movilidad.dao.bd.ConexionBD.obtenerConexion();
            try (java.sql.Statement st = con.createStatement()) {
                try (java.sql.ResultSet rs = st.executeQuery("SELECT email, patente FROM conductor_vehiculo ORDER BY email, patente")) {
                    System.out.println("=== CONDUCTOR_VEHICULO EN BD ===");
                    while (rs.next()) {
                        System.out.println("CV: " + rs.getString("email") + " -> " + rs.getString("patente"));
                    }
                }
                // Eliminar registros temporales de tests
                st.executeUpdate("DELETE FROM conductor_vehiculo WHERE email IN ('prueba@example.com', 'moto@example.com', 'cliente_moto@test.com', 'motoqueiro@test.com')");
                st.executeUpdate("DELETE FROM usuario WHERE email IN ('prueba@example.com', 'moto@example.com', 'cliente_moto@test.com', 'motoqueiro@test.com')");
                st.executeUpdate("DELETE FROM vehiculo WHERE patente IN ('XX123YY', 'MO123TO', 'MOT999')");
            }
        } catch (Exception e) {
            System.out.println("Omitiendo limpieza de BD por falta de conexión remota: " + e.getMessage());
        }
    }

    @Test
    public void testSolicitarMotoApareceEnPerfilConductorConBD() throws Exception {
        MovilidadDigital m = new MovilidadDigital();
        CargaParametros.parametros();
        AplicacionConsultas.cargarDatos(m);

        Usuario cliente1 = m.buscarUsuarioPorMail("carlos.rodriguez@email.com");
        Usuario cliente2 = m.buscarUsuarioPorMail("maria.gomez@email.com");
        Usuario choferMoto = m.buscarUsuarioPorMail("manuel.rios@email.com");

        assertNotNull(cliente1, "Carlos debe existir");
        assertNotNull(cliente2, "María debe existir");
        assertNotNull(choferMoto, "Manuel Rios (chofer moto) debe existir");

        // Buscar servicios de moto
        Servicio mototaxi = m.getServicios().stream()
                .filter(s -> s.getNombre().equalsIgnoreCase("Mototaxi"))
                .findFirst().orElseThrow(() -> new AssertionError("Mototaxi debe existir"));

        Servicio mensajeria = m.getServicios().stream()
                .filter(s -> s.getNombre().equalsIgnoreCase("Mensajería"))
                .findFirst().orElseThrow(() -> new AssertionError("Mensajería debe existir"));

        // 1. Cliente 1 solicita Mototaxi (PASAJEROS)
        Viaje vMototaxi = m.solicitarViaje(cliente1, mototaxi,
                new Ubicacion(-42.76, -65.03), new Ubicacion(-42.78, -65.04), LocalDateTime.now());

        // Verificar que aparezca en el perfil de Manuel Rios
        java.util.List<Viaje> solicitudesManuel = m.obtenerSolicitudesDisponibles(choferMoto);
        assertTrue(solicitudesManuel.contains(vMototaxi),
                "La solicitud de Mototaxi debe aparecer en el panel del chofer de moto Manuel Rios");

        // 2. Cliente 2 solicita Mensajería (ENVIOS)
        Viaje vMensajeria = m.solicitarViaje(cliente2, mensajeria,
                new Ubicacion(-42.76, -65.03), new Ubicacion(-42.78, -65.04), LocalDateTime.now());

        solicitudesManuel = m.obtenerSolicitudesDisponibles(choferMoto);
        assertTrue(solicitudesManuel.contains(vMensajeria),
                "La solicitud de Mensajería debe aparecer en el panel del chofer de moto Manuel Rios");

        // 3. Manuel acepta el viaje de Mototaxi
        m.aceptarViaje(vMototaxi.getId(), choferMoto, LocalDateTime.now());
        assertEquals(EstadoViaje.ACEPTADO, vMototaxi.estadoActual());
        assertEquals(choferMoto, vMototaxi.getConductor());
        assertEquals(choferMoto.getConductor().getVehiculoActivo(), vMototaxi.getVehiculo());
    }

    @Test
    public void testSimulacionHoraPicoAltaDemanda() {
        // Escenario de "hora pico" utilizando el modelo real de dominio:
        // Productor genera viajes reales en MovilidadDigital, encolando en BufferViajes(3)
        // y conductores concurrentes atienden, transicionan y finalizan los viajes en el modelo.
        SimuladorUber simuladorHoraPico = new SimuladorUber(
            3,   // Duración: 3 segundos
            2,   // 2 conductores atendiendo viajes
            10,  // 10 viajes generados rápidamente
            3,   // Capacidad reducida de espera
            60   // Cadencia rápida de pedidos
        );

        assertDoesNotThrow(() -> simuladorHoraPico.iniciarSimulacion());
        assertTrue(simuladorHoraPico.getTotalViajesCompletados() > 0, "Debe haber completado al menos un viaje");
    }
}
