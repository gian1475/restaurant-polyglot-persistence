package com.restaurante.modelo;

/**
 * Representa la entidad Insumo almacenada en Oracle.
 * 
 * @author Gianf
 */
public class Insumo {
    private int insumoId;
    private String nombreInsumo;
    private String unidadMedida;
    private double stockActual;
    private double stockMinimo;

    public Insumo() {}

    public Insumo(int insumoId, String nombreInsumo, String unidadMedida, double stockActual, double stockMinimo) {
        this.insumoId = insumoId;
        this.nombreInsumo = nombreInsumo;
        this.unidadMedida = unidadMedida;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
    }

    public int getInsumoId() {
        return insumoId;
    }

    public void setInsumoId(int insumoId) {
        this.insumoId = insumoId;
    }

    public String getNombreInsumo() {
        return nombreInsumo;
    }

    public void setNombreInsumo(String nombreInsumo) {
        this.nombreInsumo = nombreInsumo;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public double getStockActual() {
        return stockActual;
    }

    public void setStockActual(double stockActual) {
        this.stockActual = stockActual;
    }

    public double getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(double stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    @Override
    public String toString() {
        return nombreInsumo + " (" + unidadMedida + ") - Stock: " + stockActual;
    }
}
