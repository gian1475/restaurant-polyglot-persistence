package com.restaurante.vista;

import com.restaurante.util.ComponentBuilder;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Pantalla informativa y administrativa que muestra el esquema de seguridad
 * y privilegios del sistema para la sustentación del proyecto.
 * 
 * @author Gianf
 */
public class GestionRolesFrame extends JFrame {

    private JTable tblRoles;
    private DefaultTableModel modelRoles;
    private JButton btnCerrar;

    public GestionRolesFrame() {
        super("Esquema de Seguridad - Privilegios del Sistema");
        inicializarUI();
        cargarEsquemaSeguridad();
    }

    private void inicializarUI() {
        setSize(800, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ComponentBuilder.COLOR_BACKGROUND);
        setContentPane(mainPanel);

        // Header
        mainPanel.add(ComponentBuilder.crearHeaderPantalla(
            "GESTIÓN DE ROLES & ESQUEMA DE SEGURIDAD", 
            "Descripción técnica del control de acceso basado en roles (RBAC) definido en las bases de datos"
        ), BorderLayout.NORTH);

        // Panel Central
        JPanel panelCentro = new JPanel(new BorderLayout(15, 15));
        panelCentro.setOpaque(false);
        panelCentro.setBorder(new EmptyBorder(15, 15, 15, 15));

        // JTable
        String[] columnas = {"Nombre Rol", "Nivel de Acceso", "Permisos PostgreSQL (Front)", "Permisos Oracle (Back)"};
        modelRoles = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRoles = new JTable(modelRoles);
        ComponentBuilder.estilarTabla(tblRoles);
        JScrollPane scroll = new JScrollPane(tblRoles);
        panelCentro.add(scroll, BorderLayout.CENTER);

        // Nota explicativa
        JTextArea txtNota = new JTextArea(
            "Nota para Sustentación:\n" +
            "Los roles listados corresponden a los definidos físicamente mediante comandos DDL (CREATE ROLE) " +
            "tanto en PostgreSQL como en Oracle. La aplicación Java Swing intercepta el login del operador " +
            "y hereda dinámicamente estos permisos a nivel de Driver JDBC, impidiendo o permitiendo " +
            "la ejecución de SELECT/INSERT/UPDATE según corresponda a la cuenta."
        );
        txtNota.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        txtNota.setForeground(ComponentBuilder.COLOR_TEXT_MUTED);
        txtNota.setBackground(Color.WHITE);
        txtNota.setEditable(false);
        txtNota.setLineWrap(true);
        txtNota.setWrapStyleWord(true);
        txtNota.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 224, 230)),
            new EmptyBorder(8, 8, 8, 8)
        ));
        panelCentro.add(txtNota, BorderLayout.SOUTH);

        mainPanel.add(panelCentro, BorderLayout.CENTER);

        // Botón inferior
        JPanel panelBotones = ComponentBuilder.crearCard();
        panelBotones.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 5));

        btnCerrar = ComponentBuilder.crearBoton("Cerrar", ComponentBuilder.COLOR_PRIMARY, Color.WHITE);
        panelBotones.add(btnCerrar);
        mainPanel.add(panelBotones, BorderLayout.SOUTH);

        btnCerrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void cargarEsquemaSeguridad() {
        modelRoles.setRowCount(0);
        
        modelRoles.addRow(new Object[]{
            "rol_admin",
            "ACCESO TOTAL (100%)",
            "ALL PRIVILEGES (Control total en esquema public)",
            "ALL PRIVILEGES (sucursal, empleado, producto, insumo)"
        });

        modelRoles.addRow(new Object[]{
            "rol_supervisor",
            "SUPERVISIÓN (LECTURA/REPORTES)",
            "SELECT (Lectura de todas las tablas)",
            "SELECT (Lectura y monitoreo de movimientos)"
        });

        modelRoles.addRow(new Object[]{
            "rol_cajero",
            "REGISTRO DE PEDIDOS Y CAJA",
            "SELECT, INSERT, UPDATE en pedido, detalle, pago",
            "SELECT en producto y categoria"
        });

        modelRoles.addRow(new Object[]{
            "rol_cocinero",
            "ATENCIÓN Y RECETAS (NO PAGOS)",
            "SELECT, UPDATE (estado_pedido) en pedido y detalle",
            "SELECT en producto, insumos, e INSERT en movimientos"
        });
    }
}
