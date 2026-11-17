package movilidad.simulacion;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitor concurrente que implementa un búfer acotado de solicitudes de viajes
 * para resolver el problema del Productor-Consumidor.
 *
 * <p>Utiliza un cerrojo reentrante equitativo ({@link ReentrantLock}) y dos variables
 * de condición ({@link Condition}):
 * <ul>
 *   <li>{@code noLleno}: suspende a los productores cuando la cola alcanza su capacidad máxima.</li>
 *   <li>{@code noVacio}: suspende a los conductores cuando no hay viajes pendientes.</li>
 * </ul>
 */
public class BufferViajes {
    private final Queue<String> colaViajes;
    private final int capacidadMaxima;
    private final Lock lock;
    private final Condition noLleno;
    private final Condition noVacio;
    private boolean simulacionActiva;

    /**
     * Crea un nuevo búfer con la capacidad máxima indicada.
     *
     * @param capacidadMaxima cantidad máxima de viajes que pueden esperar simultáneamente en la cola
     * @throws IllegalArgumentException si la capacidad es menor o igual a cero
     */
    public BufferViajes(int capacidadMaxima) {
        if (capacidadMaxima <= 0) {
            throw new IllegalArgumentException("La capacidad del buffer debe ser mayor a cero.");
        }
        this.colaViajes = new LinkedList<>();
        this.capacidadMaxima = capacidadMaxima;
        this.lock = new ReentrantLock(true); // Lock justo (FIFO)
        this.noLleno = lock.newCondition();
        this.noVacio = lock.newCondition();
        this.simulacionActiva = true;
    }

    /**
     * Agrega una solicitud de viaje al búfer. Si la cola está llena, el hilo productor
     * espera pasivamente hasta que un conductor libere un espacio.
     *
     * @param viaje identificador o descripción del viaje a encolar
     * @throws InterruptedException si el hilo es interrumpido durante la espera
     */
    public void agregarViaje(String viaje) throws InterruptedException {
        lock.lock();
        try {
            while (colaViajes.size() == capacidadMaxima && simulacionActiva) {
                noLleno.await(); // Espera si la cola está llena
            }
            if (!simulacionActiva) return;

            colaViajes.add(viaje);
            System.out.println("[BUFFER] Nuevo viaje en espera: " + viaje + " | Pendientes: " + colaViajes.size());
            noVacio.signal(); // Avisa a un conductor que hay viaje disponible
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retira y devuelve una solicitud de viaje pendiente para que sea atendida por un conductor.
     * Si la cola está vacía y la simulación está activa, el conductor espera pasivamente.
     *
     * @return el identificador del viaje retirado, o {@code null} si la simulación ha finalizado
     * @throws InterruptedException si el hilo es interrumpido durante la espera
     */
    public String tomarViaje() throws InterruptedException {
        lock.lock();
        try {
            while (colaViajes.isEmpty() && simulacionActiva) {
                noVacio.await(); // El conductor espera si no hay viajes
            }
            if (!simulacionActiva && colaViajes.isEmpty()) {
                return null;
            }
            String viaje = colaViajes.poll();
            noLleno.signal(); // Avisa al productor que se liberó un lugar
            return viaje;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Detiene la recepción de nuevos viajes y despierta a todos los hilos
     * bloqueados en las variables de condición para permitir un apagado ordenado.
     */
    public void detener() {
        lock.lock();
        try {
            this.simulacionActiva = false;
            noLleno.signalAll();
            noVacio.signalAll(); // Despierta a todos los hilos bloqueados para que puedan terminar
        } finally {
            lock.unlock();
        }
    }

    /**
     * Indica si el búfer se encuentra activo y admitiendo operaciones.
     *
     * @return {@code true} si la simulación está activa; {@code false} en caso contrario
     */
    public boolean isSimulacionActiva() {
        lock.lock();
        try {
            return simulacionActiva;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Devuelve la capacidad máxima del búfer.
     *
     * @return tamaño máximo de la cola
     */
    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    /**
     * Devuelve la cantidad de viajes que esperan ser atendidos actualmente en la cola.
     *
     * @return cantidad de solicitudes pendientes
     */
    public int getCantidadPendientes() {
        lock.lock();
        try {
            return colaViajes.size();
        } finally {
            lock.unlock();
        }
    }
}