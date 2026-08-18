package com.restaurante.vista;

import com.restaurante.conexion.ConexionManager;
import com.restaurante.util.ComponentBuilder;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Pantalla principal del sistema. Habilita y deshabilita opciones
 * según el rol del usuario que inició sesión.
 * Muestra el estado de conexión de las 4 bases de datos en tiempo real.
 * 
 * @author Gianf
 */
public class MenuPrincipalFrame extends JFrame {

    private JButton btnPedidos;
    private JButton btnInventario;
    private JButton btnMongo;
    private JButton btnHive;
    private JButton btnRoles;
    private JButton btnLogout;

    public MenuPrincipalFrame() {
        super("Restaurante Multi-Motor - Menú Principal");
        inicializarUI();
        aplicarPermisosPorRol();
    }

    private void inicializarUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 520);
        setLocationRelativeTo(null);

        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(ComponentBuilder.COLOR_BACKGROUND);
        setContentPane(panelPrincipal);

        // Cabecera principal con información del usuario
        ConexionManager cm = ConexionManager.getInstancia();
        String subtitulo = "Operador: " + cm.getNombreUsuarioActual() + "  |  Rol: " + cm.getRolUsuarioActual();
        JPanel panelHeader = ComponentBuilder.crearHeaderPantalla("DASHBOARD CONTROL GENERAL", subtitulo);
        panelPrincipal.add(panelHeader, BorderLayout.NORTH);

        // Contenido Central: Panel con botones en Grid
        JPanel panelCentral = new JPanel(new BorderLayout(15, 15));
        panelCentral.setOpaque(false);
        panelCentral.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Grid de Opciones
        JPanel panelGrid = new JPanel(new GridLayout(2, 3, 15, 15));
        panelGrid.setOpaque(false);

        btnPedidos = ComponentBuilder.crearBotonPrincipal("<html><center>Registrar Pedido<br><small>PostgreSQL & Oracle</small></center></html>");
        btnInventario = ComponentBuilder.crearBoton("<html><center>Control Inventario<br><small>Oracle SQL</small></center></html>", new Color(46, 204, 113), Color.WHITE);
        btnMongo = ComponentBuilder.crearBoton("<html><center>Pedidos NoSQL<br><small>MongoDB Atlas</small></center></html>", new Color(155, 89, 182), Color.WHITE);
        btnHive = ComponentBuilder.crearBoton("<html><center>Métricas Ventas<br><small>Hadoop / Hive</small></center></html>", new Color(241, 196, 15), Color.DARK_GRAY);
        btnRoles = ComponentBuilder.crearBoton("<html><center>Ver Roles/Permisos<br><small>Esquema Seguridad</small></center></html>", ComponentBuilder.COLOR_DARK_HEADER, Color.WHITE);
        btnLogout = ComponentBuilder.crearBoton("<html><center>Cerrar Sesión<br><small>Salir del Sistema</small></center></html>", ComponentBuilder.COLOR_WARNING, Color.WHITE);

        panelGrid.add(btnPedidos);
        panelGrid.add(btnInventario);
        panelGrid.add(btnMongo);
        panelGrid.add(btnHive);
        panelGrid.add(btnRoles);
        panelGrid.add(btnLogout);

        panelCentral.add(panelGrid, BorderLayout.CENTER);

        // Barra inferior: Estado de los Motores de Base de Datos
        JPanel panelStatus = ComponentBuilder.crearCard();
        panelStatus.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 5));
        
        panelStatus.add(new JLabel("Motores:"));
        panelStatus.add(crearIndicadorMotor("PostgreSQL", cm.getConexionPostgres() != null));
        panelStatus.add(crearIndicadorMotor("Oracle DB", cm.getConexionOracle() != null));
        panelStatus.add(crearIndicadorMotor("MongoDB", cm.getBaseDatosMongo() != null));
        panelStatus.add(crearIndicadorMotor("Apache Hive", cm.getConexionHive() != null));

        panelCentral.add(panelStatus, BorderLayout.SOUTH);
        panelPrincipal.add(panelCentral, BorderLayout.CENTER);

        // Eventos de botones
        btnPedidos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new RegistroPedidoFrame().setVisible(true);
            }
        });

        btnInventario.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new InventarioFrame().setVisible(true);
            }
        });

        btnMongo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PedidosMongoFrame().setVisible(true);
            }
        });

        btnHive.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new ReportesHiveFrame().setVisible(true);
            }
        });

        btnRoles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new GestionRolesFrame().setVisible(true);
            }
        });

        btnLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ConexionManager.getInstancia().cerrarTodo();
                new LoginFrame().setVisible(true);
                dispose();
            }
        });
    }

    /**
     * Aplica la restricción de visibilidad y acceso en base al rol autenticado.
     */
    private void aplicarPermisosPorRol() {
        String rol = ConexionManager.getInstancia().getRolUsuarioActual();

        switch (rol) {
            case "ADMIN":
                // Acceso completo
                break;
            case "SUPERVISOR":
                btnPedidos.setEnabled(false);
                btnRoles.setEnabled(false);
                btnPedidos.setToolTipText("No tiene permisos para modificar pedidos.");
                btnRoles.setToolTipText("Solo administradores pueden ver gestión de roles.");
                break;
            case "CAJERO":
                btnInventario.setEnabled(false);
                btnMongo.setEnabled(false);
                btnHive.setEnabled(false);
                btnRoles.setEnabled(false);
                break;
            case "COCINERO":
                btnPedidos.setEnabled(false);
                btnHive.setEnabled(false);
                btnRoles.setEnabled(false);
                break;
            default: // Invitado o bloqueados
                btnPedidos.setEnabled(false);
                btnInventario.setEnabled(false);
                btnMongo.setEnabled(false);
                btnHive.setEnabled(false);
                btnRoles.setEnabled(false);
                break;
        }
    }

    /**
     * Genera un label visual que actúa como semáforo del estado de conexión (verde/rojo).
     */
    private JPanel crearIndicadorMotor(String motor, boolean online) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        
        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 16));
        dot.setForeground(online ? ComponentBuilder.COLOR_SUCCESS : ComponentBuilder.COLOR_WARNING);
        
        JLabel name = new JLabel(motor + (online ? " [OK]" : " [OFF]"));
        name.setFont(ComponentBuilder.FONT_BODY);
        name.setForeground(ComponentBuilder.COLOR_TEXT_MAIN);

        p.add(dot);
        p.add(name);
        return p;
    }
}
