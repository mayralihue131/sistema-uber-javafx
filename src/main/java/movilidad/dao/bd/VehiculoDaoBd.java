package movilidad.dao.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import movilidad.dao.dto.VehiculoDTO;
import movilidad.dao.interfaces.VehiculoDAO;

/**
 * Implementación JDBC de {@link VehiculoDAO} para la base de datos PostgreSQL de la cátedra.
 *
 * <p>Esquema real consultado en el servidor:
 * <ul>
 *   <li>Tabla {@code vehiculo} (patente, modelo, capacidad_pasajeros, tipo_vehiculo, categoria_vehiculo, tipo_servicios)</li>
 * </ul>
 */
public class VehiculoDaoBd implements VehiculoDAO {

    @Override
    public List<VehiculoDTO> obtenerTodos() throws Exception {
        List<VehiculoDTO> lista = new ArrayList<>();

        String sql = "SELECT patente, modelo, capacidad_pasajeros, " +
                     "tipo_vehiculo, categoria_vehiculo, tipo_servicios FROM vehiculo ORDER BY patente";

        Connection con = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int tipoVehiculoInt = rs.getInt("tipo_vehiculo");
                String tipoVehiculo = (tipoVehiculoInt == 1) ? "MOTO" : "AUTO";

                int catInt = rs.getInt("categoria_vehiculo");
                String categoriaVehiculo = switch (catInt) {
                    case 1 -> "CONFORT";
                    case 2 -> "PREMIUM";
                    default -> "ESTANDAR";
                };

                // Decodificación de tipo_servicios (máscara de bits: 1=PASAJEROS, 2=ENVIOS, 3=AMBOS)
                int servBits = rs.getInt("tipo_servicios");
                List<String> tiposServicio = new ArrayList<>();
                if ((servBits & 1) != 0) {
                    tiposServicio.add("PASAJEROS");
                }
                if ((servBits & 2) != 0) {
                    tiposServicio.add("ENVIOS");
                }
                if (tiposServicio.isEmpty()) {
                    tiposServicio.add("PASAJEROS");
                }

                lista.add(new VehiculoDTO(
                        rs.getString("patente").trim(),
                        rs.getString("modelo").trim(),
                        rs.getInt("capacidad_pasajeros"),
                        tipoVehiculo,
                        categoriaVehiculo,
                        tiposServicio
                ));
            }
        }
        return lista;
    }

    @Override
    public void guardar(VehiculoDTO vehiculo, String emailConductor, String licenciaConducir) {
        if (vehiculo == null) {
            throw new IllegalArgumentException("El vehículo no puede ser null.");
        }

        Connection con = ConexionBD.obtenerConexion();
        try {
            boolean autoCommitOriginal = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                int tipoVehiculoInt = "MOTO".equalsIgnoreCase(vehiculo.getTipoVehiculo()) ? 1 : 0;
                int catInt = switch (vehiculo.getCategoriaVehiculo() == null ? "" : vehiculo.getCategoriaVehiculo().toUpperCase()) {
                    case "CONFORT" -> 1;
                    case "PREMIUM" -> 2;
                    default -> 0;
                };

                int servBits = 0;
                if (vehiculo.getTiposServicio() != null) {
                    for (String s : vehiculo.getTiposServicio()) {
                        if ("PASAJEROS".equalsIgnoreCase(s)) servBits |= 1;
                        if ("ENVIOS".equalsIgnoreCase(s)) servBits |= 2;
                    }
                }
                if (servBits == 0) servBits = 1;

                // 1. Insertar o actualizar registro en tabla vehiculo
                String sqlCheck = "SELECT 1 FROM vehiculo WHERE patente = ?";
                try (PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
                    psCheck.setString(1, vehiculo.getPatente());
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            String sqlUpdate = """
                                    UPDATE vehiculo
                                    SET modelo = ?, capacidad_pasajeros = ?, tipo_vehiculo = ?, categoria_vehiculo = ?, tipo_servicios = ?
                                    WHERE patente = ?
                                    """;
                            try (PreparedStatement psUp = con.prepareStatement(sqlUpdate)) {
                                psUp.setString(1, vehiculo.getModelo());
                                psUp.setInt(2, vehiculo.getCapacidadPasajeros());
                                psUp.setInt(3, tipoVehiculoInt);
                                psUp.setInt(4, catInt);
                                psUp.setInt(5, servBits);
                                psUp.setString(6, vehiculo.getPatente());
                                psUp.executeUpdate();
                            }
                        } else {
                            String sqlInsert = """
                                    INSERT INTO vehiculo (patente, modelo, capacidad_pasajeros, tipo_vehiculo, categoria_vehiculo, tipo_servicios)
                                    VALUES (?, ?, ?, ?, ?, ?)
                                    """;
                            try (PreparedStatement psIn = con.prepareStatement(sqlInsert)) {
                                psIn.setString(1, vehiculo.getPatente());
                                psIn.setString(2, vehiculo.getModelo());
                                psIn.setInt(3, vehiculo.getCapacidadPasajeros());
                                psIn.setInt(4, tipoVehiculoInt);
                                psIn.setInt(5, catInt);
                                psIn.setInt(6, servBits);
                                psIn.executeUpdate();
                            }
                        }
                    }
                }

                // 2. Asociar en tabla conductor_vehiculo si hay conductor
                if (emailConductor != null && !emailConductor.isBlank()) {
                    String sqlRelCheck = "SELECT 1 FROM conductor_vehiculo WHERE email = ? AND patente = ?";
                    try (PreparedStatement psRelCheck = con.prepareStatement(sqlRelCheck)) {
                        psRelCheck.setString(1, emailConductor.trim().toLowerCase());
                        psRelCheck.setString(2, vehiculo.getPatente());
                        try (ResultSet rsRel = psRelCheck.executeQuery()) {
                            if (!rsRel.next()) {
                                String sqlRelInsert = "INSERT INTO conductor_vehiculo (email, patente) VALUES (?, ?)";
                                try (PreparedStatement psRelInsert = con.prepareStatement(sqlRelInsert)) {
                                    psRelInsert.setString(1, emailConductor.trim().toLowerCase());
                                    psRelInsert.setString(2, vehiculo.getPatente());
                                    psRelInsert.executeUpdate();
                                }
                            }
                        }
                    }

                    // 3. Actualizar licencia de conducir en usuario si corresponde
                    if (licenciaConducir != null && !licenciaConducir.isBlank()) {
                        String sqlLic = "UPDATE usuario SET licencia_conducir = ? WHERE email = ?";
                        try (PreparedStatement psLic = con.prepareStatement(sqlLic)) {
                            psLic.setString(1, licenciaConducir.trim());
                            psLic.setString(2, emailConductor.trim().toLowerCase());
                            psLic.executeUpdate();
                        }
                    }
                }

                con.commit();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(autoCommitOriginal);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error al registrar el vehículo en la base de datos: " + e.getMessage(), e);
        }
    }
}
