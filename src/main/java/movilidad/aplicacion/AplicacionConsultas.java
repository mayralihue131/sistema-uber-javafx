package movilidad.aplicacion;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import movilidad.dao.txt.CargaDatos;
import movilidad.dao.txt.CargaParametros;
import movilidad.logica.MovilidadDigital;
import movilidad.modelo.*;

public class AplicacionConsultas {

    public static void main(String[] args) {

        MovilidadDigital aplicacion = new MovilidadDigital();

        try {
            CargaParametros.parametros();
            cargarDatos(aplicacion);

            try (Scanner teclado = new Scanner(System.in)) {
                boolean continuar = true;

                while (continuar) {
                    System.out.println("\n===== MOVILIDAD DIGITAL =====");
                    System.out.println("1. Consultar servicios");
                    System.out.println("2. Solicitar viaje");
                    System.out.println("3. Consultar viajes y mostrar resumen");
                    System.out.println("0. Salir");
                    System.out.print("Opción: ");

                    String opcion = teclado.nextLine().trim();

                    try {
                        switch (opcion) {
                            case "1":
                                consultarServicios(aplicacion, teclado);
                                break;
                            case "2":
                                solicitarViaje(aplicacion, teclado);
                                break;
                            case "3":
                                consultarViajes(aplicacion, teclado);
                                break;
                            case "0":
                                continuar = false;
                                break;
                            default:
                                System.out.println("Opción incorrecta");
                        }
                    } catch (IllegalArgumentException
                            | IllegalStateException e) {
                        System.out.println("No se pudo realizar: "
                                + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error al cargar archivos: "
                    + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("Error en los datos: " + e.getMessage());
        }
    }

    public static void cargarDatos(MovilidadDigital aplicacion)
            throws IOException {

        List<Vehiculo> vehiculos = new ArrayList<>();

        for (List<String> fila : CargaDatos.cargarVehiculos(
                CargaParametros.getArchivoVehiculos())) {

            Vehiculo vehiculo = new Vehiculo(
                    fila.get(0),
                    fila.get(1),
                    Integer.parseInt(fila.get(2)),
                    TipoVehiculo.valueOf(fila.get(3)),
                    CategoriaVehiculo.valueOf(fila.get(4)),
                    (Ubicacion) null);

            for (int i = 5; i < fila.size(); i++) {
                if (!fila.get(i).isBlank()) {
                    vehiculo.agregarTipoServicio(
                            TipoServicio.valueOf(fila.get(i)));
                }
            }

            vehiculos.add(vehiculo);
        }

        for (List<String> fila : CargaDatos.cargarUsuarios(
                CargaParametros.getArchivoUsuarios())) {

            Usuario usuario = new Usuario(
                    fila.get(0), fila.get(1), fila.get(2));

            aplicacion.agregarUsuario(usuario);

            if (fila.size() > 3 && !fila.get(3).isBlank()) {
                String licencia = fila.get(3);
                boolean primero = true;

                for (int i = 4; i < fila.size(); i++) {
                    String patente = fila.get(i);

                    if (patente.isBlank()) {
                        continue;
                    }

                    Vehiculo encontrado = null;

                    for (Vehiculo vehiculo : vehiculos) {
                        if (patente.equals(vehiculo.getPatente())) {
                            encontrado = vehiculo;
                            break;
                        }
                    }

                    if (encontrado == null) {
                        throw new IllegalArgumentException(
                                "Patente inexistente: " + patente);
                    }

                    if (primero) {
                        aplicacion.registrarConductor(
                                usuario, licencia, encontrado);
                        primero = false;
                    } else {
                        aplicacion.agregarVehiculo(usuario, encontrado);
                    }
                }

                if (primero) {
                    throw new IllegalArgumentException(
                            "Falta la patente en la fila de "
                                    + usuario.getEmail());
                }

                Vehiculo activo =
                        usuario.getConductor().getVehiculoActivo();

                aplicacion.definirCategoriaAceptada(
                        usuario, activo.getCategoriaVehiculo());

                aplicacion.cambiarRolActivo(
                        usuario, RolUsuario.CONDUCTOR);

                aplicacion.ponerDisponible(usuario);
            }
        }

        for (List<String> fila : CargaDatos.cargarServicios(
                CargaParametros.getArchivoServicios())) {

            Servicio servicio = new Servicio(
                    fila.get(0),
                    Double.parseDouble(fila.get(1)),
                    Double.parseDouble(fila.get(2)),
                    Double.parseDouble(fila.get(3)),
                    CategoriaVehiculo.valueOf(fila.get(5)),
                    TipoVehiculo.valueOf(fila.get(4)),
                    TipoServicio.valueOf(fila.get(6)));

            aplicacion.agregarServicio(servicio);
        }


    }

    private static List<Servicio> consultarServicios(
            MovilidadDigital aplicacion, Scanner teclado) {

        System.out.print("Tipo de servicio (PASAJEROS / ENVIOS): ");
        TipoServicio tipo = TipoServicio.valueOf(
                teclado.nextLine().trim().toUpperCase());

        System.out.print("Categoría (ESTANDAR / CONFORT / PREMIUM): ");
        CategoriaVehiculo categoria = CategoriaVehiculo.valueOf(
                teclado.nextLine().trim().toUpperCase());

        List<Servicio> servicios =
                aplicacion.consultarServicios(tipo, categoria);

        if (servicios.isEmpty()) {
            System.out.println("No hay servicios para esa consulta.");
        }

        for (int i = 0; i < servicios.size(); i++) {
            Servicio servicio = servicios.get(i);

            System.out.printf(
                    "%d. %s | %s | Base: $%.2f"
                    + " | Km: $%.2f | Minuto: $%.2f%n",
                    i + 1,
                    servicio.getNombre(),
                    servicio.getTipoVehiculo(),
                    servicio.getTarifaBase(),
                    servicio.getPrecioKm(),
                    servicio.getPrecioMinuto());
        }

        return servicios;
    }

    private static void solicitarViaje(
            MovilidadDigital aplicacion, Scanner teclado) {

        System.out.print("Email del cliente: ");
        Usuario cliente = aplicacion.buscarUsuarioPorMail(
                teclado.nextLine().trim());

        if (cliente == null) {
            throw new IllegalArgumentException(
                    "El cliente no está registrado");
        }

        List<Servicio> servicios =
                consultarServicios(aplicacion, teclado);

        if (servicios.isEmpty()) {
            return;
        }

        Servicio servicio = elegir(servicios, teclado);

        System.out.println("ORIGEN");
        Ubicacion origen = ingresarUbicacion(teclado);

        System.out.println("DESTINO");
        Ubicacion destino = ingresarUbicacion(teclado);

        double minutos = estimarMinutos(origen, destino);
        double km = origen.calcularDistancia(destino);
        double costo = servicio.calcularCosto(km, minutos);

        System.out.printf("Distancia estimada: %.2f km%n", km);
        System.out.printf("Tiempo estimado: %.2f minutos%n", minutos);
        System.out.printf("Precio estimado: $%.2f%n", costo);

        System.out.print("¿Confirmar solicitud? (SI / NO): ");

        if (!teclado.nextLine().trim().equalsIgnoreCase("SI")) {
            return;
        }

        Viaje viaje = aplicacion.solicitarViaje(
                cliente, servicio, origen, destino,
                LocalDateTime.now());

        List<Usuario> compatibles =
                aplicacion.buscarConductoresDisponibles(viaje);

        List<Usuario> conductores = new ArrayList<>();

        // Comprobar también el tipo de vehículo y el rango de categorías.
        for (Usuario usuario : compatibles) {
            Conductor conductor = usuario.getConductor();
            Vehiculo vehiculo = conductor.getVehiculoActivo();

            CategoriaVehiculo minima =
                    conductor.getCategoriaVehiculoActivo();

            CategoriaVehiculo solicitada =
                    servicio.getCategoriaVehiculo();

            if (vehiculo.getTipoVehiculo() == servicio.getTipoVehiculo()
                    && minima != null
                    && vehiculo.getCategoriaVehiculo() != null
                    && solicitada.getCodigo() >= minima.getCodigo()
                    && solicitada.getCodigo()
                        <= vehiculo.getCategoriaVehiculo().getCodigo()) {
                conductores.add(usuario);
            }
        }

        System.out.println("\nCONDUCTORES DISPONIBLES");

        for (Usuario conductor : conductores) {
            System.out.println(conductor.getNombre()
                    + " | " + conductor.getEmail());
        }

        if (conductores.isEmpty()) {
            System.out.println(
                    "No hay conductores compatibles en este momento.");
        }

        mostrarResumen(viaje);
    }

    private static Ubicacion ingresarUbicacion(Scanner teclado) {
        System.out.print("Latitud: ");
        double latitud = Double.parseDouble(
                teclado.nextLine().trim().replace(',', '.'));

        System.out.print("Longitud: ");
        double longitud = Double.parseDouble(
                teclado.nextLine().trim().replace(',', '.'));

        if (!Double.isFinite(latitud) || !Double.isFinite(longitud)
                || latitud < -90 || latitud > 90
                || longitud < -180 || longitud > 180) {
            throw new IllegalArgumentException(
                    "Las coordenadas no son válidas");
        }

        return new Ubicacion(latitud, longitud);
    }

    private static double estimarMinutos(
            Ubicacion origen, Ubicacion destino) {

        Properties parametros = new Properties();

        try (FileInputStream archivo =
                new FileInputStream("src/main/resources/config.properties")) {
            parametros.load(archivo);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer la configuración", e);
        }

        String valor = parametros.getProperty("velocidadPromedioKmH");

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar velocidadPromedioKmH");
        }

        double velocidad = Double.parseDouble(valor.trim());

        if (!Double.isFinite(velocidad) || velocidad <= 0) {
            throw new IllegalStateException(
                    "La velocidad debe ser positiva y finita");
        }

        return origen.calcularDistancia(destino) / velocidad * 60;
    }

    private static void consultarViajes(
            MovilidadDigital aplicacion, Scanner teclado) {

        List<Viaje> viajes = aplicacion.getViajes();

        if (viajes.isEmpty()) {
            System.out.println("Todavía no hay viajes solicitados.");
            return;
        }

        for (int i = 0; i < viajes.size(); i++) {
            Viaje viaje = viajes.get(i);

            System.out.println((i + 1) + ". "
                    + viaje.getCliente().getNombre()
                    + " | " + viaje.getServicio().getNombre()
                    + " | " + viaje.estadoActual());
        }

        mostrarResumen(elegir(viajes, teclado));
    }

    private static <T> T elegir(List<T> opciones, Scanner teclado) {
        System.out.print("Elegí un número: ");
        int numero = Integer.parseInt(teclado.nextLine().trim());

        if (numero < 1 || numero > opciones.size()) {
            throw new IllegalArgumentException(
                    "El número está fuera de las opciones");
        }

        return opciones.get(numero - 1);
    }

    private static void mostrarResumen(Viaje viaje) {

        System.out.println("\n================================================");
        System.out.println("          RESUMEN DE VIAJE SOLICITADO");
        System.out.println("================================================");

        System.out.println("ID: " + viaje.getId());
        System.out.println("Cliente: "
                + viaje.getCliente().getNombre()
                + " (" + viaje.getCliente().getEmail() + ")");
        System.out.println("Origen: " + viaje.getOrigen());
        System.out.println("Destino: " + viaje.getDestino());

        Servicio servicio = viaje.getServicio();

        System.out.println("\n------------- SERVICIO -------------");
        System.out.println("Nombre: " + servicio.getNombre());
        System.out.println("Tipo: " + servicio.getTipoServicio());
        System.out.println("Categoría: "
                + servicio.getCategoriaVehiculo());
        System.out.println("Tarifa base: $" + servicio.getTarifaBase());

        System.out.println("\n------------- ASIGNACIÓN -------------");

        if (viaje.getConductor() == null) {
            System.out.println("Conductor: pendiente de aceptación");
        } else {
            System.out.println("Conductor: "
                    + viaje.getConductor().getNombre());
        }

        if (viaje.getVehiculo() != null) {
            System.out.println("Vehículo: "
                    + viaje.getVehiculo().getModelo()
                    + " | Patente: "
                    + viaje.getVehiculo().getPatente());
        }

        System.out.println("\n------------- HISTORIAL -------------");

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        for (RegistroViaje registro : viaje.getRegistrosViaje()) {
            System.out.println(registro.getEstadoViaje()
                    + " -> "
                    + registro.getFechaHora().format(formato));
        }

        System.out.println("================================================");
    }
}