package com.restaurante.vista;

import com.restaurante.dao.PedidoMongoDAO;
import com.restaurante.util.ComponentBuilder;
import org.bson.Document;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla que recupera y muestra los pedidos operativos de MongoDB Atlas (NoSQL).
 * Cuenta con un panel inferior expandible para mostrar el detalle de items embebidos.
 * 
 * @author Gianf
 */
public class PedidosMongoFrame extends JFrame {

    private JTable tblPedidos;
    private DefaultTableModel modelPedidos;
    private JTable tblItems;
    private DefaultTableModel modelItems;
    private JButton btnCerrar;
    private JButton btnActualizar;
    private JTextArea txtRawJson;

    private List<Document> listaDocumentos = new ArrayList<>();

    public PedidosMongoFrame() {
        super("Pedidos Operativos - MongoDB NoSQL");
        inicializarUI();
        cargarPedidosMongo();
    }

    private void inicializarUI() {
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ComponentBuilder.COLOR_BACKGROUND);
        setContentPane(mainPanel);

        // Header
        mainPanel.add(ComponentBuilder.crearHeaderPantalla(
            "PEDIDOS OPERATIVOS (NoSQL - MongoDB)", 
            "Consulta ágil de pedidos documentales JSON sin JOINs (ideal para cocina/delivery)"
        ), BorderLayout.NORTH);

        // SplitPane Central
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setOpaque(false);
        splitPane.setBorder(new EmptyBorder(15, 15, 15, 15));
        splitPane.setResizeWeight(0.5);

        // Panel Superior: Tabla de Cabeceras
        JPanel panelSuperior = ComponentBuilder.crearCard();
        panelSuperior.setLayout(new BorderLayout(5, 5));
        
        JLabel lblListado = new JLabel("Listado de Documentos en la Colección:");
        lblListado.setFont(ComponentBuilder.FONT_SUBTITLE);
        lblListado.setForeground(ComponentBuilder.COLOR_DARK_HEADER);
        panelSuperior.add(lblListado, BorderLayout.NORTH);

        String[] colsCabecera = {"ID Mongo", "ID Relac.", "Sucursal", "Cliente", "Tipo", "Estado", "Monto Total", "Fecha"};
        modelPedidos = new DefaultTableModel(colsCabecera, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblPedidos = new JTable(modelPedidos);
        ComponentBuilder.estilarTabla(tblPedidos);
        JScrollPane scrollSuperior = new JScrollPane(tblPedidos);
        panelSuperior.add(scrollSuperior, BorderLayout.CENTER);

        // Panel Inferior: Detalle del documento seleccionado
        JPanel panelInferior = ComponentBuilder.crearCard();
        panelInferior.setLayout(new BorderLayout(10, 10));

        JLabel lblDetalle = new JLabel("Items embebidos del Pedido Seleccionado:");
        lblDetalle.setFont(ComponentBuilder.FONT_SUBTITLE);
        lblDetalle.setForeground(ComponentBuilder.COLOR_DARK_HEADER);
        panelInferior.add(lblDetalle, BorderLayout.NORTH);

        // Sub-panel inferior con Split horizontal: Tabla de items a la izquierda, JSON crudo a la derecha
        JSplitPane splitDetalle = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitDetalle.setDividerLocation(520);
        splitDetalle.setOpaque(false);
        splitDetalle.setResizeWeight(0.5);

        // Tabla de items
        String[] colsItems = {"ID Producto", "Nombre Producto", "Cantidad", "Precio Unit.", "Subtotal"};
        modelItems = new DefaultTableModel(colsItems, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblItems = new JTable(modelItems);
        ComponentBuilder.estilarTabla(tblItems);
        JScrollPane scrollItems = new JScrollPane(tblItems);
        splitDetalle.setLeftComponent(scrollItems);

        // JSON text area
        JPanel panelJson = new JPanel(new BorderLayout());
        panelJson.setOpaque(false);
        panelJson.add(new JLabel("Vista Documental JSON:"), BorderLayout.NORTH);
        
        txtRawJson = new JTextArea();
        txtRawJson.setEditable(false);
        txtRawJson.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtRawJson.setBackground(new Color(248, 249, 250));
        txtRawJson.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230)));
        JScrollPane scrollJson = new JScrollPane(txtRawJson);
        panelJson.add(scrollJson, BorderLayout.CENTER);
        
        splitDetalle.setRightComponent(panelJson);
        panelInferior.add(splitDetalle, BorderLayout.CENTER);

        splitPane.setTopComponent(panelSuperior);
        splitPane.setBottomComponent(panelInferior);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Botones de acción
        JPanel panelBotones = ComponentBuilder.crearCard();
        panelBotones.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 5));

        btnActualizar = ComponentBuilder.crearBoton("Actualizar NoSQL", ComponentBuilder.COLOR_PRIMARY, Color.WHITE);
        btnCerrar = ComponentBuilder.crearBoton("Cerrar", ComponentBuilder.COLOR_TEXT_MUTED, Color.WHITE);

        panelBotones.add(btnActualizar);
        panelBotones.add(btnCerrar);
        mainPanel.add(panelBotones, BorderLayout.SOUTH);

        // LISTENERS
        tblPedidos.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    mostrarDetallePedido();
                }
            }
        });

        btnActualizar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cargarPedidosMongo();
            }
        });

        btnCerrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void cargarPedidosMongo() {
        try {
            modelPedidos.setRowCount(0);
            modelItems.setRowCount(0);
            txtRawJson.setText("");
            
            PedidoMongoDAO dao = new PedidoMongoDAO();
            listaDocumentos = dao.listarPedidosOperativos();

            for (Document doc : listaDocumentos) {
                String id = doc.getString("_id");
                Object idRel = doc.get("pedido_id_relacional");
                
                Document sucursal = (Document) doc.get("sucursal");
                String sucNombre = sucursal != null ? sucursal.getString("nombre") : "N/A";
                
                Document cliente = (Document) doc.get("cliente");
                String cliNombre = cliente != null ? cliente.getString("nombre") : "N/A";
                
                String tipo = doc.getString("tipo_pedido");
                String estado = doc.getString("estado");
                Object totalObj = doc.get("total");
                Double total = (totalObj instanceof Number) ? ((Number) totalObj).doubleValue() : null;
                Object fecha = doc.get("fecha");
                
                modelPedidos.addRow(new Object[]{
                    id,
                    idRel != null ? idRel.toString() : "",
                    sucNombre,
                    cliNombre,
                    tipo,
                    estado,
                    total != null ? String.format("S/ %.2f", total) : "",
                    fecha != null ? fecha.toString() : ""
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error al conectar o consultar MongoDB Atlas:\n" + e.getMessage() + 
                "\n\nVerifique si su clúster está activo y las credenciales del archivo de configuración.", 
                "Error NoSQL", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarDetallePedido() {
        int index = tblPedidos.getSelectedRow();
        if (index == -1 || index >= listaDocumentos.size()) {
            return;
        }

        modelItems.setRowCount(0);
        Document doc = listaDocumentos.get(index);

        // Cargar JSON Formateado
        txtRawJson.setText(formatJson(doc.toJson()));
        txtRawJson.setCaretPosition(0);

        // Obtener la sublista de items embebidos
        @SuppressWarnings("unchecked")
        List<Document> items = (List<Document>) doc.get("items");
        if (items != null) {
            for (Document it : items) {
                Object pId = it.get("producto_id");
                String nombre = it.getString("nombre");
                Object cant = it.get("cantidad");
                Object precio = it.get("precio_unitario");
                
                double cVal = (cant instanceof Number) ? ((Number) cant).doubleValue() : 0.0;
                double pVal = (precio instanceof Number) ? ((Number) precio).doubleValue() : 0.0;
                double sub = cVal * pVal;

                modelItems.addRow(new Object[]{
                    pId != null ? pId.toString() : "",
                    nombre,
                    cant != null ? cant.toString() : "",
                    String.format("S/ %.2f", pVal),
                    String.format("S/ %.2f", sub)
                });
            }
        }
    }

    /** Formateador básico indentador de JSON para visualización legible */
    private String formatJson(String json) {
        StringBuilder format = new StringBuilder();
        int indent = 0;
        for (char c : json.toCharArray()) {
            if (c == '{' || c == '[') {
                format.append(c).append("\n");
                indent++;
                for (int i = 0; i < indent; i++) format.append("  ");
            } else if (c == '}' || c == ']') {
                format.append("\n");
                indent--;
                for (int i = 0; i < indent; i++) format.append("  ");
                format.append(c);
            } else if (c == ',') {
                format.append(c).append("\n");
                for (int i = 0; i < indent; i++) format.append("  ");
            } else {
                format.append(c);
            }
        }
        return format.toString();
    }
}
