package movilidad.simulacion;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimuladorUber {
    // Parámetros fácilmente configurables según consigna
    private int duracionSimulacionSegundos;
    private int cantidadConductores;
    private int maxViajes;
    private int capacidadCola;
    private int intervaloGeneracionMs;

    public SimuladorUber() {
        this(15, 3, 20, 5, 800);
    }

    public SimuladorUber(int duracionSimulacionSegundos, int cantidadConductores, int maxViajes, int capacidadCola, int intervaloGeneracionMs) {
        this.duracionSimulacionSegundos = duracionSimulacionSegundos;
        this.cantidadConductores = cantidadConductores;
        this.maxViajes = maxViajes;
        this.capacidadCola = capacidadCola;
        this.intervaloGeneracionMs = intervaloGeneracionMs;
    }

    public int getDuracionSimulacionSegundos() {
        return duracionSimulacionSegundos;
    }

    public void setDuracionSimulacionSegundos(int duracionSimulacionSegundos) {
        this.duracionSimulacionSegundos = duracionSimulacionSegundos;
    }

    public int getCantidadConductores() {
        return cantidadConductores;
    }

    public void setCantidadConductores(int cantidadConductores) {
        this.cantidadConductores = cantidadConductores;
    }

    public int getMaxViajes() {
        return maxViajes;
    }

    public void setMaxViajes(int maxViajes) {
        this.maxViajes = maxViajes;
    }

    public int getCapacidadCola() {
        return capacidadCola;
    }

    public void setCapacidadCola(int capacidadCola) {
        this.capacidadCola = capacidadCola;
    }

    public int getIntervaloGeneracionMs() {
        return intervaloGeneracionMs;
    }

    public void setIntervaloGeneracionMs(int intervaloGeneracionMs) {
        this.intervaloGeneracionMs = intervaloGeneracionMs;
    }

    public void iniciarSimulacion() {
        System.out.println("=== INICIANDO SIMULACIÓN UBER ===");
        System.out.println("Duración: " + duracionSimulacionSegundos + "s | Conductores: " + cantidadConductores + " | Max Viajes: " + maxViajes);

        BufferViajes buffer = new BufferViajes(capacidadCola);
        
        // Creamos un pool con 1 hilo para el productor + N hilos para los conductores
        ExecutorService executor = Executors.newFixedThreadPool(cantidadConductores + 1);

        // 1. Lanzar hilo productor
        ProductorViajes productor = new ProductorViajes(buffer, maxViajes, intervaloGeneracionMs);
        executor.execute(productor);

        // 2. Lanzar un hilo por cada conductor
        for (int i = 1; i <= cantidadConductores; i++) {
            ConductorConsumidor conductor = new ConductorConsumidor("Conductor-" + i, buffer);
            executor.execute(conductor);
        }

        // 3. Controlar el tiempo de ejecución de la simulación
        try {
            Thread.sleep(duracionSimulacionSegundos * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            detenerSimulacion(buffer, executor);
        }
    }

    private void detenerSimulacion(BufferViajes buffer, ExecutorService executor) {
        System.out.println("\n=== TIEMPO CUMPLIDO: DETENIENDO SIMULACIÓN ===");
        buffer.detener(); // Libera hilos en espera en las Conditions
        executor.shutdownNow(); // Interrumpe hilos activos o durmiendo
        
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                System.err.println("Algunos hilos tardaron en cerrarse.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("=== SIMULACIÓN FINALIZADA ===");
    }

    // Método main de prueba rápida (luego puedes llamarlo desde un botón en JavaFX)
    public static void main(String[] args) {
        SimuladorUber simulador = new SimuladorUber(
            15,   // Duración: 15 segundos
            3,    // 3 conductores simultáneos
            20,   // Máximo 20 viajes generados
            5,    // Capacidad máxima de 5 viajes en espera
            800   // Se genera un viaje cada 800 ms
        );
        simulador.iniciarSimulacion();
    }
}