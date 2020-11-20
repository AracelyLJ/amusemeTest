package com.example.aracelylj.amusemeapp.Administrador;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.aracelylj.amusemeapp.FirebaseData;
import com.example.aracelylj.amusemeapp.R;

import java.util.HashMap;

public class Usuarios extends AppCompatActivity {


    private FirebaseData firebaseData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        firebaseData = new FirebaseData(this);
        HashMap<String,String> usrs = firebaseData.get_usuarios();

        //Toast.makeText(getApplicationContext(), "USUARIOS: \n"+usrs.toString(), Toast.LENGTH_SHORT).show(); Error aqui

    }
}