package movilidad.testPrueba;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import movilidad.logica.MovilidadDigital;
import movilidad.modelo.*;

public class MovilidadDigitalTest {

    private MovilidadDigital app;
    private Usuario cliente;
    private Servicio servicio;
    private Ubicacion origen;
    private Ubicacion destino;

    @BeforeEach
    void preparar() {
        app = new MovilidadDigital();

        cliente = new Usuario(
                "Ana", "2801234567", "ana@mail.com");

        servicio = new Servicio(
                "Estándar", 2000, 200, 100,
                CategoriaVehiculo.ESTANDAR,
                TipoVehiculo.AUTO,
                TipoServicio.PASAJEROS);

        origen = new Ubicacion(-42.76, -65.03);
        destino = new Ubicacion(-42.78, -65.05);

        app.agregarUsuario(cliente);
        app.agregarServicio(servicio);
    }

    @Test
    void registrarUsuarioCorrectamente() {
        assertEquals(1, app.getUsuarios().size());
        assertSame(cliente,
                app.buscarUsuarioPorMail("ana@mail.com"));
    }

    @Test
    void rechazarUsuarioNull() {
        assertThrows(IllegalArgumentException.class,
                () -> app.agregarUsuario(null));

        assertEquals(1, app.getUsuarios().size());
    }

    @Test
    void rechazarEmailDuplicado() {
        Usuario repetido = new Usuario(
                "Otra persona", "2807654321", "ANA@mail.com");

        assertThrows(IllegalArgumentException.class,
                () -> app.agregarUsuario(repetido));

        assertEquals(1, app.getUsuarios().size());
    }

    @Test
    void solicitarViajeCorrectamente() {
        Viaje viaje = app.solicitarViaje(
                cliente, servicio, origen, destino,
                LocalDateTime.of(2026, 10, 7, 18, 0));

        assertEquals(EstadoViaje.SOLICITADO,
                viaje.estadoActual());

        assertEquals(1, app.getViajes().size());

        // El mismo viaje debe aparecer una sola vez.
        assertEquals(1, cliente.getCliente().getViajes().size());
        assertSame(viaje, cliente.getCliente().getViajes().get(0));
    }

    @Test
    void rechazarClienteNoRegistrado() {
        Usuario desconocido = new Usuario(
                "Pedro", "2809999999", "pedro@mail.com");

        assertThrows(IllegalArgumentException.class,
                () -> app.solicitarViaje(
                        desconocido, servicio, origen, destino,
                        LocalDateTime.of(2026, 10, 7, 18, 0)));

        assertTrue(app.getViajes().isEmpty());
    }
}