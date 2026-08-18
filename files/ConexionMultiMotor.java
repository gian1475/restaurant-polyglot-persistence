// =====================================================================
// ARTEFACTO 4 — JAVA / JDBC
// Conexión simultánea a PostgreSQL (front-office) y Oracle (back-office)
// =====================================================================
//
// Drivers necesarios (agrégalos al classpath o como dependencias Maven):
//   - org.postgresql:postgresql:42.7.x
//   - com.oracle.database.jdbc:ojdbc11:23.x
//
// Esta clase es el "puente" que resuelve el problema de las FK lógicas:
// antes de insertar un pedido en PostgreSQL, valida en Oracle que el
// empleado, la sucursal y el producto realmente existen.

// package com.restaurante.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConexionMultiMotor {

    // ---- Configuración PostgreSQL ----
    private static final String PG_URL = com.restaurante.conexion.ConfiguracionConexiones.PG_URL != null ? 
            com.restaurante.conexion.ConfiguracionConexiones.PG_URL : "jdbc:postgresql://localhost:5432/neondb?sslmode=require";
    private static final String PG_USER = com.restaurante.conexion.ConfiguracionConexiones.PG_ADMIN_USER != null ?
            com.restaurante.conexion.ConfiguracionConexiones.PG_ADMIN_USER : "neondb_owner";
    private static final String PG_PASS = com.restaurante.conexion.ConfiguracionConexiones.PG_ADMIN_PASS != null ?
            com.restaurante.conexion.ConfiguracionConexiones.PG_ADMIN_PASS : "";

    // ---- Configuración Oracle ----
    private static final String ORA_URL = com.restaurante.conexion.ConfiguracionConexiones.ORA_URL != null ?
            com.restaurante.conexion.ConfiguracionConexiones.ORA_URL : "jdbc:oracle:thin:@db_high?TNS_ADMIN=./wallet";
    private static final String ORA_USER = com.restaurante.conexion.ConfiguracionConexiones.ORA_USER != null ?
            com.restaurante.conexion.ConfiguracionConexiones.ORA_USER : "admin";
    private static final String ORA_PASS = com.restaurante.conexion.ConfiguracionConexiones.ORA_PASS != null ?
            com.restaurante.conexion.ConfiguracionConexiones.ORA_PASS : "";

    private Connection conexionPostgres;
    private Connection conexionOracle;

    /** Abre ambas conexiones y reporta su estado de manera independiente. */
    public void conectar() {
        // Conectar a PostgreSQL
        try {
            conexionPostgres = DriverManager.getConnection(PG_URL, PG_USER, PG_PASS);
            System.out.println("PostgreSQL: Conectado con éxito [OK]");
        } catch (SQLException e) {
            System.err.println("PostgreSQL: Error de conexión [ERROR] - " + e.getMessage());
        }

        // Conectar a Oracle
        try {
            conexionOracle = DriverManager.getConnection(ORA_URL, ORA_USER, ORA_PASS);
            System.out.println("Oracle: Conectado con éxito [OK]");
        } catch (SQLException e) {
            System.err.println("Oracle: Error de conexión [ERROR] - " + e.getMessage());
        }
    }

    public void cerrar() throws SQLException {
        if (conexionPostgres != null && !conexionPostgres.isClosed())
            conexionPostgres.close();
        if (conexionOracle != null && !conexionOracle.isClosed())
            conexionOracle.close();
    }

    /**
     * Valida en Oracle que el empleado, sucursal y producto existen,
     * y que el producto está disponible. Esto reemplaza la FK física
     * que no se puede crear entre motores distintos.
     */
    public boolean validarReferenciasOracle(int empleadoId, int sucursalId, int productoId) throws SQLException {
        String sql = "SELECT " +
                "  (SELECT COUNT(*) FROM empleado WHERE empleado_id = ? AND sucursal_id = ?) AS emp_ok, " +
                "  (SELECT COUNT(*) FROM producto WHERE producto_id = ? AND disponible = 'S') AS prod_ok " +
                "FROM dual";

        try (PreparedStatement ps = conexionOracle.prepareStatement(sql)) {
            ps.setInt(1, empleadoId);
            ps.setInt(2, sucursalId);
            ps.setInt(3, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("emp_ok") > 0 && rs.getInt("prod_ok") > 0;
                }
            }
        }
        return false;
    }

    /**
     * Registra un pedido en PostgreSQL, pero solo después de validar
     * sus referencias cruzadas contra Oracle. Demuestra el uso
     * simultáneo de ambos motores en una sola operación de negocio.
     */
    public int registrarPedidoConValidacionCruzada(int clienteId, int empleadoId, int sucursalId,
            int productoId, int cantidad, double precioUnitario,
            String tipoPedido) throws SQLException {

        if (!validarReferenciasOracle(empleadoId, sucursalId, productoId)) {
            throw new IllegalStateException(
                    "Referencia inválida: el empleado/sucursal/producto no existe o el producto no está disponible (verificado en Oracle).");
        }

        String sqlPedido = "INSERT INTO pedido (cliente_id, empleado_id, sucursal_id, tipo_pedido) " +
                "VALUES (?, ?, ?, ?) RETURNING pedido_id";
        String sqlDetalle = "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad, precio_unitario) " +
                "VALUES (?, ?, ?, ?)";

        int pedidoId;
        boolean autoCommitOriginal = conexionPostgres.getAutoCommit();
        try {
            conexionPostgres.setAutoCommit(false);

            try (PreparedStatement ps = conexionPostgres.prepareStatement(sqlPedido)) {
                ps.setInt(1, clienteId);
                ps.setInt(2, empleadoId);
                ps.setInt(3, sucursalId);
                ps.setString(4, tipoPedido);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    pedidoId = rs.getInt("pedido_id");
                }
            }

            try (PreparedStatement ps = conexionPostgres.prepareStatement(sqlDetalle)) {
                ps.setInt(1, pedidoId);
                ps.setInt(2, productoId);
                ps.setInt(3, cantidad);
                ps.setDouble(4, precioUnitario);
                ps.executeUpdate();
            }

            conexionPostgres.commit();
            System.out.println("Pedido #" + pedidoId + " registrado y validado contra Oracle correctamente.");
            return pedidoId;

        } catch (SQLException e) {
            conexionPostgres.rollback();
            throw e;
        } finally {
            conexionPostgres.setAutoCommit(autoCommitOriginal);
        }
    }

    /**
     * Consulta combinada: trae el nombre real del empleado/producto desde Oracle
     * para enriquecer un pedido que vive en PostgreSQL (join "manual" entre
     * motores).
     */
    public void imprimirPedidoEnriquecido(int pedidoId) throws SQLException {
        String sqlPg = "SELECT pedido_id, empleado_id, sucursal_id FROM pedido WHERE pedido_id = ?";
        int empleadoId = -1, sucursalId = -1;

        try (PreparedStatement ps = conexionPostgres.prepareStatement(sqlPg)) {
            ps.setInt(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    empleadoId = rs.getInt("empleado_id");
                    sucursalId = rs.getInt("sucursal_id");
                }
            }
        }

        if (empleadoId == -1) {
            System.out.println("Pedido no encontrado.");
            return;
        }

        String sqlOra = "SELECT nombres, apellidos FROM empleado WHERE empleado_id = ?";
        try (PreparedStatement ps = conexionOracle.prepareStatement(sqlOra)) {
            ps.setInt(1, empleadoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Pedido #" + pedidoId + " atendido por: " +
                            rs.getString("nombres") + " " + rs.getString("apellidos") +
                            " (sucursal_id=" + sucursalId + ")");
                }
            }
        }
    }

    // ---- Punto de entrada de prueba ----
    public static void main(String[] args) {
        ConexionMultiMotor conexion = new ConexionMultiMotor();
        conexion.conectar();

        try {
            // Solo intentar pruebas de negocio si ambas conexiones están activas
            if (conexion.conexionPostgres != null && !conexion.conexionPostgres.isClosed() &&
                conexion.conexionOracle != null && !conexion.conexionOracle.isClosed()) {
                
                System.out.println("\n--- Ejecutando pruebas de negocio ---");
                int nuevoPedido = conexion.registrarPedidoConValidacionCruzada(
                        1, // cliente_id (Postgres)
                        3, // empleado_id (validado en Oracle)
                        1, // sucursal_id (validado en Oracle)
                        1, // producto_id (validado en Oracle)
                        2, // cantidad
                        18.00, // precio_unitario
                        "SALON");

                conexion.imprimirPedidoEnriquecido(nuevoPedido);
            } else {
                System.out.println("\n[AVISO] No se ejecutaron pruebas de negocio por fallas de conexión.");
            }
        } catch (SQLException e) {
            System.err.println("Error durante la prueba de negocio: " + e.getMessage());
        } finally {
            try {
                conexion.cerrar();
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexiones: " + e.getMessage());
            }
        }
    }
}
