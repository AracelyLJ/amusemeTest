package com.example.aracelylj.amusemeapp.Correo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.aracelylj.amusemeapp.R;

public class CorreoActivity extends AppCompatActivity {

    private EditText destinatario;
    private EditText mensaje;
    private Button enviar;
    private EnviarCorreo correo;
    private String destinatario_str;
    private String mensaje_str;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_correo);

        destinatario = (EditText) findViewById(R.id.destinatario);
        mensaje = (EditText)findViewById(R.id.mensaje);
        enviar = (Button) findViewById(R.id.enviar_correo);

        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                destinatario_str = destinatario.getText().toString();
                mensaje_str = mensaje.getText().toString();

                if(!destinatario_str.isEmpty() && !mensaje_str.isEmpty()) {
                    correo = new EnviarCorreo("CORREO ACTIVITY",destinatario_str, mensaje_str);
                    correo.enviarCorreo();
                    Toast.makeText(CorreoActivity.this, "Correo enviado, checalo ;)", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(CorreoActivity.this, "Campos vacios", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
