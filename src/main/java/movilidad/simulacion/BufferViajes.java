package movilidad.simulacion;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import movilidad.modelo.Viaje;

/**
 * Monitor concurrente que implementa un búfer acotado de solicitudes de viajes ({@link Viaje})
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
    private final Queue<Viaje> colaViajes;
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
     * Agrega una solicitud de viaje real al búfer. Si la cola está llena, el hilo productor
     * espera pasivamente hasta que un conductor libere un espacio.
     *
     * @param viaje entidad del viaje a encolar
     * @throws InterruptedException si el hilo es interrumpido durante la espera
     */
    public void agregarViaje(Viaje viaje) throws InterruptedException {
        if (viaje == null) return;
        lock.lock();
        try {
            while (colaViajes.size() == capacidadMaxima && simulacionActiva) {
                noLleno.await();
            }
            if (!simulacionActiva) return;

            colaViajes.add(viaje);
            String servicioNombre = (viaje.getServicio() != null) ? viaje.getServicio().getNombre() : "General";
            System.out.println("[BUFFER] Nuevo viaje en espera: Viaje #"
                    + viaje.getId().toString().substring(0, 8)
                    + " (" + servicioNombre + ") | Pendientes: " + colaViajes.size());
            noVacio.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retira y devuelve una solicitud de viaje pendiente para que sea atendida por un conductor.
     * Si la cola está vacía y la simulación está activa, el conductor espera pasivamente.
     *
     * @return la entidad {@link Viaje} retirada, o {@code null} si la simulación ha finalizado
     * @throws InterruptedException si el hilo es interrumpido durante la espera
     */
    public Viaje tomarViaje() throws InterruptedException {
        lock.lock();
        try {
            while (colaViajes.isEmpty() && simulacionActiva) {
                noVacio.await();
            }
            if (!simulacionActiva && colaViajes.isEmpty()) {
                return null;
            }
            Viaje viaje = colaViajes.poll();
            noLleno.signal();
            return viaje;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retira el primer viaje encolado compatible con el conductor especificado según las reglas
     * de {@link movilidad.logica.MovilidadDigital}. Si no hay viajes compatibles en el momento,
     * espera pasivamente la llegada de nuevas solicitudes.
     *
     * @param conductor usuario conductor que solicita el viaje
     * @param sistema   instancia de lógica de movilidad para verificación de compatibilidad
     * @return viaje asignado compatible, o {@code null} si la simulación se detuvo
     * @throws InterruptedException si el hilo es interrumpido
     */
    public Viaje tomarViajeCompatible(movilidad.modelo.Usuario conductor, movilidad.logica.MovilidadDigital sistema)
            throws InterruptedException {
        lock.lock();
        try {
            while (simulacionActiva) {
                if (colaViajes.isEmpty()) {
                    noVacio.await();
                    continue;
                }

                java.util.Iterator<Viaje> iter = colaViajes.iterator();
                while (iter.hasNext()) {
                    Viaje candidato = iter.next();
                    boolean compatible;
                    synchronized (sistema) {
                        compatible = sistema.buscarConductoresDisponibles(candidato).contains(conductor);
                    }
                    if (compatible) {
                        iter.remove();
                        noLleno.signal();
                        return candidato;
                    }
                }

                // Hay viajes pero ninguno compatible con este chofer; espera brevemente a nuevos pedidos
                noVacio.await(200, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            return null;
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
            noVacio.signalAll();
            noLleno.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Devuelve la capacidad máxima configurada en el búfer.
     *
     * @return capacidad máxima
     */
    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    /**
     * Devuelve la cantidad actual de viajes encolados pendientes de atención.
     *
     * @return viajes pendientes
     */
    public int getCantidadPendientes() {
        lock.lock();
        try {
            return colaViajes.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Indica si el monitor se encuentra activo admitiendo operaciones.
     *
     * @return {@code true} si está activo
     */
    public boolean isSimulacionActiva() {
        lock.lock();
        try {
            return simulacionActiva;
        } finally {
            lock.unlock();
        }
    }
}
