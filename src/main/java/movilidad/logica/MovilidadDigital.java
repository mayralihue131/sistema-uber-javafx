package movilidad.logica;

import java.time.Duration;

import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.List;

import java.util.UUID;

import movilidad.dao.interfaces.UsuarioDAO;
import movilidad.dao.memoria.UsuarioDAOMemoria;
import movilidad.dao.txt.CargaParametros;
import movilidad.modelo.*;

public class MovilidadDigital {

    private UsuarioDAO usuarioDAO;
    private List<Viaje> viajes;
    private List<Servicio> servicios;

    public MovilidadDigital() {
        this(new UsuarioDAOMemoria());

    }

    public MovilidadDigital(UsuarioDAO usuarioDAO) {

        this.usuarioDAO = usuarioDAO;
        this.viajes = new ArrayList<>();
        this.servicios = new ArrayList<>();

    }

    public List<Viaje> getViajes() {
        return new ArrayList<>(viajes);

    }

    public List<Usuario> getUsuarios() {
        return usuarioDAO.listarTodos();
    }

    public List<Servicio> getServicios() {
        return new ArrayList<>(servicios);
    }

    public void agregarUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario no puede ser null");
        }

        if (usuario.getEmail() == null
                || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "El email no puede estar vacío");
        }

        if (usuarioDAO.buscarPorEmail(usuario.getEmail()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario con ese email");
        }

        usuarioDAO.guardar(usuario);

    }

    // Buscar un usuario registrado por email.

    public Usuario buscarUsuarioPorMail(String mail) {

        if (mail == null || mail.isBlank()) {
            return null;
        }

        return usuarioDAO.buscarPorEmail(mail);

    }

    // Habilitar al usuario como conductor con su primer vehículo.

    public void registrarConductor(Usuario usuario, String licencia, Vehiculo vehiculo) {

        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser null");
        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }

        if (licencia == null || licencia.isBlank()) {
            throw new IllegalArgumentException("La licencia no puede estar vacía");
        }

        if (vehiculo == null) {
            throw new IllegalArgumentException("El vehiculo no puede ser null");
        }

        for (Usuario usuarioRegistrado : usuarioDAO.listarTodos()) {

            if (usuario.getEmail().equals(usuarioRegistrado.getEmail())) {

                if (usuarioRegistrado.getConductor() != null) {

                    throw new IllegalStateException("El usuario ya es conductor");

                }

                usuarioRegistrado.altaConductor(licencia, vehiculo);

                return;

            }

        }

        throw new IllegalArgumentException("El usuario no está registrado");

    }

    // Agregar un vehículo al conductor del usuario.

    public void agregarVehiculo(Usuario usuario, Vehiculo vehiculo) {

        if (usuario == null) {

            throw new IllegalArgumentException("El usuario no puede ser null");

        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {

            throw new IllegalArgumentException("El email no puede estar vacío");

        }

        if (vehiculo == null) {

            throw new IllegalArgumentException("El vehiculo no puede ser null");

        }

        for (Usuario usuarioRegistrado : usuarioDAO.listarTodos()) {

            if (usuario.getEmail().equals(usuarioRegistrado.getEmail())) {

                if (usuarioRegistrado.getConductor() == null) {

                    throw new IllegalArgumentException("El usuario no es conductor");

                }

                if (usuarioRegistrado.getConductor().getVehiculos().contains(vehiculo)) {

                    throw new IllegalArgumentException("El conductor ya tiene registrado este vehículo");

                }

                usuarioRegistrado.getConductor().agregarVehiculo(vehiculo);

                return;

            }

        }

        throw new IllegalArgumentException("El usuario no está registrado");

    }

    // Agregar PASAJEROS o ENVIOS a un vehículo del conductor.

    public void agregarTipoServicioVehiculo(Usuario usuario, Vehiculo vehiculo, TipoServicio tipoServicio) {

        if (usuario == null) {

            throw new IllegalArgumentException("El usuario no puede ser null");

        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {

            throw new IllegalArgumentException("El email no puede estar vacío");

        }

        if (vehiculo == null) {

            throw new IllegalArgumentException("El vehiculo no puede estar vacio");

        }

        if (tipoServicio == null) {

            throw new IllegalArgumentException("No puede ser null el tipo de servicio es envio o pasajeros");

        }

        for (Usuario usuarioRegistrado : usuarioDAO.listarTodos()) {

            if (usuarioRegistrado.getEmail().equals(usuario.getEmail())) {

                Conductor conductor = usuarioRegistrado.getConductor();

                if (conductor == null) {

                    throw new IllegalStateException("El usuario no es conductor");

                }

                for (Vehiculo vehiculoRegistrado : conductor.getVehiculos()) {

                    if (vehiculoRegistrado.equals(vehiculo)) {

                        vehiculoRegistrado.agregarTipoServicio(tipoServicio);

                        return;

                    }

                }

                throw new IllegalArgumentException("El vehículo no pertenece al conductor");

            }

        }

        throw new IllegalArgumentException("El usuario no está registrado");

    }

    // Elegir un vehículo propio cuando no haya un viaje en curso.

    public void seleccionarVehiculoActivo(Usuario usuario, Vehiculo vehiculo) {

        if (usuario == null) {

            throw new IllegalArgumentException("El usuario no puede ser null");

        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {

            throw new IllegalArgumentException("El email no puede estar vacío");

        }

        if (vehiculo == null) {

            throw new IllegalArgumentException("El vehiculo no puede ser null");

        }

        for (Usuario usuarioRegistrado : usuarioDAO.listarTodos()) {

            if (usuarioRegistrado.getEmail().equals(usuario.getEmail())) {

                Conductor conductor = usuarioRegistrado.getConductor();

                if (conductor == null) {

                    throw new IllegalStateException("El usuario no es conductor");

                }

                EstadoConductor estadoActual = conductor.getEstadoConductor();

                if (estadoActual == EstadoConductor.VIAJE_A_ORIGEN || estadoActual == EstadoConductor.VIAJE_A_DESTINO) {

                    throw new IllegalStateException("No puede cambiar de vehículo durante un viaje");

                }

                for (Vehiculo vehiculoRegistrado : conductor.getVehiculos()) {

                    if (vehiculoRegistrado.equals(vehiculo)) {

                        conductor.setVehiculoActivo(vehiculoRegistrado);

                        return;

                    }

                }

                throw new IllegalArgumentException("El vehículo no pertenece al conductor");

            }

        }

        throw new IllegalArgumentException("El usuario no está registrado");

    }

    // Definir hasta qué categoría menor acepta solicitudes.

    public void definirCategoriaAceptada(Usuario usuario, CategoriaVehiculo categoria) {

        if (usuario == null) {

            throw new IllegalArgumentException("El usuario no puede ser null");

        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {

            throw new IllegalArgumentException("El email no puede estar vacío");

        }

        if (categoria == null) {

            throw new IllegalArgumentException("La categoría no puede ser null");

        }

        for (Usuario usuarioRegistrado : usuarioDAO.listarTodos()) {

            if (usuarioRegistrado.getEmail().equals(usuario.getEmail())) {

                Conductor conductor = usuarioRegistrado.getConductor();

                if (conductor == null) {

                    throw new IllegalStateException("El usuario no es conductor");

                }

                EstadoConductor estadoActual = conductor.getEstadoConductor();

                if (estadoActual == EstadoConductor.VIAJE_A_ORIGEN || estadoActual == EstadoConductor.VIAJE_A_DESTINO) {

                    throw new IllegalStateException("No puede modificar la categoría aceptada durante un viaje");

                }

                Vehiculo vehiculoActivo = conductor.getVehiculoActivo();

                if (vehiculoActivo == null || vehiculoActivo.getCategoriaVehiculo() == null) {

                    throw new IllegalStateException("El conductor necesita un vehículo activo con categoría");

                }

                if (categoria.getCodigo() > vehiculoActivo.getCategoriaVehiculo().getCodigo()) {

                    throw new IllegalArgumentException("La categoría aceptada no puede superar la del vehículo");

                }

                conductor.setCategoriaVehiculoActivo(categoria);

                return;

            }

        }

        throw new IllegalArgumentException("El usuario no está registrado");

    }

    // Validar conductor, licencia y vehículo activo.

    public void ponerDisponible(Usuario usuario) {

        if (usuario == null) {

            throw new IllegalArgumentException("El usuario no puede ser null");

        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {

            throw new IllegalArgumentException("El email no puede estar vacío");

        }

        for (Usuario usuarioRegistrado : usuarioDAO.listarTodos()) {

            if (usuarioRegistrado.getEmail().equals(usuario.getEmail())) {

                Conductor conductor = usuarioRegistrado.getConductor();

                if (conductor == null) {

                    throw new IllegalStateException("El usuario no es conductor");

                }

                if (conductor.getVehiculoActivo() == null) {

                    throw new IllegalStateException("El conductor no tiene un vehículo activo");

                }

                String licencia = conductor.getLicenciaConducir();

                if (licencia == null || licencia.isBlank()) {

                    throw new IllegalStateException("El conductor no tiene una licencia válida");

                }

                EstadoConductor estadoActual = conductor.getEstadoConductor();

                if (estadoActual == EstadoConductor.VIAJE_A_ORIGEN || estadoActual == EstadoConductor.VIAJE_A_DESTINO) {

                    throw new IllegalStateException("El conductor tiene un viaje en curso");

                }

                conductor.setEstadoConductor(EstadoConductor.DISPONIBLE);

                return;

            }

        }

        throw new IllegalArgumentException("El usuario no está registrado");

    }

    // Dejar de recibir solicitudes si no tiene un viaje en curso.

    public void ponerFueraDeServicio(Usuario usuario) {

        if (usuario == null) {

            throw new IllegalArgumentException("El usuario no puede estar vacio");

        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {

            throw new IllegalArgumentException("El email no puede estar vacío");

        }

        for (Usuario usuarioRegistrado : usuarioDAO.listarTodos()) {

            if (usuario.getEmail().equals(usuarioRegistrado.getEmail())) {

                Conductor conductor = usuarioRegistrado.getConductor();

                if (conductor == null) {

                    throw new IllegalStateException("El usuario no es conductor");

                }

                EstadoConductor estadoActual = conductor.getEstadoConductor();

                if (estadoActual == EstadoConductor.VIAJE_A_ORIGEN || estadoActual == EstadoConductor.VIAJE_A_DESTINO) {

                    throw new IllegalStateException("El conductor tiene un viaje en curso");

                }

                conductor.setEstadoConductor(EstadoConductor.FUERA_DE_SERVICIO);

                return;

            }

        }

        throw new IllegalArgumentException("El usuario no está registrado");

    }

    // Cambiar el rol activo del usuario.

    public void cambiarRolActivo(Usuario usuario, RolUsuario rol) {

        if (usuario == null) {

            throw new IllegalArgumentException("El usuario no puede ser null");

        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {

            throw new IllegalArgumentException("El email no puede estar vacío");

        }

        if (rol == null) {

            throw new IllegalArgumentException("El rol no puede ser null");

        }

        for (Usuario usuarioRegistro : usuarioDAO.listarTodos()) {

            if (usuario.getEmail().equals(usuarioRegistro.getEmail())) {

                if (rol == RolUsuario.CONDUCTOR && usuarioRegistro.getConductor() == null) {

                    throw new IllegalStateException("El usuario no es conductor");

                }

                usuarioRegistro.cambiarRolActivo(rol);

                return;

            }

        }

        throw new IllegalArgumentException("El usuario no está registrado");

    }

    public void agregarServicio(Servicio servicio) {

        if (servicio == null) {

            throw new IllegalArgumentException("El servicio no puede ser null");

        }

        String nombre = servicio.getNombre();

        if (nombre == null || nombre.isBlank()) {

            throw new IllegalArgumentException("El nombre no puede estar vacío");

        }

        for (Servicio registrado : servicios) {

            if (nombre.equals(registrado.getNombre())) {

                throw new IllegalArgumentException("El servicio ya está registrado");

            }

        }

        servicios.add(servicio);

    }

    // Buscar servicios del tipo y categoría solicitados.

    public List<Servicio> consultarServicios(TipoServicio tipoServicio, CategoriaVehiculo categoria) {

        if (tipoServicio == null || categoria == null) {

            throw new IllegalArgumentException("El tipo de servicio y la categoría no pueden ser null");

        }

        List<Servicio> resultado = new ArrayList<>();

        for (Servicio servicio : servicios) {

            if (servicio.getTipoServicio() == tipoServicio && servicio.getCategoriaVehiculo() == categoria) {

                resultado.add(servicio);

            }

        }

        return resultado;

    }

    // Estimar los minutos de recorrido usando la distancia entre ubicaciones

    public double estimarTiempo(Servicio servicio, Ubicacion origen, Ubicacion destino) {

        if (servicio == null || origen == null || destino == null) {

            throw new IllegalArgumentException("El servicio, origen y destino no pueden ser null");

        }

        double distanciaKm = origen.calcularDistancia(destino);

        double velocidadPromedioKmH = CargaParametros.getVelocidadPromedioKmH();

        return (distanciaKm / velocidadPromedioKmH) * 60.0;

    }

    // hasta aca

    // Crear la solicitud y asociarla al cliente.

    public Viaje solicitarViaje(Usuario cliente, Servicio servicio, Ubicacion origen, Ubicacion destino,
            LocalDateTime fechaHora) {

        if (cliente == null || servicio == null || origen == null || destino == null || fechaHora == null) {

            throw new IllegalArgumentException("Los datos del viaje no pueden ser nulos");

        }

        if (!usuarioDAO.listarTodos().contains(cliente) || !servicios.contains(servicio)) {

            throw new IllegalArgumentException("El cliente y el servicio deben estar registrados");

        }

        Viaje viaje = new Viaje(origen, destino, cliente, servicio);

        viaje.solicitar(fechaHora);

        viajes.add(viaje);

        return viaje;

    }

    // Buscar usuarios con conductores disponibles y compatibles.

    public List<Usuario> buscarConductoresDisponibles(Viaje viaje) {

        if (viaje == null || viaje.getServicio() == null) {

            return new ArrayList<>();

        }

        List<Usuario> disponibles = new ArrayList<>();

        for (Usuario usuario : usuarioDAO.listarTodos()) {

            Conductor conductor = usuario.getConductor();

            if (conductor == null

                    || conductor.getEstadoConductor() != EstadoConductor.DISPONIBLE

                    || conductor.getVehiculoActivo() == null

                    || !conductor.getVehiculoActivo().getTipoServicio().contains(viaje.getServicio().getTipoServicio())

                    || conductor.getVehiculoActivo().getTipoVehiculo() != viaje.getServicio().getTipoVehiculo()
                    || conductor.getCategoriaVehiculoActivo() == null
                    || conductor.getVehiculoActivo().getCategoriaVehiculo() == null
                    || viaje.getServicio().getCategoriaVehiculo().getCodigo() < conductor.getCategoriaVehiculoActivo()
                            .getCodigo()
                    || viaje.getServicio().getCategoriaVehiculo().getCodigo() > conductor.getVehiculoActivo()
                            .getCategoriaVehiculo().getCodigo()

                    || usuario.equals(viaje.getCliente())) {

                continue;

            }

            disponibles.add(usuario);

        }

        return disponibles;

    }

    // Obtener las solicitudes que un conductor puede aceptar.

    public List<Viaje> obtenerSolicitudesDisponibles(Usuario conductor) {

        if (conductor == null || conductor.getConductor() == null) {

            return new ArrayList<>();

        }

        List<Viaje> solicitudes = new ArrayList<>();

        for (Viaje viaje : viajes) {

            if (viaje.estadoActual() == EstadoViaje.SOLICITADO
                    && buscarConductoresDisponibles(viaje).contains(conductor)) {

                solicitudes.add(viaje);

            }

        }

        return solicitudes;

    }

    // Aceptar una solicitud todavía libre y comenzar el viaje al origen.

    public void aceptarViaje(UUID idViaje, Usuario conductor, LocalDateTime fechaHora) {

        Viaje viaje = viajeRequerido(idViaje);

        validarConductor(conductor);

        if (!buscarConductoresDisponibles(viaje).contains(conductor)) {

            throw new IllegalStateException("El conductor no está disponible o no es compatible");

        }

        viaje.aceptar(fechaHora, conductor);

        conductor.getConductor().setEstadoConductor(EstadoConductor.VIAJE_A_ORIGEN);

    }

    // Iniciar el traslado al destino con el conductor asignado.

    public void iniciarViaje(UUID idViaje, Usuario conductor, LocalDateTime fechaHora) {

        Viaje viaje = viajeRequerido(idViaje);

        validarConductorDelViaje(viaje, conductor);

        if (conductor.getConductor().getEstadoConductor() != EstadoConductor.VIAJE_A_ORIGEN) {

            throw new IllegalStateException("El conductor no está viajando al origen");

        }

        viaje.iniciar(fechaHora);

        conductor.getConductor().setEstadoConductor(EstadoConductor.VIAJE_A_DESTINO);

    }

    // Finalizar el traslado y dejar disponible al conductor.

    public void finalizarViaje(UUID idViaje, Usuario conductor, LocalDateTime fechaHora) {

        Viaje viaje = viajeRequerido(idViaje);

        validarConductorDelViaje(viaje, conductor);

        if (conductor.getConductor().getEstadoConductor() != EstadoConductor.VIAJE_A_DESTINO) {

            throw new IllegalStateException("El conductor no está trasladando al cliente");

        }

        viaje.finalizar(fechaHora, viaje.getCalificacionCliente(), viaje.getCalificacionConductor());

        conductor.getConductor().setEstadoConductor(EstadoConductor.DISPONIBLE);

    }

    // Registrar la calificación que el cliente da al conductor.

    public void calificarConductor(UUID idViaje, Usuario cliente, CalificacionViaje calificacion) {

        Viaje viaje = viajeRequerido(idViaje);

        validarCalificacion(viaje, cliente, viaje.getCliente(), calificacion);

        viaje.setCalificacionConductor(calificacion);

    }

    // Registrar la calificación que el conductor da al cliente.

    public void calificarCliente(UUID idViaje, Usuario conductor, CalificacionViaje calificacion) {

        Viaje viaje = viajeRequerido(idViaje);

        validarCalificacion(viaje, conductor, viaje.getConductor(), calificacion);

        viaje.setCalificacionCliente(calificacion);

    }

    // Calcular el cargo según quién cancela y el estado del viaje.

    public double calcularCostoCancelacion(UUID idViaje, Usuario usuario, LocalDateTime fechaHora,
            double kilometrosRecorridos) {

        Viaje viaje = viajeRequerido(idViaje);

        if (usuario == null || fechaHora == null || !Double.isFinite(kilometrosRecorridos)
                || kilometrosRecorridos < 0) {

            throw new IllegalArgumentException("Los datos de cancelación no son válidos");

        }

        if (!usuario.equals(viaje.getCliente()) && !usuario.equals(viaje.getConductor())) {

            throw new IllegalArgumentException("El usuario no participa en el viaje");

        }

        if (viaje.estadoActual() == EstadoViaje.SOLICITADO && usuario.equals(viaje.getCliente())) {

            return 0.0;

        }

        if (viaje.estadoActual() != EstadoViaje.ACEPTADO && viaje.estadoActual() != EstadoViaje.INICIADO) {

            throw new IllegalStateException("El viaje no puede cancelarse en su estado actual");

        }

        if (usuario.equals(viaje.getConductor())) {

            return 0.0;

        }

        LocalDateTime aceptacion = viaje.getRegistrosViaje().stream()
                .filter(r -> r.getEstadoViaje() == EstadoViaje.ACEPTADO)
                .findFirst().orElseThrow().getFechaHora();
        if (fechaHora.isBefore(aceptacion))
            throw new IllegalArgumentException("La fecha no puede ser anterior a la aceptación");
        double minutos = Duration.between(aceptacion, fechaHora).toMillis() / 60000.0;

        return viaje.getServicio().calcularCosto(kilometrosRecorridos, minutos);

    }

    // Cancelar, registrar el motivo y devolver el importe a cobrar.

    public double cancelarViaje(UUID idViaje, Usuario usuario, LocalDateTime fechaHora, String motivo,
            double kilometrosRecorridos) {

        Viaje viaje = viajeRequerido(idViaje);
        double costo = calcularCostoCancelacion(idViaje, usuario, fechaHora, kilometrosRecorridos);
        viaje.cancelar(fechaHora, usuario, motivo);

        if (viaje.getConductor() != null) {
            viaje.getConductor().getConductor().setEstadoConductor(EstadoConductor.DISPONIBLE);
        }

        return costo;

    }

    // Rechazar una solicitud pendiente cuyo tiempo de espera venció.

    public void rechazarViaje(UUID idViaje, LocalDateTime fechaHora) {

        Viaje viaje = viajeRequerido(idViaje);

        viaje.rechazar(fechaHora);

        if (viaje.getConductor() != null) {

            viaje.getConductor().getConductor().setEstadoConductor(EstadoConductor.DISPONIBLE);

        }

    }

    // Revisar las solicitudes y devolver las rechazadas en esta revisión.

    public List<Viaje> rechazarSolicitudesVencidas(LocalDateTime ahora) {

        if (ahora == null) {

            throw new IllegalArgumentException("La fecha no puede ser nula");

        }

        List<Viaje> rechazadas = new ArrayList<>();

        for (Viaje viaje : viajes) {

            if (viaje.estadoActual() == EstadoViaje.SOLICITADO

                    && !viaje.getRegistrosViaje().isEmpty()

                    && Duration.between(viaje.getRegistrosViaje().get(0).getFechaHora(), ahora).toMinutes() >= 5) {

                rechazarViaje(viaje.getId(), ahora);

                rechazadas.add(viaje);

            }

        }

        return rechazadas;

    }

    // Buscar un viaje por su UUID.

    public Viaje buscarViajePorId(UUID idViaje) {

        if (idViaje == null) {

            return null;

        }

        for (Viaje viaje : viajes) {

            if (idViaje.equals(viaje.getId())) {

                return viaje;

            }

        }

        return null;

    }

    // Consultar los viajes del usuario como cliente.

    public List<Viaje> obtenerViajesCliente(Usuario usuario) {

        List<Viaje> resultado = new ArrayList<>();

        if (usuario == null) {

            return resultado;

        }

        for (Viaje viaje : viajes) {

            if (usuario.equals(viaje.getCliente())) {

                resultado.add(viaje);

            }

        }

        return resultado;

    }

    // Consultar los viajes del usuario como conductor.

    public List<Viaje> obtenerViajesConductor(Usuario usuario) {

        List<Viaje> resultado = new ArrayList<>();

        if (usuario == null) {

            return resultado;

        }

        for (Viaje viaje : viajes) {

            if (usuario.equals(viaje.getConductor())) {

                resultado.add(viaje);

            }

        }

        return resultado;

    }

    private Viaje viajeRequerido(UUID idViaje) {

        Viaje viaje = buscarViajePorId(idViaje);

        if (viaje == null) {

            throw new IllegalArgumentException("No existe un viaje con ese identificador");

        }

        return viaje;

    }

    private void validarConductor(Usuario conductor) {

        if (conductor == null || conductor.getConductor() == null) {

            throw new IllegalArgumentException("El usuario no está habilitado como conductor");

        }

    }

    private void validarConductorDelViaje(Viaje viaje, Usuario conductor) {

        validarConductor(conductor);

        if (!conductor.equals(viaje.getConductor())) {

            throw new IllegalArgumentException("El conductor no está asignado al viaje");

        }

    }

    private void validarCalificacion(Viaje viaje, Usuario usuario, Usuario participanteEsperado, CalificacionViaje calificacion) {

        if (usuario == null || !usuario.equals(participanteEsperado)) {

            throw new IllegalArgumentException("El usuario no corresponde al viaje");

        }

        if (calificacion == null || viaje.estadoActual() != EstadoViaje.FINALIZADO) {

            throw new IllegalStateException("El viaje debe estar finalizado y la calificación ser válida");

        }

    }

    private boolean categoriaCompatible(CategoriaVehiculo disponible, CategoriaVehiculo solicitada) {

        return disponible != null && solicitada != null && disponible.getCodigo() >= solicitada.getCodigo();

    }

}