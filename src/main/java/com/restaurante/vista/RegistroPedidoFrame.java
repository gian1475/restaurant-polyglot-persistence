package com.restaurante.vista;

import com.restaurante.conexion.ConexionManager;
import com.restaurante.dao.ClienteDAO;
import com.restaurante.dao.EmpleadoDAO;
import com.restaurante.dao.PedidoDAO;
import com.restaurante.dao.ProductoDAO;
import com.restaurante.modelo.Cliente;
import com.restaurante.modelo.DetallePedido;
import com.restaurante.modelo.Empleado;
import com.restaurante.modelo.Pedido;
import com.restaurante.modelo.Producto;
import com.restaurante.util.ComponentBuilder;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de registro de pedidos. Realiza la carga dinámica desde PostgreSQL 
 * y Oracle, y ejecuta la validación cruzada antes de persistir la transacción.
 * 
 * @author Gianf
 */
public class RegistroPedidoFrame extends JFrame {

    private JComboBox<Cliente> cbClientes;
    private JComboBox<Empleado> cbEmpleados;
    private JComboBox<String> cbSucursal; // Carga dinámica de Oracle
    private JComboBox<Producto> cbProductos;
    private JComboBox<String> cbTipoPedido;
    private JTextField txtDireccion;
    private JSpinner spCantidad;
    private JButton btnAgregarProducto;
    private JButton btnEliminarProducto;
    private JTable tblDetalle;
    private DefaultTableModel modelDetalle;
    private JLabel lblTotal;
    private JButton btnGuardar;
    private JButton btnCancelar;

    private List<DetallePedido> detallesLista = new ArrayList<>();
    private double montoTotal = 0.0;

    public RegistroPedidoFrame() {
        super("Registrar Nuevo Pedido - Validación Cruzada");
        inicializarUI();
        cargarCombos();
    }

    private void inicializarUI() {
        setSize(850, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ComponentBuilder.COLOR_BACKGROUND);
        setContentPane(mainPanel);

        // Cabecera
        mainPanel.add(ComponentBuilder.crearHeaderPantalla(
            "REGISTRO DE PEDIDOS", 
            "Combina información de PostgreSQL (Clientes) y Oracle (Sucursal/Empleados/Productos)"
        ), BorderLayout.NORTH);

        // Panel Central dividido
        JPanel panelCentro = new JPanel(new BorderLayout(15, 15));
        panelCentro.setOpaque(false);
        panelCentro.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Formulario Superior (Datos de Cabecera)
        JPanel panelCabecera = ComponentBuilder.crearCard();
        panelCabecera.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Fila 0: Cliente y Sucursal
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1;
        panelCabecera.add(new JLabel("Cliente (Postgres):"), gbc);
        cbClientes = new JComboBox<>();
        gbc.gridx = 1; gbc.weightx = 0.4;
        panelCabecera.add(cbClientes, gbc);

        gbc.gridx = 2; gbc.weightx = 0.1;
        panelCabecera.add(new JLabel("Sucursal (Oracle):"), gbc);
        cbSucursal = new JComboBox<>();
        gbc.gridx = 3; gbc.weightx = 0.4;
        panelCabecera.add(cbSucursal, gbc);

        // Fila 1: Empleado y Tipo de Pedido
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.1;
        panelCabecera.add(new JLabel("Atendido por (Oracle):"), gbc);
        cbEmpleados = new JComboBox<>();
        gbc.gridx = 1; gbc.weightx = 0.4;
        panelCabecera.add(cbEmpleados, gbc);

        gbc.gridx = 2; gbc.weightx = 0.1;
        panelCabecera.add(new JLabel("Tipo Pedido (Postgres):"), gbc);
        cbTipoPedido = new JComboBox<>(new String[]{"SALON", "DELIVERY"});
        gbc.gridx = 3; gbc.weightx = 0.4;
        panelCabecera.add(cbTipoPedido, gbc);

        // Fila 2: Dirección Delivery (sólo visible para DELIVERY)
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.1;
        panelCabecera.add(new JLabel("Dirección Delivery:"), gbc);
        txtDireccion = new JTextField();
        txtDireccion.setEnabled(false); // Inicia deshabilitado ya que por defecto es SALON
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.9;
        panelCabecera.add(txtDireccion, gbc);

        panelCentro.add(panelCabecera, BorderLayout.NORTH);

        // Panel de Carga de Productos (Detalle)
        JPanel panelCargaProductos = ComponentBuilder.crearCard();
        panelCargaProductos.setLayout(new BorderLayout(10, 10));

        JPanel panelFiltrosProd = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelFiltrosProd.setOpaque(false);
        panelFiltrosProd.add(new JLabel("Producto (Oracle):"));
        cbProductos = new JComboBox<>();
        cbProductos.setPreferredSize(new Dimension(220, 28));
        panelFiltrosProd.add(cbProductos);

        panelFiltrosProd.add(new JLabel("Cantidad:"));
        spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        spCantidad.setPreferredSize(new Dimension(60, 28));
        panelFiltrosProd.add(spCantidad);

        btnAgregarProducto = ComponentBuilder.crearBotonPrincipal("Agregar Producto");
        panelFiltrosProd.add(btnAgregarProducto);

        btnEliminarProducto = ComponentBuilder.crearBoton("Eliminar", ComponentBuilder.COLOR_WARNING, Color.WHITE);
        panelFiltrosProd.add(btnEliminarProducto);

        panelCargaProductos.add(panelFiltrosProd, BorderLayout.NORTH);

        // JTable del Detalle
        String[] columnas = {"ID Producto", "Producto", "Precio Unit.", "Cantidad", "Subtotal"};
        modelDetalle = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblDetalle = new JTable(modelDetalle);
        ComponentBuilder.estilarTabla(tblDetalle);
        JScrollPane scrollTabla = new JScrollPane(tblDetalle);
        panelCargaProductos.add(scrollTabla, BorderLayout.CENTER);

        // Footer del Panel Central (Total de compra)
        JPanel panelFooterDetalle = new JPanel(new BorderLayout());
        panelFooterDetalle.setOpaque(false);
        lblTotal = new JLabel("Total: S/ 0.00 ");
        lblTotal.setFont(ComponentBuilder.FONT_SUBTITLE);
        lblTotal.setForeground(ComponentBuilder.COLOR_DARK_HEADER);
        panelFooterDetalle.add(lblTotal, BorderLayout.EAST);

        panelCargaProductos.add(panelFooterDetalle, BorderLayout.SOUTH);
        panelCentro.add(panelCargaProductos, BorderLayout.CENTER);
        mainPanel.add(panelCentro, BorderLayout.CENTER);

        // Panel de Control Inferior (Guardar / Cancelar)
        JPanel panelControl = ComponentBuilder.crearCard();
        panelControl.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 5));

        btnGuardar = ComponentBuilder.crearBoton("Guardar Pedido", ComponentBuilder.COLOR_SUCCESS, Color.WHITE);
        btnCancelar = ComponentBuilder.crearBoton("Cancelar", ComponentBuilder.COLOR_TEXT_MUTED, Color.WHITE);

        panelControl.add(btnGuardar);
        panelControl.add(btnCancelar);
        mainPanel.add(panelControl, BorderLayout.SOUTH);

        // CONTROL DE EVENTOS
        cbTipoPedido.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean esDelivery = "DELIVERY".equals(cbTipoPedido.getSelectedItem());
                txtDireccion.setEnabled(esDelivery);
                if (!esDelivery) txtDireccion.setText("");
            }
        });

        btnAgregarProducto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                agregarProductoATabla();
            }
        });

        btnEliminarProducto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eliminarProductoDeTabla();
            }
        });

        btnGuardar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                guardarPedido();
            }
        });

        btnCancelar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void cargarCombos() {
        try {
            // 1. Cargar Clientes (PostgreSQL)
            ClienteDAO clienteDAO = new ClienteDAO();
            List<Cliente> clientes = clienteDAO.listarClientes();
            for (Cliente c : clientes) {
                cbClientes.addItem(c);
            }

            // 2. Cargar Sucursales (Oracle)
            cargarSucursalesOracle();

            // 3. Cargar Empleados (Oracle)
            EmpleadoDAO empleadoDAO = new EmpleadoDAO();
            List<Empleado> empleados = empleadoDAO.listarEmpleadosActivos();
            for (Empleado emp : empleados) {
                cbEmpleados.addItem(emp);
            }

            // 4. Cargar Productos (Oracle)
            ProductoDAO productoDAO = new ProductoDAO();
            List<Producto> productos = productoDAO.listarProductosDisponibles();
            for (Producto p : productos) {
                cbProductos.addItem(p);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error al cargar datos dinámicos:\n" + e.getMessage(), 
                "Error de Base de Datos", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarSucursalesOracle() throws SQLException {
        Connection conn = ConexionManager.getInstancia().getConexionOracle();
        if (conn == null) return;
        String sql = "SELECT sucursal_id, nombre FROM sucursal ORDER BY sucursal_id";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("sucursal_id");
                String nom = rs.getString("nombre");
                cbSucursal.addItem(id + " - " + nom);
            }
        }
    }

    private void agregarProductoATabla() {
        Producto prod = (Producto) cbProductos.getSelectedItem();
        int cant = (Integer) spCantidad.getValue();

        if (prod == null) return;

        // Comprobar si ya se agregó el producto
        for (int i = 0; i < tblDetalle.getRowCount(); i++) {
            int existingId = (Integer) tblDetalle.getValueAt(i, 0);
            if (existingId == prod.getProductoId()) {
                JOptionPane.showMessageDialog(this, "El producto ya ha sido agregado. Elimínelo si desea cambiar la cantidad.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        double subtotal = prod.getPrecio() * cant;
        modelDetalle.addRow(new Object[]{
            prod.getProductoId(),
            prod.getNombreProducto(),
            prod.getPrecio(),
            cant,
            subtotal
        });

        DetallePedido detalle = new DetallePedido(prod.getProductoId(), prod.getNombreProducto(), cant, prod.getPrecio());
        detallesLista.add(detalle);

        recalcularTotal();
    }

    private void eliminarProductoDeTabla() {
        int index = tblDetalle.getSelectedRow();
        if (index == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un producto del detalle para eliminarlo.", "Seleccione fila", JOptionPane.WARNING_MESSAGE);
            return;
        }

        detallesLista.remove(index);
        modelDetalle.removeRow(index);
        recalcularTotal();
    }

    private void recalcularTotal() {
        montoTotal = 0.0;
        for (DetallePedido det : detallesLista) {
            montoTotal += det.getSubtotal();
        }
        lblTotal.setText(String.format("Total: S/ %.2f ", montoTotal));
    }

    private void guardarPedido() {
        if (detallesLista.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe agregar al menos un producto al pedido.", "Falta Detalle", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Cliente cliente = (Cliente) cbClientes.getSelectedItem();
        Empleado empleado = (Empleado) cbEmpleados.getSelectedItem();
        String sucursalStr = (String) cbSucursal.getSelectedItem();

        if (cliente == null || empleado == null || sucursalStr == null) {
            JOptionPane.showMessageDialog(this, "Asegúrese de cargar correctamente todos los combos.", "Datos faltantes", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Obtener ID de la sucursal del combo string "1 - Sede Miraflores"
        int sucursalId = Integer.parseInt(sucursalStr.split(" - ")[0]);

        // Construir modelo de negocio Pedido
        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getClienteId());
        pedido.setEmpleadoId(empleado.getEmpleadoId());
        pedido.setSucursalId(sucursalId);
        pedido.setTipoPedido((String) cbTipoPedido.getSelectedItem());
        pedido.setDireccionDelivery(txtDireccion.getText().trim());
        pedido.setDetalles(detallesLista);

        try {
            PedidoDAO dao = new PedidoDAO();
            int newId = dao.registrarPedidoConValidacionCruzada(pedido);

            JOptionPane.showMessageDialog(this, 
                "¡Éxito!\nPedido #" + newId + " registrado.\n" +
                "• Validado en Oracle (Sucursal, Empleado, Disponibilidad del Producto)\n" +
                "• Persistido atómicamente en PostgreSQL.", 
                "Pedido Registrado", 
                JOptionPane.INFORMATION_MESSAGE
            );

            // Limpiar formulario
            detallesLista.clear();
            modelDetalle.setRowCount(0);
            recalcularTotal();
            txtDireccion.setText("");

        } catch (IllegalStateException ise) {
            // Error de validación cruzada arrojado por el DAO
            JOptionPane.showMessageDialog(this, 
                "FALLO DE VALIDACIÓN CRUZADA:\n" + ise.getMessage(), 
                "Validación de Referencia Inexistente", 
                JOptionPane.ERROR_MESSAGE
            );
        } catch (SQLException sqle) {
            // Error SQL
            JOptionPane.showMessageDialog(this, 
                "ERROR DE BASE DE DATOS (Transacción Abortada):\n" + sqle.getMessage(), 
                "Error JDBC", 
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
