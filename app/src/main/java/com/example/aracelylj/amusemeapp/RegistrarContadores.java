package com.example.aracelylj.amusemeapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aracelylj.amusemeapp.Correo.EnviarCorreo;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.Registrar;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;


import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.grpc.okhttp.internal.Util;

public class RegistrarContadores extends AppCompatActivity implements View.OnClickListener{

    private EditText prizes;
    private ImageView camPrizes;
    private Button btn_registrar;
    private TextView txtNomMaquina,txtSemanaAnterior;

    private FirebaseData firebaseData;
    private String nombreMaqActual;
    private String aliasMaqActual;
    private String tipoMaqActual;
    private String sucursalMaqActual;
    private LocationManager ubicacion;
    private Ubicacion u;
    private Dialog final_dialog;
    //private BubbleNotification bubble;
    Dialog dialog;

    // QR Lector variables
    private int band=0;
    private Bitmap imageBitmap;
    private int maxIntentos = 0;


    private String stringContadores;
    private HashMap<Integer,String> idNom_Contadores;
    HashMap<String, String> contValues;
    private ArrayList<EditText> editTexts;
    private ArrayList<String> editTextLabels;
    ViewGroup layout;

    private ArrayList<String> maqFaltantes;
    private ArrayList<String> maqRegistradas;
    private HashMap<String, String> nvoRegistro = new HashMap<>();
    int semAnterior = 0;
    private ArrayList<HashMap<String,String>> semana1 = new ArrayList<>();
    private ArrayList<HashMap<String,String>> semana2 = new ArrayList<>();
    private HashMap<String,String> contAnteriores = new HashMap<>();
    private ArrayList<String> sucFaltantes = new ArrayList<>();

    private EnviarCorreo correo;
    private String destinatarioCorreo;
    private String reporte_ganancias;

    private String procesoPago="";

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_contadores);
        checkMSGPermission();
        firebaseData = new FirebaseData(this);

        initializeViews();
        getInitialData();
        layout = findViewById(R.id.content);
        //findAllViews(layout,band);

        //if (!semana1.isEmpty())
        //    checkSemanaAnterior(semana1.get(0).get("semanaFiscal"),String.valueOf(Global.numSemana));


        //bubble = new BubbleNotification(RegistrarContadores.this);
        destinatarioCorreo = "ara.lj.uaa@gmail.com";
        final_dialog = new Dialog(RegistrarContadores.this);

        //firebaseData.getDBTemp();

        Global.temp_Registradas = firebaseData.get_tempRegistradas();
        Global.temp_Faltantes = firebaseData.get_tempFaltantes();
        //Toast.makeText(getApplicationContext(), "Faltantes: "+Global.temp_Faltantes+" Tam: "+Global.temp_Faltantes.size(), Toast.LENGTH_SHORT).show();
        //Toast.makeText(getApplicationContext(), "Registradas: "+Global.temp_Registradas+" Tam: "+Global.temp_Registradas.size(), Toast.LENGTH_SHORT).show();


        //Toast.makeText(getApplicationContext(), "tamaño: "+Global.temp_Registradas.size(), Toast.LENGTH_SHORT).show();
        if (Global.temp_Registradas.isEmpty()){
            inicializarTemporales(Global.maquinas);
        }else if (Global.temp_Registradas.contains(aliasMaqActual)){
            irAinicioDialog("La máquina: "+firebaseData.getTipoByAlias(aliasMaqActual)+" de la sucursal "+
                    firebaseData.getSucursalByAlias(aliasMaqActual)+" ya ha sido registrada.","ENTENDIDO");
            return;
        }else if (!Global.temp_Faltantes.contains(aliasMaqActual)){
            if (Global.temp_Faltantes.isEmpty()){
                //Toast.makeText(getApplicationContext(), "Iniciando temporales", Toast.LENGTH_SHORT).show();
                inicializarTemporales(Global.maquinas);
            }else{
                irAinicioDialog("No se ha terminado de registrar la sucursal: "+firebaseData.getSucursalByAlias(Global.temp_Faltantes.get(0)),"ENTENDIDO");
                return;
            }
        }
        firebaseData.get_tiposContadores(Global.tipos);
        //Global.maq_Registradas = firebaseData.get_maquinasRegistradas();
        Global.maqsxsucursal = firebaseData.get_maqsBySucursal(aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1),Global.maquinas);

        Global.sucPorReg = firebaseData.getSucPorRegistrarByUser(firebaseData.currentUserID);
        //Toast.makeText(getApplicationContext(), Global.sucPorReg.toString(), Toast.LENGTH_SHORT).show();
        Global.sucReg = firebaseData.getSucRegistradasByUser(firebaseData.currentUserID);
        Global.arraySucReg = new ArrayList<>(Global.sucReg);
        //Toast.makeText(getApplicationContext(), Global.sucReg.toString(), Toast.LENGTH_SHORT).show();
        //Toast.makeText(getApplicationContext(), "sucporReg  "+Global.sucPorReg.size()+"   SucReg  "+Global.sucReg.size(), Toast.LENGTH_SHORT).show();

        //firebaseData.reiniciarSucursal();
        if (Global.sucReg.contains(aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1))){
            AlertDialog.Builder builder = new AlertDialog.Builder(RegistrarContadores.this);
            builder.setMessage("Esta sucursal ya ha sido registrada, ¿desea volver a registrarla desde el inicio?")
                    .setPositiveButton("SI", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            firebaseData.reiniciarSucursal();
                            Global.arraySucReg.remove(aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1));
                            Global.sucReg = Global.arraySucReg;
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent inicio = new Intent(RegistrarContadores.this, MainActivity.class);
                            startActivity(inicio);
                        }
                    })
                    .setCancelable(false);
            builder.create().show();
        }

    }

    /******************** OBTENER UBICACIÓN ***************/
    @Override
    protected void onStart() {
        super.onStart();
        ubicacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //if(direccion == null) new Ubicacion(this,ubicacion);//statusGPS();
        if(Global.direccion == null){
            u = new Ubicacion(this,ubicacion);
        }
        //direccion = u.getDireccion(); // comentar y poner var global al registrar
    }

    /**************** INITIAL FUNCTIONS   ***************************/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // Esto es lo que hace mi botón al pulsar ir a atrás
            final AlertDialog.Builder builder = new AlertDialog.Builder(RegistrarContadores.this);
            builder.setMessage("Se perderá la información que está registradndo. ¿Desea regresar?")
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Toast.makeText(getApplicationContext(), "registradas: "+Global.maq_Registradas, Toast.LENGTH_SHORT).show();
                        }
                    }).setCancelable(false)
                    .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (Global.temp_Registradas.isEmpty()){
                                //firebaseData.quitarTempFaltante(aliasMaqActual);
                                firebaseData.cleanCollection("temp_Faltantes_"+firebaseData.currentUserID);
                            }
                            Intent inicio = new Intent(RegistrarContadores.this, MainActivity.class);
                            startActivity(inicio);
                        }
                    }).setCancelable(false);
            builder.create().show();

            //Intent intent = new Intent(RegistrarContadores.this, MainActivity.class);
            //startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onClick(View v)
    {
        if (v.getId()!=R.id.registrarMaquina && v instanceof ImageView){
            band = v.getId();
            dispatchTakePictureIntent();
        }else if (v.getId() == R.id.registrarMaquina){
            contValues = preRegistro();
            if (contValues!=null)
                registrarContadores();
            /*HashMap<String,String> valores = null;
            valores = preRegistro();

            if (valores != null){
                dialogoValores();
            }*/


        }else if(v.getId() == R.id.camPrizes){
            band = R.id.camPrizes;
            dispatchTakePictureIntent();
        }

    }
    public void initializeViews()
    {
        prizes= findViewById(R.id.prizes);
        btn_registrar = findViewById(R.id.registrarMaquina);
        txtNomMaquina = findViewById(R.id.nombreMaquina);
        txtSemanaAnterior = findViewById(R.id.datosAnteriores);
        camPrizes= findViewById(R.id.camPrizes);
        prizes.setEnabled(true);
        prizes.setText("");
        camPrizes.setOnClickListener(this);
        btn_registrar.setOnClickListener(this);
    }
    public void getInitialData()
    {
        // From previous activity
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if(extras!=null){
            Global.maq_Registradas = firebaseData.get_maquinasRegistradas();
            HashMap<String,String> maq = new HashMap<>();
            stringContadores = "";
            nombreMaqActual = extras.getString("NOMBRE");
            aliasMaqActual= extras.getString("ALIAS");
            tipoMaqActual= extras.getString("TIPO");
            sucursalMaqActual= extras.getString("SUCURSAL");
            txtNomMaquina.setText(getString(R.string.str_nomMaquina,tipoMaqActual,sucursalMaqActual));
            stringContadores = extras.getString("CONTADORES");
            setCamposContadores(stringContadores);
            semAnterior = firebaseData.get_numSemAnterior(Global.numSemana, aliasMaqActual,Global.maq_Registradas);
            semana1 = firebaseData.get_maquinasRegistradasXsemana(semAnterior,aliasMaqActual,Global.maq_Registradas);
            semana2 = firebaseData.get_maquinasRegistradasXsemana(Global.numSemana,aliasMaqActual,Global.maq_Registradas);
            for (int i=0; i<semana1.size();i++){
                if (semana1.get(i).get("alias").equals(aliasMaqActual)){
                    txtSemanaAnterior.append("("+semana1.get(i).get("semanaFiscal")+")\n");
                    txtSemanaAnterior.append("\n"+semana1.get(i).get("alias")+"\n");
                    txtSemanaAnterior.append(semana1.get(i).get("fecha")+"\n");
                    for(Map.Entry<String,String>entry:semana1.get(i).entrySet()){
                        if (entry.getKey().contains("*"))
                            txtSemanaAnterior.append(entry.getKey()+" : "+entry.getValue()+"\n");
                            contAnteriores.put(entry.getKey(),entry.getValue());
                    }
                }
            }

        }


    }
    public void setCamposContadores(String contadores)
    {
        idNom_Contadores = new HashMap<>();

        String contsArray [] = contadores.split(",");
        if (contsArray.length%3 != 0) {
            Global.dialogo("Error: La informacion relacionada con los contadores es incorrecta. Favor de verificar la Base de Datos.", getApplicationContext());
            Intent intent = new Intent(RegistrarContadores.this,MainActivity.class);
            startActivity(intent);
        }

        // Dynamic counter views
        editTexts = new ArrayList<>();
        editTextLabels = new ArrayList<>();
        for (String ca : contsArray) {
            if (!isInteger(ca)) // Si el contador tiene nombre
            {
                // Layout
                ViewGroup layout;
                layout = findViewById(R.id.content);
                LayoutInflater inflater = LayoutInflater.from(this);
                int id = R.layout.campocontador;
                LinearLayout linearLayout = (LinearLayout) inflater.inflate(id, null, false);

                int _id = View.generateViewId();
                TextView textView = linearLayout.findViewById(R.id.txtViewCounter);
                EditText editText = linearLayout.findViewById(R.id.editTxtCounter);
                ImageView imageView = linearLayout.findViewById(R.id.camCounter);

                editText.setId(_id);
                editText.setEnabled(true);
                idNom_Contadores.put(_id,ca);
                imageView.setId(_id);
                imageView.setOnClickListener(this);

                textView.setText(getString(R.string.str_contador,ca.toUpperCase()));
                editTexts.add(editText);
                editTextLabels.add(ca);

                layout.addView(linearLayout);

                Global.intentos.put(ca,0);
            }
        }
        idNom_Contadores.put(R.id.camPrizes,"*prizes");
        Global.intentos.put("prizes",0);
        //Toast.makeText(getApplicationContext(), "idNom_Contadores: "+idNom_Contadores, Toast.LENGTH_SHORT).show();
    }
    public void checkSemanaAnterior(String s1, String s2){
        //firebaseData.borrarSemana(semana1);
        if (s1.equals(s2)){
            final ArrayList<HashMap<String,String>> sem1 = semana1;
            final AlertDialog.Builder builder = new AlertDialog.Builder(RegistrarContadores.this);
            builder.setMessage("Las máquinas de esta sucursal ya han sido registradas esta semana. ¿Desea reiniciar los datos?")
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent inicio = new Intent(RegistrarContadores.this, MainActivity.class);
                            startActivity(inicio);
                        }
                    }).setCancelable(false)
                    .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(RegistrarContadores.this);
                            builder.setMessage("Se borrarán todos los registros de la semana: "+Global.numSemana+" de esta sucursal. ¿Desea continuar?")
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent inicio = new Intent(RegistrarContadores.this, MainActivity.class);
                                            startActivity(inicio);
                                        }
                                    }).setCancelable(false)
                                    .setPositiveButton("SI", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            firebaseData.borrarSemana(semana1);
                                        }
                                    }).setCancelable(false);
                            builder.create().show();

                        }
                    }).setCancelable(false);
            builder.create().show();
        }
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
    /*ublic void enviarSMS(String number, String message){
        Log.e("INICIANDO: ","ENTRA A FUNCION");
        if (msgPermission(Manifest.permission.SEND_SMS)){

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number,null,message,null,null);
        }else{
            Toast.makeText(RegistrarContadores.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            Log.e("ERRORCITO: ","MENSAJE ENVIADO");

        }
        Log.e("FINAL: ","FIN DE LA FUNCION");
    }*/

    /************** LECTOR DE CARACTERES *******************/
    public void dispatchTakePictureIntent()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            String deviceName = getDeviceName().substring(0,2);
            if (deviceName.equals("LG")) {
                imageBitmap = RotateBitmap(imageBitmap, 90);
            }

        }
        if (imageBitmap!=null)
            detectText();
    }
    private String getDeviceName() // Verificar que sea LG
    {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }
    private String capitalize(String s) // ToUperCase
    {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
    private static Bitmap RotateBitmap(Bitmap source, float angle) // Rotar imagen
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    public void detectText()
    {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextDetector detector = FirebaseVision.getInstance().getVisionTextDetector();
        detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                //processTxt(firebaseVisionText);
                List<FirebaseVisionText.Block> blocks = firebaseVisionText.getBlocks();
                if (blocks.size() == 0){
                    Global.dialogo("Error al leer contador: No se pudo leer el texto.",RegistrarContadores.this);
                    actualizarIntentos(band);
                    return;
                }
                String texto = "";
                for (FirebaseVisionText.Block block : firebaseVisionText.getBlocks())
                    texto += block.getText();

                // validaciones
                texto = cambiarLetrasXnumeros(texto);
                if(!validarTexto(texto)) return;

                fillCountField(layout,band,texto);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    /***************** Validaciones para lector  **************************/
    public String cambiarLetrasXnumeros(String text)
    {
        String estado="";
        text = text.replace('O','0');
        text = text.replace('o','0');
        text = text.replace('U','0');
        text = text.replace('u','0');
        text = text.replace('l','1');
        text = text.replace('I','1');
        text = text.replace('i','1');
        text = text.replace('Z','2');
        text = text.replace('z','2');
        text = text.replace('S','5');
        text = text.replace('s','5');
        text = text.replace('G','6');
        text = text.replace('c','6');
        text = text.replace('C','6');
        text = text.replace('T','7');
        text = text.replace('t','7');
        text = text.replace('/','7');
        text = text.replace('B','8');
        text = text.replace('&','8');
        text = text.replace(" ","");
        text = text.replace(".","");
        text = text.replace("-","");

        return text;
    }
    public boolean validarTexto(String texto)
    {
        if (texto.length()<6){
            Global.dialogo("Error al leer contador: Número de caracteres menor al esperado. \nPor favor, intente de nuevo.\n\nTexto leído: "+texto,RegistrarContadores.this);
            fillCountField(layout,band,"");
            actualizarIntentos(band);
            return false;
        }
        if (!isInteger(texto)){
            Global.dialogo("Error al leer contador: El texto leído contiene letras.\nPor favor, intente de nuevo.\n\nTexto leído: "+texto,RegistrarContadores.this);
            fillCountField(layout,band,"");
            actualizarIntentos(band);
            return false;
        }
        return true;
    }
    public boolean isInteger(String numero)
    {
        for (int i = 0; i < numero.length(); i++)
            if(!Character.isDigit(numero.charAt(i)))
                return false;
        return true;
    }
    public void actualizarIntentos(int band)
    {
        String cont = idNom_Contadores.get(band);
        Integer actualCount = Global.intentos.get(cont);
        EditText editText=null;
        if (band == R.id.camPrizes){
            editText = findViewById(R.id.prizes);
        }else{
            editText = findViewById(band);
        }
        editText.setText("");
        if (actualCount>=maxIntentos){
            editText.setEnabled(true);
            /*if (band == R.id.camPrizes){
                EditText editText = findViewById(R.id.prizes);
                editText.setEnabled(true);
            }else{

                EditText editText = findViewById(band);
                editText.setEnabled(true);
            }*/
        }
        actualCount += 1;
        Global.intentos.put(cont,actualCount); //asignar el valor

    }

    /******************* Hacer registro  ***********************/
    public HashMap<String, String> preRegistro(){

        // ********** Hacer validaciones
        // Get count fields
        layout = findViewById(R.id.content);
        contValues = new HashMap<>();
        getCountFields(layout);
        // Campos vacíos
        for (EditText e: editTexts){
            if (TextUtils.isEmpty(e.getText().toString())){
                e.setError("Este campo debe ser registrado.");
                return null;
            }
        }
        if (TextUtils.isEmpty(prizes.getText().toString())){
            prizes.setError("Este campo debe ser registrado.");
            return null;
        }

        // Contadores menores
        if (hay_cont_menor(contAnteriores,contValues)) return null;

        // Si en alguna máquina se ganó algún premio
        if (saca_premio(contAnteriores,contValues)) {
            ThreadSMS tsms = new ThreadSMS("4751073063","--IMPORTANTE-- SE HAN GANADO PREMIOS EN LA MAQUINA: "+firebaseData.getTipoByAlias(aliasMaqActual)
                    + " DE LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual)+"\nREGISTRADO POR USUARIO"+firebaseData.getUsuarioActivo());
            new Thread(tsms).start();
           /*tsms = new ThreadSMS("4491057920","--IMPORTANTE-- SE HAN GANADO PREMIOS EN LA MAQUINA: "+firebaseData.getTipoByAlias(aliasMaqActual)
                    + " DE LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual)+"\nREGISTRADO POR USUARIO"+firebaseData.getUsuarioActivo());
            new Thread(tsms).start();
            tsms = new ThreadSMS("4492121134","--IMPORTANTE-- SE HAN GANADO PREMIOS EN LA MAQUINA: "+firebaseData.getTipoByAlias(aliasMaqActual)
                    + " DE LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual)+"\nREGISTRADO POR USUARIO"+firebaseData.getUsuarioActivo());
            new Thread(tsms).start();*/
        }


        return contValues;
    }
    public void dialogoValores(){
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(RegistrarContadores.this);
        final View mView = getLayoutInflater().inflate(R.layout.dialog_confirmcont, null);
        final Button confirmButton = (Button) mView.findViewById(R.id.btn_ok);
        final Button cancelButton = (Button) mView.findViewById(R.id.btn_cancel);

        final ArrayList<EditText> confirmEditTexts = new ArrayList<>();
        for(Map.Entry<Integer,String>entry:idNom_Contadores.entrySet()){
            ViewGroup layout;
            layout = mView.findViewById(R.id.contentConts);
            LayoutInflater inflater = LayoutInflater.from(this);
            int id = R.layout.campo_confirm_cont;
            LinearLayout linearLayout = (LinearLayout) inflater.inflate(id, null, false);

            EditText editText = linearLayout.findViewById(R.id.confirmEDTXT);
            editText.setId((int)entry.getKey());
            editText.setHint(entry.getValue().toUpperCase());

            confirmEditTexts.add(editText);

            layout.addView(linearLayout);
        }
        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();
        dialog.setCancelable(false);

        cancelButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                dialog.dismiss();
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String,String> valores = new HashMap<>();
                for (HashMap.Entry<String, String> entry : contValues.entrySet()) {
                    for (int i=0; i<confirmEditTexts.size(); i++){
                        valores.put(confirmEditTexts.get(i).getHint().toString().toLowerCase(),confirmEditTexts.get(i).getText().toString());
                    }
                }
                if (valoresIguales(valores)){
                    registrarContadores();
                    dialog.dismiss();
                }else{
                    Toast toast = Toast.makeText(getApplicationContext(),"Uno o mas valores no coinciden, favor de corregir", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    //Toast.makeText(getApplicationContext(), "Uno o mas valores no coinciden, favor de corregir", Toast.LENGTH_SHORT).setGravity(Gravity.CENTER,0,0).show();
                }
            }
        });
    }
    public boolean valoresIguales(HashMap<String, String> valores){

        for(Map.Entry<String,String>entry:contValues.entrySet()){
            if (!entry.getValue().equals(valores.get(entry.getKey()))){
                return false;
            }
        }
        return true;
    }
    public void registrarContadores()
    {

        // ********** Realizar el registro
            // Enviar correo inicial
        if (Global.temp_Registradas.isEmpty()){
            ThreadCorreo tc = new ThreadCorreo(1);
            new Thread(tc).start();
        }
            // Comprobar datos y llenar
        if (Global.direccion==null) Global.direccion="Ubicacion desconocida";
        Global.actualizarHorayFecha();
        nvoRegistro = new HashMap<>();
        nvoRegistro.put("nombre",nombreMaqActual);
        nvoRegistro.put("alias",aliasMaqActual);
        nvoRegistro.put("fecha",Global.formateador.format(Global.fechaDate));
        nvoRegistro.put("hora",String.valueOf(Global.dateFormat.format(Global.date)));
        nvoRegistro.put("ubicacion",Global.direccion);
        nvoRegistro.put("semanaFiscal",Global.numSemana+"");
        nvoRegistro.put("usuario",firebaseData.currentUserID);
        nvoRegistro.put("idRegistro",firebaseData.currentUserID);

            // Crear contadores
        String coinsReg="";
        for (HashMap.Entry<String, String> entry : contValues.entrySet()) {
            nvoRegistro.put(entry.getKey(),entry.getValue());
            if (entry.getKey().equals("*coins"))
                coinsReg = entry.getValue();
        }

        ThreadSMS tsms = new ThreadSMS("4751073063","MAQUINA: "+firebaseData.getTipoByAlias(aliasMaqActual)+ " REGISTRADA."+
                "\nSUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual)+" \nUSUARIO: "+firebaseData.getUsuarioActivo()+"\n VALOR CONTADOR: "+coinsReg);
        new Thread(tsms).start();
        /*tsms = new ThreadSMS("4491057920","MAQUINA: "+firebaseData.getTipoByAlias(aliasMaqActual)+ " REGISTRADA."+
                "\nSUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual)+" \nUSUARIO: "+firebaseData.getUsuarioActivo()+"\n VALOR CONTADOR: "+coinsReg);
        new Thread(tsms).start();
        tsms = new ThreadSMS("4492121134","MAQUINA: "+firebaseData.getTipoByAlias(aliasMaqActual)+ " REGISTRADA."+
                "\nSUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual)+" \nUSUARIO: "+firebaseData.getUsuarioActivo()+"\n VALOR CONTADOR: "+coinsReg);
        new Thread(tsms).start();*/
            // Update Data Base
        firebaseData.put_registroContador(nvoRegistro);
        firebaseData.put_tempRegistradas(aliasMaqActual);
        firebaseData.quitarTempFaltante(aliasMaqActual);

        Global.temp_Registradas = firebaseData.get_tempRegistradas();
        Global.temp_Faltantes = firebaseData.get_tempFaltantes();
        int x = Global.maqsxsucursal.size()-Global.temp_Registradas.size()-1;

        int numFaltantes = Global.temp_Faltantes.size()-1;
        String mensaje = "";

        if (x>1){
            nuevaBurbuja(x);
            mensaje = "La máquina " + tipoMaqActual + " ha sido registrada. \nSucursal: "+sucursalMaqActual+" \nQuedan " + x + " por registrar.";
            Intent intent = new Intent(RegistrarContadores.this, MainActivity.class);
            startActivity(intent);
            firebaseData.ref.child("usuarios").child(firebaseData.currentUserID).child("sucRegistradas").
                    setValue(getStringSucReg());
        }else if(x==1){
            nuevaBurbuja(x);
            mensaje = "La máquina " + tipoMaqActual + " ha sido registrada. \nSucursal: "+sucursalMaqActual+" \nQueda " + x + " por registrar.";
           Intent intent = new Intent(RegistrarContadores.this, MainActivity.class);
            startActivity(intent);
            firebaseData.ref.child("usuarios").child(firebaseData.currentUserID).child("sucRegistradas").
                    setValue(getStringSucReg());
        }else{
            tsms = new ThreadSMS("4751073063","SE TERMINO DE REGISTRAR LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual));
            new Thread(tsms).start();
            /*tsms = new ThreadSMS("4491057920","SE TERMINO DE REGISTRAR LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual));
            new Thread(tsms).start();
            tsms = new ThreadSMS("4492121134","SE TERMINO DE REGISTRAR LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual));
            new Thread(tsms).start();*/
            //firebaseData.ref.child("reg_"+firebaseData.currentUserID).child("usuario").setValue("Global.s");???
            firebaseData.cleanCollection("temp_Registradas_"+firebaseData.currentUserID);
            //Global.sucReg.add(aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1));
            mensajeFinal();
        }

    }
    public boolean hay_cont_menor(HashMap<String,String> contAnteriores,HashMap<String, String>contActuales){
        if (semana1.size()==0) return false;
        for (HashMap.Entry<String, String> entry : contValues.entrySet()) {
            Integer anterior = Integer.parseInt(contAnteriores.get(entry.getKey()));
            Integer actual = Integer.parseInt(contActuales.get(entry.getKey()));
            if (actual<anterior){
                Global.dialogo("El valor "+entry.getKey()+" es menor al valor registrado anteriormente. Por favor, corrige el dato.",RegistrarContadores.this);
                return true;
            }
        }
        return false;
    }
    public boolean saca_premio(HashMap<String,String> contAnteriores,HashMap<String, String>contActuales){
        if (semana1.size()==0) return false;
        for (HashMap.Entry<String, String> entry : contValues.entrySet()){
            Integer pAnterior = Integer.parseInt(contAnteriores.get("*prizes"));
            Integer pActual   = Integer.parseInt(contActuales.get("*prizes"));
            if (pActual>pAnterior){
                Toast.makeText(getApplicationContext(), "SI hubo premios", Toast.LENGTH_SHORT).show();
                return true;
            }

        }
        //Toast.makeText(getApplicationContext(), "No hubo premio", Toast.LENGTH_SHORT).show();
        return false;
    }
    public void fillCountField(ViewGroup layout,int band,String txt)
    {
        if (band!=R.id.camPrizes){
            EditText editText = findViewById(band);
            editText.setText(txt);
            for (int i = 0; i < editTexts.size(); i++) {
                View view = layout.getChildAt(i);
                if (view instanceof ViewGroup)
                    fillCountField((ViewGroup) view, band,txt);
                else if (view instanceof EditText && view.getId()==band) {
                    EditText edittext = (EditText) view;
                    edittext.setText(txt);
                }
            }
        }else{
            prizes.setText(txt);
        }
    }
    public void getCountFields(ViewGroup layout)
    {
        // Obtener valores de edittexts
        //Toast.makeText(getApplicationContext(), "Edittextssize -> "+editTexts.size(), Toast.LENGTH_SHORT).show();
        for (int i=0; i<editTextLabels.size(); i++){
            contValues.put(editTextLabels.get(i), editTexts.get(i).getText().toString());
        }
        contValues.put("*prizes",prizes.getText().toString());
        //Toast.makeText(getApplicationContext(), "ContValues -> "+contValues, Toast.LENGTH_SHORT).show();
    }
    public void inicializarTemporales(ArrayList<HashMap<String,String>> maquinas)
    {
        String aux = "";
        String cveSucursal = aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1);
        for (HashMap<String,String> m: maquinas){
            aux = m.get("alias").charAt(0)+""+m.get("alias").charAt(1);
            //Toast.makeText(getApplication(), "cve: "+cveSucursal+"\naux: "+aux, Toast.LENGTH_SHORT).show();
            if (cveSucursal.equals(aux) && !aliasMaqActual.equals(m.get("alias"))){
                firebaseData.put_tempFaltantes(m.get("alias"));
                //Global.temp_Faltantes.add(m.get("alias"));
            }
        }

        firebaseData.cleanCollection("tempRegistradas");
    }
    public void dineroPorPagar( )
    {
        String cveSucursal = aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1);
        //ArrayList<HashMap<String,String>> semana1 = new ArrayList<>();
        //ArrayList<HashMap<String,String>> semana2 = new ArrayList<>();

        int semAnterior = firebaseData.get_numSemAnterior(Global.numSemana, aliasMaqActual,Global.maq_Registradas);
        if (semAnterior!=-1){
            //semana1 = inicializada en función: getInitialData
            //Toast.makeText(getApplicationContext(), "Semana anterior: \n"+semana1, Toast.LENGTH_SHORT).show();
            semana2 = firebaseData.get_maquinasRegistradasXsemana(Global.numSemana,aliasMaqActual,Global.maq_Registradas);
            semana2.add(nvoRegistro);
        }else{
            Toast.makeText(getApplicationContext(), "No hay registros de la semana anterior.", Toast.LENGTH_SHORT).show();
            ThreadCorreo tc = new ThreadCorreo(3);
            new Thread(tc).start();
            return;
        }

        ArrayList<HashMap<String,String>> sucRegistrada = new ArrayList<>();
        for (int i=0; i<semana1.size(); i++){ // Recorrido
            for (int j=0; j<semana2.size(); j++){ // Recorrido

                if (semana1.get(i).get("alias").equals(semana2.get(j).get("alias"))){ // Encuentra mismo alias
                    procesoPago+="<br></br>\n<br></br>MAQUINA  "+semana1.get(i).get("alias")+"<br></br>\n<br></br>";
                    HashMap<String,String> hm = new HashMap<>();
                    hm.put("alias",semana1.get(i).get("alias"));
                    for(Map.Entry<String,String>entry: semana1.get(i).entrySet()){ // Recorre atributos de máquina registrada
                        if (entry.getKey().charAt(0)=='*'){
                            long numSem1, numSem2, numRestar;
                            try {
                                numSem1 = Long.parseLong(semana1.get(i).get(entry.getKey())+"");
                                numSem2 = Long.parseLong(semana2.get(i).get(entry.getKey())+"");
                                procesoPago+="numSem1: "+semana1.get(i).get("semanaFiscal")+"  numSem2: "+semana2.get(i).get("semanaFiscal")+"<br></br>\n<br></br>";
                                procesoPago+="valCont1: "+numSem1+"  valCont2: "+numSem2+"<br></br>\n<br></br>";
                                if (numSem2>numSem1)
                                    numRestar = (numSem2-numSem1);
                                else
                                    numRestar = 0;
                            }catch (Exception e){
                                //Global.dialogo("No están completas las máquinas registradaas de semanas anteriores. Se tomarán valores de contadores como 0 (cero)",RegistrarContadores.this);
                                Toast.makeText(getApplicationContext(), "No están completas las máquinas registradaas de semanas anteriores. Se tomarán valores de contadores como 0 (cero)", Toast.LENGTH_SHORT).show();
                                numSem1=0;
                                numSem2=0;
                                numRestar=0;
                            }
                            procesoPago+="numRestar: "+numRestar+"<br></br>\n<br></br>";

                            // AQUÍ MULTIPLICAR POR EL MULTIPLICADOR DEL TIPO DE MÁQUINA
                            //Toast.makeText(getApplicationContext(), "TyC desde Registrar Contadores -> "+Global.tiposycontadores, Toast.LENGTH_SHORT).show();
                            String auxTipoMaq =  semana1.get(i).get("alias").charAt(2)+""+semana1.get(i).get("alias").charAt(3);
                            procesoPago+="auxTipoMaq: "+auxTipoMaq+"<br></br>\n<br></br>";
                            ArrayList<HashMap<String,String>> arrayContadores = Global.tiposycontadores.get(auxTipoMaq);
                            //Toast.makeText(getApplicationContext(), "Alias: "+semana1.get(i).get("alias")+
                            //        "   Máquina: "+auxTipoMaq+"\nArrayContadores -> "+arrayContadores, Toast.LENGTH_SHORT).show();
                            long numMultiplicado = 1;
                            String numDividido = "";
                            for (HashMap<String,String>contador: arrayContadores){
                                if (contador.get("contador").equals(entry.getKey())){
                                    procesoPago+="Contador: "+contador.get("contador")+"<br></br>\n<br></br>";
                                    numMultiplicado = numRestar*Integer.parseInt(contador.get("multiplicador"));
                                    procesoPago+="numMultiplicado: "+numMultiplicado+"<br></br>\n<br></br>";
                                    numDividido = numMultiplicado/Integer.parseInt(contador.get("divisor"))+"";
                                    procesoPago+="***numDividido***: "+numDividido+"<br></br>\n<br></br>";
                                }else if(entry.getKey().equals("*prizes")){
                                    numDividido = String.valueOf(numRestar);
                                }
                            }

                            hm.put(entry.getKey(),numDividido);
                        }
                    }
                    //Toast.makeText(getApplicationContext(), "Se agrega: "+hm, Toast.LENGTH_SHORT).show();
                    sucRegistrada.add(hm);

                }

            }
        }
        //Toast.makeText(getApplicationContext(), "SucRegistrada: "+sucRegistrada.toString(), Toast.LENGTH_SHORT).show();

        //firebaseData.put_sucursalRegistrada(sucRegistrada);
        reporte_ganancias =reporte_ganancias(sucRegistrada);

        ThreadCorreo tc = new ThreadCorreo(2);
        new Thread(tc).start();
    }

    public String reporte_ganancias(ArrayList<HashMap<String,String>> sucursalRegistrada){
        String reporte = "";
        long totalSucursal = 0;
        long deudaUsuario = Integer.parseInt(firebaseData.getDepositoUsuario(firebaseData.currentUserID));
        for (HashMap<String,String> sr : sucursalRegistrada){
            long totalMaquina = 0;
            reporte += "<br></br>"+sr.get("alias") +"<br></br>";
            for (Map.Entry <String,String>entry:sr.entrySet()){
                if (entry.getKey().charAt(0)=='*'){
                    reporte+= entry.getKey() + ":  " + entry.getValue()+"<br></br>";
                    if (!entry.getKey().equals("*prizes")) totalMaquina+= Long.parseLong(entry.getValue());
                }
            }
            reporte+="subTotal: $"+totalMaquina+".00<br></br>";
            totalSucursal+=totalMaquina;
        }
        reporte+="<br></br>Total: $"+totalSucursal+".00<br></br>";

        reporte+="<br></br><br></br>Total a depositar por el usuario: $"+(totalSucursal+deudaUsuario)+".00<br></br>";
        reporte+="<br></br><br></br>Monto anterior $"+deudaUsuario+".00<br></br>";

        reporte+="<br></br>Usuario: "+firebaseData.getUsuarioActivo()+
                "<br></br>Ubicación: "+Global.direccion+
                "</br><br>Fecha:  "+Global.formateador.format(Global.fechaDate)+
                "</br><br>Hora:   "+Global.dateFormat.format(Global.date)+
                "</br><br>Semana fiscal:   "+Global.numSemana+
                "<br></br><br></br>  Atte: AmuseMe.";
        //Global.dialogo(reporte,RegistrarContadores.this);
        firebaseData.putDepositoUsuario(firebaseData.currentUserID,totalSucursal);
        return reporte;
    }

    /*****************  Mensajes y alertas **************************/
    public void nuevaBurbuja(int numFaltantes)
    {
        MediaPlayer mp= MediaPlayer.create(this, R.raw.notificacion);
        mp.start();
        String mensaje = "";
        if (numFaltantes>1){
            mensaje = "La máquina " + tipoMaqActual + " ha sido registrada. \nSucursal: "+sucursalMaqActual+" \nQuedan " + numFaltantes + " por registrar.";
        }else{
            mensaje = "La máquina " + tipoMaqActual + " ha sido registrada. \nSucursal: "+sucursalMaqActual+" \nQueda " + numFaltantes + " por registrar.";
        }

        //bubble.addNewBubble(numFaltantes, mensaje,"done");
    }
    private void mensajeFinal()
    {

        //dineroPorPagar();
        // Obtener sucursales faltantes
        String strSucFalt = "";
        for (int i=0; i<Global.sucPorReg.size(); i++){
            String cve = Global.sucPorReg.get(i);
            if (!Global.sucReg.contains(cve) && !Global.sucPorReg.get(i).equals(aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1))) {
                sucFaltantes.add(Global.sucPorReg.get(i));
                strSucFalt+=Global.sucursales.get(cve)+"\n";

            }
        }

        dialog = new Dialog(RegistrarContadores.this);
        //se asigna el layout
        dialog.setContentView(R.layout.cardview_message);
        // Editar texto
        TextView finalMsg = dialog.findViewById(R.id.textView2);
        if (!sucFaltantes.isEmpty())
            finalMsg.setText("Se terminó de registrar la sucursal: "+sucursalMaqActual+" \n\n Te faltan: \n"+strSucFalt);
        else {
            finalMsg.setText("Se terminó de registrar la sucursal: " + sucursalMaqActual + " \n\n ¡TERMINASTE DE REGISTRAR TODAS LAS SUCURSALES!");
            finalMsg.setTextColor(getResources().getColor(R.color.colorRojo));
        }
        //boton para cerrar dialog
        ImageView close = dialog.findViewById(R.id.imageView);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //dialog.dismiss();
                //Intent intent = new Intent(RegistrarContadores.this, MainActivity.class);
                //startActivity(intent);
            }
        });
        // Botón para enviar cálculos y actualizar BD
        Button doneButton = dialog.findViewById(R.id.sucDoneButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String auxUpdate = "";

                if (sucFaltantes.isEmpty()){
                    firebaseData.ref.child("usuarios").child(firebaseData.currentUserID).child("sucRegistradas").setValue("");
                }else{
                    // Obtener String de sucursales registradas


                    firebaseData.ref.child("usuarios").child(firebaseData.currentUserID).child("sucRegistradas").
                            setValue(getStringSucReg()+aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1)+",");

                    dialog.dismiss();
                    Intent intent = new Intent(RegistrarContadores.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        });
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }
    public String getStringSucReg(){
        StringBuffer strSR = new StringBuffer();
        for (String s : Global.sucReg){
            strSR.append(s);
            strSR.append(",");
        }
        return strSR.toString();
    }
    public void irAinicioDialog(String msg, String button){
        AlertDialog.Builder builder = new AlertDialog.Builder(RegistrarContadores.this);
        builder.setMessage(msg)
                .setPositiveButton(button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent inicio = new Intent(RegistrarContadores.this, MainActivity.class);
                        startActivity(inicio);
                    }
                }).setCancelable(false);
        builder.create().show();
    }

    public void correosIniciar(){
        if (Global.direccion == null)
            Global.direccion = "(Dirección desconocida)";
        String mensaje_str = "Le informamos que se estan registrando las maquinas de la sucursal: "+ sucursalMaqActual
                + ". Con ubicacion: "+Global.direccion+"\n  Usuario: "+firebaseData.getUsuarioActivo()+
                "\nAtte: AmuseMe.";
        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación",destinatarioCorreo, mensaje_str);
        correo.enviarCorreo();

       /* correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación", "gencovending@gmail.com", mensaje_str);
        correo.enviarCorreo();

        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación", "diazserranoricardo1@gmail.com", mensaje_str);
        correo.enviarCorreo();*/

    }
    public void correosTerminar(){
        //String destinatario_str = "ara.lj.uaa@gmail.com";
        String mensaje_str = "Le informamos que se han registrado los contadores de la sucursal: "+ sucursalMaqActual +
                "<br></br><br></br>Ganancias por máquinas:<br></br>";
        String tipoSemana = aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1)+Global.numSemana;

        //AQUI LLAMAR A FUNCIÓN DE FIREBASE PARA OBTENER LOS DATOS
        //mensaje_str += firebaseData.get_valoresCorreoFinal(tipoSemana);
        mensaje_str += reporte_ganancias;
        //Toast.makeText(getApplicationContext(), valoresCorreo, Toast.LENGTH_SHORT).show();

        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación",destinatarioCorreo, mensaje_str+"<br></br>"+procesoPago);
        correo.enviarCorreo();

        /*correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación\"","gencovending@gmail.com", mensaje_str);
        correo.enviarCorreo();

        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación\"","diazserranoricardo1@gmail.com", mensaje_str);
        correo.enviarCorreo();*/
    }
    public void correosNoSemanaAnterior(){
        if (Global.direccion == null)
            Global.direccion = "(Dirección desconocida)";
        String mensaje_str = "Le informamos que se han registrado los contadores de la sucursal: "+ sucursalMaqActual +
                "<br></br>No existen registros anteriores de máquinas de esta sucursal, por lo tanto no se realizaron los cálculos de las ganancias."+
                "<br></br>Usuario"+firebaseData.getUsuarioActivo()+
                "<br></br>Ubicación de la sucursal:  "+Global.direccion+
                "</br><br>Fecha:  "+Global.formateador.format(Global.fechaDate)+
                "</br><br>Hora:   "+Global.dateFormat.format(Global.date)
                +" <br></br>  Atte: AmuseMe.";
        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación",destinatarioCorreo, mensaje_str);
        correo.enviarCorreo();

        /*correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación","gencovending@gmail.com", mensaje_str);
        correo.enviarCorreo();

        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación","diazserranoricardo1@gmail.com", mensaje_str);
        correo.enviarCorreo();*/
    }

    class ThreadCorreo implements Runnable {

        private int clave ;

        ThreadCorreo(int clave){
            this.clave = clave;
        }

        @Override
        public void run() {
            if (clave==1){
                correosIniciar();
            }else if (clave==2){
                correosTerminar();
            }else{
                correosNoSemanaAnterior();
            }
        }
    }

    class ThreadSMS implements Runnable{
        private String numero;
        private String mensaje;

        ThreadSMS(String numero, String mensaje){
            this.numero = numero;
            this.mensaje = mensaje;
        }

        @Override
        public void run() {
            if (msgPermission(Manifest.permission.SEND_SMS)){

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(this.numero,null,this.mensaje,null,null);
            }else{
                Toast.makeText(RegistrarContadores.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
