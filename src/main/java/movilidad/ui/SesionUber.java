package movilidad.ui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import movilidad.aplicacion.AplicacionConsultas;
import movilidad.dao.DAOFactory;
import movilidad.dao.DAOLoader;
import movilidad.dao.txt.CargaParametros;
import movilidad.logica.MovilidadDigital;
import movilidad.modelo.*;

/** Conserva las acciones en texto; reconstruye y valida antes de guardar cada cambio. */
public final class SesionUber {
    public MovilidadDigital sistema;
    public String correo;
    public UUID viajeSeleccionado;
    private final Path archivo;
    private final List<String[]> eventos = new ArrayList<>();

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
    
    public void ingresar(String email) {
        Usuario u = sistema.getUsuarios().stream()
            .filter(usuario -> usuario.getEmail().equalsIgnoreCase(email.trim()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Ese correo no está registrado. Creá una cuenta."
            ));

        correo = u.getEmail();
    }
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

        DAOFactory fabrica = DAOLoader.getFactory();
        if (fabrica.persisteUsuariosEnOrigen()) {
            Usuario usuario = new Usuario(nombreLimpio, telefonoLimpio, emailLimpio);
            fabrica.getUsuarioDAO().guardar(usuario);
            sistema.agregarUsuario(usuario);
        } else {
            ejecutar("USUARIO", nombreLimpio, telefonoLimpio, emailLimpio);
        }
    }

    public void ejecutar(String accion, String... args) throws IOException {
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
                if (m.getUsuarios().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(e[4]))) throw new IllegalArgumentException("Ese correo ya está registrado.");
                m.agregarUsuario(new Usuario(e[2],e[3],e[4]));
            }
            case "VEHICULO" -> {
                Usuario u = usuario(m,e[2]);
                if (m.getUsuarios().stream().filter(x -> x.getConductor()!=null).flatMap(x -> x.getConductor().getVehiculos().stream()).anyMatch(v -> v.getPatente().equalsIgnoreCase(e[4])))
                    throw new IllegalArgumentException("La patente ya está registrada.");
                Vehiculo v = new Vehiculo(e[4],e[5],Integer.parseInt(e[6]),TipoVehiculo.valueOf(e[7]),CategoriaVehiculo.valueOf(e[8]),(Ubicacion)null);
                for(String tipo : e[9].split(",")) v.agregarTipoServicio(TipoServicio.valueOf(tipo));
                if (u.getConductor()==null) m.registrarConductor(u,e[3],v); else m.agregarVehiculo(u,v);
            }
            case "ACTIVO" -> m.seleccionarVehiculoActivo(usuario(m,e[2]),vehiculo(m,e[2],e[3]));
            case "CATEGORIA" -> m.definirCategoriaAceptada(usuario(m,e[2]),CategoriaVehiculo.valueOf(e[3]));
            case "DISPONIBLE" -> {
                Usuario u=usuario(m,e[2]);
                if(u.getCliente().enViaje()) throw new IllegalStateException("Tenés un viaje activo como cliente.");
                m.cambiarRolActivo(u,RolUsuario.CONDUCTOR); m.ponerDisponible(u);
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
    public boolean vencer() throws IOException {
        boolean cambio=false;
        for(Viaje v : sistema.getViajes()) {
            if(v.estadoActual()==EstadoViaje.SOLICITADO && java.time.Duration.between(v.getRegistrosViaje().get(0).getFechaHora(),LocalDateTime.now()).toMinutes()>=5) {
                ejecutar("VENCER",v.getId().toString()); cambio=true;
            }
        }
        return cambio;
    }
}
