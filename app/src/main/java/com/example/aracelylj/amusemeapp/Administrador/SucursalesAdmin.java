package com.example.aracelylj.amusemeapp.Administrador;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import com.example.aracelylj.amusemeapp.ElementViews.ViewsSucursal;
import com.example.aracelylj.amusemeapp.FirebaseData;
import com.example.aracelylj.amusemeapp.Global;
import com.example.aracelylj.amusemeapp.Modelos.Sucursal;
import com.example.aracelylj.amusemeapp.R;

import java.util.ArrayList;
import java.util.Map;

public class SucursalesAdmin extends AppCompatActivity {

    private ProgressDialog progressDialog;
    private ListView listaSucursales;
    private ArrayList<ViewsSucursal> lista;
    private ArrayList<Sucursal> datosSucursales;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sucursales_admin);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Cargando sucursales...");
        progressDialog.setCancelable(false);
        progressDialog.show();


        listaSucursales= (ListView) findViewById(R.id.listSucursales);

        lista = new ArrayList<ViewsSucursal>();
        datosSucursales= new ArrayList<Sucursal>();


        Toast.makeText(getApplicationContext(), Global.sucursales.toString(), Toast.LENGTH_SHORT).show();
        int i=0;
        for (Map.Entry<String,String>entry:Global.sucursales.entrySet()){
            datosSucursales.add(new Sucursal(entry.getKey(),entry.getValue()));
            //String key, String value, String ciudad, String observaciones
            lista.add(new ViewsSucursal(i,entry.getKey(),entry.getValue(),R.drawable.logo2_ , R.drawable.ic_edit, R.drawable.ic_delete));
            i++;
        }
        progressDialog.dismiss();





    }
}
