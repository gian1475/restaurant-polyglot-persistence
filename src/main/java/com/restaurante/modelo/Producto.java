package com.restaurante.modelo;

/**
 * Representa la entidad Producto almacenada en Oracle.
 * 
 * @author Gianf
 */
public class Producto {
    private int productoId;
    private String nombreProducto;
    private int categoriaId;
    private double precio;
    private String disponible;

    public Producto() {}

    public Producto(int productoId, String nombreProducto, double precio, String disponible) {
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.precio = precio;
        this.disponible = disponible;
    }

    public int getProductoId() {
        return productoId;
    }

    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public int getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(int categoriaId) {
        this.categoriaId = categoriaId;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getDisponible() {
        return disponible;
    }

    public void setDisponible(String disponible) {
        this.disponible = disponible;
    }

    @Override
    public String toString() {
        return nombreProducto + " (S/ " + String.format("%.2f", precio) + ")";
    }
}
