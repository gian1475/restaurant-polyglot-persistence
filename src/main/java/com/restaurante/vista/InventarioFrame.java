package com.restaurante.vista;

import com.restaurante.conexion.ConexionManager;
import com.restaurante.dao.InventarioDAO;
import com.restaurante.modelo.Insumo;
import com.restaurante.util.ComponentBuilder;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Pantalla que muestra el inventario de insumos (Oracle) y resalta en rojo
 * aquellos por debajo del stock mínimo. Permite ejecutar el SP de salida.
 * 
 * @author Gianf
 */
public class InventarioFrame extends JFrame {

    private JTable tblInsumos;
    private DefaultTableModel modelInsumos;
    private JButton btnSalida;
    private JButton btnRefrescar;
    private JButton btnCerrar;

    public InventarioFrame() {
        super("Inventario de Insumos - Oracle SQL");
        inicializarUI();
        cargarInsumos();
    }

    private void inicializarUI() {
        setSize(780, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ComponentBuilder.COLOR_BACKGROUND);
        setContentPane(mainPanel);

        // Header
        mainPanel.add(ComponentBuilder.crearHeaderPantalla(
            "CONTROL DE INVENTARIO (Oracle)", 
            "Muestra los insumos y destaca en alerta aquellos cuyo Stock Actual es menor o igual al Mínimo"
        ), BorderLayout.NORTH);

        // Panel Central
        JPanel panelCentro = new JPanel(new BorderLayout(15, 15));
        panelCentro.setOpaque(false);
        panelCentro.setBorder(new EmptyBorder(15, 15, 15, 15));

        // JTable
        String[] columnas = {"ID Insumo", "Insumo", "Unidad Medida", "Stock Actual", "Stock Mínimo", "Estado"};
        modelInsumos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblInsumos = new JTable(modelInsumos);
        ComponentBuilder.estilarTabla(tblInsumos);
        
        // Custom cell renderer to color rows in Red if Stock <= Stock Minimo
        tblInsumos.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                double stock = (Double) table.getValueAt(row, 3);
                double minimo = (Double) table.getValueAt(row, 4);
                
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                } else {
                    if (stock <= minimo) {
                        c.setBackground(new Color(254, 224, 224)); // Rojo suave
                        c.setForeground(ComponentBuilder.COLOR_WARNING.darker());
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(ComponentBuilder.COLOR_TEXT_MAIN);
                    }
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(tblInsumos);
        panelCentro.add(scroll, BorderLayout.CENTER);
        mainPanel.add(panelCentro, BorderLayout.CENTER);

        // Barra inferior de botones
        JPanel panelBotones = ComponentBuilder.crearCard();
        panelBotones.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 5));

        btnSalida = ComponentBuilder.crearBotonPrincipal("Registrar Salida de Insumo (SP)");
        btnRefrescar = ComponentBuilder.crearBoton("Actualizar", ComponentBuilder.COLOR_DARK_HEADER, Color.WHITE);
        btnCerrar = ComponentBuilder.crearBoton("Cerrar", ComponentBuilder.COLOR_TEXT_MUTED, Color.WHITE);

        panelBotones.add(btnSalida);
        panelBotones.add(btnRefrescar);
        panelBotones.add(btnCerrar);
        mainPanel.add(panelBotones, BorderLayout.SOUTH);

        // EVENTOS
        btnRefrescar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cargarInsumos();
            }
        });

        btnCerrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        btnSalida.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirDialogoSalidaInsumo();
            }
        });
    }

    private void cargarInsumos() {
        try {
            modelInsumos.setRowCount(0);
            InventarioDAO dao = new InventarioDAO();
            List<Insumo> lista = dao.listarInsumos();
            for (Insumo i : lista) {
                String estado = i.getStockActual() <= i.getStockMinimo() ? "BAJO MINIMO" : "OPTIMO";
                modelInsumos.addRow(new Object[]{
                    i.getInsumoId(),
                    i.getNombreInsumo(),
                    i.getUnidadMedida(),
                    i.getStockActual(),
                    i.getStockMinimo(),
                    estado
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error al leer insumos desde Oracle:\n" + e.getMessage(), 
                "Error de Base de Datos", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirDialogoSalidaInsumo() {
        int index = tblInsumos.getSelectedRow();
        if (index == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un insumo de la tabla primero.", "Selección Requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int insumoId = (Integer) tblInsumos.getValueAt(index, 0);
        String nombreInsumo = (String) tblInsumos.getValueAt(index, 1);
        double stockActual = (Double) tblInsumos.getValueAt(index, 3);

        // Crear formulario de diálogo custom
        JPanel dialogPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        dialogPanel.add(new JLabel("Insumo:"));
        dialogPanel.add(new JLabel(nombreInsumo + " (ID: " + insumoId + ")"));

        dialogPanel.add(new JLabel("Sucursal:"));
        JComboBox<String> cbSuc = new JComboBox<>();
        // Cargar sucursales desde Oracle
        try {
            Connection conn = ConexionManager.getInstancia().getConexionOracle();
            if (conn != null) {
                String sql = "SELECT sucursal_id, nombre FROM sucursal ORDER BY sucursal_id";
                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        cbSuc.addItem(rs.getInt("sucursal_id") + " - " + rs.getString("nombre"));
                    }
                }
            }
        } catch (Exception ex) {
            cbSuc.addItem("1 - Sede Miraflores");
            cbSuc.addItem("2 - Sede San Borja");
        }
        dialogPanel.add(cbSuc);

        dialogPanel.add(new JLabel("Cantidad a retirar:"));
        JTextField txtCant = new JTextField();
        dialogPanel.add(txtCant);

        dialogPanel.add(new JLabel("Motivo de salida:"));
        JTextField txtMotivo = new JTextField();
        dialogPanel.add(txtMotivo);

        int result = JOptionPane.showConfirmDialog(this, dialogPanel, 
                "Registrar Salida — Oracle sp_registrar_salida_insumo", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String cantStr = txtCant.getText().trim();
            String motivo = txtMotivo.getText().trim();

            if (cantStr.isEmpty() || motivo.isEmpty() || cbSuc.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Debe completar todos los campos.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                double cantidad = Double.parseDouble(cantStr);
                if (cantidad <= 0) {
                    throw new NumberFormatException();
                }

                if (cantidad > stockActual) {
                    JOptionPane.showMessageDialog(this, "No puede retirar más stock del disponible actualmente.", "Stock Insuficiente", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int sucursalId = Integer.parseInt(((String) cbSuc.getSelectedItem()).split(" - ")[0]);

                InventarioDAO dao = new InventarioDAO();
                dao.registrarSalidaInsumo(insumoId, sucursalId, cantidad, motivo);

                JOptionPane.showMessageDialog(this, 
                    "Salida registrada correctamente.\nEl procedimiento actualizó el stock e insertó el movimiento.", 
                    "Salida Registrada", 
                    JOptionPane.INFORMATION_MESSAGE);

                cargarInsumos(); // Recargar

            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Por favor, ingrese un valor de cantidad numérico positivo.", "Formato Inválido", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException sqle) {
                JOptionPane.showMessageDialog(this, "Fallo al ejecutar el SP de Oracle:\n" + sqle.getMessage(), "Error SP", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
