package movilidad.ui;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import movilidad.modelo.*;
import static org.junit.jupiter.api.Assertions.*;

class SesionUberTest {
    @TempDir Path carpeta;
    private SesionUber nueva() throws Exception {return new SesionUber(carpeta.resolve("eventos.txt"));}

    @org.junit.jupiter.api.BeforeEach
    @org.junit.jupiter.api.AfterEach
    void limpiarBD() {
        try {
            java.sql.Connection con = movilidad.dao.bd.ConexionBD.obtenerConexion();
            try (java.sql.Statement st = con.createStatement()) {
                st.executeUpdate("DELETE FROM conductor_vehiculo WHERE email IN ('prueba@example.com', 'moto@example.com', 'PRUEBA@example.com')");
                st.executeUpdate("DELETE FROM usuario WHERE email IN ('prueba@example.com', 'moto@example.com', 'PRUEBA@example.com')");
                st.executeUpdate("DELETE FROM vehiculo WHERE patente IN ('XX123YY', 'MO123TO')");
            }
        } catch (Exception ignored) {}
    }

    private String pedir(SesionUber s) throws Exception {
        String id=UUID.randomUUID().toString();
        s.ejecutar("SOLICITAR","paula.vazquez@email.com",id,"Estandar","-42.77","-65.04","-42.78","-65.02");return id;
    }
    @Test void recorridoCompletoSeRecuperaConCalificaciones() throws Exception {
        SesionUber s=nueva();String id=pedir(s);
        s.ejecutar("CATEGORIA","juan.perez@email.com","ESTANDAR");
        s.ejecutar("ACEPTAR","juan.perez@email.com",id);
        s.ejecutar("INICIAR","juan.perez@email.com",id);
        s.ejecutar("FINALIZAR","juan.perez@email.com",id);
        s.ejecutar("CALIFICAR_CONDUCTOR","paula.vazquez@email.com",id,"EXCELENTE");
        s.ejecutar("CALIFICAR_CLIENTE","juan.perez@email.com",id,"BUENO");
        SesionUber recuperada=nueva();Viaje v=recuperada.sistema.buscarViajePorId(UUID.fromString(id));
        assertEquals(EstadoViaje.FINALIZADO,v.estadoActual());assertEquals(4,v.getRegistroViaje().size());
        assertEquals(CalificacionViaje.EXCELENTE,v.getCalificacionConductor());assertEquals(CalificacionViaje.BUENO,v.getCalificacionCliente());
        assertFalse(v.getCliente().getCliente().enViaje());
    }
    @Test void cancelarEnCursoLiberaAlConductor() throws Exception {
        SesionUber s=nueva();String id=pedir(s);
        s.ejecutar("ACEPTAR","ana.martinez@email.com",id);
        s.ejecutar("INICIAR","ana.martinez@email.com",id);
        s.ejecutar("CANCELAR","paula.vazquez@email.com",id,"Cambio de planes","1.0");
        assertEquals(EstadoViaje.CANCELADO,nueva().sistema.buscarViajePorId(UUID.fromString(id)).estadoActual());
        assertEquals(EstadoConductor.DISPONIBLE,s.sistema.buscarUsuarioPorMail("ana.martinez@email.com").getConductor().getEstadoConductor());
    }
    @Test void registroYVehiculoPersistenYDuplicadosNoAlteranArchivo() throws Exception {
        SesionUber s=nueva();s.ejecutar("USUARIO","Prueba","123","prueba@example.com");
        s.ejecutar("VEHICULO","prueba@example.com","B1","XX123YY","Auto prueba","4","AUTO","ESTANDAR","PASAJEROS,ENVIOS");
        s.ejecutar("DISPONIBLE","prueba@example.com");
        assertThrows(IllegalArgumentException.class,()->s.ejecutar("USUARIO","Duplicado","1","PRUEBA@example.com"));
        SesionUber r=nueva();assertEquals(EstadoConductor.DISPONIBLE,r.sistema.buscarUsuarioPorMail("prueba@example.com").getConductor().getEstadoConductor());
        assertEquals(1,r.sistema.getUsuarios().stream().filter(u->u.getEmail().equalsIgnoreCase("prueba@example.com")).count());
    }
    @Test void noPuedeAceptarSuPropioViajeNiUnVehiculoIncompatible() throws Exception {
        SesionUber s=nueva();String id=pedir(s);
        s.ejecutar("USUARIO","Motociclista","123","moto@example.com");
        s.ejecutar("VEHICULO","moto@example.com","B1","MO123TO","Moto","1","MOTO","ESTANDAR","PASAJEROS");
        s.ejecutar("DISPONIBLE","moto@example.com");
        assertThrows(IllegalStateException.class,()->s.ejecutar("ACEPTAR","moto@example.com",id));
        assertEquals(EstadoViaje.SOLICITADO,nueva().sistema.buscarViajePorId(UUID.fromString(id)).estadoActual());
        assertThrows(IllegalStateException.class,()->pedir(s));
    }
    @Test void unaSolicitudVencidaSeRechazaYSeGuarda() throws Exception {
        SesionUber s=nueva();String id=pedir(s);
        Path archivo=s.archivo();String linea=java.nio.file.Files.readString(archivo).trim();String[] partes=linea.split("\\|");
        partes[1]=java.util.Base64.getEncoder().encodeToString(java.time.LocalDateTime.now().minusMinutes(6).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Files.writeString(archivo,String.join("|",partes)+"\n");
        SesionUber recuperada=nueva();assertTrue(recuperada.vencer());
        assertEquals(EstadoViaje.RECHAZADO,nueva().sistema.buscarViajePorId(UUID.fromString(id)).estadoActual());
    }

    @Test void solicitarMotoApareceEnPerfilConductor() throws Exception {
        SesionUber s = nueva();
        String id = UUID.randomUUID().toString();
        // Carlos Rodriguez pide Mototaxi
        s.ejecutar("SOLICITAR", "carlos.rodriguez@email.com", id, "Mototaxi", "-42.77", "-65.04", "-42.78", "-65.02");
        // Manuel Rios (chofer de moto) abre su sesión
        Usuario manuel = s.sistema.buscarUsuarioPorMail("manuel.rios@email.com");
        java.util.List<Viaje> disponibles = s.sistema.obtenerSolicitudesDisponibles(manuel);
        assertTrue(disponibles.stream().anyMatch(v -> v.getId().equals(UUID.fromString(id))),
                "Manuel Rios debe ver la solicitud de moto");
        s.ejecutar("ACEPTAR", "manuel.rios@email.com", id);
        assertEquals(EstadoViaje.ACEPTADO, s.sistema.buscarViajePorId(UUID.fromString(id)).estadoActual());
    }
}
