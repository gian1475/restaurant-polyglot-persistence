package com.restaurante.modelo;

import java.sql.Date;

/**
 * Representa la entidad Empleado almacenada en Oracle.
 * 
 * @author Gianf
 */
public class Empleado {
    private int empleadoId;
    private String nombres;
    private String apellidos;
    private String dni;
    private int cargoId;
    private int sucursalId;
    private Date fechaContratacion;
    private String estado;

    public Empleado() {}

    public Empleado(int empleadoId, String nombres, String apellidos, int sucursalId) {
        this.empleadoId = empleadoId;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.sucursalId = sucursalId;
    }

    public int getEmpleadoId() {
        return empleadoId;
    }

    public void setEmpleadoId(int empleadoId) {
        this.empleadoId = empleadoId;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public int getCargoId() {
        return cargoId;
    }

    public void setCargoId(int cargoId) {
        this.cargoId = cargoId;
    }

    public int getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(int sucursalId) {
        this.sucursalId = sucursalId;
    }

    public Date getFechaContratacion() {
        return fechaContratacion;
    }

    public void setFechaContratacion(Date fechaContratacion) {
        this.fechaContratacion = fechaContratacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return nombres + " " + apellidos + " (ID: " + empleadoId + ")";
    }
}
