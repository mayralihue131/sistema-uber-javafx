package movilidad.ui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import movilidad.aplicacion.AplicacionConsultas;
import movilidad.dao.DAOFactory;
import movilidad.dao.DAOLoader;
import movilidad.dao.dto.VehiculoDTO;
import movilidad.dao.txt.CargaParametros;
import movilidad.logica.MovilidadDigital;
import movilidad.modelo.*;

/**
 * Gestor de sesión y persistencia por registro de transacciones (event journal) para la interfaz JavaFX.
 *
 * <p>Conserva todas las mutaciones del modelo en un archivo de texto con codificación Base64.
 * Antes de consolidar un cambio, reconstruye y valida el estado completo en memoria en una copia
 * tentativa para garantizar consistencia atómica y tolerancia a fallos.
 */
public final class SesionUber {
    public MovilidadDigital sistema;
    public String correo;
    public UUID viajeSeleccionado;
    private final Path archivo;
    private final List<String[]> eventos = new ArrayList<>();

    /**
     * Inicializa una sesión cargando los parámetros globales y reproduciendo los eventos
     * previos registrados en el archivo journal especificado.
     *
     * @param archivo ruta al archivo de persistencia de eventos (.txt)
     * @throws IOException si ocurre un error de lectura o corrupción de datos
     */
    public SesionUber(Path archivo) throws IOException {
        this.archivo = archivo.toAbsolutePath();
        CargaParametros.parametros();
        if (Files.exists(archivo)) {
            int linea = 0;
            for (String s : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                linea++;
                if (s.isBlank()) continue;
                try {
                    String[] campos = s.split("\\|", -1);
                    for (int i=0;i<campos.length;i++) campos[i] = new String(Base64.getDecoder().decode(campos[i]),StandardCharsets.UTF_8);
                    eventos.add(campos);
                } catch (RuntimeException e) { throw new IOException("Registro dañado en línea " + linea + " de " + archivo,e); }
            }
        }
        try { sistema = reconstruir(eventos); }
        catch (RuntimeException e) { throw new IOException("No se pueden recuperar los datos guardados: " + e.getMessage(),e); }
    }
    public Usuario usuario() { return sistema.buscarUsuarioPorMail(correo); }
    public Viaje viaje() { return sistema.buscarViajePorId(viajeSeleccionado); }
    public Path archivo() { return archivo; }

    /**
     * Autentica e inicia sesión para el usuario con el correo provisto.
     *
     * @param email dirección de correo electrónico del usuario
     * @throws IllegalArgumentException si el correo no está registrado en el sistema
     */
    public void ingresar(String email) {
        Usuario u = sistema.getUsuarios().stream()
            .filter(usuario -> usuario.getEmail().equalsIgnoreCase(email.trim()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Ese correo no está registrado. Creá una cuenta."
            ));

        correo = u.getEmail();
    }

    /**
     * Registra un nuevo usuario en el sistema y persiste el evento.
     *
     * @param nombre nombre completo del usuario
     * @param telefono número telefónico de contacto
     * @param email correo electrónico único
     * @throws IOException si falla la escritura en disco
     * @throws IllegalArgumentException si el correo está vacío o duplicado
     */
    public void registrarUsuario(String nombre, String telefono, String email)
            throws IOException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email no puede estar vacío.");
        }
        String emailLimpio = email.trim();
        if (sistema.getUsuarios().stream()
                .anyMatch(usuario -> usuario.getEmail().equalsIgnoreCase(emailLimpio))) {
            throw new IllegalArgumentException("Ese correo ya está registrado.");
        }

        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String telefonoLimpio = telefono == null ? "" : telefono.trim();

        ejecutar("USUARIO", nombreLimpio, telefonoLimpio, emailLimpio);
    }

    /**
     * Aplica una mutación de dominio, la valida atómicamente y la almacena en el journal de eventos.
     * Si la fábrica DAO activa persiste en base de datos o almacenamiento externo, sincroniza el registro.
     *
     * @param accion identificador del comando (e.g. USUARIO, VEHICULO, SOLICITAR, etc.)
     * @param args argumentos específicos de la acción
     * @throws IOException si falla la persistencia atómica en el archivo journal
     */
    public void ejecutar(String accion, String... args) throws IOException {
        if ("USUARIO".equals(accion)) {
            String email = args.length > 2 && args[2] != null ? args[2].trim() : "";
            if (sistema.getUsuarios().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email))) {
                throw new IllegalArgumentException("Ese correo ya está registrado.");
            }
        } else if ("VEHICULO".equals(accion)) {
            String pat = args.length > 2 && args[2] != null ? args[2].trim() : "";
            if (sistema.getUsuarios().stream()
                    .filter(x -> x.getConductor() != null)
                    .flatMap(x -> x.getConductor().getVehiculos().stream())
                    .anyMatch(v -> v.getPatente().equalsIgnoreCase(pat))) {
                throw new IllegalArgumentException("La patente ya está registrada.");
            }
        }

        String[] evento = new String[args.length+2];
        evento[0] = accion; evento[1] = LocalDateTime.now().toString();
        System.arraycopy(args,0,evento,2,args.length);
        List<String[]> siguiente = new ArrayList<>(eventos); siguiente.add(evento);
        MovilidadDigital candidato = reconstruir(siguiente);
        Files.createDirectories(archivo.getParent());
        Path temporal = Files.createTempFile(archivo.getParent(),"uber-", ".tmp");
        try {
            List<String> lineas = new ArrayList<>();
            for (String[] ev : siguiente) {
                List<String> partes = new ArrayList<>();
                for (String c : ev) partes.add(Base64.getEncoder().encodeToString(c.getBytes(StandardCharsets.UTF_8)));
                lineas.add(String.join("|",partes));
            }
            Files.write(temporal,lineas,StandardCharsets.UTF_8);
            try { Files.move(temporal,archivo,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporal,archivo,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporal); }
        eventos.add(evento); sistema = candidato;

        DAOFactory fabrica = DAOLoader.getFactory();
        if ("USUARIO".equals(accion) && fabrica.persisteUsuariosEnOrigen()) {
            try {
                fabrica.getUsuarioDAO().guardar(new Usuario(args[0], args[1], args[2]));
            } catch (Exception ex) {
                System.err.println("Advertencia al persistir usuario en BD: " + ex.getMessage());
            }
        } else if ("VEHICULO".equals(accion) && fabrica.persisteVehiculosEnOrigen()) {
            try {
                String email = args[0];
                String lic = args[1];
                String pat = args[2];
                String mod = args[3];
                int cap = Integer.parseInt(args[4]);
                String tipoV = args[5];
                String catV = args[6];
                List<String> tiposServ = Arrays.asList(args[7].split(","));
                VehiculoDTO dto = new VehiculoDTO(pat, mod, cap, tipoV, catV, tiposServ);
                fabrica.getVehiculoDAO().guardar(dto, email, lic);
            } catch (Exception ex) {
                System.err.println("Advertencia al persistir vehículo en BD: " + ex.getMessage());
            }
        }
    }
    private MovilidadDigital reconstruir(List<String[]> lista) throws IOException {
        MovilidadDigital m = new MovilidadDigital();
        AplicacionConsultas.cargarDatos(m);
        for (String[] e : lista) aplicar(m,e);
        return m;
    }
    private Usuario usuario(MovilidadDigital m,String email) {
        Usuario u = m.buscarUsuarioPorMail(email);
        if (u==null) throw new IllegalArgumentException("No existe el usuario.");
        return u;
    }
    private Vehiculo vehiculo(MovilidadDigital m,String email,String patente) {
        return usuario(m,email).getConductor().getVehiculos().stream().filter(v -> v.getPatente().equals(patente)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("El vehículo no pertenece al conductor."));
    }
    private void aplicar(MovilidadDigital m,String[] e) {
        LocalDateTime fecha = LocalDateTime.parse(e[1]);
        switch(e[0]) {
            case "USUARIO" -> {
                if (m.getUsuarios().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(e[4]))) {
                    break;
                }
                m.agregarUsuario(new Usuario(e[2],e[3],e[4]));
            }
            case "VEHICULO" -> {
                Usuario u = usuario(m,e[2]);
                if (m.getUsuarios().stream().filter(x -> x.getConductor()!=null).flatMap(x -> x.getConductor().getVehiculos().stream()).anyMatch(v -> v.getPatente().equalsIgnoreCase(e[4]))) {
                    break;
                }
                String[] tipos = e[9].split(",");
                Vehiculo v = new Vehiculo(e[4], e[5], Integer.parseInt(e[6]),
                        TipoVehiculo.valueOf(e[7]), CategoriaVehiculo.valueOf(e[8]),
                        TipoServicio.valueOf(tipos[0]));
                for (int i = 1; i < tipos.length; i++) {
                    v.agregarTipoServicio(TipoServicio.valueOf(tipos[i]));
                }
                if (u.getConductor()==null) m.registrarConductor(u,e[3],v); else m.agregarVehiculo(u,v);
            }
            case "ACTIVO" -> m.seleccionarVehiculoActivo(usuario(m,e[2]),vehiculo(m,e[2],e[3]));
            case "CATEGORIA" -> m.definirCategoriaAceptada(usuario(m,e[2]),CategoriaVehiculo.valueOf(e[3]));
            case "DISPONIBLE" -> {
                Usuario u=usuario(m,e[2]);
                if(u.getCliente().enViaje()) throw new IllegalStateException("Tenés un viaje activo como cliente.");
                if (u.getRolActivo() != RolUsuario.CONDUCTOR) m.cambiarRolActivo(u,RolUsuario.CONDUCTOR);
                m.ponerDisponible(u);
            }
            case "FUERA" -> m.ponerFueraDeServicio(usuario(m,e[2]));
            case "SOLICITAR" -> {
                Usuario u=usuario(m,e[2]);
                if(u.getConductor()!=null && (u.getConductor().getEstadoConductor()==EstadoConductor.VIAJE_A_ORIGEN || u.getConductor().getEstadoConductor()==EstadoConductor.VIAJE_A_DESTINO))
                    throw new IllegalStateException("Finalizá tu viaje como conductor antes de solicitar otro.");
                m.cambiarRolActivo(u,RolUsuario.CLIENTE);
                Servicio s=m.getServicios().stream().filter(x -> x.getNombre().equals(e[4])).findFirst().orElseThrow();
                Viaje v=m.solicitarViaje(u,s,new Ubicacion(Double.parseDouble(e[5]),Double.parseDouble(e[6])),new Ubicacion(Double.parseDouble(e[7]),Double.parseDouble(e[8])),fecha);
                v.setId(UUID.fromString(e[3]));
            }
            case "ACEPTAR" -> m.aceptarViaje(UUID.fromString(e[3]),usuario(m,e[2]),fecha);
            case "INICIAR" -> m.iniciarViaje(UUID.fromString(e[3]),usuario(m,e[2]),fecha);
            case "FINALIZAR" -> m.finalizarViaje(UUID.fromString(e[3]),usuario(m,e[2]),fecha);
            case "CANCELAR" -> m.cancelarViaje(UUID.fromString(e[3]),usuario(m,e[2]),fecha,e[4],Double.parseDouble(e[5]));
            case "CALIFICAR_CONDUCTOR" -> m.calificarConductor(UUID.fromString(e[3]),usuario(m,e[2]),CalificacionViaje.valueOf(e[4]));
            case "CALIFICAR_CLIENTE" -> m.calificarCliente(UUID.fromString(e[3]),usuario(m,e[2]),CalificacionViaje.valueOf(e[4]));
            case "VENCER" -> m.rechazarViaje(UUID.fromString(e[2]),fecha);
            default -> throw new IllegalArgumentException("Acción desconocida: " + e[0]);
        }
    }
    /**
     * Evalúa todos los viajes solicitados y rechaza/vence automáticamente aquellos
     * que hayan superado el tiempo de espera límite (5 minutos).
     *
     * @return {@code true} si al menos un viaje venció y cambió de estado, {@code false} en caso contrario
     * @throws IOException si ocurre un fallo al persistir el evento de vencimiento
     */
    public boolean vencer() throws IOException {
        boolean cambio=false;
        for(Viaje v : sistema.getViajes()) {
            if(v.estadoActual()==EstadoViaje.SOLICITADO && java.time.Duration.between(v.getRegistroViaje().get(0).getFechaHora(),LocalDateTime.now()).toMinutes()>=5) {
                ejecutar("VENCER",v.getId().toString()); cambio=true;
            }
        }
        return cambio;
    }
}