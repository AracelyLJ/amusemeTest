package com.example.aracelylj.amusemeapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.aracelylj.amusemeapp.Correo.EnviarCorreo;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.zxing.Result;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{


    private FirebaseData firebaseData;
    private ZXingScannerView vistascanner;
    //private String direccion;
    private LocationManager ubicacion;
    private Ubicacion u;
    private ProgressDialog progressDialog;

    private FirebaseFunctions mFunctions;


    private int MY_PERMISSION = 1000;

    private static final long MIN_TIEMPO_ENTRE_UPDATES = 1000 * 60 * 1; //Minimo tiempo para updates en Milisegundos ( 1 minuto )
    private static final long MIN_CAMBIO_DISTANCIA_PARA_UPDATES = (long) 1.5; // //Minima distancia para updates en metros. ( 1.5 metros )

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onStart();
        setContentView(R.layout.activity_main);
        checkDrawPermission();

        FirebaseMessaging.getInstance().subscribeToTopic("enviaratodos").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                //Toast.makeText(getApplicationContext(), "suscrito al enviar a todos.", Toast.LENGTH_SHORT).show();
            }
        });
        mFunctions = FirebaseFunctions.getInstance();

        // Variables
        //direccion = null;
        firebaseData = new FirebaseData(this);
        firebaseData.getDBFija();
        try {
            String nomUser = firebaseData.getUsuarioActivo(); /// ESTO NOSTA JALANDO
            //Toast.makeText(getApplicationContext(), "Bienvenid@ "+nomUser, Toast.LENGTH_SHORT).show();
            setTitle("Usuario: "+nomUser);
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), "User not found.", Toast.LENGTH_SHORT).show();
        }
        //Toast.makeText(getApplicationContext(), "MÁQUINAS!! global: \n"+Global.maquinas, Toast.LENGTH_SHORT).show();
        //firebaseData.getDBTemp();

        Global.tiposycontadores = firebaseData.get_tiposContadores(Global.tipos);
        Global.maq_Registradas = firebaseData.get_maquinasRegistradas();
        firebaseData.getIdRegistroByUser(firebaseData.currentUserID);

        /*if (sucRegs!= null) {
            for (int i = 0; i < sucRegs.length; i++)
                Toast.makeText(getApplicationContext(), sucRegs[i], Toast.LENGTH_SHORT).show();
        */
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if(extras!=null){

        }

    }

    private Task<String> getDataTest(String text) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("push", true);

        return mFunctions
                .getHttpsCallable("getData")
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
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.registrarVisita:
                Escanear();
                break;
            case R.id.cerrarSesionUser:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                break;
            case R.id.registrarContadores:
                EscanearyRegistrar();
                // llamaratopico();
                break;
            case R.id.registrarDeposito:
                Intent intent = new Intent(MainActivity.this,RegistrarDeposito.class);
                startActivity(intent);
                // llamarespecifico();
                break;
            default:
                break;
        }
    }


    private void llamaratopico(){
        RequestQueue myrequest = Volley.newRequestQueue(getApplicationContext());
        JSONObject json = new JSONObject();

        try{
            //String token = "cerF9wJUpXM:APA91bFcbACzbDSo7Xg3rX2jEw1_nYAMjLB6lOdKIIE9iT7ngfDsqqwUt2rDP5IynxHsQg12tNpAI3g3YyIZ0qqggGQoxRGsdVK5woEldyBXpqB-Dm1Du9nj4wOcENQWnO8umyJGzT7t";
            Toast.makeText(getApplicationContext(), "SE MANDA NOTIF A ", Toast.LENGTH_SHORT).show();
            json.put("to","/topics/"+"enviaratodos");
            JSONObject notificacion = new JSONObject();
            notificacion.put("titulo","soy un titulo");
            notificacion.put("detalle", "soy un detalle");

            json.put("data", notificacion);

            String URL = "https://fcm.googleapis.com/fcm/send";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL, json, null, null){
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> header = new HashMap<>();
                    header.put("content-type", "application/json");
                    header.put("authorization", "key=AAAA4oqpl7Y:APA91bFX936OVFETu4WooaKaXT1i-p1cmU7Uek9-5ogF-BoKMldHc9rWZwpVWLvpxN2NgKEN6H7Oe7VOky4xQO6k-m_mB1qXRG_S0i9nC8QA8NAQgSvoGCFju5PWULLcsQedQjGcdZxg");
                    return header;

                }
            };
            myrequest.add(request);



        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void llamarespecifico(){
        RequestQueue myrequest = Volley.newRequestQueue(getApplicationContext());
        JSONObject json = new JSONObject();

        try{
            String token = "cerF9wJUpXM:APA91bFcbACzbDSo7Xg3rX2jEw1_nYAMjLB6lOdKIIE9iT7ngfDsqqwUt2rDP5IynxHsQg12tNpAI3g3YyIZ0qqggGQoxRGsdVK5woEldyBXpqB-Dm1Du9nj4wOcENQWnO8umyJGzT7t";
            Toast.makeText(getApplicationContext(), "SE MANDA NOTIF A "+token, Toast.LENGTH_SHORT).show();
            json.put("to",token);
            JSONObject notificacion = new JSONObject();
            notificacion.put("titulo","soy un titulo");
            notificacion.put("detalle", "soy un detalle");

            json.put("data", notificacion);

            String URL = "https://fcm.googleapis.com/fcm/send";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL, json, null, null){
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> header = new HashMap<>();
                    header.put("content-type", "application/json");
                    header.put("authorization", "key=AAAA4oqpl7Y:APA91bFX936OVFETu4WooaKaXT1i-p1cmU7Uek9-5ogF-BoKMldHc9rWZwpVWLvpxN2NgKEN6H7Oe7VOky4xQO6k-m_mB1qXRG_S0i9nC8QA8NAQgSvoGCFju5PWULLcsQedQjGcdZxg");
                    return header;

                }
            };
            myrequest.add(request);



        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    /************************ PRESIONA ATRÁS EN LAS LOS LECTORES QR *******************/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // Esto es lo que hace mi botón al pulsar ir a atrás
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /******************** FUNCIONES GENERALES ****************/
    public void checkDrawPermission(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        }

    }

    public void habilitarBotones(boolean band){
        findViewById(R.id.registrarVisita).setEnabled(band);
        findViewById(R.id.registrarContadores).setEnabled(band);
        findViewById(R.id.registrarDeposito).setEnabled(band);
    }

    /******************** REGISTRAR VISITA   ****************/
    public void Escanear(){
        //ArrayList<HashMap<String,String>> maquinas =  firebaseData.get_maquinas(); //firebaseData.maquinas();

        //Toast.makeText(this, maquinas.toString(), Toast.LENGTH_SHORT).show();
        vistascanner= new ZXingScannerView(this);
        vistascanner.setResultHandler(new Zxingscanner("visit",Global.maquinas));
        setContentView(vistascanner);
        vistascanner.startCamera();
    }

    /******************** REGISTRAR CONTADORES **************/
    public void EscanearyRegistrar(){

        //HashMap<String,String> sucursales =  firebaseData.get_sucursales(); //sucursales();

        /*
        Toast.makeText(getApplicationContext(), "Tipo by Alias: "+firebaseData.getTipoByAlias("LFKM01"), Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "Tipo by Nombre: "+firebaseData.getTipoByNombre("1HIAWKKD"), Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "Sucursal by Alias: "+firebaseData.getSucursalByAlias("LFKM01"), Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "Sucursal by nombre: "+firebaseData.getSucursalByNombre("1HIAWKKD"), Toast.LENGTH_SHORT).show();
        */
        vistascanner= new ZXingScannerView(this);
        vistascanner.setResultHandler(new Zxingscanner("counters",Global.maquinas));
        setContentView(vistascanner);
        vistascanner.startCamera();
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
        //direccion = u.getDireccion();
        //Global.direccion = u.getDireccion();
    }

    /*********************** PRIVATE CLASSES ******************************/
    class Zxingscanner implements ZXingScannerView.ResultHandler{

        private String tipo;
        private ArrayList<HashMap<String,String>> maquinas;

        public Zxingscanner(String tipo, ArrayList<HashMap<String,String>> maquinas){
            this.tipo = tipo;
            this.maquinas = maquinas;
        }

        @Override
        public void handleResult(Result result) {
            String maquina = result.getText();
            setContentView(R.layout.activity_main);
            if (tipo.equals("visit")){
                registrarVisitaBD(maquina);
            }else if(tipo.equals("counters")){
                registrarContadores(maquina);
            }
        }
        public void registrarVisitaBD(String maquina){

            ArrayList<String> nom_maquinas = getMaqList();
            if(nom_maquinas.contains(maquina)){

                //lugar
                if (Global.direccion==null)Global.direccion="Ubicacion desconocida";
                Global.actualizarHorayFecha();

                Map<String,String> visitada = new HashMap<>();
                visitada.put("nombre",maquina);
                visitada.put("fecha",Global.formateador.format(Global.fechaDate));
                visitada.put("hora",Global.dateFormat.format(Global.date));
                visitada.put("ubicacion",Global.direccion);
                visitada.put("semanaFiscal",Global.numSemana+"");
                visitada.put("usuario",firebaseData.currentUserID);

                firebaseData.put_visitada(visitada);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Se ha registrado la visita a la máquina: " + firebaseData.getTipoByNombre(maquina)+
                        "\n\nEn sucursal: "+firebaseData.getSucursalByNombre(maquina))
                        .setPositiveButton("Hecho", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                builder.create().show();

                return;
            }else{
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Error: No existe máquina registrada con el QR: " + maquina+" por favor, revisa que el código sea correcto.")
                        .setPositiveButton("Hecho", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                builder.create().show();
            }
        }

        public String getContadores(ArrayList<HashMap<String,String>> tipos, HashMap<String,String> maqActual)
        {
            String tipoMaqActual = maqActual.get("alias").charAt(2)+""+maqActual.get("alias").charAt(3);
            for (HashMap<String,String> tipo : tipos){
                if(tipo.get("clave").equals(tipoMaqActual)){
                    return tipo.get("contadores");
                }
            }
            return "contNull";
        }
        public void registrarContadores(String maquina)
        {

            ArrayList<HashMap<String,String>> tipos =  Global.tipos;//firebaseData.get_tipos();

            ArrayList<String> nom_maquinas = getMaqList();
            HashMap<String,String> maqActual = new HashMap<>();

            if(nom_maquinas.contains(maquina)) {
                for (int i=0; i<maquinas.size(); i++){
                    if (maquinas.get(i).get("nombre").equals(maquina)){
                        maqActual = maquinas.get(i);
                    }
                }
                try { // Comprueba que la máquina exista en la BD
                    maqActual.get("alias");
                }catch (Exception e){
                    Toast.makeText(MainActivity.this, "No se encontró la máquina", Toast.LENGTH_SHORT).show();
                }

                String contadores = getContadores(tipos,maqActual);
                if (contadores.equals("contNull")) {
                    Global.dialogo("Error: No es posible registrar la máquina. No se encontró información de los contadores.", getApplicationContext());
                    return;
                }

                Intent intent = new Intent(MainActivity.this, RegistrarContadores.class);
                intent.putExtra("NOMBRE",maqActual.get("nombre"));
                intent.putExtra("ALIAS",maqActual.get("alias"));
                intent.putExtra("TIPO",firebaseData.getTipoByAlias(maqActual.get("alias")));
                intent.putExtra("SUCURSAL",firebaseData.getSucursalByAlias(maqActual.get("alias")));
                intent.putExtra("CONTADORES",contadores);

                startActivity(intent);
            }else{
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Error: No estás conectado a internet o no existe máquina registrada con el QR: " + maquina+" por favor, revisa que el código sea correcto.")
                        .setPositiveButton("Hecho", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                builder.create().show();
            }
        }
        public ArrayList<String> getMaqList(){
            ArrayList<String> nomMaquinas = new ArrayList<>();
            for (int i=0; i<maquinas.size(); i++){
                nomMaquinas.add(maquinas.get(i).get("nombre"));
            }
            return nomMaquinas;
        }
    }

    public void sendCalculos(){
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

        Global.dialogo(mensaje_str, MainActivity.this);

        EnviarCorreo correo = new EnviarCorreo("***REGISTRO*** AmuseMe Notificación",
                "aracelycat11@hotmail.com", mensaje_str);
        correo.enviarCorreo();

    }

    class ThreadCorreo implements Runnable {

        private int clave ;

        ThreadCorreo(int clave){
            this.clave = clave;
        }

        @Override
        public void run() {
            if (clave==1){
                sendCalculos();
            }
        }
    }

}
