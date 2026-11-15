package movilidad.simulacion;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import movilidad.modelo.Viaje;

final class DatosSimulacion {
    private final BlockingQueue<Viaje> solicitudes = new LinkedBlockingQueue<>();
    private final long fin;
    private volatile boolean produccionTerminada;

    DatosSimulacion(int duracionSegundos) {
        fin = System.nanoTime() + TimeUnit.SECONDS.toNanos(duracionSegundos);
    }

    long tiempoRestante() {
        return fin - System.nanoTime();
    }

    void publicar(Viaje viaje) {
        solicitudes.add(viaje);
    }

    void terminarProduccion() {
        produccionTerminada = true;
    }

    boolean puedeConsumir() {
        return tiempoRestante() > 0 && (!produccionTerminada || !solicitudes.isEmpty());
    }

    Viaje esperarSolicitud() throws InterruptedException {
        long espera = Math.min(tiempoRestante(), TimeUnit.MILLISECONDS.toNanos(50));
        return espera > 0 ? solicitudes.poll(espera, TimeUnit.NANOSECONDS) : null;
    }
}
