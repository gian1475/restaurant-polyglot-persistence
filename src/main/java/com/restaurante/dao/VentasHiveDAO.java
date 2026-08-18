package com.restaurante.dao;

import com.restaurante.conexion.ConexionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones DAO para consultas analíticas sobre Apache Hive (Hadoop).
 * Realiza agregaciones tipo Big Data para reportes de gerencia.
 * 
 * @author Gianf
 */
public class VentasHiveDAO {

    /**
     * Obtiene métricas agregadas de ventas históricas por sucursal desde Hive.
     * Retorna una lista de filas donde cada fila es un arreglo:
     * [sucursal_id, total_vendido, num_pedidos]
     */
    public List<Object[]> obtenerReporteVentasPorSucursal() throws SQLException {
        List<Object[]> reporte = new ArrayList<>();
        Connection conn = ConexionManager.getInstancia().getConexionHive();
        
        if (conn == null) {
            throw new SQLException("Hive: Sin conexión activa.");
        }

        String sql = "SELECT sucursal_id, SUM(monto) AS total_vendido, COUNT(*) AS num_pedidos " +
                     "FROM ventas_hist " +
                     "GROUP BY sucursal_id " +
                     "ORDER BY total_vendido DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] fila = new Object[3];
                fila[0] = rs.getInt("sucursal_id");
                fila[1] = rs.getDouble("total_vendido");
                fila[2] = rs.getInt("num_pedidos");
                reporte.add(fila);
            }
        }
        return reporte;
    }
}
