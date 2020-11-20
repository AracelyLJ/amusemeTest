package com.example.aracelylj.amusemeapp.Modelos;

public class Maquina {

    private String clave;
    private String alias;
    private String observaciones;
    private String imagen;
    private long renta;

    public Maquina(String clave, String alias, String observaciones, String imagen, long renta) {
        this.clave = clave;
        this.alias = alias;
        this.observaciones = observaciones;
        this.imagen = imagen;
        this.renta = renta;
    }

    public String getClave() {
        return clave;
    }

    public String getAlias() {
        return alias;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public String getImagen() {
        return imagen;
    }

    public long getRenta() {
        return renta;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setClave(String clave){
        this.clave = clave;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public void setRenta(long renta) {
        this.renta = renta;
    }
}