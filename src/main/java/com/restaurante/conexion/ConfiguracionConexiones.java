package com.restaurante.conexion;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Clase que centraliza la carga y gestión de las conexiones
 * para los cuatro motores de bases de datos del sistema.
 * 
 * Permite cargar dinámicamente las credenciales desde un archivo
 * externo 'config.properties', desde el classpath o desde variables
 * de entorno del sistema, evitando exponer secretos en el código fuente.
 * 
 * @author Gianf
 */
public class ConfiguracionConexiones {

    private static final String CONFIG_FILE_NAME = "config.properties";

    // ---- CONFIGURACIÓN POSTGRESQL (Front-office / Neon) ----
    public static String PG_URL;
    public static String PG_ADMIN_USER;
    public static String PG_ADMIN_PASS;

    // ---- CONFIGURACIÓN ORACLE (Back-office / Cloud Autonomous) ----
    public static String ORA_URL;
    public static String ORA_USER;
    public static String ORA_PASS;

    // ---- CONFIGURACIÓN MONGODB (NoSQL / Atlas) ----
    public static String MONGO_URI;
    public static String MONGO_DB_NAME;

    // ---- CONFIGURACIÓN HIVE (Big Data / Docker Local) ----
    public static String HIVE_URL;
    public static String HIVE_USER;
    public static String HIVE_PASS;

    static {
        cargarConfiguracion();
    }

    /**
     * Carga las propiedades desde el archivo local o classpath, con fallback a variables de entorno.
     */
    public static void cargarConfiguracion() {
        Properties prop = new Properties();
        boolean cargado = false;

        // 1. Intentar cargar desde el directorio de trabajo actual (raíz del proyecto)
        File archivoLocal = new File(CONFIG_FILE_NAME);
        if (archivoLocal.exists()) {
            try (InputStream input = new FileInputStream(archivoLocal)) {
                prop.load(input);
                cargado = true;
                System.out.println("[ConfiguracionConexiones] Configuración cargada desde archivo local: " + archivoLocal.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("[ConfiguracionConexiones] Error al leer " + CONFIG_FILE_NAME + " local: " + e.getMessage());
            }
        }

        // 2. Si no se encontró en la raíz, intentar cargar desde el classpath
        if (!cargado) {
            try (InputStream input = ConfiguracionConexiones.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
                if (input != null) {
                    prop.load(input);
                    cargado = true;
                    System.out.println("[ConfiguracionConexiones] Configuración cargada desde classpath.");
                }
            } catch (Exception e) {
                System.err.println("[ConfiguracionConexiones] Error al leer " + CONFIG_FILE_NAME + " desde classpath: " + e.getMessage());
            }
        }

        if (!cargado) {
            System.out.println("[ConfiguracionConexiones] Aviso: No se encontró 'config.properties'. Usando variables de entorno o valores por defecto.");
        }

        // Asignar variables con prioridad: config.properties > Variable de Entorno > Fallback por defecto
        PG_URL = obtenerPropiedad(prop, "PG_URL", "jdbc:postgresql://localhost:5432/neondb?sslmode=require");
        PG_ADMIN_USER = obtenerPropiedad(prop, "PG_ADMIN_USER", "neondb_owner");
        PG_ADMIN_PASS = obtenerPropiedad(prop, "PG_ADMIN_PASS", "");

        ORA_URL = obtenerPropiedad(prop, "ORA_URL", "jdbc:oracle:thin:@localhost:1521/XEPDB1");
        ORA_USER = obtenerPropiedad(prop, "ORA_USER", "admin");
        ORA_PASS = obtenerPropiedad(prop, "ORA_PASS", "");

        MONGO_URI = obtenerPropiedad(prop, "MONGO_URI", "mongodb://localhost:27017/restaurante_nosql");
        MONGO_DB_NAME = obtenerPropiedad(prop, "MONGO_DB_NAME", "restaurante_nosql");

        HIVE_URL = obtenerPropiedad(prop, "HIVE_URL", "jdbc:hive2://localhost:10000/default");
        HIVE_USER = obtenerPropiedad(prop, "HIVE_USER", "");
        HIVE_PASS = obtenerPropiedad(prop, "HIVE_PASS", "");
    }

    private static String obtenerPropiedad(Properties prop, String clave, String valorDefecto) {
        String val = prop.getProperty(clave);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        String envVal = System.getenv(clave);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        return valorDefecto;
    }
}
