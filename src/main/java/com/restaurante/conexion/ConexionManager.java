package com.restaurante.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Administrador global de las conexiones a las 4 fuentes de datos del restaurante.
 * Implementa el patrón Singleton para facilitar el acceso en toda la aplicación.
 * 
 * @author Gianf
 */
public class ConexionManager {

    private static ConexionManager instancia;

    private Connection conexionPostgres;
    private Connection conexionOracle;
    private MongoClient mongoClient;
    private MongoDatabase baseDatosMongo;
    private Connection conexionHive;

    // Rol del usuario actualmente autenticado en la aplicación
    private String rolUsuarioActual = "INVITADO";
    private String nombreUsuarioActual = "";

    private ConexionManager() {
        // Constructor privado para Singleton
    }

    public static synchronized ConexionManager getInstancia() {
        if (instancia == null) {
            instancia = new ConexionManager();
        }
        return instancia;
    }

    /**
     * Intenta conectar con PostgreSQL utilizando credenciales específicas (para Login).
     * Si tiene éxito, guarda esta conexión como la activa.
     */
    public boolean conectarPostgresConUsuario(String usuario, String contrasena) {
        try {
            // Registrar driver
            Class.forName("org.postgresql.Driver");
            
            // Cerrar conexión anterior si existiese
            if (conexionPostgres != null && !conexionPostgres.isClosed()) {
                conexionPostgres.close();
            }

            conexionPostgres = DriverManager.getConnection(ConfiguracionConexiones.PG_URL, usuario, contrasena);
            this.nombreUsuarioActual = usuario;
            this.rolUsuarioActual = determinarRolDesdeUsuario(usuario);
            System.out.println("PostgreSQL: Conectado como " + usuario + " [OK]");
            return true;
        } catch (Exception e) {
            System.err.println("PostgreSQL: Error de autenticación/conexión para " + usuario + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Conecta a todas las bases de datos de apoyo (Oracle, MongoDB, Hive) 
     * utilizando las credenciales administrativas de configuración.
     * Si alguna falla, no interrumpe el flujo principal pero lo reporta.
     */
    public void conectarBasesDeDatosApoyo() {
        // 1. Conexión a Oracle
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            if (conexionOracle == null || conexionOracle.isClosed()) {
                conexionOracle = DriverManager.getConnection(
                    ConfiguracionConexiones.ORA_URL, 
                    ConfiguracionConexiones.ORA_USER, 
                    ConfiguracionConexiones.ORA_PASS
                );
                System.out.println("Oracle: Conectado con éxito [OK]");
            }
        } catch (Exception e) {
            System.err.println("Oracle: No se pudo conectar - " + e.getMessage());
            conexionOracle = null;
        }

        // 2. Conexión a MongoDB
        try {
            if (mongoClient == null) {
                mongoClient = MongoClients.create(ConfiguracionConexiones.MONGO_URI);
                baseDatosMongo = mongoClient.getDatabase(ConfiguracionConexiones.MONGO_DB_NAME);
                // Forzar una consulta simple para verificar el estado de la conexión
                baseDatosMongo.runCommand(new org.bson.Document("ping", 1));
                System.out.println("MongoDB: Conectado con éxito [OK]");
            }
        } catch (Exception e) {
            System.err.println("MongoDB: No se pudo conectar - " + e.getMessage());
            mongoClient = null;
            baseDatosMongo = null;
        }

        // 3. Conexión a Hive
        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
            if (conexionHive == null || conexionHive.isClosed()) {
                conexionHive = DriverManager.getConnection(
                    ConfiguracionConexiones.HIVE_URL, 
                    ConfiguracionConexiones.HIVE_USER, 
                    ConfiguracionConexiones.HIVE_PASS
                );
                System.out.println("Hive: Conectado con éxito [OK]");
            }
        } catch (Exception e) {
            System.err.println("Hive: No se pudo conectar - " + e.getMessage());
            conexionHive = null;
        }
    }

    /**
     * Determina el rol de la aplicación basándose en el nombre de usuario de la BD.
     */
    private String determinarRolDesdeUsuario(String usuario) {
        if (usuario == null) return "INVITADO";
        String usrLower = usuario.toLowerCase();
        if (usrLower.contains("admin")) {
            return "ADMIN";
        } else if (usrLower.contains("super")) {
            return "SUPERVISOR";
        } else if (usrLower.contains("caje") || usrLower.contains("caja")) {
            return "CAJERO";
        } else if (usrLower.contains("coci") || usrLower.contains("chef")) {
            return "COCINERO";
        }
        return "ADMIN"; // Rol por defecto si es superusuario general (ej. neondb_owner)
    }

    public Connection getConexionPostgres() {
        return conexionPostgres;
    }

    public Connection getConexionOracle() {
        return conexionOracle;
    }

    public MongoDatabase getBaseDatosMongo() {
        return baseDatosMongo;
    }

    public Connection getConexionHive() {
        return conexionHive;
    }

    public String getRolUsuarioActual() {
        return rolUsuarioActual;
    }

    public String getNombreUsuarioActual() {
        return nombreUsuarioActual;
    }

    /**
     * Cierra de manera segura todas las conexiones activas.
     */
    public void cerrarTodo() {
        try {
            if (conexionPostgres != null && !conexionPostgres.isClosed()) {
                conexionPostgres.close();
                System.out.println("PostgreSQL: Conexión cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar Postgres: " + e.getMessage());
        } finally {
            conexionPostgres = null;
        }

        try {
            if (conexionOracle != null && !conexionOracle.isClosed()) {
                conexionOracle.close();
                System.out.println("Oracle: Conexión cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar Oracle: " + e.getMessage());
        } finally {
            conexionOracle = null;
        }

        try {
            if (mongoClient != null) {
                mongoClient.close();
                System.out.println("MongoDB: Conexión cerrada.");
            }
        } catch (Exception e) {
            System.err.println("Error al cerrar MongoDB: " + e.getMessage());
        } finally {
            mongoClient = null;
            baseDatosMongo = null;
        }

        try {
            if (conexionHive != null && !conexionHive.isClosed()) {
                conexionHive.close();
                System.out.println("Hive: Conexión cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar Hive: " + e.getMessage());
        } finally {
            conexionHive = null;
        }
    }
}
