package com.example.aracelylj.amusemeapp.Modelos;

import java.io.Serializable;

public class Sucursal implements Serializable {

    private String key;
    private String value;

    public Sucursal(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getkey() {
        return key;
    }

    public void setkey(String key) {
        this.key = key;
    }

    public String getvalue() {
        return value;
    }

    public void setvalue(String value) {
        this.value = value;
    }

}
