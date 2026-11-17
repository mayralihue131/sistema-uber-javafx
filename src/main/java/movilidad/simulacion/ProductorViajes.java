package movilidad.simulacion;

import java.util.Random;

/**
 * Hilo productor que simula la llegada continua de solicitudes de viaje de pasajeros
 * en la plataforma de movilidad.
 *
 * <p>Genera viajes con una cadencia configurable ({@code intervaloMs}) y los deposita en el
 * {@link BufferViajes} compartido. Si el búfer se llena, el productor se suspende de forma
 * segura sin consumir CPU hasta que haya lugar disponible.
 */
public class ProductorViajes implements Runnable {
    private final BufferViajes buffer;
    private final int maxViajesAGenerar;
    private final int intervaloMs;
    private final Random random;

    /**
     * Construye un nuevo productor de solicitudes.
     *
     * @param buffer             búfer monitor compartido
     * @param maxViajesAGenerar  límite total de viajes que creará el hilo
     * @param intervaloMs        cadencia de generación en milisegundos
     * @throws IllegalArgumentException si el búfer es nulo
     */
    public ProductorViajes(BufferViajes buffer, int maxViajesAGenerar, int intervaloMs) {
        if (buffer == null) throw new IllegalArgumentException("El buffer no puede ser nulo.");
        this.buffer = buffer;
        this.maxViajesAGenerar = maxViajesAGenerar;
        this.intervaloMs = intervaloMs;
        this.random = new Random();
    }

    @Override
    public void run() {
        int contadorViajes = 1;
        try {
            while (buffer.isSimulacionActiva() && !Thread.currentThread().isInterrupted() && contadorViajes <= maxViajesAGenerar) {
                // Aquí instanciarías tu objeto Viaje real de tu modelo
                String nuevoViaje = "Viaje #" + contadorViajes + " (Distancia: " + (random.nextInt(15) + 1) + " km)";
                
                buffer.agregarViaje(nuevoViaje);
                contadorViajes++;
                
                Thread.sleep(intervaloMs); // Simula el tiempo entre pedidos de clientes
            }
            System.out.println("[PRODUCTOR] Finalizó la generación de viajes.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[PRODUCTOR] Hilo interrumpido.");
        }
    }
}