package movilidad.simulacion;

import java.util.Random;

public class ConductorConsumidor implements Runnable {
    private final String nombreConductor; // Puedes pasar tu objeto Conductor aquí
    private final BufferViajes buffer;
    private final Random random;

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
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[CONDUCTOR - " + nombreConductor + "] Turno finalizado por interrupción.");
        }
        System.out.println("[CONDUCTOR - " + nombreConductor + "] Desconectado.");
    }
}