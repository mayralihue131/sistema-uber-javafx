package movilidad.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Viaje {

    private UUID id;
    private String motivoCancelacion;
    private Ubicacion origen;
    private Ubicacion destino;
    private Usuario cliente;
    private Usuario conductor;
    private Vehiculo vehiculo;
    private Servicio servicio;
    private RolUsuario rolCancela;
    private CalificacionViaje calificacionConductor;
    private CalificacionViaje calificacionCliente;
    private List<RegistroViaje> registrosViaje;

    public Viaje() {
        this.id = UUID.randomUUID();
        this.registrosViaje = new ArrayList<>();
    }

    public Viaje(Ubicacion origen, Ubicacion destino, Usuario cliente, Servicio servicio) {
        this.id = UUID.randomUUID();
        this.origen = origen;
        this.destino = destino;
        this.cliente = cliente;
        this.servicio = servicio;
        this.registrosViaje = new ArrayList<>();
    }
    public Viaje(Usuario cliente, Ubicacion origen,
            Ubicacion destino, Servicio servicio) {

        this(origen, destino, cliente, servicio);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public Ubicacion getOrigen() {
        return origen;
    }

    public void setOrigen(Ubicacion origen) {
        this.origen = origen;
    }

    public Ubicacion getDestino() {
        return destino;
    }

    public void setDestino(Ubicacion destino) {
        this.destino = destino;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public Usuario getConductor() {
        return conductor;
    }

    public void setConductor(Usuario conductor) {
        this.conductor = conductor;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }

    public RolUsuario getRolCancela() {
        return rolCancela;
    }

    public void setRolCancela(RolUsuario rolCancela) {
        this.rolCancela = rolCancela;
    }

    public CalificacionViaje getCalificacionConductor() {
        return calificacionConductor;
    }

    public void setCalificacionConductor(CalificacionViaje calificacionConductor) {
        this.calificacionConductor = calificacionConductor;
    }

    public CalificacionViaje getCalificacionCliente() {
        return calificacionCliente;
    }

    public void setCalificacionCliente(CalificacionViaje calificacionCliente) {
        this.calificacionCliente = calificacionCliente;
    }

    public List<RegistroViaje> getRegistrosViaje() {
        return new ArrayList<>(registrosViaje);
    }
    public List<RegistroViaje> getRegistroViaje() {
        return getRegistrosViaje();
    }

    public void solicitar(LocalDateTime fechaHora) {
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y la hora de la solicitud no pueden ser nulas");
        }

        if (cliente == null || cliente.getCliente() == null) {
            throw new IllegalStateException("El viaje no tiene un cliente valido");
        }

        if (cliente.getCliente().enViaje()) {
            throw new IllegalStateException("El cliente ya se encuentra en un viaje");
        }

        if (this.registrosViaje == null) {
            this.registrosViaje = new ArrayList<>();
        }

        if (!this.registrosViaje.isEmpty()) {
            throw new IllegalStateException("El viaje ya ha sido solicitado previamente");
        }

        RegistroViaje registro = new RegistroViaje(fechaHora, EstadoViaje.SOLICITADO);
        this.registrosViaje.add(registro);
        if (!cliente.getCliente().getViajes().contains(this)) {
            cliente.getCliente().agregarViaje(this);
        }
    }

    public void aceptar(LocalDateTime fechaHora, Usuario conductor) {

        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora no pueden ser nulas");
        }
        if (conductor == null) {
            throw new IllegalArgumentException("El conductor no puede ser nulo");
        }
        if (conductor.getConductor() == null) {
            throw new IllegalArgumentException("El usuario no esta habilitado como conductor");
        }

        if (this.estadoActual() != EstadoViaje.SOLICITADO) {
            throw new IllegalStateException("El viaje solo puede ser aceptado si se encuentra en estado SOLICITADO");
        }

        if (this.cliente != null && this.cliente.equals(conductor)) {
            throw new IllegalStateException("El conductor no puede ser el mismo usuario que solicito el viaje");
        }

        if (conductor.getConductor().getEstadoConductor() != EstadoConductor.DISPONIBLE) {
            throw new IllegalStateException("El conductor no esta disponible");
        }

        Vehiculo vehiculoActivo = conductor.getConductor().getVehiculoActivo();
        if (vehiculoActivo == null) {
            throw new IllegalStateException("El conductor no tiene un vehiculo activo");
        }

        if (servicio == null || servicio.getCategoriaVehiculo() == null
                || vehiculoActivo.getCategoriaVehiculo() == null
                || conductor.getConductor().getCategoriaVehiculoActivo() == null
                || vehiculoActivo.getTipoVehiculo() != servicio.getTipoVehiculo()
                || !vehiculoActivo.getTipoServicio().contains(servicio.getTipoServicio())
                || servicio.getCategoriaVehiculo().getCodigo() < conductor.getConductor().getCategoriaVehiculoActivo().getCodigo()
                || servicio.getCategoriaVehiculo().getCodigo() > vehiculoActivo.getCategoriaVehiculo().getCodigo()) {
            throw new IllegalStateException("El vehículo o la categoría aceptada no son compatibles");
        }

        this.vehiculo = vehiculoActivo;
        this.conductor = conductor;
        RegistroViaje registro = new RegistroViaje(fechaHora, EstadoViaje.ACEPTADO);
        this.registrosViaje.add(registro);
        conductor.getConductor().setEstadoConductor(EstadoConductor.VIAJE_A_ORIGEN);
    }

    public void iniciar(LocalDateTime fechaHora) {

    	if(fechaHora == null) {
    		throw new IllegalArgumentException("La fecha y hora no pueden ser nulas");
    	}
    		
    	if (conductor == null || conductor.getConductor() == null) {
    	    throw new IllegalStateException("El viaje no tiene un conductor habilitado");
    	}
    	if (this.estadoActual() == EstadoViaje.INICIADO) {
            throw new IllegalStateException("El viaje ya está iniciado");
        }
    	
        if (this.estadoActual() != EstadoViaje.ACEPTADO) {
    	    throw new IllegalStateException("El viaje no está en condiciones de iniciarse");
    	}

        if (conductor.getConductor().getEstadoConductor() != EstadoConductor.VIAJE_A_ORIGEN) {
            throw new IllegalStateException("El conductor no esta viajando al origen");
        }

        RegistroViaje registro = new RegistroViaje(fechaHora, EstadoViaje.INICIADO);
        this.registrosViaje.add(registro);
        conductor.getConductor().setEstadoConductor(EstadoConductor.VIAJE_A_DESTINO);
    }

    public void finalizar(LocalDateTime fechaHora, CalificacionViaje calificacionCliente,
            CalificacionViaje calificacionConductor) {
    	
    		if(fechaHora == null) {
    			throw new IllegalArgumentException("La fecha y hora no pueden ser nulas");
    		}
    		if(this.estadoActual() != EstadoViaje.INICIADO) {
            throw new IllegalStateException("El viaje solo puede finalizarse si está INICIADO");
    		}
    		if (conductor == null || conductor.getConductor() == null) {
    		    throw new IllegalArgumentException("El usuario no está habilitado como conductor");
    		}
    		
    		this.calificacionConductor = calificacionConductor;
    		this.calificacionCliente = calificacionCliente;
    		
    		RegistroViaje registro = new RegistroViaje(fechaHora, EstadoViaje.FINALIZADO);
        this.registrosViaje.add(registro);
        conductor.getConductor().setEstadoConductor(EstadoConductor.DISPONIBLE);
      
    }

    public void cancelar(LocalDateTime fechaHora, Usuario usuario, String motivo) {
    	
    		if(fechaHora == null) {
    			throw new IllegalArgumentException("La fecha y hora no pueden ser nulas");  			
    		}

    		if(usuario == null) {
    			throw new IllegalArgumentException("El usuario no puede ser nulo");  			

    		}
    		if(motivo == null || motivo.isBlank()) {
    			throw new IllegalArgumentException("El motivo tiene que ser explicado");  			

    		}
        if (this.estadoActual() != EstadoViaje.SOLICITADO
                && this.estadoActual() != EstadoViaje.ACEPTADO
                && this.estadoActual() != EstadoViaje.INICIADO) {
            throw new IllegalStateException("El viaje no puede cancelarse en su estado actual");

    		}
    		boolean esCliente = usuario.equals(this.cliente);
    		boolean esConductor = usuario.equals(this.conductor);
    		
    		if(!esCliente && !esConductor) {
    			throw new IllegalArgumentException("Solo puede cancelar el cliente o el conudctor el viaje");  			

    		}
    		   		
    		this.motivoCancelacion = motivo;
    		
    		if(esCliente) {
    			this.rolCancela = RolUsuario.CLIENTE;
    		}else{
    			this.rolCancela = RolUsuario.CONDUCTOR;
    		}
    		RegistroViaje registro = new RegistroViaje(fechaHora, EstadoViaje.CANCELADO);
    		this.registrosViaje.add(registro);
        if (this.conductor != null && this.conductor.getConductor() != null) {
            this.conductor.getConductor().setEstadoConductor(EstadoConductor.DISPONIBLE);
        }
    }
    
    public void rechazar(LocalDateTime fechaHora) {
    		if(fechaHora == null) {
    			throw new IllegalArgumentException("La fecha y hora no pueden ser nulas");
    		}
        if (this.estadoActual() != EstadoViaje.SOLICITADO
                && this.estadoActual() != EstadoViaje.ACEPTADO) {
            throw new IllegalStateException("El viaje debe estar solicitado o aceptado");
    		}

        if (this.estadoActual() == EstadoViaje.ACEPTADO
                && (this.conductor == null || this.conductor.getConductor() == null)) {
            throw new IllegalStateException("El viaje tiene que tener un conductor");

    		}
    		       
    		RegistroViaje registro = new RegistroViaje(fechaHora,EstadoViaje.RECHAZADO);
    		this.registrosViaje.add(registro);
      
    }

    public EstadoViaje estadoActual() {
        if (registrosViaje == null || registrosViaje.isEmpty()) {
            return null;
        }
        return registrosViaje.get(registrosViaje.size() - 1).getEstadoViaje();
    }

    @Override
    public String toString() {
        return "Viaje{" + "id=" + id + ", motivoCancelacion='" + motivoCancelacion + '\'' + ", origen=" + origen
                + ", destino=" + destino + ", cliente=" + (cliente != null ? cliente.getNombre() : "null")
                + ", conductor=" + (conductor != null ? conductor.getNombre() : "null") + ", vehiculo="
                + (vehiculo != null ? vehiculo.getPatente() : "null") + ", servicio="
                + (servicio != null ? servicio.getNombre() : "null") + ", rolCancela=" + rolCancela
                + ", calificacionConductor=" + calificacionConductor + ", calificacionCliente=" + calificacionCliente
                + '}';
    }
}