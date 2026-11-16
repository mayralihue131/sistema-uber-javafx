package movilidad.simulacion;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
// Importa aquí tu clase Viaje del modelo, ej: import modelo.Viaje;

public class BufferViajes {
    private final Queue<String> colaViajes; // Cambia String por tu clase Viaje
    private final int capacidadMaxima;
    private final Lock lock;
    private final Condition noLleno;
    private final Condition noVacio;
    private boolean simulacionActiva;

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

    public boolean isSimulacionActiva() {
        lock.lock();
        try {
            return simulacionActiva;
        } finally {
            lock.unlock();
        }
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public int getCantidadPendientes() {
        lock.lock();
        try {
            return colaViajes.size();
        } finally {
            lock.unlock();
        }
    }
}