package movilidad.simulacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import movilidad.logica.MovilidadDigital;
import movilidad.modelo.RolUsuario;
import movilidad.modelo.Servicio;
import movilidad.modelo.Ubicacion;
import movilidad.modelo.Usuario;
import movilidad.modelo.Viaje;

/**
 * Hilo productor que simula la llegada continua de solicitudes reales de viaje
 * de pasajeros en la plataforma {@link MovilidadDigital}.
 *
 * <p>Genera viajes con entidades del modelo de dominio y los deposita en el
 * {@link BufferViajes} compartido. Si el búfer se llena, el productor se suspende de forma
 * segura sin consumir CPU hasta que haya lugar disponible.
 */
public class ProductorViajes implements Runnable {
    private final MovilidadDigital sistema;
    private final BufferViajes buffer;
    private final int maxViajesAGenerar;
    private final int intervaloMs;
    private final Random random;

    /**
     * Construye un nuevo productor de solicitudes integrado al modelo de dominio.
     *
     * @param sistema            instancia central de lógica de movilidad
     * @param buffer             búfer monitor compartido
     * @param maxViajesAGenerar  límite total de viajes que creará el hilo
     * @param intervaloMs        cadencia de generación en milisegundos
     * @throws IllegalArgumentException si el sistema o el búfer son nulos
     */
    public ProductorViajes(MovilidadDigital sistema, BufferViajes buffer, int maxViajesAGenerar, int intervaloMs) {
        if (sistema == null) throw new IllegalArgumentException("El sistema no puede ser nulo.");
        if (buffer == null) throw new IllegalArgumentException("El buffer no puede ser nulo.");
        this.sistema = sistema;
        this.buffer = buffer;
        this.maxViajesAGenerar = maxViajesAGenerar;
        this.intervaloMs = intervaloMs;
        this.random = new Random();
    }

    @Override
    public void run() {
        int contador = 1;
        try {
            while (buffer.isSimulacionActiva() && !Thread.currentThread().isInterrupted() && contador <= maxViajesAGenerar) {
                Viaje nuevoViaje;
                synchronized (sistema) {
                    Usuario cliente = obtenerOcrearClienteDisponible(contador);
                    Servicio servicio = seleccionarServicio();
                    Ubicacion origen = generarUbicacionAleatoria();
                    Ubicacion destino = generarUbicacionAleatoria();

                    nuevoViaje = sistema.solicitarViaje(cliente, servicio, origen, destino, LocalDateTime.now());
                }

                buffer.agregarViaje(nuevoViaje);
                contador++;

                Thread.sleep(intervaloMs); // Simula el tiempo entre pedidos de clientes
            }
            System.out.println("[PRODUCTOR] Finalizó la generación de viajes.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[PRODUCTOR] Hilo interrumpido.");
        } catch (Exception e) {
            System.err.println("[PRODUCTOR] Error al generar viaje: " + e.getMessage());
        }
    }

    private Usuario obtenerOcrearClienteDisponible(int indice) {
        List<Usuario> clientes = sistema.getUsuarios().stream()
                .filter(u -> u.getRolActivo() == RolUsuario.CLIENTE && !u.getCliente().enViaje())
                .toList();

        if (!clientes.isEmpty()) {
            return clientes.get(random.nextInt(clientes.size()));
        }

        // Si todos los clientes están en viaje, se crea un nuevo cliente para la simulación
        Usuario nuevo = new Usuario("Pasajero Sim " + indice, "+54911000" + indice, "pasajero" + indice + "@sim.com");
        sistema.agregarUsuario(nuevo);
        return nuevo;
    }

    private Servicio seleccionarServicio() {
        List<Servicio> servicios = sistema.getServicios();
        if (servicios.isEmpty()) {
            throw new IllegalStateException("No hay servicios configurados en el sistema.");
        }
        return servicios.get(random.nextInt(servicios.size()));
    }

    private Ubicacion generarUbicacionAleatoria() {
        double minLat = -42.86;
        double maxLat = -42.65;
        double minLon = -65.06;
        double maxLon = -64.88;

        double lat = minLat + (maxLat - minLat) * random.nextDouble();
        double lon = minLon + (maxLon - minLon) * random.nextDouble();
        return new Ubicacion(lat, lon);
    }
}
