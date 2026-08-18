package com.restaurante.dao;

import com.restaurante.conexion.ConexionManager;
import com.restaurante.modelo.Empleado;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones DAO para la tabla empleado en Oracle.
 * 
 * @author Gianf
 */
public class EmpleadoDAO {

    /**
     * Obtiene los empleados activos desde Oracle.
     */
    public List<Empleado> listarEmpleadosActivos() throws SQLException {
        List<Empleado> lista = new ArrayList<>();
        Connection conn = ConexionManager.getInstancia().getConexionOracle();
        if (conn == null) {
            throw new SQLException("Sin conexión activa a Oracle.");
        }

        String sql = "SELECT empleado_id, nombres, apellidos, sucursal_id FROM empleado WHERE estado = 'ACTIVO' ORDER BY apellidos, nombres";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Empleado e = new Empleado(
                    rs.getInt("empleado_id"),
                    rs.getString("nombres"),
                    rs.getString("apellidos"),
                    rs.getInt("sucursal_id")
                );
                lista.add(e);
            }
        }
        return lista;
    }
}
