package com.restaurante.modelo;

import java.sql.Date;

/**
 * Representa la entidad Cliente almacenada en PostgreSQL.
 * 
 * @author Gianf
 */
public class Cliente {
    private int clienteId;
    private String nombres;
    private String apellidos;
    private String email;
    private String telefono;
    private Date fechaRegistro;

    public Cliente() {}

    public Cliente(int clienteId, String nombres, String apellidos, String email, String telefono) {
        this.clienteId = clienteId;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.telefono = telefono;
    }

    public int getClienteId() {
        return clienteId;
    }

    public void setClienteId(int clienteId) {
        this.clienteId = clienteId;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Date getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Date fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String toString() {
        return nombres + " " + apellidos + " (ID: " + clienteId + ")";
    }
}
