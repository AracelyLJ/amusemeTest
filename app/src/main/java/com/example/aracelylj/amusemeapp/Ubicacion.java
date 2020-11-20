package com.example.aracelylj.amusemeapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Ubicacion extends AppCompatActivity {

    private Context context;
    private LocationManager ubicacion;
    private ProgressDialog progressDialog;
    private String direccion;
    private static final long MIN_TIEMPO_ENTRE_UPDATES = 1000 * 60 * 1; //Minimo tiempo para updates en Milisegundos ( 1 minuto )
    private static final long MIN_CAMBIO_DISTANCIA_PARA_UPDATES = (long) 1.5; // //Minima distancia para updates en metros. ( 1.5 metros )

    public Ubicacion(Context context, LocationManager ubicacion){
        this.context = context;
        this.ubicacion = ubicacion;
        statusGPS();
    }

    public void statusGPS() {
        //ubicacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!ubicacion.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.i("GPS", "NO ACTIVADO");
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Enciende tu GPS")
                    .setPositiveButton("Entendido", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //statusGPS();
                        }
                    }).setCancelable(true);
            builder.create().show();
        } else {
            Log.i("GPS", "ACTIVADO");
            revisarPermisos();
        }
    }
    private void registrarLocalizacion() {
        //ubicacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        //ubicacion.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new miLocalizacionListener());
        ubicacion.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIEMPO_ENTRE_UPDATES, MIN_CAMBIO_DISTANCIA_PARA_UPDATES, new miLocalizacionListener());
        ubicacion.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIEMPO_ENTRE_UPDATES, MIN_CAMBIO_DISTANCIA_PARA_UPDATES, new miLocalizacionListener());
    }
    private void revisarPermisos() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Proporciona los permisos a la aplicación")
                    .setPositiveButton("Entendido", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            revisarPermisos();
                        }
                    });
            builder.create().show();
            ActivityCompat.requestPermissions((Activity) context, new String[]{ android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA }, 1);
        }else{
            registrarLocalizacion();

        }
    }
    public String getDireccion(){
        return direccion;
    }

    private class miLocalizacionListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            String str_direc = "";
            try {
                List<Address> direc = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(), 1);
                str_direc = direc.get(0).getAddressLine(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //direccion = str_direc;
            Global.direccion = str_direc;
            //Toast.makeText(context,"Te encuentras en: "+Global.direccion,Toast.LENGTH_SHORT).show(); // EEEEEEEAAAAAA
            //progressDialog.cancel();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i("Provider/Status: ", provider + "/" + String.valueOf(status));
        }

        @Override
        public void onProviderEnabled(String provider) {
            //habilitarBotones(true);
        }

        @Override
        public void onProviderDisabled(String provider) {
            //habilitarBotones(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Por favor, enciende tu GPS")
                    .setPositiveButton("Entendido", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //statusGPS();
                        }
                    }).setCancelable(false);
            builder.create().show();
        }
    }

}
