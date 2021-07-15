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
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
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
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.iid.Registrar;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;


import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import io.grpc.okhttp.internal.Util;
import com.example.aracelylj.amusemeapp.RegistrarDeposito;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
    // private BubbleNotification bubble;
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
    private int contSem1;
    private ArrayList<HashMap<String,String>> semana1 = new ArrayList<>();
    private ArrayList<HashMap<String,String>> semana2 = new ArrayList<>();
    private HashMap<String,String> contAnteriores = new HashMap<>();
    private HashMap<String,ArrayList<String>> contadoresDeMaquinas = new HashMap<>();
    HashMap<String,HashMap<String,Integer>> restasDeContadores;
    private ArrayList<String> sucFaltantes = new ArrayList<>();

    private EnviarCorreo correo;
    private String destinatarioCorreo;

    private String msjFinalCorreo;
    private FirebaseFunctions mFunctions;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;


    /******************************** VARIABLES PARA FOTO *************************/
    File photoFile = null;
    Uri photoURI;
    static final int CAPTURE_IMAGE_REQUEST = 2;
    private static final String IMAGE_DIRECTORY_NAME = "AMUSEME";
    //private final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;
    private String mCurrentPhotoPath;
    private String ubicacionImagen = "";
    private String indicadorFoto;
    private ArrayList<String> arrayFotos;
    private ArrayList<Uri> arrayUris = new ArrayList<>();
    private ArrayList<String> arrayUbi = new ArrayList<>();
    private ArrayList<String> arrayNombres = new ArrayList<>();

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

        // bubble = new BubbleNotification(RegistrarContadores.this);
        destinatarioCorreo = "ara.lj.uaa@gmail.com";
        final_dialog = new Dialog(RegistrarContadores.this);

        //firebaseData.getDBTemp();

        //  Actualizar temporales
        Global.temp_Registradas = firebaseData.get_tempRegistradas();
        Global.temp_Faltantes = firebaseData.get_tempFaltantes();
        mFunctions = FirebaseFunctions.getInstance();

        // Comprobaciones de registros anteriores
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

        // contFotos = idNom_Contadores.size();
        arrayFotos = new ArrayList<>();

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
            String dateTime = "";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dateTime = LocalDateTime.now().toString();
            }else{
                dateTime = Global.fechaDate.toString();
            }

            SimpleDateFormat yy = new SimpleDateFormat("YY");
            SimpleDateFormat mm = new SimpleDateFormat("MM");
            SimpleDateFormat dd = new SimpleDateFormat("dd");
            Date date = new Date();
            String f = yy.format(date) + "_" + mm.format(date)  + "_" + dd.format(date);

            indicadorFoto = idNom_Contadores.get(band);

            activeTakePhoto();
            //dispatchTakePictureIntent();
        }else if (v.getId() == R.id.registrarMaquina){
            contValues = preRegistro();
            if (contValues!=null)
                registrarContadores();
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
            // Toast.makeText(getApplicationContext(), Global.maq_Registradas.toString(), Toast.LENGTH_SHORT).show();
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
            semana1 = firebaseData.get_maquinasRegistradasXsemana(semAnterior,aliasMaqActual);
            semana2 = firebaseData.get_maquinasRegistradasXsemana(Global.numSemana,aliasMaqActual);
            contadoresDeMaquinas = getKeyContadores(semana1);
            restasDeContadores = new HashMap<>();

            // OBTENER DATOS DE LAS SUCURSALES REGISTRADAS Y POR REGISTRAR
            // PARA CALCULOS Y COMPROBAR SI YA SE REGISTRÓ LA SUCURSAL
            firebaseData.get_tiposContadores(Global.tipos);
            Global.maqsxsucursal = firebaseData.get_maqsBySucursal(aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1),Global.maquinas);
            Global.sucPorReg = firebaseData.getSucPorRegistrarByUser(firebaseData.currentUserID);
            Global.sucReg = firebaseData.getSucRegistradasByUser(firebaseData.currentUserID);
            Global.arraySucReg = new ArrayList<>(Global.sucReg);

            try {
                contSem1 = Integer.parseInt(Objects.requireNonNull(semana1.get(0).get("contRegistro")));
            }catch (Exception e){
                contSem1 = 0;
            }
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
    public HashMap<String,ArrayList<String>> getKeyContadores(ArrayList<HashMap<String,String>> semana1){
        HashMap<String,ArrayList<String>> hm = new HashMap<>();
        if (semana1!=null) {
            for (HashMap<String,String> maq: semana1){
                ArrayList<String> conts = new ArrayList<>();
                for (Map.Entry<String, String> entry : maq.entrySet()) {
                    if (entry.getKey().startsWith("*"))
                        conts.add(entry.getKey());
                }
                hm.put(maq.get("alias"),conts);
            }
        }

        return hm;
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
//        if (imageBitmap!=null)
//            detectText();
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
//        if (photoURI==null){
//            Global.dialogo("Es nesario tomar fotografía al contador",RegistrarContadores.this);
//            return null;
//        }
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

        // Faltan fotos por tomar
//        if (editTexts.size()+1 != arrayFotos.size()) {
//            Global.dialogo("Es nesario tomar fotografía a todos los contadores", RegistrarContadores.this);
//            return null;
//        }

        // Si en alguna máquina se ganó algún premio
        if (saca_premio(contAnteriores,contValues)) {
            ThreadSMS tsms = new ThreadSMS("4751073063","--IMPORTANTE-- SE HAN GANADO PREMIOS EN LA MAQUINA: "+firebaseData.getTipoByAlias(aliasMaqActual)
                    + " DE LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual)+"\nREGISTRADO POR USUARIO"+firebaseData.getUsuarioActivo());
            new Thread(tsms).start();
//           tsms = new ThreadSMS("4491057920","--IMPORTANTE-- SE HAN GANADO PREMIOS EN LA MAQUINA: "+firebaseData.getTipoByAlias(aliasMaqActual)
//                    + " DE LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual)+"\nREGISTRADO POR USUARIO"+firebaseData.getUsuarioActivo());
//            new Thread(tsms).start();
//            tsms = new ThreadSMS("4492121134","--IMPORTANTE-- SE HAN GANADO PREMIOS EN LA MAQUINA: "+firebaseData.getTipoByAlias(aliasMaqActual)
//                    + " DE LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual)+"\nREGISTRADO POR USUARIO"+firebaseData.getUsuarioActivo());
//            new Thread(tsms).start();
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
    public void registrarContadores() {

        // ********** Realizar el registro
            // Enviar correo inicial
//        if (Global.temp_Registradas.isEmpty()){
//            ThreadCorreo tc = new ThreadCorreo(1);
//            new Thread(tc).start();
//        }
            // Comprobar datos y llenar
        if (Global.direccion==null) Global.direccion="Ubicacion desconocida";
        Global.actualizarHorayFecha();
        nvoRegistro = new HashMap<>();
        nvoRegistro.put("nombre",nombreMaqActual);
        nvoRegistro.put("alias",aliasMaqActual);
        nvoRegistro.put("fecha",Global.formateador.format(Global.fechaDate));
        nvoRegistro.put("hora",Global.dateFormat.format(Global.date));
        nvoRegistro.put("ubicacion",Global.direccion);
        nvoRegistro.put("semanaFiscal",Global.numSemana+"");
        nvoRegistro.put("usuario",firebaseData.currentUserID);
        nvoRegistro.put("contRegistro",String.valueOf(contSem1+1));

            // Crear contadores
        for (HashMap.Entry<String, String> entry : contValues.entrySet()) {
            nvoRegistro.put(entry.getKey(),entry.getValue());
        }


             //Update Data Base
        firebaseData.put_registroContador(nvoRegistro);
        firebaseData.put_tempRegistradas(aliasMaqActual);
        firebaseData.quitarTempFaltante(aliasMaqActual);
        // firebaseData.uploadImage(photoURI,ubicacionImagen);
        uploadImages(arrayUris, arrayUbi, arrayNombres);
        Global.temp_Registradas = firebaseData.get_tempRegistradas();
        Global.temp_Faltantes = firebaseData.get_tempFaltantes();
        int x = Global.maqsxsucursal.size()-Global.temp_Registradas.size()-1;


        if (x>1){
            //nuevaBurbuja(x);
            // mensaje = "La máquina " + tipoMaqActual + " ha sido registrada. \nSucursal: "+sucursalMaqActual+" \nQuedan " + x + " por registrar.";
            Intent intent = new Intent(RegistrarContadores.this, MainActivity.class);
            startActivity(intent);
            firebaseData.ref.child("usuarios").child(firebaseData.currentUserID).child("sucRegistradas").
                    setValue(getStringSucReg());
        }else if(x==1){
            // nuevaBurbuja(x);
            // mensaje = "La máquina " + tipoMaqActual + " ha sido registrada. \nSucursal: "+sucursalMaqActual+" \nQueda " + x + " por registrar.";
           Intent intent = new Intent(RegistrarContadores.this, MainActivity.class);
            startActivity(intent);
            firebaseData.ref.child("usuarios").child(firebaseData.currentUserID).child("sucRegistradas").
                    setValue(getStringSucReg());
        }else{
            ThreadSMS tsms = new ThreadSMS("4751073063","SE TERMINO DE REGISTRAR LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual));
            new Thread(tsms).start();
//            tsms = new ThreadSMS("4492121134","SE TERMINO DE REGISTRAR LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual));
//            new Thread(tsms).start();
//            tsms = new ThreadSMS("4491057920","SE TERMINO DE REGISTRAR LA SUCURSAL: "+firebaseData.getSucursalByAlias(aliasMaqActual));
//            new Thread(tsms).start();


            firebaseData.cleanCollection("temp_Registradas_"+firebaseData.currentUserID);
            mensajeFinal();
        }

    }
    public boolean hay_cont_menor(HashMap<String,String> contAnteriores,HashMap<String, String>contActuales){
        if (semana1.size()==0) return false;
        for (HashMap.Entry<String, String> entry : contValues.entrySet()) {
            int anterior;
            try {
                anterior = Integer.parseInt(contActuales.get(entry.getKey()));
            }catch (Exception e){
                anterior = Integer.parseInt(contAnteriores.get(entry.getKey()));
            }
//            if (Objects.equals(contAnteriores.get(entry.getKey()), "null")){
//                anterior = Integer.parseInt(contActuales.get(entry.getKey()));
//            }else{
//                anterior = Integer.parseInt(contAnteriores.get(entry.getKey()));
//            }
            int actual = Integer.parseInt(contActuales.get(entry.getKey()));
//            Toast.makeText(getApplicationContext(), entry.getKey()+ "  "+entry.getValue(), Toast.LENGTH_SHORT).show();
//            Toast.makeText(getApplicationContext(), "anterior:  "+anterior, Toast.LENGTH_SHORT).show();
//            Toast.makeText(getApplicationContext(), "actual:  "+actual, Toast.LENGTH_SHORT).show();

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
            int pAnterior;
            try {
                pAnterior = Integer.parseInt(contActuales.get("*prizes"));
            }catch (Exception e){
                pAnterior = Integer.parseInt(contAnteriores.get("*prizes"));
            }
//            if (Objects.equals(contAnteriores.get(entry.getKey()), "null")){
//                pAnterior = Integer.parseInt(contActuales.get("*prizes"));
//            }else{
//                pAnterior = Integer.parseInt(contAnteriores.get("*prizes"));
//            }
            int pActual   = Integer.parseInt(contActuales.get("*prizes"));
            if (pActual>pAnterior){
                // Toast.makeText(getApplicationContext(), "SI hubo premios", Toast.LENGTH_SHORT).show();
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
        if (semana1.isEmpty()){
            Toast.makeText(getApplicationContext(), "No hay registros de la semana anterior.", Toast.LENGTH_SHORT).show();
            ThreadCorreo tc = new ThreadCorreo(-1);
            new Thread(tc).start();
            return;
        }

        //restasDeContadores = new HashMap<>(); // Contiene todas las maquinas y las restas de sus contadores
//        HashMap<String,Integer> contadores; // Contiene el valor de cada contador
//        int resta;
//        for (int i=0; i<semana1.size(); i++){
//            HashMap<String,String> aux = semana1.get(i);
//            //Toast.makeText(getApplicationContext(), i+"   Maquina: "+aux.get("alias"), Toast.LENGTH_SHORT).show();
//            //Toast.makeText(getApplicationContext(), "Conts: "+contadoresDeMaquinas.get(aux.get("alias")), Toast.LENGTH_SHORT).show();
//            contadores = new HashMap<>();
//            for (String c: Objects.requireNonNull(contadoresDeMaquinas.get(aux.get("alias")))){
//                //Toast.makeText(getApplicationContext(), c, Toast.LENGTH_SHORT).show();
//                //Toast.makeText(getApplicationContext(), "Sem1: "+semana1.get(i).get(c), Toast.LENGTH_SHORT).show();
//                //Toast.makeText(getApplicationContext(), "Sem2: "+semana2.get(i).get(c), Toast.LENGTH_SHORT).show();
//                if (semana1.size() == semana2.size()){
//                    resta = Integer.parseInt(Objects.requireNonNull(semana2.get(i).get(c))) - Integer.parseInt(Objects.requireNonNull(semana1.get(i).get(c)));
//                    contadores.put(c,resta);
//                }else {
//                    contadores.put(c,0);
//                }
//
//            }
//            restasDeContadores.put(aux.get("alias"),contadores);
//        }

        // Hacer multiplicaciones y divisiones
//        HashMap<String,HashMap<String,Integer>> productoFinal = new HashMap<>(); // Contiene todas las maquinas y las restas de sus contadores multiplicados y divididos
//        String cve = "";
//        for ( Map.Entry<String,HashMap<String,Integer>> entry:  restasDeContadores.entrySet()){
//            cve = entry.getKey().charAt(2)+""+entry.getKey().charAt(3)+"";
//            //Global.tiposycontadores.get(cve) // -> Hashmap cve y los contadores y sus multiplicadores y sus divisores
//            for(int i=0; i<Global.tiposycontadores.get(cve).size();i++){
//                String nomCont = Global.tiposycontadores.get(cve).get(i).get("contador");
//                Integer mult = Integer.valueOf(Objects.requireNonNull(Global.tiposycontadores.get(cve).get(i).get("multiplicador")));
//                int divi = Integer.parseInt(Objects.requireNonNull(Global.tiposycontadores.get(cve).get(i).get("divisor")));
//                Integer valor = Integer.valueOf(entry.getValue().get(nomCont))*mult/divi;
//                entry.getValue().put(nomCont,valor);
//            }
//        }

        //reporte_ganancias(restasDeContadores);
        ThreadCorreo tc = new ThreadCorreo(2);
        new Thread(tc).start();

    }

    public String reporte_ganancias(HashMap<String,HashMap<String,Integer>> restasDeContadores){
        msjFinalCorreo = "Le informamos que se han registrado los contadores de la sucursal: "+ sucursalMaqActual +
                "<br></br><br></br>Ganancias por máquinas:<br></br>";
        long subTotal;
        long totalSucursal = 0;
        long deudaUsuario = Integer.parseInt(firebaseData.getDepositoUsuario(firebaseData.currentUserID));

        // Obtener valores
        for (Map.Entry<String,HashMap<String,Integer>> maquina: restasDeContadores.entrySet()){
            msjFinalCorreo+= "<br></br>"+maquina.getKey() +"<br></br>";
            subTotal = 0;
            for (Map.Entry<String,Integer> cont: maquina.getValue().entrySet()){
                msjFinalCorreo+= cont.getKey() + ":  " + cont.getValue()+"<br></br>";
                if (!cont.getKey().equals("*prizes")){
                    subTotal+=cont.getValue();
                }
            }
            msjFinalCorreo+="subTotal: $"+subTotal+".00<br></br>";
            totalSucursal+=subTotal;
        }

        msjFinalCorreo+="<br></br>Total: $"+totalSucursal+".00<br></br>";
        msjFinalCorreo+="<br></br><br></br>Total a depositar por el usuario: $"+(totalSucursal+deudaUsuario)+".00<br></br>";
        msjFinalCorreo+="<br></br><br></br>Monto anterior $"+deudaUsuario+".00<br></br>";

        msjFinalCorreo+="<br></br>Usuario: "+firebaseData.getUsuarioActivo()+
                "<br></br>Ubicación: "+Global.direccion+
                "</br><br>Fecha:  "+Global.formateador.format(Global.fechaDate)+
                "</br><br>Hora:   "+Global.dateFormat.format(Global.date)+
                "</br><br>Semana fiscal:   "+Global.numSemana+
                "<br></br><br></br>  Atte: AmuseMe.";

        firebaseData.putDepositoUsuario(firebaseData.currentUserID,totalSucursal);
        return msjFinalCorreo;
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
        realizarCalculosSemanales("ñacañaca");

        if (!sucFaltantes.isEmpty())
            finalMsg.setText("Se terminó de registrar la sucursal: "+sucursalMaqActual+" \n\n Te faltan: \n"+strSucFalt);
        else {
            finalMsg.setText("Se terminó de registrar la sucursal: " + sucursalMaqActual + " \n\n ¡TERMINASTE DE REGISTRAR TODAS LAS SUCURSALES!");
            finalMsg.setTextColor(getResources().getColor(R.color.colorRojo));
            ThreadCorreo tc = new ThreadCorreo(3);
            new Thread(tc).start();
        }
//        //boton para cerrar dialog
//        ImageView close = dialog.findViewById(R.id.imageView);
//        close.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //dineroPorPagar();
//                //dialog.dismiss();
//                //Intent intent = new Intent(RegistrarContadores.this, MainActivity.class);
//                //startActivity(intent);
//            }
//        });
        // Botón para enviar cálculos y actualizar BD
        Button doneButton = dialog.findViewById(R.id.sucDoneButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //dineroPorPagar();

                if (sucFaltantes.isEmpty()){
                    firebaseData.ref.child("usuarios").child(firebaseData.currentUserID).child("sucRegistradas").setValue("");
                    // Se mandan los cálculos

                }else{
                    firebaseData.ref.child("usuarios").child(firebaseData.currentUserID).child("sucRegistradas").
                            setValue(getStringSucReg()+aliasMaqActual.charAt(0)+""+aliasMaqActual.charAt(1)+",");
                    Intent intent = new Intent(RegistrarContadores.this, MainActivity.class);
                    startActivity(intent);
                }
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }
    public Task<String> realizarCalculosSemanales(String text) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("push", true);

        Toast.makeText(getApplicationContext(), "Calculando valores por sucursal...", Toast.LENGTH_SHORT).show();

        return mFunctions
                .getHttpsCallable("https_function")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        String result = (String) task.getResult().getData();
                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                        return result;
                    }
                });
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

//        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación", "gencovending@gmail.com", mensaje_str);
//        correo.enviarCorreo();
//
//        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación", "diazserranoricardo1@gmail.com", mensaje_str);
//        correo.enviarCorreo();

    }
    public void correosTerminar(){

        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación",destinatarioCorreo, msjFinalCorreo);
        correo.enviarCorreo();

//        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación\"","gencovending@gmail.com", msjFinalCorreo);
//        correo.enviarCorreo();
//
//        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación\"","diazserranoricardo1@gmail.com", msjFinalCorreo);
//        correo.enviarCorreo();
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

//        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación","gencovending@gmail.com", mensaje_str);
//        correo.enviarCorreo();
//
//        correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación","diazserranoricardo1@gmail.com", mensaje_str);
//        correo.enviarCorreo();
    }
    public void correosCalculosSemanales(){
        SimpleDateFormat yy = new SimpleDateFormat("YYYY");
        SimpleDateFormat mm = new SimpleDateFormat("M");
        SimpleDateFormat dd = new SimpleDateFormat("dd");
        Date date = new Date();
        String f = yy.format(date) + "_" + mm.format(date)  + "_" + "23"; //dd.format(date);
        Toast.makeText(getApplicationContext(), f, Toast.LENGTH_SHORT).show();

        HashMap<String, ArrayList<String>> calcs = firebaseData.getCalculosSemanales(f);

        String mensaje_str = "Cálculos de dinero por depositar esta semana: <br></br><br></br>";

        for(Map.Entry<String ,ArrayList<String>>entry:calcs.entrySet()){

            if (!entry.getKey().equals("TOTAL")){
                mensaje_str += entry.getKey() + "<br></br>";
                for (int i=0; i<entry.getValue().size(); i++){
                    mensaje_str += "&nbsp;&nbsp;&nbsp;&nbsp;" + entry.getValue().get(i) + "<br></br>";
                }
            }

        }
        mensaje_str += "<br></br>TOTAL: <br></br>";
        for (int i=0; i<calcs.get("TOTAL").size(); i++){
            mensaje_str += "&nbsp;&nbsp;&nbsp;&nbsp;" + calcs.get("TOTAL").get(i) + "<br></br>";
        }

        correo = new EnviarCorreo("$$$ SUCURSALES REGISTRADAS $$$ AmuseMe Notificación",
                destinatarioCorreo, mensaje_str);
        correo.enviarCorreo();

//        correo = new EnviarCorreo("$$$ SUCURSALES REGISTRADAS $$$ AmuseMe Notificación","gencovending@gmail.com", mensaje_str);
//        correo.enviarCorreo();
//
//        correo = new EnviarCorreo("$$$ SUCURSALES REGISTRADAS $$$ AmuseMe Notificación","diazserranoricardo1@gmail.com", mensaje_str);
//        correo.enviarCorreo();

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
                Toast.makeText(getApplicationContext(), photoURI.toString(), Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "photoURI.toString()", Toast.LENGTH_SHORT).show();
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
                        takePictureIntent.putExtras(getIntent().getExtras());
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
                        if (!arrayFotos.contains(indicadorFoto))
                            arrayFotos.add(indicadorFoto);
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
    private void showDialog(Context context, String msg, String button){
        AlertDialog.Builder builder = new AlertDialog.Builder(RegistrarContadores.this);
        builder.setMessage(msg)
                .setPositiveButton(button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).setCancelable(false);
        builder.create().show();
    }
    private void activeTakePhoto() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 110);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                captureImage();
            }
            else
            {
                captureImage2();
            }
        }
        String dateTime = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dateTime = LocalDateTime.now().toString();
        }else{
            dateTime = Global.fechaDate.toString();
        }
        SimpleDateFormat yy = new SimpleDateFormat("YY");
        SimpleDateFormat mm = new SimpleDateFormat("MM");
        SimpleDateFormat dd = new SimpleDateFormat("dd");
        Date date = new Date();
        String f = yy.format(date) + "_" + mm.format(date)  + "_" + dd.format(date);

        indicadorFoto = idNom_Contadores.get(band);
        // uploadImage(photoURI,ubicacionImagen);
        ubicacionImagen = "contadores/"
                + sucursalMaqActual + "/" + f;
        String nombre = aliasMaqActual+"_"+indicadorFoto+"_"+dateTime+"_"+ new Random().nextInt(9999);

        arrayUris.add(photoURI);
        arrayUbi.add(ubicacionImagen);
        arrayNombres.add(nombre);

    }

    public void uploadImages(ArrayList<Uri> photoURIs, ArrayList<String> ubicaciones, ArrayList<String> nombres){
        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference;
            for (int i =0 ; i < photoURIs.size(); i++) {
                storageReference = storage.getReference(ubicaciones.get(i));
                // final StorageReference ref = storageReference.child(Objects.requireNonNull(photoURIs.get(i).getLastPathSegment()));
                final StorageReference ref = storageReference.child(nombres.get(i));
                ref.putFile(photoURIs.get(i))
                        .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            }
                        });

            }
        }catch (Exception e){
            Log.e("Error1", Objects.requireNonNull(e.getMessage()));
        }


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
            }else if (clave == 3){
                correosCalculosSemanales();
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
