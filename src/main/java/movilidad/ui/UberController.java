package movilidad.ui;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.util.StringConverter;
import movilidad.aplicacion.AplicacionUberFX;
import movilidad.modelo.*;

public final class UberController {
    private final AplicacionUberFX app;
    private final String pantalla;
    private Timeline reloj;
    private Ubicacion origenBuscado,destinoBuscado;
    private final List<Lugar> lugares=List.of(
        new Lugar("Centro · punto de ejemplo",-42.77,-65.04),
        new Lugar("Universidad · punto de ejemplo",-42.78,-65.02),
        new Lugar("Terminal · punto de ejemplo",-42.76,-65.04),
        new Lugar("Costanera · punto de ejemplo",-42.77,-65.03));
    private record Lugar(String nombre,double lat,double lon) { @Override public String toString(){return nombre;} }
    @FunctionalInterface private interface Accion { void ejecutar() throws Exception; }
    @FXML private Label mensaje,nombreSesion,saludo,cantidadViajes,cantidadActivos,resumenActivo,explicacionActivo;
    @FXML private TextField campoCorreo,nombre,telefono,correoRegistro;
    @FXML private ComboBox<Lugar> origenLugar,destinoLugar;
    @FXML private CheckBox manual,pasajeros,envios;
    @FXML private VBox coordenadas,bloqueCancelar,bloqueCalificar;
    @FXML private TextField latitudOrigen,longitudOrigen,latitudDestino,longitudDestino;
    @FXML private ComboBox<TipoServicio> tipoServicio;
    @FXML private ComboBox<CategoriaVehiculo> categoria,categoriaMinima,categoriaVehiculo;
    @FXML private ComboBox<TipoVehiculo> tipoVehiculo;
    @FXML private ComboBox<Vehiculo> vehiculoActivo;
    @FXML private ComboBox<String> rolConsulta;
    @FXML private ComboBox<CalificacionViaje> calificacion;
    @FXML private ListView<Servicio> opciones,listaServicios;
    @FXML private ListView<Viaje> listaViajes,solicitudes,viajesConductor;
    @FXML private ListView<String> historial;
    @FXML private Button btnConfirmar,btnVerActivo,btnIniciar,btnFinalizar,btnCancelar,btnCalificar;
    @FXML private Label estadoConductor,ayudaConductor,tituloVehiculo;
    @FXML private TextField licencia,patente,modelo,capacidad;
    @FXML private Label estadoViaje,tituloEstado,explicacionEstado,idViaje,rutaViaje,datosServicio,precioViaje,datosParticipantes,miCalificacion;
    @FXML private ProgressBar progreso;
    @FXML private TextField motivoCancelacion,kmRecorridos;

    public UberController(AplicacionUberFX app,String pantalla){this.app=app;this.pantalla=pantalla;}
    private SesionUber s(){return app.sesion;}
    public void detener(){if(reloj!=null)reloj.stop();}
    private void intentar(Accion accion){
        try { if(mensaje!=null)mensaje.setText(""); accion.ejecutar(); }
        catch(Exception e){ if(mensaje!=null)mensaje.setText(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()); }
    }
    private void ir(String archivo){intentar(()->app.mostrar(archivo));}
    public void preparar(Parent root){
        if(nombreSesion!=null)nombreSesion.setText(s().usuario().getNombre()+" · "+s().correo);
        String activo=switch(pantalla){case "SolicitarViajeMapa.fxml"->"Solicitar viaje";case "ConsultarServicios.fxml"->"Servicios";case "MisViajes.fxml","DetalleViaje.fxml"->"Mis viajes";case "PanelConductor.fxml"->"Panel conductor";case "RegistroConductor.fxml"->"Mis vehículos";default->"Inicio";};
        root.lookupAll(".nav").forEach(n->{if(n instanceof Button b && b.getText().equals(activo))b.getStyleClass().add("nav-active");});
        intentar(()->{
            s().vencer();
            switch(pantalla){
                case "MenuPrincipal.fxml" -> actualizarMenu();
                case "SolicitarViaje.fxml" -> prepararSolicitud();
                case "ConsultarServicios.fxml" -> { cargarFiltros(); buscarServicios(); }
                case "MisViajes.fxml" -> {
                    configurarViajes(listaViajes); rolConsulta.getItems().setAll("Cliente","Conductor","Todos");
                    rolConsulta.setValue("Cliente"); rolConsulta.setOnAction(e->actualizarViajes()); actualizarViajes();
                }
                case "PanelConductor.fxml" -> prepararPanel();
                case "RegistroConductor.fxml" -> prepararVehiculo();
                case "DetalleViaje.fxml" -> {calificacion.getItems().setAll(Arrays.stream(CalificacionViaje.values()).filter(c->c!=CalificacionViaje.NO_CALIFICADO).toList());kmRecorridos.setText("0");actualizarDetalle();}
                default -> {}
            }
        });
        if(!pantalla.equals("Inicio.fxml")&&!pantalla.equals("RegistroUsuario.fxml")){
            reloj=new Timeline(new KeyFrame(Duration.seconds(2),e->{try {
                boolean cambio=s().vencer();
                if(pantalla.equals("DetalleViaje.fxml"))pintarDetalle();
                if(pantalla.equals("PanelConductor.fxml"))refrescarListasConductor();
                if(cambio && pantalla.equals("MisViajes.fxml"))actualizarViajes();
                if(cambio && pantalla.equals("MenuPrincipal.fxml"))actualizarMenu();
            } catch(Exception ex) {mensaje.setText("No se pudo actualizar: "+ex.getMessage());}})); reloj.setCycleCount(Timeline.INDEFINITE);reloj.play();
        }
        root.lookupAll(".field-group").forEach(n->HBox.setHgrow(n,Priority.ALWAYS));
    }
    @FXML private void abrirInicio(){ir("Inicio.fxml");}
    @FXML private void ingresar(){intentar(()->{s().ingresar(campoCorreo.getText());app.mostrar("MenuPrincipal.fxml");});}
    @FXML private void abrirRegistroUsuario(){ir("RegistroUsuario.fxml");}
    @FXML private void registrarUsuario(){intentar(()->{
        String n=requerido(nombre,"nombre"),tel=requerido(telefono,"teléfono"),mail=requerido(correoRegistro,"correo").toLowerCase(Locale.ROOT);
        if(!mail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalArgumentException("Revisá el formato del correo.");
        s().ejecutar("USUARIO",n,tel,mail);s().ingresar(mail);app.mostrar("MenuPrincipal.fxml");
    });}
    @FXML private void cerrarSesion(){s().correo=null;s().viajeSeleccionado=null;ir("Inicio.fxml");}
    @FXML private void abrirMenu(){ir("MenuPrincipal.fxml");}
    @FXML private void abrirSolicitud(){intentar(()->{
        Viaje activo=activoCliente();
        if(activo!=null){s().viajeSeleccionado=activo.getId();app.mostrar("DetalleViaje.fxml");return;}
        if(ocupado(s().usuario()))throw new IllegalStateException("Primero finalizá el viaje que estás conduciendo.");
        app.mostrar("SolicitarViajeMapa.fxml");
    });}
    @FXML private void abrirServicios(){ir("ConsultarServicios.fxml");}
    @FXML private void abrirViajes(){ir("MisViajes.fxml");}
    @FXML private void abrirPanel(){ir("PanelConductor.fxml");}
    @FXML private void abrirRegistroConductor(){ir("RegistroConductor.fxml");}
    private boolean activo(Viaje v){return v.estadoActual()==EstadoViaje.SOLICITADO||v.estadoActual()==EstadoViaje.ACEPTADO||v.estadoActual()==EstadoViaje.INICIADO;}
    private boolean ocupado(Usuario u){return u.getConductor()!=null&&(u.getConductor().getEstadoConductor()==EstadoConductor.VIAJE_A_ORIGEN||u.getConductor().getEstadoConductor()==EstadoConductor.VIAJE_A_DESTINO);}
    private Viaje activoCliente(){return s().sistema.obtenerViajesCliente(s().usuario()).stream().filter(this::activo).findFirst().orElse(null);}
    private void actualizarMenu(){
        Usuario u=s().usuario();List<Viaje> viajes=s().sistema.getViajes().stream().filter(v->u.equals(v.getCliente())||u.equals(v.getConductor())).toList();
        saludo.setText("Hola, "+u.getNombre());cantidadViajes.setText(""+s().sistema.obtenerViajesCliente(u).size());cantidadActivos.setText(""+viajes.stream().filter(this::activo).count());
        Viaje v=viajes.stream().filter(this::activo).findFirst().orElse(null);btnVerActivo.setVisible(v!=null);btnVerActivo.setManaged(v!=null);
        resumenActivo.setText(v==null?"Listo para tu próximo recorrido":estadoTexto(v.estadoActual()));
        explicacionActivo.setText(v==null?"Elegí un origen, un destino y el servicio que necesitás.":"Tu viaje sigue disponible aunque cambies de cuenta o cierres la aplicación.");
    }
    @FXML private void verActivo(){intentar(()->{
        Usuario u=s().usuario();Viaje v=s().sistema.getViajes().stream().filter(this::activo).filter(x->u.equals(x.getCliente())||u.equals(x.getConductor())).findFirst().orElseThrow();abrirDetalle(v);
    });}
    private void cargarFiltros(){tipoServicio.getItems().setAll(TipoServicio.values());categoria.getItems().setAll(CategoriaVehiculo.values());tipoServicio.setValue(TipoServicio.PASAJEROS);categoria.setValue(CategoriaVehiculo.ESTANDAR);}
    private void prepararSolicitud(){
        cargarFiltros();origenLugar.getItems().setAll(lugares);destinoLugar.getItems().setAll(lugares);cambiarModo();
        btnConfirmar.setDisable(true);opciones.getSelectionModel().selectedItemProperty().addListener((o,a,b)->btnConfirmar.setDisable(b==null));
        origenLugar.valueProperty().addListener((o,a,b)->limpiar());destinoLugar.valueProperty().addListener((o,a,b)->limpiar());
        tipoServicio.valueProperty().addListener((o,a,b)->limpiar());categoria.valueProperty().addListener((o,a,b)->limpiar());
        for(TextField f:List.of(latitudOrigen,longitudOrigen,latitudDestino,longitudDestino))f.textProperty().addListener((o,a,b)->limpiar());
    }
    private void limpiar(){opciones.getItems().clear();origenBuscado=null;destinoBuscado=null;}
    @FXML private void cambiarModo(){coordenadas.setVisible(manual.isSelected());coordenadas.setManaged(manual.isSelected());origenLugar.setDisable(manual.isSelected());destinoLugar.setDisable(manual.isSelected());limpiar();}
    private double numero(TextField f,String nombre,double min,double max){
        double n;try{n=Double.parseDouble(f.getText().trim().replace(',','.'));}catch(NumberFormatException e){throw new IllegalArgumentException("Completá "+nombre+" con un número.");}
        if(!Double.isFinite(n)||n<min||n>max)throw new IllegalArgumentException(nombre+": valor fuera del rango permitido.");return n;
    }
    private Ubicacion ubicacion(boolean origen){
        if(manual.isSelected())return new Ubicacion(numero(origen?latitudOrigen:latitudDestino,"latitud",-90,90),numero(origen?longitudOrigen:longitudDestino,"longitud",-180,180));
        Lugar l=origen?origenLugar.getValue():destinoLugar.getValue();if(l==null)throw new IllegalArgumentException("Elegí origen y destino.");return new Ubicacion(l.lat,l.lon);
    }
    @FXML private void buscarOpciones(){intentar(()->{
        limpiar();Ubicacion o=ubicacion(true),d=ubicacion(false);if(o.calcularDistancia(d)<=0)throw new IllegalArgumentException("Elegí dos lugares distintos.");
        List<Servicio> servicios=s().sistema.consultarServicios(tipoServicio.getValue(),categoria.getValue());origenBuscado=o;destinoBuscado=d;
        celdas(opciones,x->x.getNombre()+" · "+x.getTipoVehiculo(),x->{
            double km=o.calcularDistancia(d),min=s().sistema.estimarTiempo(x,o,d);return String.format(java.util.Locale.forLanguageTag("es-AR"),"$%,.2f estimados · %.2f km · %.1f min",x.calcularCosto(km,min),km,min);
        });opciones.getItems().setAll(servicios);
        mensaje.setText(servicios.isEmpty()?"No hay servicios para esta combinación. Probá otra categoría.":"Seleccioná una opción resaltada para confirmar. La aceptación del conductor es un paso posterior.");
    });}
    @FXML private void confirmarViaje(){intentar(()->{
        Servicio servicio=opciones.getSelectionModel().getSelectedItem();if(servicio==null||origenBuscado==null)throw new IllegalArgumentException("Buscá y seleccioná una opción.");
        UUID id=UUID.randomUUID();s().ejecutar("SOLICITAR",s().correo,id.toString(),servicio.getNombre(),""+origenBuscado.getLatitud(),""+origenBuscado.getLongitud(),""+destinoBuscado.getLatitud(),""+destinoBuscado.getLongitud());
        s().viajeSeleccionado=id;app.mostrar("DetalleViaje.fxml");
    });}
    @FXML private void buscarServicios(){intentar(()->{
        celdas(listaServicios,x->x.getNombre()+" · "+x.getTipoServicio()+" · "+x.getTipoVehiculo(),x->String.format("Base $%,.2f · km $%,.2f · minuto $%,.2f",x.getTarifaBase(),x.getPrecioKm(),x.getPrecioMinuto()));
        listaServicios.getItems().setAll(s().sistema.consultarServicios(tipoServicio.getValue(),categoria.getValue()));
    });}
    private <T> void celdas(ListView<T> lista,Function<T,String> titulo,Function<T,String> detalle){
        lista.setCellFactory(l->new ListCell<T>(){@Override protected void updateItem(T item,boolean vacia){super.updateItem(item,vacia);setText(null);if(vacia||item==null){setGraphic(null);return;}Label t=new Label(titulo.apply(item));t.getStyleClass().add("field-label");t.setWrapText(true);Label d=new Label(detalle.apply(item));d.getStyleClass().add("muted");d.setWrapText(true);VBox b=new VBox(7,t,d);b.prefWidthProperty().bind(lista.widthProperty().subtract(58));setGraphic(b);}});
    }
    private void configurarViajes(ListView<Viaje> lista){celdas(lista,v->v.getServicio().getNombre()+" · "+estadoTexto(v.estadoActual()),v->v.getCliente().getNombre()+" · "+fecha(v.getRegistrosViaje().get(0).getFechaHora())+" · "+v.getId().toString().substring(0,8));}
    @FXML private void actualizarViajes(){intentar(()->{
        Usuario u=s().usuario();List<Viaje> viajes=s().sistema.getViajes().stream().filter(v->switch(rolConsulta.getValue()){case "Cliente"->u.equals(v.getCliente());case "Conductor"->u.equals(v.getConductor());default->u.equals(v.getCliente())||u.equals(v.getConductor());}).collect(Collectors.toCollection(ArrayList::new));Collections.reverse(viajes);listaViajes.getItems().setAll(viajes);
    });}
    private void abrirDetalle(Viaje v)throws IOException{if(v==null)throw new IllegalArgumentException("Seleccioná un viaje de la lista.");s().viajeSeleccionado=v.getId();app.mostrar("DetalleViaje.fxml");}
    @FXML private void verDetalle(){intentar(()->abrirDetalle(listaViajes.getSelectionModel().getSelectedItem()));}
    private void prepararVehiculo(){tipoVehiculo.getItems().setAll(TipoVehiculo.values());categoriaVehiculo.getItems().setAll(CategoriaVehiculo.values());tipoVehiculo.setValue(TipoVehiculo.AUTO);categoriaVehiculo.setValue(CategoriaVehiculo.ESTANDAR);pasajeros.setSelected(true);Usuario u=s().usuario();tituloVehiculo.setText(u.getConductor()==null?"Habilitar perfil de conductor":"Agregar otro vehículo");if(u.getConductor()!=null){licencia.setText(u.getConductor().getLicenciaConducir());licencia.setDisable(true);}}
    private String requerido(TextField f,String nombre){String v=f.getText().trim();if(v.isBlank())throw new IllegalArgumentException("Completá "+nombre+".");if(v.contains("\n")||v.contains("\r"))throw new IllegalArgumentException("Revisá "+nombre+".");return v;}
    @FXML private void registrarVehiculo(){intentar(()->{
        String lic=requerido(licencia,"licencia"),pat=requerido(patente,"patente").toUpperCase(Locale.ROOT),mod=requerido(modelo,"modelo");
        double cap=numero(capacidad,"capacidad",1,50);if(cap!=(int)cap)throw new IllegalArgumentException("La capacidad debe ser un número entero.");
        if(!pasajeros.isSelected()&&!envios.isSelected())throw new IllegalArgumentException("Elegí al menos un tipo de servicio.");
        String tipos=pasajeros.isSelected()?(envios.isSelected()?"PASAJEROS,ENVIOS":"PASAJEROS"):"ENVIOS";
        s().ejecutar("VEHICULO",s().correo,lic,pat,mod,""+(int)cap,tipoVehiculo.getValue().name(),categoriaVehiculo.getValue().name(),tipos);app.mostrar("PanelConductor.fxml");
    });}
    private void prepararPanel(){
        categoriaMinima.getItems().setAll(CategoriaVehiculo.values());vehiculoActivo.setConverter(new StringConverter<Vehiculo>(){public String toString(Vehiculo v){return v==null?"":v.getModelo()+" · "+v.getPatente();}public Vehiculo fromString(String x){return null;}});
        configurarViajes(solicitudes);configurarViajes(viajesConductor);actualizarPanel();
    }
    @FXML private void actualizarPanel(){intentar(()->{
        Usuario u=s().usuario();boolean conductor=u.getConductor()!=null;
        vehiculoActivo.setDisable(!conductor||ocupado(u));categoriaMinima.setDisable(!conductor||ocupado(u));
        if(conductor){vehiculoActivo.getItems().setAll(u.getConductor().getVehiculos());vehiculoActivo.setValue(u.getConductor().getVehiculoActivo());categoriaMinima.setValue(u.getConductor().getCategoriaVehiculoActivo());}
        ayudaConductor.setText(conductor?"Tu vehículo debe ofrecer el tipo de servicio y una categoría compatible. La categoría mínima no puede superar la del vehículo.":"Todavía no sos conductor. Tocá Agregar vehículo para habilitarte.");refrescarListasConductor();
    });}
    private void refrescarListasConductor(){
        Usuario u=s().usuario();estadoConductor.setText(u.getConductor()==null?"Perfil de conductor pendiente":"Estado: "+u.getConductor().getEstadoConductor().name().replace('_',' '));
        sincronizar(solicitudes,s().sistema.obtenerSolicitudesDisponibles(u));sincronizar(viajesConductor,s().sistema.obtenerViajesConductor(u).stream().filter(this::activo).toList());
    }
    private void sincronizar(ListView<Viaje> lista,List<Viaje> datos){UUID id=lista.getSelectionModel().getSelectedItem()==null?null:lista.getSelectionModel().getSelectedItem().getId();if(!lista.getItems().equals(datos)){lista.getItems().setAll(datos);if(id!=null)datos.stream().filter(v->v.getId().equals(id)).findFirst().ifPresent(v->lista.getSelectionModel().select(v));}}
    private void exigirConductor(){if(s().usuario().getConductor()==null)throw new IllegalStateException("Primero registrá tu licencia y vehículo.");}
    @FXML private void aplicarVehiculo(){intentar(()->{
        exigirConductor();Vehiculo v=vehiculoActivo.getValue();if(v==null||categoriaMinima.getValue()==null)throw new IllegalArgumentException("Seleccioná vehículo y categoría mínima.");
        if(categoriaMinima.getValue().getCodigo()>v.getCategoriaVehiculo().getCodigo())throw new IllegalArgumentException("La categoría mínima supera la del vehículo.");
        s().ejecutar("ACTIVO",s().correo,v.getPatente());s().ejecutar("CATEGORIA",s().correo,categoriaMinima.getValue().name());actualizarPanel();mensaje.setText("Vehículo y categoría actualizados.");
    });}
    @FXML private void ponerDisponible(){intentar(()->{exigirConductor();s().ejecutar("DISPONIBLE",s().correo);actualizarPanel();mensaje.setText("Ya podés recibir solicitudes compatibles.");});}
    @FXML private void fueraServicio(){intentar(()->{exigirConductor();s().ejecutar("FUERA",s().correo);actualizarPanel();});}
    @FXML private void aceptarSolicitud(){intentar(()->{
        s().vencer();Viaje v=solicitudes.getSelectionModel().getSelectedItem();if(v==null)throw new IllegalArgumentException("Seleccioná una solicitud.");
        s().ejecutar("ACEPTAR",s().correo,v.getId().toString());s().viajeSeleccionado=v.getId();app.mostrar("DetalleViaje.fxml");
    });}
    @FXML private void verViajeConductor(){intentar(()->{Viaje v=viajesConductor.getSelectionModel().getSelectedItem();if(v==null&&viajesConductor.getItems().size()==1)v=viajesConductor.getItems().get(0);abrirDetalle(v);});}
    private String fecha(LocalDateTime f){return f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));}
    private String estadoTexto(EstadoViaje e){return switch(e){case SOLICITADO->"Esperando conductor";case ACEPTADO->"Conductor en camino al origen";case INICIADO->"Viaje en curso";case FINALIZADO->"Viaje finalizado";case CANCELADO->"Viaje cancelado";case RECHAZADO->"Solicitud vencida";};}
    private String lugar(Ubicacion u){return lugares.stream().filter(l->Math.abs(l.lat-u.getLatitud())<0.000001&&Math.abs(l.lon-u.getLongitud())<0.000001).map(Lugar::nombre).findFirst().orElse(String.format("%.5f, %.5f",u.getLatitud(),u.getLongitud()));}
    @FXML private void actualizarDetalle(){intentar(()->{s().vencer();pintarDetalle();});}
    private void pintarDetalle(){
        Viaje v=s().viaje();if(v==null)throw new IllegalArgumentException("No se encontró el viaje.");
        Usuario u=s().usuario();boolean cliente=u.equals(v.getCliente()),conductor=u.equals(v.getConductor());
        if(!cliente&&!conductor)throw new IllegalStateException("Este viaje no pertenece a tu cuenta.");
        EstadoViaje e=v.estadoActual();estadoViaje.setText(e.name());tituloEstado.setText(estadoTexto(e));idViaje.setText("Solicitud "+v.getId());
        explicacionEstado.setText(switch(e){case SOLICITADO->"Tu solicitud está registrada. Un conductor compatible puede aceptarla desde su panel. Vence a los 5 minutos sin aceptación.";case ACEPTADO->"El conductor va al origen. Cuando suba el pasajero, el conductor debe iniciar el traslado.";case INICIADO->"El traslado está en curso. El conductor puede marcar la llegada al destino.";case FINALIZADO->"Llegaste al destino. Ya podés calificar a la otra persona.";case CANCELADO->"Motivo: "+v.getMotivoCancelacion();case RECHAZADO->"Pasaron 5 minutos sin aceptación. Podés solicitar otro viaje.";});
        progreso.setProgress(switch(e){case SOLICITADO->0.15;case ACEPTADO->0.4;case INICIADO->0.7;default->1.0;});
        rutaViaje.setText(lugar(v.getOrigen())+" → "+lugar(v.getDestino()));
        Servicio servicio=v.getServicio();double km=v.getOrigen().calcularDistancia(v.getDestino()),min=s().sistema.estimarTiempo(servicio,v.getOrigen(),v.getDestino());
        datosServicio.setText(servicio.getNombre()+" · "+servicio.getTipoServicio()+" · "+servicio.getCategoriaVehiculo()+String.format(" · %.2f km · %.1f min estimados",km,min));
        precioViaje.setText(String.format(java.util.Locale.forLanguageTag("es-AR"),"$%,.2f · precio estimado",servicio.calcularCosto(km,min)));
        datosParticipantes.setText("Cliente: "+v.getCliente().getNombre()+"\nConductor: "+(v.getConductor()==null?"pendiente de aceptación":v.getConductor().getNombre()+" · "+v.getVehiculo().getModelo()+" · "+v.getVehiculo().getPatente()));
        visible(btnIniciar,conductor&&e==EstadoViaje.ACEPTADO);visible(btnFinalizar,conductor&&e==EstadoViaje.INICIADO);
        visible(bloqueCancelar,e==EstadoViaje.SOLICITADO||e==EstadoViaje.ACEPTADO||e==EstadoViaje.INICIADO);
        kmRecorridos.setDisable(e==EstadoViaje.SOLICITADO||conductor);
        visible(bloqueCalificar,e==EstadoViaje.FINALIZADO);
        CalificacionViaje propia=cliente?v.getCalificacionConductor():v.getCalificacionCliente();miCalificacion.setText(propia==null||propia==CalificacionViaje.NO_CALIFICADO?"Todavía no calificaste.":"Tu evaluación guardada: "+propia.name().replace('_',' '));
        List<String> registros=v.getRegistrosViaje().stream().map(r->fecha(r.getFechaHora())+" · "+estadoTexto(r.getEstadoViaje())).toList();if(!historial.getItems().equals(registros))historial.getItems().setAll(registros);
    }
    private void visible(javafx.scene.Node n,boolean v){n.setVisible(v);n.setManaged(v);}
    private void accionViaje(String accion){intentar(()->{s().ejecutar(accion,s().correo,s().viajeSeleccionado.toString());pintarDetalle();mensaje.setText("Estado actualizado y guardado.");});}
    @FXML private void iniciarViaje(){accionViaje("INICIAR");}
    @FXML private void finalizarViaje(){accionViaje("FINALIZAR");}
    @FXML private void cancelarViaje(){intentar(()->{
        Viaje v=s().viaje();String motivo=requerido(motivoCancelacion,"el motivo");
        double km=(v.estadoActual()==EstadoViaje.SOLICITADO||s().usuario().equals(v.getConductor()))?0:numero(kmRecorridos,"kilómetros recorridos",0,100000);
        double cargo=s().sistema.calcularCostoCancelacion(v.getId(),s().usuario(),LocalDateTime.now(),km);
        Alert confirmar=new Alert(Alert.AlertType.CONFIRMATION,String.format("Cargo calculado: $%,.2f.\n¿Querés cancelar este viaje?",cargo),ButtonType.YES,ButtonType.NO);confirmar.setHeaderText("Cancelar viaje");
        if(confirmar.showAndWait().orElse(ButtonType.NO)!=ButtonType.YES)return;
        s().ejecutar("CANCELAR",s().correo,v.getId().toString(),motivo,""+km);pintarDetalle();mensaje.setText(String.format("Viaje cancelado. Cargo: $%,.2f (sin cobro electrónico).",cargo));
    });}
    @FXML private void calificar(){intentar(()->{
        if(calificacion.getValue()==null)throw new IllegalArgumentException("Elegí una calificación.");
        String accion=s().usuario().equals(s().viaje().getCliente())?"CALIFICAR_CONDUCTOR":"CALIFICAR_CLIENTE";
        s().ejecutar(accion,s().correo,s().viajeSeleccionado.toString(),calificacion.getValue().name());pintarDetalle();mensaje.setText("Tu calificación se guardó. La otra persona no puede verla.");
    });}
}
