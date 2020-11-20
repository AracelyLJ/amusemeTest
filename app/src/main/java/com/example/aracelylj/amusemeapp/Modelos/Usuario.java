package com.example.aracelylj.amusemeapp.Modelos;

public class Usuario {

    private String key;
    private String correo;
    private String nombre;
    private String nickname;
    private String password;
    private String rol;
    private String tel;
    private String sucursales;
    private String fotoPersonal;
    private String fotoIFE;
    private String estatus;

    public Usuario() {
        this.key = "";
        this.correo = "";
        this.nombre = "";
        this.nickname = "";
        this.password = "";
        this.rol = "";
        this.tel = "";
        this.sucursales = null;
        this.fotoPersonal = "-";
        this.fotoIFE = "-";
        this.estatus = estatus;
    }

    public Usuario(String string, String correo, String nombre, String nickname, String password, String rol, String estatus) {
        this.key = "";
        this.correo = "";
        this.nombre = "";
        this.nickname = "";
        this.password = "";
        this.rol = "";
        this.tel = "";
        this.sucursales = null;
        this.fotoPersonal = "-";
        this.fotoIFE = "-";
        this.estatus = estatus;
    }

    public Usuario(String key, String correo, String nombre, String nickname, String password, String rol, String tel, String sucursales, String fotoPersonal, String fotoIFE, String estatus) {
        this.key = key;
        this.correo = correo;
        this.nombre = nombre;
        this.nickname = nickname;
        this.password = password;
        this.rol = rol;
        this.tel = tel;
        this.sucursales = sucursales;
        this.fotoPersonal = fotoPersonal;
        this.fotoIFE = fotoIFE;
        this.estatus = estatus;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getSucursales() {
        return sucursales;
    }

    public void setSucursales(String sucursales) {
        this.sucursales = sucursales;
    }

    public String getFotoPersonal() {
        return fotoPersonal;
    }

    public void setFotoPersonal(String fotoPersonal) {
        this.fotoPersonal = fotoPersonal;
    }

    public String getFotoIFE() {
        return fotoIFE;
    }

    public void setFotoIFE(String fotoIFE) {
        this.fotoIFE = fotoIFE;
    }

    public String getEstatus() {
        return estatus;
    }

    public void setEstatus(String estatus) {
        this.estatus = estatus;
    }
}
