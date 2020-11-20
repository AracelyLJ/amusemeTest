package com.example.aracelylj.amusemeapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.aracelylj.amusemeapp.Administrador.SucursalesAdmin;
import com.example.aracelylj.amusemeapp.Administrador.Usuarios;
import com.google.firebase.auth.FirebaseAuth;

public class  MainAdministrador extends AppCompatActivity implements View.OnClickListener{

    private Button btnReporte, btnMaquina, btnTipo_maquina, btnUsuario, btnSurcursal, btnCorreo, btnSalir;

    private FirebaseData firebaseData;

    String number,message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onStart();
        setContentView(R.layout.activity_main_administrador);

        Toast.makeText(this, "Entra a administrador", Toast.LENGTH_SHORT).show();

        firebaseData = new FirebaseData(this);
        firebaseData.getDBFija();
        firebaseData.getDBTemp();



    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            /*case R.id.btnNotificacion:
                Intent in = new Intent(MainAdministrador.this, SucursalesAdmin.class);
                startActivity(in);
                break;*/
            case R.id.btnsucursales:
                Intent is = new Intent(MainAdministrador.this, SucursalesAdmin.class);
                startActivity(is);
                onSend();
                //whatsapp(MainAdministrador.this);
                break;
            case R.id.btnmaquina:
                Toast.makeText(getApplicationContext(), "Antes de mandar Mensaje", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btnTipoMaquina:
                Toast.makeText(getApplicationContext(), "Antes de mandar Mensaje", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btnUsuario:
                Toast.makeText(getApplicationContext(), "Usuarios", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainAdministrador.this, Usuarios.class));
                break;
            case R.id.btnCorreo:
                Toast.makeText(getApplicationContext(), "Antes de mandar Mensaje", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btnReportes:
                Toast.makeText(getApplicationContext(), "Antes de mandar Mensaje", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btnSalir:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainAdministrador.this, LoginActivity.class));
                break;
            default:
                break;
        }

    }

    @SuppressLint("NewApi")
    public void whatsapp(Activity activity) {
        String phone = "4751073063";
        String formattedNumber = PhoneNumberUtils.formatNumber(phone);// Util.formatPhone(phone);
        Toast.makeText(this, "Enviando a: "+formattedNumber, Toast.LENGTH_SHORT).show();

        try{
            Intent sendIntent =new Intent("android.intent.action.MAIN");
            sendIntent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.Conversation"));
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT,"");
            sendIntent.putExtra("jid", formattedNumber +"@s.whatsapp.net");
            sendIntent.setPackage("com.whatsapp");
            activity.startActivity(sendIntent);
            Toast.makeText(this, "se debió enviar ", Toast.LENGTH_SHORT).show();
        }
        catch(Exception e)
        {
            Toast.makeText(activity,"Error/n"+ e.toString(),Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(this, "Fin de la función", Toast.LENGTH_SHORT).show();
    }
    public void onSend(){
        number = "4751073063";
        message = "LLEGA NOTIFICACIÓN";

        if (checkPermission(Manifest.permission.SEND_SMS)){
            SmsManager smsManager = SmsManager.getDefault();
            Toast.makeText(getApplicationContext(), "smgr generado", Toast.LENGTH_SHORT).show();
            smsManager.sendTextMessage(number,null,message,null,null);
            Toast.makeText(getApplicationContext(), "Mensaje enviado", Toast.LENGTH_SHORT).show();

        }else{
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }
    public boolean checkPermission(String permission){
        int check = ContextCompat.checkSelfPermission(this,permission);
        return (check == PackageManager.PERMISSION_GRANTED);
    }

}
