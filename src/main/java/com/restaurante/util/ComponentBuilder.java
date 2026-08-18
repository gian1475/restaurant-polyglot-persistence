package com.restaurante.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Utilidades para construir componentes visuales en Java Swing con una estética
 * moderna, limpia y premium (colores curados, fuentes legibles, bordes planos).
 * 
 * @author Gianf
 */
public class ComponentBuilder {

    // Paleta de colores Premium
    public static final Color COLOR_PRIMARY = new Color(52, 152, 219);     // Azul brillante elegante
    public static final Color COLOR_PRIMARY_HOVER = new Color(41, 128, 185);
    public static final Color COLOR_BACKGROUND = new Color(245, 246, 250);   // Fondo general claro
    public static final Color COLOR_CARD = new Color(255, 255, 255);         // Fondo de contenedores/tarjetas
    public static final Color COLOR_DARK_HEADER = new Color(44, 62, 80);     // Azul marino oscuro para cabeceras
    public static final Color COLOR_TEXT_MAIN = new Color(44, 62, 80);
    public static final Color COLOR_TEXT_MUTED = new Color(127, 140, 141);
    public static final Color COLOR_WARNING = new Color(231, 76, 60);        // Rojo de alerta
    public static final Color COLOR_SUCCESS = new Color(46, 204, 113);       // Verde de éxito

    // Fuentes
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    /**
     * Aplica el Look & Feel básico del sistema operativo o el Flat/Metal de Java
     * y configura propiedades generales.
     */
    public static void inicializarTema() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Silencioso, usar por defecto
        }
    }

    /**
     * Crea un botón plano estilizado con efectos hover.
     */
    public static JButton crearBoton(String texto, Color background, Color foreground) {
        JButton btn = new JButton(texto);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(background);
        btn.setForeground(foreground);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover Effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (background.equals(COLOR_PRIMARY)) {
                    btn.setBackground(COLOR_PRIMARY_HOVER);
                } else {
                    btn.setBackground(background.darker());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(background);
            }
        });

        return btn;
    }

    /**
     * Crea un botón principal de acción (azul elegante).
     */
    public static JButton crearBotonPrincipal(String texto) {
        return crearBoton(texto, COLOR_PRIMARY, Color.WHITE);
    }

    /**
     * Crea un panel contenedor con bordes limpios e internos (tarjeta).
     */
    public static JPanel crearCard() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        return panel;
    }

    /**
     * Configura y aplica estilos modernos a una JTable.
     */
    public static void estilarTabla(JTable tabla) {
        tabla.setFont(FONT_BODY);
        tabla.setRowHeight(28);
        tabla.setBackground(COLOR_CARD);
        tabla.setGridColor(new Color(236, 240, 241));
        tabla.setSelectionBackground(new Color(212, 230, 241));
        tabla.setSelectionForeground(COLOR_DARK_HEADER);

        // Estilo del Header
        JTableHeader header = tabla.getTableHeader();
        header.setFont(FONT_BODY_BOLD);
        header.setBackground(COLOR_DARK_HEADER);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);

        // Alineación y bordes de celdas
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        tabla.setDefaultRenderer(Object.class, renderer);
    }

    /**
     * Crea una cabecera de formulario o pantalla.
     */
    public static JPanel crearHeaderPantalla(String titulo, String subtitulo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_DARK_HEADER);
        panel.setBorder(new EmptyBorder(15, 25, 15, 25));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(FONT_TITLE);
        lblTitulo.setForeground(Color.WHITE);
        panel.add(lblTitulo, BorderLayout.NORTH);

        if (subtitulo != null && !subtitulo.isEmpty()) {
            JLabel lblSub = new JLabel(subtitulo);
            lblSub.setFont(FONT_BODY);
            lblSub.setForeground(new Color(200, 214, 229));
            panel.add(lblSub, BorderLayout.SOUTH);
        }

        return panel;
    }
}
