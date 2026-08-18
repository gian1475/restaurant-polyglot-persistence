package com.restaurante.modelo;

/**
 * Representa el Detalle de Pedido almacenado en PostgreSQL.
 * 
 * @author Gianf
 */
public class DetallePedido {
    private int detalleId;
    private int pedidoId;
    private int productoId;
    private int cantidad;
    private double precioUnitario;
    
    // Campo auxiliar para visualización en la interfaz Swing
    private String nombreProductoAux;

    public DetallePedido() {}

    public DetallePedido(int productoId, String nombreProductoAux, int cantidad, double precioUnitario) {
        this.productoId = productoId;
        this.nombreProductoAux = nombreProductoAux;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public int getDetalleId() {
        return detalleId;
    }

    public void setDetalleId(int detalleId) {
        this.detalleId = detalleId;
    }

    public int getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(int pedidoId) {
        this.pedidoId = pedidoId;
    }

    public int getProductoId() {
        return productoId;
    }

    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getSubtotal() {
        return cantidad * precioUnitario;
    }

    public String getNombreProductoAux() {
        return nombreProductoAux;
    }

    public void setNombreProductoAux(String nombreProductoAux) {
        this.nombreProductoAux = nombreProductoAux;
    }
}
