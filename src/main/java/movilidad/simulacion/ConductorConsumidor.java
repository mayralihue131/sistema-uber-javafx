package movilidad.simulacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import movilidad.logica.MovilidadDigital;
import movilidad.modelo.CalificacionViaje;
import movilidad.modelo.EstadoConductor;
import movilidad.modelo.RolUsuario;
import movilidad.modelo.Usuario;
import movilidad.modelo.Viaje;

/**
 * Hilo consumidor que representa a un chofer en la plataforma.
 * Consume instancias reales de {@link Viaje} del {@link BufferViajes} e interactúa directamente
 * con {@link MovilidadDigital} transitando los estados del modelo (ACEPTADO, INICIADO, FINALIZADO).
 */
public class ConductorConsumidor implements Runnable {
    private final MovilidadDigital sistema;
    private final Usuario conductor;
    private final BufferViajes buffer;
    private final Random random;
    private int viajesCompletados;

    /**
     * Construye un nuevo conductor consumidor asociado a un {@link Usuario} del modelo.
     *
     * @param sistema   instancia de lógica de movilidad
     * @param conductor usuario con rol de conductor dado de alta y vehículo asignado
     * @param buffer    búfer monitor compartido
     * @throws IllegalArgumentException si algún argumento es nulo
     */
    public ConductorConsumidor(MovilidadDigital sistema, Usuario conductor, BufferViajes buffer) {
        if (sistema == null || conductor == null || buffer == null) {
            throw new IllegalArgumentException("Los parámetros del conductor no pueden ser nulos.");
        }
        this.sistema = sistema;
        this.conductor = conductor;
        this.buffer = buffer;
        this.random = new Random();
        this.viajesCompletados = 0;
    }

    public Usuario getConductor() {
        return conductor;
    }

    public int getViajesCompletados() {
        return viajesCompletados;
    }

    @Override
    public void run() {
        try {
            // Asegura que el conductor comience disponible en el modelo
            synchronized (sistema) {
                if (conductor.getConductor() != null) {
                    if (conductor.getRolActivo() != RolUsuario.CONDUCTOR) {
                        sistema.cambiarRolActivo(conductor, RolUsuario.CONDUCTOR);
                    }
                    sistema.ponerDisponible(conductor);
                }
            }

            while (buffer.isSimulacionActiva() && !Thread.currentThread().isInterrupted()) {
                Viaje viaje = buffer.tomarViajeCompatible(conductor, sistema);
                if (viaje == null) break;

                synchronized (sistema) {
                    sistema.aceptarViaje(viaje.getId(), conductor, LocalDateTime.now());
                }

                String srv = (viaje.getServicio() != null) ? viaje.getServicio().getNombre() : "General";
                System.out.println("[CONDUCTOR - " + conductor.getNombre() + "] Aceptó: Viaje #"
                        + viaje.getId().toString().substring(0, 8) + " (" + srv + ")");

                // Simula tiempo de traslado hacia el origen del pasajero (80 a 250 ms)
                Thread.sleep(random.nextInt(170) + 80);

                synchronized (sistema) {
                    sistema.iniciarViaje(viaje.getId(), conductor, LocalDateTime.now());
                }
                System.out.println("[CONDUCTOR - " + conductor.getNombre() + "] Iniciado traslado de: "
                        + viaje.getCliente().getNombre());

                // Simula tiempo de viaje hacia el destino (100 a 350 ms)
                Thread.sleep(random.nextInt(250) + 100);

                synchronized (sistema) {
                    viaje.setCalificacionCliente(CalificacionViaje.EXCELENTE);
                    viaje.setCalificacionConductor(CalificacionViaje.EXCELENTE);
                    sistema.finalizarViaje(viaje.getId(), conductor, LocalDateTime.now());
                }
                viajesCompletados++;
                System.out.println("[CONDUCTOR - " + conductor.getNombre() + "] Completó: Viaje #"
                        + viaje.getId().toString().substring(0, 8) + " | Total chofer: " + viajesCompletados);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[CONDUCTOR - " + conductor.getNombre() + "] Turno finalizado por interrupción.");
        } catch (Exception e) {
            System.err.println("[CONDUCTOR - " + conductor.getNombre() + "] Error operativo: " + e.getMessage());
        } finally {
            synchronized (sistema) {
                if (conductor.getConductor() != null && conductor.getConductor().getEstadoConductor() == EstadoConductor.DISPONIBLE) {
                    try {
                        sistema.ponerFueraDeServicio(conductor);
                    } catch (Exception ignored) {}
                }
            }
            System.out.println("[CONDUCTOR - " + conductor.getNombre() + "] Desconectado.");
        }
    }
}
