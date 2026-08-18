package com.restaurante.util;

import com.restaurante.conexion.ConfiguracionConexiones;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ExportadorDatos {
    public static void main(String[] args) {
        System.out.println("Iniciando exportación de ventas desde PostgreSQL...");
        String csvFile = "files/ventas_historicas.csv";

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(
                    ConfiguracionConexiones.PG_URL, 
                    ConfiguracionConexiones.PG_ADMIN_USER, 
                    ConfiguracionConexiones.PG_ADMIN_PASS)) {
                
                System.out.println("Conexión a PostgreSQL establecida.");
                
                String query = "SELECT pedido_id, sucursal_id, fecha_pedido, monto FROM vw_ventas_pedido";
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query);
                     PrintWriter pw = new PrintWriter(new FileWriter(csvFile))) {
                    
                    // Escribir cabecera
                    pw.println("pedido_id,sucursal_id,fecha_pedido,monto");
                    
                    int count = 0;
                    while (rs.next()) {
                        int pedidoId = rs.getInt("pedido_id");
                        int sucursalId = rs.getInt("sucursal_id");
                        String fecha = rs.getString("fecha_pedido");
                        double monto = rs.getDouble("monto");
                        
                        pw.println(pedidoId + "," + sucursalId + "," + fecha + "," + monto);
                        count++;
                    }
                    
                    System.out.println("Exportación terminada. Se escribieron " + count + " registros en " + csvFile);
                }
            }
        } catch (Exception e) {
            System.err.println("Error durante la exportación: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
