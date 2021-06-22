package com.example.aracelylj.amusemeapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.aracelylj.amusemeapp.Correo.EnviarCorreo;
import com.google.firebase.iid.Registrar;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class RegistrarDeposito extends AppCompatActivity implements View.OnClickListener {

    ImageView foto;
    EditText monto, horayfecha;
    Button registrarDeposito;

    File photoFile = null;
    Uri photoURI;
    static final int CAPTURE_IMAGE_REQUEST = 1;
    private static final String IMAGE_DIRECTORY_NAME = "AMUSEME";
    private final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;
    private String mCurrentPhotoPath;

    FirebaseData firebaseData;

    private String ubicacionImagen = "";
    private EnviarCorreo correo;
    private String porDepositar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_deposito);
        firebaseData = new FirebaseData(this);

        foto = (ImageView) findViewById(R.id.foto);
        monto = (EditText) findViewById(R.id.monto);
        horayfecha = (EditText) findViewById(R.id.horayFecha);
        registrarDeposito = (Button) findViewById(R.id.registrarDeposito);

        foto.setOnClickListener(this);
        registrarDeposito.setOnClickListener(this);
        monto.setText("");

        String ubicacion="";
        if (Global.direccion==null)ubicacion = "Ubicación desconocida";
        else ubicacion = Global.direccion;
        Global.actualizarHorayFecha();

        String hf =
                "Fecha: "+Global.formateador.format(Global.fechaDate)+
                        "\nHora: "+String.valueOf(Global.dateFormat.format(Global.date))+
                        "\n\n"+ubicacion;
        horayfecha.setText(hf);

        porDepositar = firebaseData.getDepositoUsuario(firebaseData.currentUserID);

        //Global.dialogo("Se espera un depósito de: "+porDepositar+" pesos.",RegistrarDeposito.this);


    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.foto:

                ubicacionImagen = "depositos/date_"+Global.fechaDate+"_"+UUID.randomUUID().toString();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    captureImage();
                }
                else
                {
                    captureImage2();
                }
                break;
            case R.id.registrarDeposito:
                registrarDeposito();
                // correoCalculosSemanales();
                break;
        }
    }

    /**************  CAPTURA DE FOTOS  ***********************/
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            }
        }
    }
    private void captureImage2()
    {
        try {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            photoFile = createImageFile4();
            if(photoFile!=null)
            {
                //displayMessage(getBaseContext(),photoFile.getAbsolutePath());
                Log.i("Mayank",photoFile.getAbsolutePath());
                photoURI = Uri.fromFile(photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, CAPTURE_IMAGE_REQUEST);
            }
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(),"Camera is not available.",Toast.LENGTH_LONG).show();
            Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
        }
    }
    private void captureImage()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
        else
        {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go

                try {

                    photoFile = createImageFile();
                    //displayMessage(getBaseContext(),photoFile.getAbsolutePath());
                    Log.i("Mayank",photoFile.getAbsolutePath());

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(this,
                                "com.example.aracelylj.amusemeapp.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
                    }
                } catch (Exception ex) {
                    showDialog(getApplicationContext(),ex.getMessage(),"OK");
                }


            }else
            {
                //displayMessage(getBaseContext(),"Nullll");
            }
        }
    }
    private File createImageFile4()
    {
        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_DIRECTORY_NAME);
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Toast.makeText(this, "Unable to create directory.", Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        return mediaFile;

    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /*********** REGISTRAR DEPÓSITO  **************/
    private void registrarDeposito(){
        String smonto = monto.getText().toString();
        if (smonto.equals("")){
            Global.dialogo("Es necesario registrar el monto.",RegistrarDeposito.this);
            return;
        };


        long depositoEsperado = Long.parseLong(porDepositar)-1000;
        long depositoRealizado = Long.parseLong(smonto);


        if (depositoRealizado<depositoEsperado){
            Toast.makeText(getApplicationContext(), "Se depositó menos de lo que se esperaba", Toast.LENGTH_SHORT).show();
            enviarSMS("4751073063",":::ALERTA::: \nEL DEPOSITO REALIZADO POR: "+firebaseData.getUsuarioActivo()+
                    " ES DE: "+depositoRealizado+" PESOS. \nLA CANTIDAD MINIMA DEBERIA SER: "+depositoEsperado+" PESOS.");
        }else {
            firebaseData.reiniciarDepositoUsuario(firebaseData.currentUserID);
            Toast.makeText(getApplicationContext(), "Depósito registrado con éxito", Toast.LENGTH_SHORT).show();
            enviarSMS("4751073063","El usuario: "+firebaseData.getUsuarioActivo()+" ralizó un depósito de: "+depositoRealizado+
                    "\nEl depósito esperado era de: "+depositoEsperado);
            enviarSMS("4491057920","El usuario: "+firebaseData.getUsuarioActivo()+" ralizó un depósito de: "+depositoRealizado+
                    "\nEl depósito esperado era de: "+depositoEsperado);
        }
        firebaseData.ref.child("usuarios").child(firebaseData.currentUserID).child("porDepositar").setValue("0");

        if (photoURI==null){
            Global.dialogo("Es nesario tomar fotografía al depósito (para hacerlo selecciona la cámara).",RegistrarDeposito.this);
            return;
        }
        firebaseData.put_deposito(monto.getText().toString(),ubicacionImagen);
        firebaseData.uploadImage(photoURI,ubicacionImagen);

        ThreadCorreo tc = new ThreadCorreo();
        new Thread(tc).start();

        AlertDialog.Builder builder = new AlertDialog.Builder(RegistrarDeposito.this);
        builder.setMessage("El depósito se ha registrado con éxito.")
                .setPositiveButton("Hecho", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent inicio = new Intent(RegistrarDeposito.this,MainActivity.class);
                        startActivity(inicio);
                    }
                });
        builder.create().show();


    }

    public void checkMSGPermission(){
        if (!msgPermission(Manifest.permission.SEND_SMS)){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},SEND_SMS_PERMISSION_REQUEST_CODE);
        }
    }
    public boolean msgPermission(String permission){
        int check = ContextCompat.checkSelfPermission(this,permission);
        return (check == PackageManager.PERMISSION_GRANTED);
    }
    public void enviarSMS(String number, String message){
        Log.e("INICIANDO: ","ENTRA A FUNCION");
        if (msgPermission(Manifest.permission.SEND_SMS)){

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number,null,message,null,null);
        }else{
            Toast.makeText(RegistrarDeposito.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            Log.e("ERRORCITO: ","MENSAJE ENVIADO");

        }
        Log.e("FINAL: ","FIN DE LA FUNCION");
    }



    private void showDialog(Context context, String msg, String button){
        AlertDialog.Builder builder = new AlertDialog.Builder(RegistrarDeposito.this);
        builder.setMessage(msg)
                .setPositiveButton(button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).setCancelable(false);
        builder.create().show();
    }

    public void correoDeposito(){
        if (Global.direccion == null)
            Global.direccion = "(Dirección desconocida)";
        String mensaje_str = "Le informamos que se ha realizado un depósito con el monto de: $"+monto.getText().toString()
                +" pesos. La fotografía fué guardada en el dispositivo y en la nube."
                +"<br></br>Atte: AmuseMe.";
        mensaje_str+="<br></br><br></br><br></br>Ubicación: "+Global.direccion+
                "</br><br></br></br><br></br>Fecha:  "+Global.formateador.format(Global.fechaDate)+
                "</br><br></br></br><br></br>Hora:   "+String.valueOf(Global.dateFormat.format(Global.date));
        correo = new EnviarCorreo("***DEPÓSITO*** AmuseMe Notificación","gencovending@gmail.com", mensaje_str);
        correo.enviarCorreo();

        correo = new EnviarCorreo("***DEPÓSITO*** AmuseMe Notificación","ara.lj.uaa@gmail.com", mensaje_str);
        correo.enviarCorreo();


    }
    public void correoCalculosSemanales(){
        firebaseData.getCalculosSemanales();
    }
    class ThreadCorreo implements Runnable {

        @Override
        public void run() {
            correoDeposito();
        }
    }
}


