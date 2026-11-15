package movilidad.simulacion;

import java.util.Random;

public class ProductorViajes implements Runnable {
    private final BufferViajes buffer;
    private final int maxViajesAGenerar;
    private final int intervaloMs;
    private final Random random;

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