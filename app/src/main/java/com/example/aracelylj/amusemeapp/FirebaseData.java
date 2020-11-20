package com.example.aracelylj.amusemeapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.aracelylj.amusemeapp.Modelos.Maquina;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseData extends AppCompatActivity {

    public Firebase ref;
    public DataSnapshot dataSnapshot_;
    public FirebaseAuth mAuth;
    public FirebaseUser currentUser;
    public String currentUserID;
    public ArrayList<HashMap<String,String>> maquinas;
    ArrayList<HashMap<String,String>> regXsemana;

    public ArrayList<HashMap<String,String>> tipos;
    public HashMap<String,String> sucursales;
    public HashMap<String,String> usuarios;
    public ArrayList<String> maquinasRegistradas, maquinasFaltantes;

    public FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Context context;
    public ProgressDialog progressDialog;
    public String auxAlias = "";

    // Storage Firebase
    public FirebaseStorage storage;
    public StorageReference storageReference;


    public FirebaseData(final Context context){
        this.context = context;
        Firebase.setAndroidContext(context);
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.mAuth = FirebaseAuth.getInstance();
        if(this.mAuth.getCurrentUser()!=null)
            this.currentUserID = mAuth.getCurrentUser().getUid();
        this.ref = new Firebase("https://amusebd-8797e.firebaseio.com/");

        this.ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                dataSnapshot_ = dataSnapshot;
                Global._dataSnapshot = dataSnapshot_;
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


        maquinas = new ArrayList<>();
        sucursales = new HashMap<>();
        tipos = new ArrayList<>();

        maquinasRegistradas = new ArrayList<>();
        maquinasFaltantes = new ArrayList<>();

        regXsemana = new ArrayList<>();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    /********************** FIREBASE CLOUD FIRESTORE ************/

    public void show_progressDialog(String text){
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(text);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    public void getDBFija(){
        ArrayList<HashMap<String,String>> maquinas =  this.get_maquinas();// firebaseData.maquinas();
        Global.maquinas = maquinas;
        Global.sucursales = this.get_sucursales();
        Global.tipos = this.get_tipos();
    }
    public void getDBTemp(){
        Global.temp_Registradas = this.get_tempRegistradas();
        Global.temp_Faltantes = this.get_tempFaltantes();
        //Global.maq_Registradas = this.get_maquinasRegistradas(Global.numSemana);
        Global.tiposycontadores = this.get_tiposContadores(Global.tipos);
    }

    public ArrayList<HashMap<String,String>> get_maquinas(){

        //aqui hacer truco como en realtime firebase para obtener las maquinas, sucursales, y tipos
        //o buscar otra forma sin el complete listener o algo así
        //show_progressDialog("Obteniendo maquinas...");
        db.collection("maquinas")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                HashMap<String,String> maquina = new HashMap<>();
                                maquina.put("nombre",document.get("nombre").toString());
                                maquina.put("alias",document.get("alias").toString());
                                maquina.put("imagen",document.get("imagen").toString());
                                maquina.put("observaciones",document.get("observaciones").toString());
                                maquina.put("renta",document.get("renta").toString());
                                maquinas.add(maquina);
                            }
                            Global.maquinas = maquinas;
                            //Toast.makeText(context, "MÁQUINAS!!: \n"+Global.maquinas, Toast.LENGTH_SHORT).show();
                            //progressDialog.cancel();
                        } else {
                            Toast.makeText(context, "Error:  "+ "Error getting documents."+ task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        return maquinas;
    }
    public HashMap<String,String> get_sucursales(){
        db.collection("sucursales")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            show_progressDialog("Obteniendo sucursales...");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //Toast.makeText(context, document.get("key").toString()+"  "+document.get("value").toString(), Toast.LENGTH_SHORT).show();
                                sucursales.put(document.get("key").toString(),document.get("value").toString());
                            }
                            progressDialog.cancel();
                        } else {
                            Log.w("TAG No hecho", "Error getting documents.", task.getException());
                            Toast.makeText(context, "TAG No hecho "+ "Error getting documents."+ task.getException(), Toast.LENGTH_SHORT).show();

                        }
                    }
                });
        return sucursales;
    }
    public HashMap<String, String> get_usuarios(){
        db.collection("usuarios")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            show_progressDialog("Obteniendo sucursales...");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //Toast.makeText(context, document.get("key").toString()+"  "+document.get("value").toString(), Toast.LENGTH_SHORT).show();
                                usuarios.put(document.get("key").toString(),document.get("value").toString());
                            }
                            progressDialog.cancel();
                        } else {
                            Log.w("TAG No hecho", "Error getting documents.", task.getException());
                            Toast.makeText(context, "TAG No hecho "+ "Error getting documents."+ task.getException(), Toast.LENGTH_SHORT).show();

                        }
                    }
                });
        return usuarios;
    }


    public ArrayList<HashMap<String,String>> get_tipos(){
        db.collection("tipoMaquina")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            show_progressDialog("Obteniendo tipos...");
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                HashMap<String,String> tipo = new HashMap<>();
                                tipo.put("clave",document.get("clave").toString());
                                tipo.put("nombre",document.get("nombre").toString());
                                tipo.put("contadores",document.get("contadores").toString());
                                tipo.put("observaciones",document.get("observaciones").toString());
                                //Toast.makeText(context, document.getId() + " => " + document.getData(), Toast.LENGTH_SHORT).show();
                                tipos.add(tipo);
                            }
                            progressDialog.cancel();
                        } else {
                            Toast.makeText(context, "Error:  "+ "Error getting documents."+ task.getException(), Toast.LENGTH_SHORT).show();

                        }
                    }
                });
        return tipos;
    }
    public HashMap<String,ArrayList<HashMap<String,String>>> get_tiposContadores(ArrayList<HashMap<String,String>> tipos){
        HashMap<String,ArrayList<HashMap<String,String>>> tiposycontaodres = new HashMap<>();

        for (HashMap<String,String> hmtipo: tipos){

            String contadores[] =  hmtipo.get("contadores").split(",");

            //Toast.makeText(context, "Clave -> "+hmtipo.get("clave"), Toast.LENGTH_SHORT).show();

            ArrayList<HashMap<String,String>> arrayContadores = new ArrayList<>();
            int i = 0;
            do{
                HashMap<String,String> hmContadores = new HashMap<>();
                hmContadores.put("contador",contadores[i]);
                hmContadores.put("multiplicador",contadores[i+1]);
                hmContadores.put("divisor",contadores[i+2]);
                i+=3;
                arrayContadores.add(hmContadores);
            }while (i<contadores.length);

            //Toast.makeText(context, "Clave -> "+hmtipo.get("clave")+"\nhmContadores -> "+arrayContadores, Toast.LENGTH_SHORT).show();
            tiposycontaodres.put(hmtipo.get("clave"),arrayContadores);



        }
        Global.tiposycontadores = tiposycontaodres;
        return tiposycontaodres;
    }
    public ArrayList<String> get_tempRegistradas(){
        maquinasRegistradas = new ArrayList<>();
        for (DataSnapshot ds: Global._dataSnapshot.child("temp_Registradas_"+currentUserID).getChildren()){
            maquinasRegistradas.add(ds.getKey());
        }
        /*this.ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                dataSnapshot_ = dataSnapshot;
                Global._dataSnapshot = dataSnapshot_;
                //verificarDatos();
                for(DataSnapshot ds: dataSnapshot_.getChildren()){
                    if(ds.getKey().equals("temp_Registradas_"+currentUserID)){
                        for (DataSnapshot ds1: ds.getChildren()){
                            //maqFaltantes.add(ds1.getKey());
                            maquinasRegistradas.add(ds1.getKey());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });*/
        /*db.collection("tempRegistradas")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            show_progressDialog("Obteniendo maquinas registradas...");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                maquinasRegistradas.add(document.get("alias").toString());
                                Toast.makeText(context, "REG -> "+document.get("alias").toString(), Toast.LENGTH_SHORT).show();
                            }
                            progressDialog.cancel();
                        } else {
                            Toast.makeText(context, "Error:  "+ "Error getting documents."+ task.getException(), Toast.LENGTH_SHORT).show();

                        }
                    }
                });*/
        return maquinasRegistradas;
    }
    public ArrayList<String> get_tempFaltantes(){

        maquinasFaltantes = new ArrayList<>();
        for (DataSnapshot ds: Global._dataSnapshot.child("temp_Faltantes_"+currentUserID).getChildren()){
            maquinasFaltantes.add(ds.getKey());
        }

        /*this.ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                dataSnapshot_ = dataSnapshot;
                //verificarDatos();
                for(DataSnapshot ds: dataSnapshot_.getChildren()){
                    if(ds.getKey().equals("temp_Faltantes_"+currentUserID)){
                        for (DataSnapshot ds1: ds.getChildren()){
                            //maqFaltantes.add(ds1.getKey());
                            maquinasFaltantes.add(ds1.getKey());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });*/
        //Toast.makeText(context, "size maqf -> "+maquinasFaltantes.size(), Toast.LENGTH_SHORT).show();
        return maquinasFaltantes;
    }
    public ArrayList<HashMap<String,String>> get_maquinasRegistradas(){
        ArrayList<HashMap<String,String>> maqRegs = new ArrayList<>();
        DataSnapshot dataSnapshot = Global._dataSnapshot.child("maquinasRegistradas");
        for (DataSnapshot ds: dataSnapshot.getChildren()){
            HashMap<String,String> hm = new HashMap<>();
            for (DataSnapshot ds1: ds.getChildren()){
                hm.put(ds1.getKey(),ds1.getValue().toString());
            }
            maqRegs.add(hm);
            Global.maq_Registradas = maqRegs;
        }
        return maqRegs;

    }
    public ArrayList<HashMap<String,String>> get_maquinasRegistradasXsemana(int numSemana, String sucursal, ArrayList<HashMap<String,String>> maquinasRegistradas){
        String cveSuc = sucursal.charAt(0)+""+sucursal.charAt(1);
        ArrayList<HashMap<String,String>> maquinasXsemana = new ArrayList<>();
        for (HashMap<String, String> maquina: maquinasRegistradas){
            String cveAux = maquina.get("alias").charAt(0)+""+maquina.get("alias").charAt(1);
            if (cveAux.equals(cveSuc) && maquina.get("semanaFiscal").equals(numSemana+"") && maquina.get("usuario").equals(currentUserID)){
                maquinasXsemana.add(maquina);
            }
        }

        //Global.dialogo(maquinasXsemana.toString(),context);
        return maquinasXsemana;
    }

    public String getTipoByAlias(String alias){
        String cveTipo = alias.charAt(2)+""+alias.charAt(3);
        ArrayList<HashMap<String,String>> tipos = Global.tipos;//get_tipos();
        for (int i=0; i<tipos.size(); i++){
            if (tipos.get(i).get("clave").equals(cveTipo)) {
                return tipos.get(i).get("nombre");
            }
        }
        return "Tipo de maquina desconocido";
    }
    public String getTipoByNombre(String maquina){
        ArrayList<HashMap<String,String>> maquinas = get_maquinas();
        for (int i=0; i<maquinas.size(); i++){
            if (maquinas.get(i).get("nombre").equals(maquina)){
                return getTipoByAlias(maquinas.get(i).get("alias"));
            }
        }
        return "Tipo de maquina desconocido";
    }
    public String getSucursalByAlias(String alias){
        String cveSucursal= alias.charAt(0)+""+alias.charAt(1);
        HashMap<String,String> sucursales = Global.sucursales;//get_sucursales();
        return sucursales.get(cveSucursal);
    }
    public String getSucursalByNombre(String maquina){
        ArrayList<HashMap<String,String>> maquinas = get_maquinas();
        for (int i=0; i<maquinas.size(); i++){
            if (maquinas.get(i).get("nombre").equals(maquina)){
                return getSucursalByAlias(maquinas.get(i).get("alias"));
            }
        }
        return "Sucursal de maquina desconocido";
    }
    public String get_valoresCorreoFinal(String tipoySemana){
        //Toast.makeText(context, "Entra a valores de correo", Toast.LENGTH_SHORT).show();
        if (Global.direccion == null)
            Global.direccion = "(Dirección desconocida)";
        String valores = "";
        DataSnapshot dataSnapshot = Global._dataSnapshot.child("sucursalesRegistradas");
        for (DataSnapshot ds: dataSnapshot.getChildren() ){ // Tipo y Semana (key)
            if (ds.getKey().equals(tipoySemana)){
                //Toast.makeText(context, "Existe sucursal registrada ", Toast.LENGTH_SHORT).show();
                for (DataSnapshot ds1: ds.getChildren()){
                    if (!ds1.getKey().equals("total"))
                        valores+="<br></br><br></br>"+ds1.getKey()+": ";

                    for (DataSnapshot ds2 : ds1.getChildren()){
                        valores+="<br></br>         "+ds2.getKey()+":    $"+ds2.getValue().toString();
                    }

                }
                valores+="<br></br><br></br>TOTAL -> $"+ds.child("total").getValue().toString();
            }


        }
        valores+="<br></br><br></br><br></br>Ubicación: "+Global.direccion+
                "</br><br></br></br><br></br>Fecha:  "+Global.formateador.format(Global.fechaDate)+
                "</br><br></br></br><br></br>Hora:   "+Global.dateFormat.format(Global.date);

        return valores;
    }
    public ArrayList<String> get_maqsBySucursal(String alias, ArrayList<HashMap<String,String>> maquinas){
        String cve = alias.charAt(0)+""+alias.charAt(1);
        ArrayList<String> maqsBySuc = new ArrayList<>();
        for (HashMap<String,String> maq: maquinas){
            String aliasAux = maq.get("alias");
            if ((aliasAux.charAt(0)+""+aliasAux.charAt(1)).equals(cve)){
                maqsBySuc.add(aliasAux);
            }
        }
        //Toast.makeText(context, "MAQUINAS: "+maqsBySuc, Toast.LENGTH_SHORT).show();
        return maqsBySuc;
    }
    public int get_numSemAnterior(int semActual, String alias,ArrayList<HashMap<String,String>> maqsRegistradas){
        int numSemanaAnterior = semActual-1;
        if (numSemanaAnterior>52)
            numSemanaAnterior = 0;
        while (numSemanaAnterior>0){
            for(HashMap<String,String> maquina: maqsRegistradas){
                if (maquina.get("alias").equals(alias) && maquina.get("semanaFiscal").equals(numSemanaAnterior+"") && maquina.get("usuario").equals(currentUserID)){
                    return numSemanaAnterior;
                }
            }
            numSemanaAnterior-=1;
        }
        return -1;
    }
    public String getUsuarioActivo(){
        DataSnapshot dataSnapshot = Global._dataSnapshot.child("usuarios");
        String nombreUsuario = "";
        for (DataSnapshot ds: dataSnapshot.getChildren()){
            if (ds.getKey().equals(currentUserID)){
                nombreUsuario = ds.child("nombre").getValue()+"";
            }
        }
        return nombreUsuario;
    }
    public String getIdRegistroByUser(String idUser){
        String idRegistro = "";
        try{
            idRegistro = Global._dataSnapshot.child("usuarios").child(idUser).child("idRegistro").getValue().toString();

        }catch (Exception e){
            Toast.makeText(context, "NO HAY ID", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(context, "ID USER:  "+idUser, Toast.LENGTH_SHORT).show();
        Toast.makeText(context, "ID REGISTRO:  "+idRegistro, Toast.LENGTH_SHORT).show();
        return idRegistro;
    }
    public String[] getSucRegistradasByUser(String idUser){
        String [] sucRegs = null;
        try{
            sucRegs = Global._dataSnapshot.child("usuarios").child(idUser).child("sucRegistradas").getValue().toString().split(",");

        }catch (Exception e){
            this.updateSucRegistradas(currentUserID,"null");
            Toast.makeText(context, "NO SE HAN REGISTRADO SUCURSALES ESTA SEMANA", Toast.LENGTH_SHORT).show();
        }
        return sucRegs;
    }
    public String getDepositoUsuario(String id){
        return Global._dataSnapshot.child("usuarios").child(id).child("porDepositar").getValue().toString();
    }


    public void put_visitada(Map<String,String> visitada){
        this.ref.child("maquinasVisitadas").push().setValue(visitada);
        /*db.collection("maquinaVisitada")
                .add(visitada)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("TAG Listo", "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG Listo", "Error adding document", e);
                        Toast.makeText(context, "Error agregando maquina visitada.", Toast.LENGTH_SHORT).show();
                    }
                });*/
    }
    public void put_registroContador(Map<String,String> registroContador){
        this.ref.child("maquinasRegistradas").push().setValue(registroContador);
        /*db.collection("maquinasRegistradas")
                .add(registroContador)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("TAG Listo", "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG Listo", "Error adding document", e);
                        Toast.makeText(context, "Error agregando maquina visitada.", Toast.LENGTH_SHORT).show();
                    }
                });*/
    }
    public void put_tempRegistradas(String maqAlias){
        this.ref.child("temp_Registradas_"+currentUserID).child(maqAlias).setValue(maqAlias);
        /*HashMap<String,String> hm= new HashMap<>();
        hm.put("alias",maqAlias);
        db.collection("tempRegistradas")
                .add(hm)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("TAG Listo", "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG Listo", "Error adding document", e);
                        Toast.makeText(context, "Error agregando maquina.", Toast.LENGTH_SHORT).show();
                    }
                });*/
    }
    public void put_tempFaltantes(String maqAlias){
        this.ref.child("temp_Faltantes_"+currentUserID).child(maqAlias).setValue(maqAlias);
        /*HashMap<String,String> hm= new HashMap<>();
        hm.put("alias",maqAlias);
        db.collection("tempFaltantes")
                .add(hm)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("TAG Listo", "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG Listo", "Error adding document", e);
                        Toast.makeText(context, "Error agregando maquina.", Toast.LENGTH_SHORT).show();
                    }
                });*/
    }
    public void put_sucursalRegistrada(ArrayList<HashMap<String,String>> sucursalRegistrada){
        //Toast.makeText(context, "Entra a función.", Toast.LENGTH_SHORT).show();
        String sucursal = sucursalRegistrada.get(0).get("alias");
        String clave = sucursal.charAt(0)+""+sucursal.charAt(1);
        String mensaje = "";
        long totalSucursal = 0;
        for (HashMap<String,String> sr : sucursalRegistrada){
            long totalMaquina = 0;
            //Toast.makeText(context, "Alias: "+sr.get("alias"), Toast.LENGTH_SHORT).show();
            Firebase datos = ref.child("sucursalesRegistradas").child(clave + Global.numSemana).child(""+sr.get("alias"));

            for (Map.Entry <String,String>entry:sr.entrySet()){
                if (entry.getKey().charAt(0)=='*'){
                    //Toast.makeText(context, "Registra "+entry.getKey(), Toast.LENGTH_SHORT).show();
                    datos.child(entry.getKey()).setValue(entry.getValue());
                    if (!entry.getKey().equals("*prizes")) totalMaquina+= Long.parseLong(entry.getValue());
                }
            }
            totalSucursal+=totalMaquina;
            datos.child("subTotal").setValue(totalMaquina+"");


        }
        ref.child("sucursalesRegistradas").child(clave + Global.numSemana).child("total").setValue(totalSucursal+"");
        putDepositoUsuario(currentUserID,totalSucursal);
        //Toast.makeText(context, "La cantidad total a depositar es: "+totalSucursal, Toast.LENGTH_SHORT).show();

    }
    public void put_deposito(String monto, String ubicacionFoto){
        Firebase deposito = ref.child("depositos").push();
        deposito.child("fecha").setValue(Global.formateador.format(Global.fechaDate));
        deposito.child("hora").setValue(String.valueOf(Global.dateFormat.format(Global.date)));
        deposito.child("ubicacion").setValue(Global.direccion);
        deposito.child("usuario").setValue(currentUserID);
        deposito.child("semanaFiscal").setValue(Global.numSemana+"");
        deposito.child("monto").setValue(monto);
        deposito.child("foto").setValue(ubicacionFoto);
    }
    public void reiniciarDepositoUsuario(String id){
        ref.child("usuarios").child(id).child("porDepositar").setValue("0");
    }
    public void putDepositoUsuario(String id, long total){

        String cant = Global._dataSnapshot.child("usuarios").child(id).child("porDepositar").getValue().toString();
        if (cant.equals("")) cant = "0";
        int porDep = Integer.parseInt(cant);
        porDep+=total;

        //Toast.makeText(context, "Cantidad total acumulada: "+porDep, Toast.LENGTH_SHORT).show();

        ref.child("usuarios").child(id).child("porDepositar").setValue(porDep+"");
    }
    public void updateSucRegistradas(String id, String sucRegistradas){
        ref.child("usuarios").child(id).child("sucRegistradas").setValue(sucRegistradas);
    }

    public void quitarTempFaltante(String alias){
        this.ref.child("temp_Faltantes_"+currentUserID).child(alias).removeValue();
        /*Global.temp_Faltantes.remove(alias);
        auxAlias = alias;
        db.collection("tempFaltantes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            show_progressDialog("Obteniendo maquinas faltantes...");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.get("alias").toString().equals(auxAlias)){

                                    delete("tempFaltantes",document.getId());
                                }
                            }
                            progressDialog.cancel();
                        } else {
                            Toast.makeText(context, "Error:  "+ "Error getting documents."+ task.getException(), Toast.LENGTH_SHORT).show();

                        }
                    }
                });*/
    }
    public void quitarTempRegistrada(String alias){
        this.ref.child("temp_Registradas"+currentUserID).child(alias).removeValue();
        /*Global.temp_Registradas.remove(alias);
        auxAlias = alias;
        db.collection("tempRegistradas")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            show_progressDialog("Obteniendo maquinas faltantes...");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.get("alias").toString().equals(auxAlias)){
                                    Toast.makeText(context, "Eliminar: "+auxAlias, Toast.LENGTH_SHORT).show();
                                    delete("tempRegistradas",document.getId());
                                }
                            }
                            progressDialog.cancel();
                        } else {
                            Toast.makeText(context, "Error:  "+ "Error getting documents."+ task.getException(), Toast.LENGTH_SHORT).show();

                        }
                    }
                });*/
    }
    public void cleanCollection(final String collection){
        this.ref.child(collection).removeValue();
    }
    public void borrarSemana(ArrayList<HashMap<String,String>> semana){
        ArrayList<HashMap<String,String>> maqRegs = new ArrayList<>();
        DataSnapshot dataSnapshot = Global._dataSnapshot.child("maquinasRegistradas");
        for (int i=0; i<semana.size(); i++){
            for (DataSnapshot ds: dataSnapshot.getChildren()){
                if (semana.get(i).get("semanaFiscal").equals(ds.child("semanaFiscal").getValue()) &&
                        semana.get(i).get("alias").equals(ds.child("alias").getValue())){
                    this.ref.child(ds.getKey()).removeValue();
                    /*Toast.makeText(context, "Borrar: "+ds.getKey() + "\nSEMANA = "+semana.get(i).get("semanaFiscal")
                                    +"\nFirebase sem = "+ds.child("alias").getValue() , Toast.LENGTH_SHORT).show();*/
                }
            }
        }
    }

    public void uploadImage(Uri photoURI, String ubicacionImagen){
        if (photoURI != null){
            //final ProgressDialog progressDialog = new ProgressDialog(this);
            //progressDialog.setTitle("Subiendo imagen a servidor...");
            //progressDialog.show();

            StorageReference ref = storageReference.child(ubicacionImagen); //
            ref.putFile(photoURI)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //progressDialog.dismiss();
                            //Toast.makeText(RegistrarDeposito.this,"Uploaded",Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //progressDialog.dismiss();
                            Global.dialogo("No se pudo subir la imagen a la base de datos",context);
                            //Toast.makeText(RegistrarDeposito.this,"No se pudo subir la imagen a la base de datos",Toast.LENGTH_SHORT).show();
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    //double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                    //progressDialog.setMessage("Uploaded "+ (int)progress + "%");
                }
            });

        }
    }

    /*************** ALTAS, BAJAS Y CAMBIOS *********************/

    public void put_sucursal(HashMap<String,String> sucursal){

    }

    /************************* HILOS  ************************/

}