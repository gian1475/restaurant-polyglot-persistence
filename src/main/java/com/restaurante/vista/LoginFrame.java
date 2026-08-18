package com.restaurante.vista;

import com.restaurante.conexion.ConexionManager;
import com.restaurante.util.ComponentBuilder;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Pantalla de Login que implementa la Autenticación de la Opción A.
 * Intenta conectarse a PostgreSQL con las credenciales ingresadas.
 * 
 * @author Gianf
 */
public class LoginFrame extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtContrasena;
    private JButton btnLogin;
    private JButton btnSalir;

    public LoginFrame() {
        super("Restaurante Multi-Motor - Acceso");
        inicializarUI();
    }

    private void inicializarUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 420);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Contenedor principal con fondo gris claro
        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(ComponentBuilder.COLOR_BACKGROUND);
        setContentPane(panelPrincipal);

        // Cabecera superior moderna
        JPanel headerPanel = ComponentBuilder.crearHeaderPantalla(
            "SISTEMA RESTAURANTE", 
            "Acceso Multi-Motor Centralizado"
        );
        panelPrincipal.add(headerPanel, BorderLayout.NORTH);

        // Formulario central
        JPanel panelForm = ComponentBuilder.crearCard();
        panelForm.setLayout(new GridBagLayout());
        panelForm.setBorder(new EmptyBorder(25, 30, 25, 30));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Label explicativo
        JLabel lblMsg = new JLabel("Ingrese sus credenciales de base de datos:");
        lblMsg.setFont(ComponentBuilder.FONT_SUBTITLE);
        lblMsg.setForeground(ComponentBuilder.COLOR_TEXT_MAIN);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panelForm.add(lblMsg, gbc);

        // Campo Usuario
        JLabel lblUsuario = new JLabel("Usuario:");
        lblUsuario.setFont(ComponentBuilder.FONT_BODY_BOLD);
        lblUsuario.setForeground(ComponentBuilder.COLOR_TEXT_MAIN);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panelForm.add(lblUsuario, gbc);

        txtUsuario = new JTextField(15);
        txtUsuario.setFont(ComponentBuilder.FONT_BODY);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panelForm.add(txtUsuario, gbc);

        // Campo Contraseña
        JLabel lblContrasena = new JLabel("Contraseña:");
        lblContrasena.setFont(ComponentBuilder.FONT_BODY_BOLD);
        lblContrasena.setForeground(ComponentBuilder.COLOR_TEXT_MAIN);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panelForm.add(lblContrasena, gbc);

        txtContrasena = new JPasswordField(15);
        txtContrasena.setFont(ComponentBuilder.FONT_BODY);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panelForm.add(txtContrasena, gbc);

        // Panel de botones
        JPanel panelBotones = new JPanel(new GridLayout(1, 2, 10, 0));
        panelBotones.setOpaque(false);

        btnLogin = ComponentBuilder.crearBotonPrincipal("Iniciar Sesión");
        btnSalir = ComponentBuilder.crearBoton("Salir", ComponentBuilder.COLOR_WARNING, Color.WHITE);

        panelBotones.add(btnLogin);
        panelBotones.add(btnSalir);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 8, 8, 8);
        panelForm.add(panelBotones, gbc);

        // Notas de ayuda con usuarios sugeridos
        JTextArea txtAyuda = new JTextArea(
            "Tipos de usuario sugeridos:\n" +
            "• rol_admin (Clave: Admin#2026)\n" +
            "• rol_cajero (Clave: Cajero#2026)\n" +
            "• rol_supervisor (Clave: Super#2026)\n" +
            "• rol_cocinero (Clave: Cocinero#2026)"
        );
        txtAyuda.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        txtAyuda.setForeground(ComponentBuilder.COLOR_TEXT_MUTED);
        txtAyuda.setEditable(false);
        txtAyuda.setOpaque(false);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 8, 0, 8);
        panelForm.add(txtAyuda, gbc);

        panelPrincipal.add(panelForm, BorderLayout.CENTER);

        // Acciones
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarLogin();
            }
        });

        btnSalir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        
        // Enter para login en campos de texto
        ActionListener enterAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarLogin();
            }
        };
        txtUsuario.addActionListener(enterAction);
        txtContrasena.addActionListener(enterAction);
    }

    private void ejecutarLogin() {
        String usuario = txtUsuario.getText().trim();
        String contrasena = new String(txtContrasena.getPassword());

        if (usuario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese el nombre de usuario.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Mostrar indicador de conexión
        btnLogin.setEnabled(false);
        btnLogin.setText("Conectando...");

        // Usar un hilo de Swing para no colgar la UI al abrir la conexión
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ConexionManager manager = ConexionManager.getInstancia();
                boolean exito = manager.conectarPostgresConUsuario(usuario, contrasena);

                if (exito) {
                    // Cargar conexiones complementarias
                    manager.conectarBasesDeDatosApoyo();
                    
                    // Abrir menú principal
                    MenuPrincipalFrame menu = new MenuPrincipalFrame();
                    menu.setVisible(true);
                    
                    // Cerrar login
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(
                        LoginFrame.this, 
                        "Error al autenticar contra PostgreSQL.\n" +
                        "Verifique sus credenciales e intente de nuevo.", 
                        "Fallo de Login", 
                        JOptionPane.ERROR_MESSAGE
                    );
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar Sesión");
                }
            }
        });
    }
}
