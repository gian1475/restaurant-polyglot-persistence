package com.restaurante.vista;

import com.restaurante.dao.VentasHiveDAO;
import com.restaurante.util.ComponentBuilder;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla que ejecuta consultas analíticas en Apache Hive (Big Data / MapReduce) 
 * y las representa de forma gráfica e interactiva.
 * 
 * @author Gianf
 */
public class ReportesHiveFrame extends JFrame {

    private JTable tblVentas;
    private DefaultTableModel modelVentas;
    private JButton btnGenerar;
    private JButton btnCerrar;
    private JLabel lblStatus;
    private GraficoPanel panelGrafico;

    private List<Object[]> datosReporte = new ArrayList<>();

    public ReportesHiveFrame() {
        super("Métricas Big Data - Apache Hive");
        inicializarUI();
    }

    private void inicializarUI() {
        setSize(850, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ComponentBuilder.COLOR_BACKGROUND);
        setContentPane(mainPanel);

        // Header
        mainPanel.add(ComponentBuilder.crearHeaderPantalla(
            "REGISTRO DE VENTAS HISTÓRICAS (HiveQL)", 
            "Agregaciones analíticas procesadas bajo Hadoop HDFS mediante consultas distribuidas"
        ), BorderLayout.NORTH);

        // Panel Central: Split horizontal (Tabla a la izquierda, Gráfico a la derecha)
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(380);
        split.setOpaque(false);
        split.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Panel Izquierdo: Tabla
        JPanel panelTabla = ComponentBuilder.crearCard();
        panelTabla.setLayout(new BorderLayout(5, 5));
        
        lblStatus = new JLabel("Presione 'Generar reporte' para iniciar la consulta.");
        lblStatus.setFont(ComponentBuilder.FONT_BODY_BOLD);
        lblStatus.setForeground(ComponentBuilder.COLOR_TEXT_MUTED);
        panelTabla.add(lblStatus, BorderLayout.NORTH);

        String[] columnas = {"ID Sucursal", "Monto Acumulado", "Cant. Pedidos"};
        modelVentas = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblVentas = new JTable(modelVentas);
        ComponentBuilder.estilarTabla(tblVentas);
        panelTabla.add(new JScrollPane(tblVentas), BorderLayout.CENTER);

        // Panel Derecho: Gráfico personalizado
        panelGrafico = new GraficoPanel();
        
        split.setLeftComponent(panelTabla);
        split.setRightComponent(panelGrafico);
        mainPanel.add(split, BorderLayout.CENTER);

        // Panel de Botones inferior
        JPanel panelBotones = ComponentBuilder.crearCard();
        panelBotones.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 5));

        btnGenerar = ComponentBuilder.crearBotonPrincipal("Generar reporte (Hive)");
        btnCerrar = ComponentBuilder.crearBoton("Cerrar", ComponentBuilder.COLOR_TEXT_MUTED, Color.WHITE);

        panelBotones.add(btnGenerar);
        panelBotones.add(btnCerrar);
        mainPanel.add(panelBotones, BorderLayout.SOUTH);

        // Acciones
        btnGenerar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarReporteHive();
            }
        });

        btnCerrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void ejecutarReporteHive() {
        btnGenerar.setEnabled(false);
        lblStatus.setText("Ejecutando MapReduce en Hive... espere...");
        lblStatus.setForeground(ComponentBuilder.COLOR_PRIMARY);

        // Ejecutar en hilo secundario para evitar congelamiento de UI (Swing Worker)
        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                VentasHiveDAO dao = new VentasHiveDAO();
                return dao.obtenerReporteVentasPorSucursal();
            }

            @Override
            protected void done() {
                try {
                    datosReporte = get();
                    modelVentas.setRowCount(0);
                    
                    for (Object[] fila : datosReporte) {
                        modelVentas.addRow(new Object[]{
                            "Sucursal ID: " + fila[0],
                            String.format("S/ %.2f", fila[1]),
                            fila[2]
                        });
                    }

                    lblStatus.setText("Consulta finalizada con éxito.");
                    lblStatus.setForeground(ComponentBuilder.COLOR_SUCCESS);
                    panelGrafico.setDatos(datosReporte);

                } catch (Exception ex) {
                    lblStatus.setText("Error al consultar Hive.");
                    lblStatus.setForeground(ComponentBuilder.COLOR_WARNING);
                    
                    JOptionPane.showMessageDialog(ReportesHiveFrame.this, 
                        "No se pudo completar la consulta en Hive:\n" + ex.getCause().getMessage() +
                        "\n\nRecuerde que Hive debe estar encendido en su contenedor Docker (puerto 10000) " +
                        "y la tabla externa 'ventas_hist' debe existir en HDFS.", 
                        "Error en Big Data", 
                        JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnGenerar.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    /**
     * Componente JPanel personalizado para renderizar un gráfico de barras simple.
     */
    private class GraficoPanel extends JPanel {
        private List<Object[]> datos = new ArrayList<>();

        public GraficoPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(15, 15, 15, 15)
            ));
        }

        public void setDatos(List<Object[]> datos) {
            this.datos = datos;
            repaint(); // Solicitar repintado para dibujar
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Dibujar título del gráfico
            g2.setFont(ComponentBuilder.FONT_SUBTITLE);
            g2.setColor(ComponentBuilder.COLOR_DARK_HEADER);
            g2.drawString("Gráfico Comparativo de Ventas (S/)", 20, 30);

            if (datos == null || datos.isEmpty()) {
                g2.setFont(ComponentBuilder.FONT_BODY);
                g2.setColor(ComponentBuilder.COLOR_TEXT_MUTED);
                g2.drawString("Sin datos cargados. Presione el botón del reporte.", 40, height / 2);
                return;
            }

            // Encontrar el valor máximo para escalar las barras
            double maxVenta = 0.0;
            for (Object[] fila : datos) {
                double venta = (Double) fila[1];
                if (venta > maxVenta) maxVenta = venta;
            }

            // Parámetros de dibujo
            int startX = 60;
            int startY = height - 60;
            int graphHeight = height - 120;
            int graphWidth = width - 100;
            int barWidth = Math.min(60, graphWidth / (datos.size() * 2));
            int gap = barWidth;

            // Ejes de referencia
            g2.setColor(new Color(200, 214, 229));
            g2.drawLine(startX, startY, startX + graphWidth, startY); // Eje X
            g2.drawLine(startX, startY, startX, startY - graphHeight); // Eje Y

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(ComponentBuilder.COLOR_TEXT_MAIN);

            // Dibujar barras
            for (int i = 0; i < datos.size(); i++) {
                Object[] fila = datos.get(i);
                int sucursalId = (Integer) fila[0];
                double venta = (Double) fila[1];

                int barHeight = (int) ((venta / maxVenta) * graphHeight);
                int x = startX + gap + i * (barWidth + gap);
                int y = startY - barHeight;

                // Relleno de la barra (color alternado)
                g2.setColor(i % 2 == 0 ? ComponentBuilder.COLOR_PRIMARY : ComponentBuilder.COLOR_SUCCESS);
                g2.fillRect(x, y, barWidth, barHeight);

                // Borde de la barra
                g2.setColor(g2.getColor().darker());
                g2.drawRect(x, y, barWidth, barHeight);

                // Etiquetas de datos
                g2.setColor(ComponentBuilder.COLOR_TEXT_MAIN);
                g2.drawString("Suc. " + sucursalId, x + (barWidth / 2) - 15, startY + 18);
                
                String valStr = String.format("S/ %.0f", venta);
                g2.drawString(valStr, x + (barWidth / 2) - 18, y - 5);
            }
        }
    }
}
