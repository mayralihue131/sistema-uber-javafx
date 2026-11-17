package movilidad.simulacion;

import java.util.Random;

/**
 * Hilo consumidor que representa a un conductor en la simulación concurrente.
 *
 * <p>Retira solicitudes de viajes desde el {@link BufferViajes}, simula el tiempo de traslado
 * mediante pausas controladas ({@link Thread#sleep}) y finaliza su ejecución cuando el búfer
 * es detenido o cuando el hilo recibe una señal de interrupción.
 */
public class ConductorConsumidor implements Runnable {
    private final String nombreConductor;
    private final BufferViajes buffer;
    private final Random random;

    /**
     * Construye un nuevo conductor consumidor.
     *
     * @param nombreConductor nombre identificador del chofer
     * @param buffer          instancia compartida del monitor de viajes
     * @throws IllegalArgumentException si alguno de los parámetros es nulo
     */
    public ConductorConsumidor(String nombreConductor, BufferViajes buffer) {
        if (nombreConductor == null || buffer == null) {
            throw new IllegalArgumentException("Los parámetros del conductor no pueden ser nulos.");
        }
        this.nombreConductor = nombreConductor;
        this.buffer = buffer;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            while (buffer.isSimulacionActiva() && !Thread.currentThread().isInterrupted()) {
                String viajeAsignado = buffer.tomarViaje();
                
                if (viajeAsignado != null) {
                    System.out.println("[CONDUCTOR - " + nombreConductor + "] Aceptó: " + viajeAsignado);
                    
                    // Simula el tiempo que tarda en completar el viaje (ej. entre 1 y 3 segundos)
                    int duracionViajeMs = (random.nextInt(3) + 1) * 1000;
                    Thread.sleep(duracionViajeMs);
                    
                    System.out.println("[CONDUCTOR - " + nombreConductor + "] Completó: " + viajeAsignado);
                } else {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[CONDUCTOR - " + nombreConductor + "] Turno finalizado por interrupción.");
        }
        System.out.println("[CONDUCTOR - " + nombreConductor + "] Desconectado.");
    }
}