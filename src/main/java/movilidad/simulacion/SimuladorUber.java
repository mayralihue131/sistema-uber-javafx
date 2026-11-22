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
import movilidad.modelo.EstadoConductor;
import movilidad.modelo.RolUsuario;
import movilidad.modelo.Servicio;
import movilidad.modelo.TipoServicio;
import movilidad.modelo.TipoVehiculo;
import movilidad.modelo.Usuario;
import movilidad.modelo.Vehiculo;

/**
 * Orquestador principal de la simulación concurrente multihilo del sistema de
 * movilidad.
 *
 * <p>
 * Coordina productores de viajes y conductores consumidores sobre el modelo de
 * dominio real
 * ({@link MovilidadDigital}, {@link Usuario}, {@link movilidad.modelo.Viaje},
 * {@link Vehiculo}).
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
    private final List<ConductorConsumidor> consumidoresActivos = new ArrayList<>();
    private final List<Usuario> conductoresCargados = new ArrayList<>();

    /**
     * Construye un simulador con parámetros predeterminados:
     * 15 segundos de duración, 3 conductores, máximo 20 viajes, búfer de 5 y 800 ms
     * de cadencia.
     */
    public SimuladorUber() {
        this(15, 3, 20, 5, 800);
    }

    /**
     * Construye un simulador con parámetros personalizados inicializando
     * automáticamente el sistema.
     *
     * @param duracionSimulacionSegundos duración máxima en segundos antes del
     *                                   apagado automático
     * @param cantidadConductores        número de hilos consumidores concurrentes
     * @param maxViajes                  límite de viajes generados por el productor
     * @param capacidadCola              capacidad máxima de la cola bloqueante
     * @param intervaloGeneracionMs      tiempo en milisegundos entre solicitudes
     *                                   consecutivas
     */
    public SimuladorUber(int duracionSimulacionSegundos, int cantidadConductores, int maxViajes, int capacidadCola,
            int intervaloGeneracionMs) {
        this(crearSistemaPorDefecto(), duracionSimulacionSegundos, cantidadConductores, maxViajes, capacidadCola,
                intervaloGeneracionMs);
    }

    /**
     * Construye un simulador sobre una instancia de {@link MovilidadDigital}
     * provista.
     *
     * @param sistema                    instancia central de lógica y datos
     * @param duracionSimulacionSegundos duración máxima en segundos
     * @param cantidadConductores        número de conductores concurrentes
     * @param maxViajes                  límite de solicitudes a crear
     * @param capacidadCola              capacidad del buffer monitor
     * @param intervaloGeneracionMs      tiempo entre solicitudes en ms
     */
    public SimuladorUber(MovilidadDigital sistema, int duracionSimulacionSegundos, int cantidadConductores,
            int maxViajes, int capacidadCola, int intervaloGeneracionMs) {
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
            System.out.println("[SIMULADOR] Configuración por archivos/BD no disponible (" + e.getMessage()
                    + "), inicializando modelo en memoria.");
        }

        // Asegurar la existencia de servicios indispensables en el modelo
        if (m.getServicios().isEmpty()) {
            m.agregarServicio(new Servicio("Estandar", 2000, 200, 100, TipoVehiculo.AUTO, CategoriaVehiculo.ESTANDAR,
                    TipoServicio.PASAJEROS));
            Servicio envios = new Servicio("Envios", 1500, 150, 75, TipoVehiculo.AUTO, CategoriaVehiculo.ESTANDAR,
                    TipoServicio.ENVIOS);
            m.agregarServicio(envios);
            Servicio moto = new Servicio("Mototaxi", 1000, 100, 50, TipoVehiculo.MOTO, CategoriaVehiculo.ESTANDAR,
                    TipoServicio.PASAJEROS);
            m.agregarServicio(moto);
        }

        // Asegurar la presencia de clientes en el modelo
        boolean hayClientes = m.getUsuarios().stream().anyMatch(u -> u.getRolActivo() == RolUsuario.CLIENTE);
        if (!hayClientes) {
            for (int i = 1; i <= 3; i++) {
                Usuario cli = new Usuario("Cliente Sim " + i, "+54911888" + i, "cliente_sim_" + i + "@sim.com");
                m.agregarUsuario(cli);
            }
        }

        // Asegurar conductores en el modelo
        boolean hayConductores = m.getUsuarios().stream().anyMatch(u -> u.getConductor() != null);
        if (!hayConductores) {
            for (int i = 1; i <= 3; i++) {
                Usuario c = new Usuario("Chofer " + i, "111" + i, "chofer" + i + "@sim.com");
                Vehiculo v = new Vehiculo("CH0" + i, "Auto " + i, 4, TipoVehiculo.AUTO, CategoriaVehiculo.PREMIUM,
                        TipoServicio.PASAJEROS);
                v.agregarTipoServicio(TipoServicio.ENVIOS);
                m.agregarUsuario(c);
                m.registrarConductor(c, "B1", v);
                m.definirCategoriaAceptada(c, CategoriaVehiculo.PREMIUM);
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

    public List<Usuario> getConductoresCargados() {
        return new ArrayList<>(conductoresCargados);
    }

    public List<ConductorConsumidor> getConsumidoresActivos() {
        return new ArrayList<>(consumidoresActivos);
    }

    public int getTotalViajesCompletados() {
        return consumidoresActivos.stream().mapToInt(ConductorConsumidor::getViajesCompletados).sum();
    }

    /**
     * Carga, prepara y disponibiliza en primer lugar a los conductores para la
     * simulación.
     *
     * <p>
     * Escanea el modelo en busca de conductores con vehículo activo asignado,
     * sincroniza su rol a {@link RolUsuario#CONDUCTOR}, configura su categoría
     * aceptada y los transiciona al estado {@link EstadoConductor#DISPONIBLE}.
     * Si la cantidad disponible es menor a {@link #cantidadConductores}, crea y
     * registra
     * conductores auxiliares en el sistema garantizando compatibilidad con los
     * servicios.
     *
     * @return lista de conductores disponibles listos para la simulación
     */
    public List<Usuario> cargarConductoresDisponibles() {
        System.out.println("\n=======================================================");
        System.out.println("=== PASO 1: CARGA DE CONDUCTORES DISPONIBLES       ===");
        System.out.println("=======================================================");

        conductoresCargados.clear();

        synchronized (sistema) {
            // 1. Obtener todos los conductores existentes en el sistema con vehículo activo
            List<Usuario> existentes = new ArrayList<>(
                    sistema.getUsuarios().stream()
                            .filter(u -> u.getConductor() != null && u.getConductor().getVehiculoActivo() != null)
                            .toList());

            // Balancear tipos de vehículos intercalando autos y motos si hay disponibles
            List<Usuario> autos = existentes.stream()
                    .filter(u -> u.getConductor().getVehiculoActivo().getTipoVehiculo() == TipoVehiculo.AUTO)
                    .toList();
            List<Usuario> motos = existentes.stream()
                    .filter(u -> u.getConductor().getVehiculoActivo().getTipoVehiculo() == TipoVehiculo.MOTO)
                    .toList();
            List<Usuario> ordenados = new ArrayList<>();
            int maxLen = Math.max(autos.size(), motos.size());
            for (int i = 0; i < maxLen; i++) {
                if (i < autos.size())
                    ordenados.add(autos.get(i));
                if (i < motos.size())
                    ordenados.add(motos.get(i));
            }
            if (ordenados.isEmpty())
                ordenados = existentes;

            for (Usuario u : ordenados) {
                if (conductoresCargados.size() >= cantidadConductores)
                    break;
                try {
                    // Si estaba en un estado intermedio de una simulación anterior, normalizar
                    if (u.getConductor().getEstadoConductor() != EstadoConductor.FUERA_DE_SERVICIO
                            && u.getConductor().getEstadoConductor() != EstadoConductor.DISPONIBLE) {
                        u.getConductor().setEstado(EstadoConductor.FUERA_DE_SERVICIO);
                    }
                    if (u.getRolActivo() != RolUsuario.CONDUCTOR) {
                        sistema.cambiarRolActivo(u, RolUsuario.CONDUCTOR);
                    }
                    // Configurar categoría aceptada mínima en ESTANDAR para que acepte todos los
                    // servicios compatibles
                    sistema.definirCategoriaAceptada(u, CategoriaVehiculo.ESTANDAR);
                    sistema.ponerDisponible(u);
                    conductoresCargados.add(u);
                } catch (Exception e) {
                    System.err.println(
                            "[AVISO] No se pudo disponibilizar al conductor " + u.getNombre() + ": " + e.getMessage());
                }
            }

            // 2. Si faltan conductores para la cantidad configurada, registrar conductores
            // auxiliares
            int auxNum = 1;
            while (conductoresCargados.size() < cantidadConductores) {
                long ts = System.currentTimeMillis();
                String email = "auxiliar_sim_" + ts + "_" + auxNum + "@sim.com";
                while (sistema.buscarUsuarioPorMail(email) != null) {
                    ts++;
                    email = "auxiliar_sim_" + ts + "_" + auxNum + "@sim.com";
                }
                String patente = "AUX" + String.format("%03d", (auxNum * 17) % 900 + 100);

                // Alternar tipo de vehículo si el sistema incluye motos
                boolean soporteMoto = sistema.getServicios().stream()
                        .anyMatch(s -> s.getTipoVehiculo() == TipoVehiculo.MOTO) && (auxNum % 2 == 0);

                TipoVehiculo tipoV = soporteMoto ? TipoVehiculo.MOTO : TipoVehiculo.AUTO;
                int capacidad = soporteMoto ? 1 : 4;
                String modelo = soporteMoto ? "Honda Wave 110" : "Fiat Cronos 1.3";

                Vehiculo veh = new Vehiculo(patente, modelo, capacidad, tipoV, CategoriaVehiculo.PREMIUM,
                        TipoServicio.PASAJEROS);
                veh.agregarTipoServicio(TipoServicio.ENVIOS);

                Usuario c = new Usuario("Conductor Auxiliar " + auxNum, "+54911000" + auxNum, email);
                sistema.agregarUsuario(c);
                sistema.registrarConductor(c, "B1-D1", veh);
                sistema.definirCategoriaAceptada(c, CategoriaVehiculo.PREMIUM);
                sistema.cambiarRolActivo(c, RolUsuario.CONDUCTOR);
                sistema.ponerDisponible(c);
                conductoresCargados.add(c);
                auxNum++;
            }
        }

        // Mostrar resumen de los conductores cargados
        System.out.println("Total de conductores disponibles cargados: " + conductoresCargados.size());
        int idx = 1;
        for (Usuario cond : conductoresCargados) {
            Vehiculo v = cond.getConductor().getVehiculoActivo();
            System.out.printf(" [%d] %-20s | Lic: %-10s | Vehículo: %-15s [%-7s] %-4s/%-8s | Estado: %s%n",
                    idx++,
                    cond.getNombre(),
                    cond.getConductor().getLicenciaConducir(),
                    v.getModelo(),
                    v.getPatente(),
                    v.getTipoVehiculo(),
                    v.getCategoriaVehiculo(),
                    cond.getConductor().getEstadoConductor());
        }
        System.out.println("=======================================================\n");

        return new ArrayList<>(conductoresCargados);
    }

    /**
     * Inicia la ejecución de la simulación concurrente.
     * Carga en primer lugar a los conductores disponibles, inicia sus hilos
     * consumidores,
     * y posteriormente arranca el generador de pedidos de viaje.
     */
    public void iniciarSimulacion() {
        System.out.println("=== INICIANDO SIMULACIÓN UBER (CON MODELO REAL) ===");
        System.out.println("Parámetros: Duración=" + duracionSimulacionSegundos + "s | Conductores Requeridos="
                + cantidadConductores + " | Max Viajes=" + maxViajes + " | Búfer=" + capacidadCola + " | Cadencia="
                + intervaloGeneracionMs + "ms");

        BufferViajes buffer = new BufferViajes(capacidadCola);
        consumidoresActivos.clear();

        // 1. CARGAR PRIMERO LOS CONDUCTORES DISPONIBLES
        List<Usuario> conductoresAsignados = cargarConductoresDisponibles();

        ExecutorService executor = Executors.newFixedThreadPool(conductoresAsignados.size() + 1);

        // 2. INICIAR HILOS CONSUMIDORES (CONDUCTORES) PRIMERO
        System.out.println("=== PASO 2: CONECTANDO CONDUCTORES AL MONITOR DE VIAJES ===");
        for (Usuario cond : conductoresAsignados) {
            ConductorConsumidor consumidor = new ConductorConsumidor(sistema, cond, buffer);
            consumidoresActivos.add(consumidor);
            executor.execute(consumidor);
        }

        // Breve espera para asegurar que los conductores estén listos en el búfer
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {
        }
        System.out.println("Conductores en línea y atentos a solicitudes en el búfer.\n");

        // 3. INICIAR HILO PRODUCTOR DE VIAJES
        System.out.println("=== PASO 3: INICIANDO PRODUCTOR DE SOLICITUDES ===");
        ProductorViajes productor = new ProductorViajes(sistema, buffer, maxViajes, intervaloGeneracionMs,
                conductoresAsignados);
        executor.execute(productor);

        // 4. CONTROLAR TIEMPO DE EJECUCIÓN
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

        mostrarEstadisticasFinales(buffer);
    }

    private void mostrarEstadisticasFinales(BufferViajes buffer) {
        System.out.println("\n=======================================================");
        System.out.println("=== RESUMEN Y ESTADÍSTICAS DE LA SIMULACIÓN         ===");
        System.out.println("=======================================================");
        int totalViajes = 0;
        for (ConductorConsumidor cons : consumidoresActivos) {
            int viajesChofer = cons.getViajesCompletados();
            totalViajes += viajesChofer;
            System.out.printf(" - Conductor %-22s: %d viajes completados%n",
                    cons.getConductor().getNombre(), viajesChofer);
        }
        System.out.println("-------------------------------------------------------");
        System.out.println("Total de viajes atendidos y completados : " + totalViajes);
        System.out.println("Viajes remanentes en cola del búfer     : " + buffer.getCantidadPendientes());
        System.out.println("=======================================================\n");
    }

    public static void main(String[] args) {
        SimuladorUber simulador = new SimuladorUber(15, 8, 20, 5, 800);
        simulador.iniciarSimulacion();
    }
}
