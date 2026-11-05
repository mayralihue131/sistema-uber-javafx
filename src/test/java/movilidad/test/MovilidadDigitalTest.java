package movilidad.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import movilidad.logica.MovilidadDigital;
import movilidad.modelo.CalificacionViaje;
import movilidad.modelo.CategoriaVehiculo;
import movilidad.modelo.EstadoConductor;
import movilidad.modelo.EstadoViaje;
import movilidad.modelo.RolUsuario;
import movilidad.modelo.Servicio;
import movilidad.modelo.TipoServicio;
import movilidad.modelo.TipoVehiculo;
import movilidad.modelo.Ubicacion;
import movilidad.modelo.Usuario;
import movilidad.modelo.Vehiculo;
import movilidad.modelo.Viaje;

class MovilidadDigitalTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 6, 10, 0);

    private MovilidadDigital movilidad;
    private Usuario cliente;
    private Usuario conductor;
    private Servicio servicio;
    private Vehiculo vehiculo;

    @BeforeEach
    void configurarEscenario() {
        movilidad = new MovilidadDigital();
        cliente = new Usuario("Cliente", "111", "cliente@mail.com");
        conductor = new Usuario("Conductor", "222", "conductor@mail.com");
        vehiculo = new Vehiculo("AA000AA", "Sedan", 4, TipoVehiculo.AUTO,
                CategoriaVehiculo.ESTANDAR, new Ubicacion(-34.6, -58.4));
        servicio = new Servicio("Pasajeros estándar", 100, 20, 5,
                CategoriaVehiculo.ESTANDAR, TipoVehiculo.AUTO, TipoServicio.PASAJEROS);

        movilidad.agregarUsuario(cliente);
        movilidad.agregarUsuario(conductor);
        movilidad.registrarConductor(conductor, "LIC-123", vehiculo);
        movilidad.agregarTipoServicioVehiculo(conductor, vehiculo, TipoServicio.PASAJEROS);
        movilidad.ponerDisponible(conductor);
        movilidad.agregarServicio(servicio);
    }

    @Test
    void registraUsuariosYEvitaDuplicadosPorEmail() {
        assertEquals(2, movilidad.getUsuarios().size());
        assertSame(cliente, movilidad.buscarUsuarioPorMail("cliente@mail.com"));
        assertThrows(IllegalArgumentException.class, () -> movilidad.agregarUsuario(
                new Usuario("Otro cliente", "333", "cliente@mail.com")));
    }

    @Test
    void consultaServiciosPorTipoYCategoriaCompatible() {
        List<Servicio> servicios = movilidad.consultarServicios(
                TipoServicio.PASAJEROS, CategoriaVehiculo.ESTANDAR);

        assertEquals(1, servicios.size());
        assertSame(servicio, servicios.get(0));
    }

    @Test
    void solicitaAceptaIniciaYFinalizaUnViaje() {
        Viaje viaje = solicitarViaje();

        assertEquals(EstadoViaje.SOLICITADO, viaje.estadoActual());
        assertTrue(movilidad.buscarConductoresDisponibles(viaje).contains(conductor));

        movilidad.aceptarViaje(viaje.getId(), conductor, INICIO.plusMinutes(1));
        assertEquals(EstadoViaje.ACEPTADO, viaje.estadoActual());
        assertEquals(EstadoConductor.VIAJE_A_ORIGEN,
                conductor.getConductor().getEstadoConductor());

        movilidad.iniciarViaje(viaje.getId(), conductor, INICIO.plusMinutes(5));
        assertEquals(EstadoViaje.INICIADO, viaje.estadoActual());
        assertEquals(EstadoConductor.VIAJE_A_DESTINO,
                conductor.getConductor().getEstadoConductor());

        movilidad.finalizarViaje(viaje.getId(), conductor, INICIO.plusMinutes(20));
        assertEquals(EstadoViaje.FINALIZADO, viaje.estadoActual());

        movilidad.calificarConductor(viaje.getId(), cliente,
                CalificacionViaje.EXCELENTE);
        movilidad.calificarCliente(viaje.getId(), conductor,
                CalificacionViaje.MUY_BUENO);
        assertEquals(CalificacionViaje.EXCELENTE,
                viaje.getCalificacionConductor());
        assertEquals(CalificacionViaje.MUY_BUENO,
                viaje.getCalificacionCliente());
        assertEquals(EstadoConductor.DISPONIBLE,
                conductor.getConductor().getEstadoConductor());
    }

    @Test
    void administraEstadoDelConductorYConfiguracionDelVehiculo() {
        assertEquals(RolUsuario.CLIENTE, conductor.getRolActivo());
        assertEquals(EstadoConductor.DISPONIBLE,
                conductor.getConductor().getEstadoConductor());

        movilidad.ponerFueraDeServicio(conductor);
        assertEquals(EstadoConductor.FUERA_DE_SERVICIO,
                conductor.getConductor().getEstadoConductor());

        movilidad.definirCategoriaAceptada(conductor,
                CategoriaVehiculo.ESTANDAR);
        movilidad.ponerDisponible(conductor);
        assertEquals(EstadoConductor.DISPONIBLE,
                conductor.getConductor().getEstadoConductor());

        movilidad.ponerFueraDeServicio(conductor);
        movilidad.cambiarRolActivo(conductor, RolUsuario.CONDUCTOR);
        assertEquals(RolUsuario.CONDUCTOR, conductor.getRolActivo());
        movilidad.cambiarRolActivo(conductor, RolUsuario.CLIENTE);
        assertEquals(RolUsuario.CLIENTE, conductor.getRolActivo());
        assertEquals(EstadoConductor.FUERA_DE_SERVICIO,
                conductor.getConductor().getEstadoConductor());
    }

    @Test
    void rechazaSolicitudesQueSuperanElTiempoDeEspera() {
        Viaje viaje = solicitarViaje();

        List<Viaje> rechazadas = movilidad.rechazarSolicitudesVencidas(INICIO.plusMinutes(5));

        assertEquals(List.of(viaje), rechazadas);
        assertEquals(EstadoViaje.RECHAZADO, viaje.estadoActual());
    }

    @Test
    void cancelarAntesDeAceptarNoTieneCosto() {
        Viaje viaje = solicitarViaje();

        double costo = movilidad.cancelarViaje(viaje.getId(), cliente, INICIO.plusMinutes(1),
                "Ya no necesito el viaje", 0);

        assertEquals(0, costo);
        assertEquals(EstadoViaje.CANCELADO, viaje.estadoActual());
        assertEquals("Ya no necesito el viaje", viaje.getMotivoCancelacion());
    }

    @Test
    void cancelarViajeAceptadoCalculaCostoYLiberaAlConductor() {
        Viaje viaje = solicitarViaje();
        movilidad.aceptarViaje(viaje.getId(), conductor, INICIO.plusMinutes(1));

        double costo = movilidad.cancelarViaje(viaje.getId(), cliente,
                INICIO.plusMinutes(3), "Cambio de planes", 2.0);

        assertTrue(costo > 0);
        assertEquals(EstadoViaje.CANCELADO, viaje.estadoActual());
        assertEquals(EstadoConductor.DISPONIBLE,
                conductor.getConductor().getEstadoConductor());
    }

    @Test
    void rechazaOperacionesConViajeInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> movilidad.iniciarViaje(null, conductor, INICIO));
        assertNull(movilidad.buscarViajePorId(UUID.randomUUID()));
    }

    @Test
    void devuelveViajesPorClienteYConductor() {
        Viaje viaje = solicitarViaje();
        movilidad.aceptarViaje(viaje.getId(), conductor, INICIO.plusMinutes(1));

        assertTrue(movilidad.obtenerViajesCliente(cliente).contains(viaje));
        assertTrue(movilidad.obtenerViajesConductor(conductor).contains(viaje));
        assertNotNull(movilidad.buscarViajePorId(viaje.getId()));
        assertFalse(movilidad.obtenerViajesCliente(conductor).contains(viaje));
    }

    private Viaje solicitarViaje() {
        return movilidad.solicitarViaje(cliente, servicio,
                new Ubicacion(-34.60, -58.40),
                new Ubicacion(-34.61, -58.41),
                INICIO);
    }
}
