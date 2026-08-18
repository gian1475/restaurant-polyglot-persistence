package com.restaurante.dao;

import com.restaurante.conexion.ConexionManager;
import com.restaurante.modelo.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones DAO para la tabla cliente en PostgreSQL.
 * 
 * @author Gianf
 */
public class ClienteDAO {

    /**
     * Obtiene la lista completa de clientes registrados.
     */
    public List<Cliente> listarClientes() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        Connection conn = ConexionManager.getInstancia().getConexionPostgres();
        if (conn == null) {
            throw new SQLException("Sin conexión activa a PostgreSQL.");
        }

        String sql = "SELECT cliente_id, nombres, apellidos, email, telefono, fecha_registro FROM cliente ORDER BY apellidos, nombres";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Cliente c = new Cliente();
                c.setClienteId(rs.getInt("cliente_id"));
                c.setNombres(rs.getString("nombres"));
                c.setApellidos(rs.getString("apellidos"));
                c.setEmail(rs.getString("email"));
                c.setTelefono(rs.getString("telefono"));
                c.setFechaRegistro(rs.getDate("fecha_registro"));
                lista.add(c);
            }
        }
        return lista;
    }
}
