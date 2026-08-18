package com.restaurante.dao;

import com.restaurante.conexion.ConexionManager;
import com.restaurante.modelo.Producto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones DAO para la tabla producto en Oracle.
 * 
 * @author Gianf
 */
public class ProductoDAO {

    /**
     * Obtiene los productos disponibles ('S') desde Oracle.
     */
    public List<Producto> listarProductosDisponibles() throws SQLException {
        List<Producto> lista = new ArrayList<>();
        Connection conn = ConexionManager.getInstancia().getConexionOracle();
        if (conn == null) {
            throw new SQLException("Sin conexión activa a Oracle.");
        }

        String sql = "SELECT producto_id, nombre_producto, precio, disponible FROM producto WHERE disponible = 'S' ORDER BY nombre_producto";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto p = new Producto(
                    rs.getInt("producto_id"),
                    rs.getString("nombre_producto"),
                    rs.getDouble("precio"),
                    rs.getString("disponible")
                );
                lista.add(p);
            }
        }
        return lista;
    }
}
