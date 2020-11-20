package com.example.aracelylj.amusemeapp.ElementViews;

import java.io.Serializable;

public class ViewsSucursal implements Serializable {

    private int id;
    private String key;
    private String value;
    private int Imagen;
    private int imgEdit;
    private int imgDelete;
    /***************/


    public ViewsSucursal(int id, String key, String value, int imagen, int imgEdit, int imgDelete) {
        this.id = id;
        this.key = key;
        this.value = value;
        Imagen = imagen;
        this.imgEdit = imgEdit;
        this.imgDelete = imgDelete;
    }

    public String getkey() {
        return key;
    }

    public void setkey(String key) {
        this.key = key;
    }

    public int getImgEdit() {
        return imgEdit;
    }

    public void setImgEdit(int imgEdit) {
        this.imgEdit = imgEdit;
    }

    public int getImgDelete() {
        return imgDelete;
    }

    public void setImgDelete(int imgDelete) {
        this.imgDelete = imgDelete;
    }



    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getvalue() {
        return value;
    }

    public void setvalue(String value) {
        this.value = value;
    }

    public int getImagen() {
        return Imagen;
    }

    public void setImagen(int imagen) {
        Imagen = imagen;
    }
}
