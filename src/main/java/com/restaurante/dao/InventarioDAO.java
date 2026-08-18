package com.restaurante.dao;

import com.restaurante.conexion.ConexionManager;
import com.restaurante.modelo.Insumo;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones DAO para la tabla insumo en Oracle.
 * Permite listar insumos y registrar salidas llamando a un procedimiento almacenado.
 * 
 * @author Gianf
 */
public class InventarioDAO {

    /**
     * Obtiene la lista completa de insumos registrados en Oracle.
     */
    public List<Insumo> listarInsumos() throws SQLException {
        List<Insumo> lista = new ArrayList<>();
        Connection conn = ConexionManager.getInstancia().getConexionOracle();
        if (conn == null) {
            throw new SQLException("Oracle: Sin conexión activa.");
        }

        String sql = "SELECT insumo_id, nombre_insumo, unidad_medida, stock_actual, stock_minimo FROM insumo ORDER BY nombre_insumo";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Insumo i = new Insumo(
                    rs.getInt("insumo_id"),
                    rs.getString("nombre_insumo"),
                    rs.getString("unidad_medida"),
                    rs.getDouble("stock_actual"),
                    rs.getDouble("stock_minimo")
                );
                lista.add(i);
            }
        }
        return lista;
    }

    /**
     * Registra una salida de insumo llamando al procedimiento almacenado sp_registrar_salida_insumo en Oracle.
     */
    public void registrarSalidaInsumo(int insumoId, int sucursalId, double cantidad, String motivo) throws SQLException {
        Connection conn = ConexionManager.getInstancia().getConexionOracle();
        if (conn == null) {
            throw new SQLException("Oracle: Sin conexión activa.");
        }

        String sqlCall = "{call sp_registrar_salida_insumo(?, ?, ?, ?)}";
        try (CallableStatement cs = conn.prepareCall(sqlCall)) {
            cs.setInt(1, insumoId);
            cs.setInt(2, sucursalId);
            cs.setDouble(3, cantidad);
            cs.setString(4, motivo);
            cs.execute();
            System.out.println("Procedimiento sp_registrar_salida_insumo ejecutado con éxito en Oracle.");
        }
    }
}
