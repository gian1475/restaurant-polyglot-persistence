package com.restaurante;

import com.restaurante.conexion.ConexionManager;
import com.restaurante.util.ComponentBuilder;
import com.restaurante.vista.LoginFrame;
import javax.swing.SwingUtilities;

/**
 * Clase principal que actúa como punto de entrada (main) 
 * para iniciar la aplicación del restaurante.
 * 
 * @author Gianf
 */
public class Main {

    public static void main(String[] args) {
        // 1. Configurar cierre ordenado de conexiones al salir
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Cerrando recursos y conexiones de bases de datos...");
                ConexionManager.getInstancia().cerrarTodo();
            }
        }));

        // 2. Inicializar el tema visual e iniciar el login en el hilo de Swing (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ComponentBuilder.inicializarTema();
                
                LoginFrame login = new LoginFrame();
                login.setVisible(true);
            }
        });
    }
}
