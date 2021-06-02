package com.example.aracelylj.amusemeapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.aracelylj.amusemeapp.Modelos.Usuario;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {


    private EditText emailField;
    private EditText passwordField;
    private CardView loginButton;
    private FirebaseData firebaseData;
    private ProgressDialog progressDialog;
    private ArrayList<Usuario> usuarios;
    private String nombreUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        //FirebaseAuth.getInstance().signOut();

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);

        firebaseData = new FirebaseData(this);
        usuarios = new ArrayList<Usuario>();

        //Toast.makeText(getApplicationContext(), "Holi"+firebaseData.currentUser.getEmail(), Toast.LENGTH_SHORT).show();


        if (firebaseData.currentUser != null) {
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Ingresando...");
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        // Iniciar
        Firebase.setAndroidContext(this);
        Firebase ref_usuarios = new Firebase("https://amusebd-8797e.firebaseio.com/usuarios");
        ref_usuarios.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                firebaseData.dataSnapshot_ = dataSnapshot;
                addData();
                //Revisar si ya está logueado
                //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseData.currentUser != null) {
                    buscarDatosUsuario(firebaseData.currentUser.getEmail());
                }else{
                    emailField.setVisibility(View.VISIBLE);
                    passwordField.setVisibility(View.VISIBLE);
                    loginButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLogin();
            }
        });

    }



    public void addData(){
        for(DataSnapshot ds: firebaseData.dataSnapshot_.getChildren()){
            //Log.e("ds",ds.getKey());
            if(ds.getChildrenCount() >= 11) {
                Usuario usuario = new Usuario(
                        ds.getKey(),
                        ds.child("correo").getValue().toString(),
                        ds.child("nombre").getValue().toString(),
                        ds.child("nickname").getValue().toString(),
                        ds.child("password").getValue().toString(),
                        ds.child("rol").getValue().toString(),
                        ds.child("tel").getValue().toString(),
                        ds.child("sucursales").getValue().toString(),
                        ds.child("fotoPersonal").getValue().toString(),
                        ds.child("fotoIFE").getValue().toString(),
                        ds.child("estatus").getValue().toString()
                );
                usuarios.add(usuario);
            }
        }
    }
    public void buscarDatosUsuario(String email){
        Usuario user = new Usuario();
        for(int i=0; i < usuarios.size(); i++){
            if(usuarios.get(i).getCorreo().equals(email)){
                user = usuarios.get(i);
                break;
            }
        }
        //Toast.makeText(LoginActivity.this, "El usuario es: " + user.getCorreo(), Toast.LENGTH_LONG).show();
        //Toast.makeText(LoginActivity.this, "El usuario es: " + user.getRol(), Toast.LENGTH_LONG).show();
        abrirActivity(user);
    }
    public void abrirActivity(Usuario user){
        progressDialog.dismiss();
        switch (user.getRol()){
            case "empleado":
                //Toast.makeText(getApplicationContext(), "Empleado", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("Usuario", user.getCorreo());
                intent.putExtra("Nombre",user.getNombre());
                startActivity(new Intent(intent));
                break;
            case "admin":
                Intent intent2 = new Intent(LoginActivity.this, MainAdministrador.class);
                intent2.putExtra("Usuario", user.getCorreo());
                intent2.putExtra("Password", user.getPassword());
                startActivity(new Intent(intent2));
                break;
            default:
                Toast.makeText(getApplicationContext(), "El usuario no está registrado.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void startLogin(){
        String email = emailField.getText().toString();
        String password = passwordField.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            //Toast.makeText(LoginActivity.this, "Hay campos vacíos", Toast.LENGTH_SHORT).show();
            AlertDialog.Builder builder = new android.app.AlertDialog.Builder(LoginActivity.this);
            builder.setMessage("Hay campos vacíos")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            builder.create().show();

        }else {

            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Ingresando...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            firebaseData.mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                progressDialog.dismiss();
                                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                builder.setMessage("Datos incorrectos. Por favor, inténtalo de nuevo.");
                                builder.create().show();
                            }else{
                                progressDialog.dismiss();
                                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                builder.setMessage("Ingresaste!");
                                builder.create().show();
                                String email = task.getResult().getUser().getEmail();
                                //Toast.makeText(getApplicationContext(), "AnTES DE BUSCARDATOS USUARIO", Toast.LENGTH_SHORT).show();
                                buscarDatosUsuario(email);
                            }
                        }
                    });
        }
    }

    @Override
    public void onBackPressed() {

    }
}
