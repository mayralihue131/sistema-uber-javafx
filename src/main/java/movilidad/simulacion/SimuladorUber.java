package movilidad.simulacion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import movilidad.aplicacion.AplicacionConsultas;
import movilidad.dao.txt.CargaParametros;
import movilidad.logica.MovilidadDigital;
import movilidad.modelo.CategoriaVehiculo;
import movilidad.modelo.RolUsuario;
import movilidad.modelo.Servicio;
import movilidad.modelo.TipoServicio;
import movilidad.modelo.TipoVehiculo;
import movilidad.modelo.Usuario;
import movilidad.modelo.Vehiculo;

/**
 * Orquestador principal de la simulación concurrente multihilo del sistema de movilidad.
 *
 * <p>Coordina productores de viajes y conductores consumidores sobre el modelo de dominio real
 * ({@link MovilidadDigital}, {@link Usuario}, {@link movilidad.modelo.Viaje}, {@link Vehiculo}).
 * Modela escenarios de concurrencia y horas pico administrando un pool de hilos
 * ({@link ExecutorService}) y el monitor {@link BufferViajes}.
 */
public class SimuladorUber {
    private final MovilidadDigital sistema;
    private int duracionSimulacionSegundos;
    private int cantidadConductores;
    private int maxViajes;
    private int capacidadCola;
    private int intervaloGeneracionMs;

    /**
     * Construye un simulador con parámetros predeterminados:
     * 15 segundos de duración, 3 conductores, máximo 20 viajes, búfer de 5 y 800 ms de cadencia.
     */
    public SimuladorUber() {
        this(15, 3, 20, 5, 800);
    }

    /**
     * Construye un simulador con parámetros personalizados inicializando automáticamente el sistema.
     *
     * @param duracionSimulacionSegundos duración máxima en segundos antes del apagado automático
     * @param cantidadConductores        número de hilos consumidores concurrentes
     * @param maxViajes                  límite de viajes generados por el productor
     * @param capacidadCola              capacidad máxima de la cola bloqueante
     * @param intervaloGeneracionMs      tiempo en milisegundos entre solicitudes consecutivas
     */
    public SimuladorUber(int duracionSimulacionSegundos, int cantidadConductores, int maxViajes, int capacidadCola, int intervaloGeneracionMs) {
        this(crearSistemaPorDefecto(), duracionSimulacionSegundos, cantidadConductores, maxViajes, capacidadCola, intervaloGeneracionMs);
    }

    /**
     * Construye un simulador sobre una instancia de {@link MovilidadDigital} provista.
     *
     * @param sistema                    instancia central de lógica y datos
     * @param duracionSimulacionSegundos duración máxima en segundos
     * @param cantidadConductores        número de conductores concurrentes
     * @param maxViajes                  límite de solicitudes a crear
     * @param capacidadCola              capacidad del buffer monitor
     * @param intervaloGeneracionMs      tiempo entre solicitudes en ms
     */
    public SimuladorUber(MovilidadDigital sistema, int duracionSimulacionSegundos, int cantidadConductores, int maxViajes, int capacidadCola, int intervaloGeneracionMs) {
        this.sistema = (sistema != null) ? sistema : crearSistemaPorDefecto();
        this.duracionSimulacionSegundos = duracionSimulacionSegundos;
        this.cantidadConductores = cantidadConductores;
        this.maxViajes = maxViajes;
        this.capacidadCola = capacidadCola;
        this.intervaloGeneracionMs = intervaloGeneracionMs;
    }

    private static MovilidadDigital crearSistemaPorDefecto() {
        MovilidadDigital m = new MovilidadDigital();
        try {
            CargaParametros.parametros();
            AplicacionConsultas.cargarDatos(m);
        } catch (Exception e) {
            Servicio s = new Servicio("Estandar", 2000, 200, 100, TipoVehiculo.AUTO, CategoriaVehiculo.ESTANDAR, TipoServicio.PASAJEROS);
            m.agregarServicio(s);
            for (int i = 1; i <= 3; i++) {
                Usuario c = new Usuario("Chofer " + i, "111" + i, "chofer" + i + "@sim.com");
                Vehiculo v = new Vehiculo("CH" + i, "Auto " + i, 4, TipoVehiculo.AUTO, CategoriaVehiculo.ESTANDAR, TipoServicio.PASAJEROS);
                m.agregarUsuario(c);
                m.registrarConductor(c, "B1", v);
                m.definirCategoriaAceptada(c, CategoriaVehiculo.ESTANDAR);
                m.cambiarRolActivo(c, RolUsuario.CONDUCTOR);
                m.ponerDisponible(c);
            }
        }
        return m;
    }

    public MovilidadDigital getSistema() {
        return sistema;
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

    /**
     * Inicia la ejecución de la simulación concurrente, lanzando el pool de hilos
     * para el productor y los conductores interactuando con el modelo de dominio.
     */
    public void iniciarSimulacion() {
        System.out.println("=== INICIANDO SIMULACIÓN UBER (CON MODELO REAL) ===");
        System.out.println("Duración: " + duracionSimulacionSegundos + "s | Conductores: " + cantidadConductores + " | Max Viajes: " + maxViajes);

        BufferViajes buffer = new BufferViajes(capacidadCola);

        // Obtener conductores disponibles del modelo
        List<Usuario> conductoresDisponibles = new ArrayList<>(
            sistema.getUsuarios().stream()
                .filter(u -> u.getConductor() != null && u.getConductor().getVehiculoActivo() != null)
                .toList()
        );

        // Si faltan conductores en el modelo para la cantidad solicitada, registrar conductores auxiliares
        while (conductoresDisponibles.size() < cantidadConductores) {
            int idx = conductoresDisponibles.size() + 1;
            Usuario c = new Usuario("Conductor Auxiliar " + idx, "+54911000" + idx, "auxiliar" + idx + "@sim.com");
            Vehiculo v = new Vehiculo("AUX0" + idx, "Cronos", 4, TipoVehiculo.AUTO, CategoriaVehiculo.PREMIUM, TipoServicio.PASAJEROS);
            v.agregarTipoServicio(TipoServicio.ENVIOS);
            sistema.agregarUsuario(c);
            sistema.registrarConductor(c, "B1-D1", v);
            sistema.definirCategoriaAceptada(c, CategoriaVehiculo.PREMIUM);
            sistema.cambiarRolActivo(c, RolUsuario.CONDUCTOR);
            sistema.ponerDisponible(c);
            conductoresDisponibles.add(c);
        }

        List<Usuario> conductoresAsignados = conductoresDisponibles.subList(0, cantidadConductores);

        ExecutorService executor = Executors.newFixedThreadPool(cantidadConductores + 1);

        // 1. Lanzar hilo productor integrado con el modelo
        ProductorViajes productor = new ProductorViajes(sistema, buffer, maxViajes, intervaloGeneracionMs);
        executor.execute(productor);

        // 2. Lanzar hilos consumidores con conductores del modelo real
        for (Usuario cond : conductoresAsignados) {
            sistema.definirCategoriaAceptada(cond, CategoriaVehiculo.ESTANDAR);
            sistema.ponerDisponible(cond);
            ConductorConsumidor consumidor = new ConductorConsumidor(sistema, cond, buffer);
            executor.execute(consumidor);
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
        buffer.detener();
        executor.shutdownNow();

        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                System.err.println("Algunos hilos tardaron en cerrarse.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("=== SIMULACIÓN FINALIZADA ===");
    }

    public static void main(String[] args) {
        SimuladorUber simulador = new SimuladorUber(15, 3, 20, 5, 800);
        simulador.iniciarSimulacion();
    }
}
