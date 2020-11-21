package com.example.aracelylj.amusemeapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import com.firebase.client.DataSnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Global {

    //fecha
    public static Calendar calendar = Calendar.getInstance();
    public static int numSemana = calendar.get(Calendar.WEEK_OF_YEAR);
    public static SimpleDateFormat formateador = new SimpleDateFormat("dd/MMMM/yyyy", new Locale("es_ES"));
    public static Date fechaDate = new Date();
    //hora
    public static Calendar cal = Calendar.getInstance();
    public static Date date = cal.getTime();
    public static DateFormat dateFormat = new SimpleDateFormat("HH:mma");
    // Contador de intentos de la cámara
    public static HashMap<String,Integer> intentos = new HashMap<>();
    // Contador burbujas
    public static int contBubbles=0;
    // Maquinas
    public static ArrayList<HashMap<String,String>> maquinas = new ArrayList<>();
    // Sucursales
    public static HashMap <String,String> sucursales = new HashMap<>();
    // Tipos
    public static ArrayList<HashMap<String,String>> tipos = new ArrayList<>();
    public static HashMap<String,ArrayList<HashMap<String,String>>> tiposycontadores = new HashMap<>();
    // Máquians por sucursal
    public static ArrayList<String> maqsxsucursal = new ArrayList<>();
    // Maquinas registradas
    public static ArrayList<String> temp_Registradas = new ArrayList<>();
    // Maquinas faltantes
    public static ArrayList<String> temp_Faltantes = new ArrayList<>();
    // Ubicación actual
    public static String direccion = null;
    // Máquinas Registradas
    public static ArrayList<HashMap<String,String>> maq_Registradas = new ArrayList<>();
    // Datasnapshot
    public static DataSnapshot _dataSnapshot;
    // Sucursales registradas por usuario, sucursales por registrar
    public static String sucReg;
    public static String sucPorReg;


    public static void dialogo(String texto, Context context)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(texto)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create().show();
    }
    public static void actualizarHorayFecha(){
        Global.calendar = Calendar.getInstance();
        Global.numSemana = calendar.get(Calendar.WEEK_OF_YEAR);
        Global.formateador = new SimpleDateFormat("dd/MMMM/yyyy", new Locale("es_ES"));
        Global.fechaDate = new Date();
        //hora
        Global.cal = Calendar.getInstance();
        Global.date = cal.getTime();
        Global.dateFormat = new SimpleDateFormat("HH:mma");

    }

}


