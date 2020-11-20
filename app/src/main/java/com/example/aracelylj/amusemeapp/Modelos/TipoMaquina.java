package com.example.aracelylj.amusemeapp.Modelos;

public class TipoMaquina {

    private String idTipoMaquina;
    private String nombre;
    private String contadores;
    private String observaciones;

    public TipoMaquina(String idTipoMaquina, String nombre, String contadores, String observaciones) {
        this.idTipoMaquina = idTipoMaquina;
        this.nombre = nombre;
        this.contadores = contadores;
        this.observaciones = observaciones;
    }

    public String getIdTipoMaquina() {
        return idTipoMaquina;
    }

    public void setIdTipoMaquina(String idTipoMaquina) {
        this.idTipoMaquina = idTipoMaquina;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getContadores() {
        return contadores;
    }

    public void setContadores(String contadores) {
        this.contadores = contadores;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
