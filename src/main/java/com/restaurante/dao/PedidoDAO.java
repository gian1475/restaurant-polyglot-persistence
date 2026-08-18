package com.restaurante.dao;

import com.restaurante.conexion.ConexionManager;
import com.restaurante.modelo.DetallePedido;
import com.restaurante.modelo.Pedido;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Operaciones DAO transaccionales de Pedido en PostgreSQL
 * con lógica de Validación Cruzada contra Oracle en tiempo real.
 * 
 * @author Gianf
 */
public class PedidoDAO {

    /**
     * Valida en Oracle si la sucursal existe, el empleado existe y está asignado a esa sucursal,
     * y si un determinado producto existe y se encuentra disponible.
     * Esto suple la restricción de llave foránea física entre motores de base de datos.
     */
    public boolean validarReferenciasEnOracle(int empleadoId, int sucursalId, int productoId) throws SQLException {
        Connection connOracle = ConexionManager.getInstancia().getConexionOracle();
        if (connOracle == null) {
            throw new SQLException("Oracle: Sin conexión activa para validar referencias.");
        }

        String sql = "SELECT " +
                     "  (SELECT COUNT(*) FROM empleado WHERE empleado_id = ? AND sucursal_id = ? AND estado = 'ACTIVO') AS emp_ok, " +
                     "  (SELECT COUNT(*) FROM producto WHERE producto_id = ? AND disponible = 'S') AS prod_ok " +
                     "FROM dual";

        try (PreparedStatement ps = connOracle.prepareStatement(sql)) {
            ps.setInt(1, empleadoId);
            ps.setInt(2, sucursalId);
            ps.setInt(3, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean empleadoValido = rs.getInt("emp_ok") > 0;
                    boolean productoValido = rs.getInt("prod_ok") > 0;
                    return empleadoValido && productoValido;
                }
            }
        }
        return false;
    }

    /**
     * Registra un pedido en PostgreSQL de forma atómica.
     * Antes de realizar la inserción, valida las referencias de negocio en Oracle.
     * Si la validación falla para algún producto, lanza una excepción de negocio
     * y la transacción no se inicia. Si ocurre un fallo SQL, se realiza un rollback.
     */
    public int registrarPedidoConValidacionCruzada(Pedido pedido) throws SQLException, IllegalStateException {
        Connection connPostgres = ConexionManager.getInstancia().getConexionPostgres();
        if (connPostgres == null) {
            throw new SQLException("PostgreSQL: Sin conexión activa para registrar el pedido.");
        }

        if (pedido.getDetalles().isEmpty()) {
            throw new IllegalStateException("El pedido debe contener al menos un detalle.");
        }

        // 1. Validación Cruzada contra Oracle para cada producto del detalle
        for (DetallePedido detalle : pedido.getDetalles()) {
            boolean valido = validarReferenciasEnOracle(pedido.getEmpleadoId(), pedido.getSucursalId(), detalle.getProductoId());
            if (!valido) {
                throw new IllegalStateException("Validación de Referencia Cruzada Fallida (Oracle): " +
                        "El empleado ID #" + pedido.getEmpleadoId() + " no está activo en la sucursal #" + pedido.getSucursalId() +
                        ", o el producto ID #" + detalle.getProductoId() + " no está disponible.");
            }
        }

        // 2. Inserción Transaccional en PostgreSQL
        String sqlPedido = "INSERT INTO pedido (cliente_id, empleado_id, sucursal_id, tipo_pedido, direccion_delivery) " +
                            "VALUES (?, ?, ?, ?, ?) RETURNING pedido_id";
        String sqlDetalle = "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad, precio_unitario) " +
                             "VALUES (?, ?, ?, ?)";

        int pedidoId = -1;
        boolean autoCommitOriginal = connPostgres.getAutoCommit();
        
        try {
            // Iniciar transacción
            connPostgres.setAutoCommit(false);

            // Insertar cabecera y obtener el ID retornado
            try (PreparedStatement ps = connPostgres.prepareStatement(sqlPedido)) {
                ps.setInt(1, pedido.getClienteId());
                ps.setInt(2, pedido.getEmpleadoId());
                ps.setInt(3, pedido.getSucursalId());
                ps.setString(4, pedido.getTipoPedido());
                ps.setString(5, pedido.getDireccionDelivery());
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        pedidoId = rs.getInt("pedido_id");
                    }
                }
            }

            if (pedidoId == -1) {
                throw new SQLException("No se pudo obtener el ID del pedido generado.");
            }

            // Insertar los detalles
            try (PreparedStatement ps = connPostgres.prepareStatement(sqlDetalle)) {
                for (DetallePedido det : pedido.getDetalles()) {
                    ps.setInt(1, pedidoId);
                    ps.setInt(2, det.getProductoId());
                    ps.setInt(3, det.getCantidad());
                    ps.setDouble(4, det.getPrecioUnitario());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Confirmar transacción
            connPostgres.commit();
            System.out.println("Transacción PostgreSQL completada con éxito. Pedido #" + pedidoId);
            return pedidoId;

        } catch (SQLException e) {
            // Deshacer cambios en caso de error
            connPostgres.rollback();
            throw e;
        } finally {
            // Restaurar estado de auto-commit original
            connPostgres.setAutoCommit(autoCommitOriginal);
        }
    }
}
