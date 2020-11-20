package com.example.aracelylj.amusemeapp.Correo;

import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import java.util.Properties;

import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EnviarCorreo {

    private String destinatario;
    private String mensaje;
    private String subject;
    private javax.mail.Session session;

    private final String correo = "amuseme2019@gmail.com";
    private final String contrasenia = "amusemeapp2020";

    public EnviarCorreo () {
        this.destinatario = "";
        this.mensaje = "";
    }

    public EnviarCorreo(String subject, String destinatario, String mensaje) {
        this.subject = subject;
        this.destinatario = destinatario;
        this.mensaje = mensaje;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void enviarCorreo () {

        Log.e("AQUÍ!!","Empieza a mandar correo");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Properties properties = new Properties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.host","smtp.googlemail.com");
        properties.put("mail.smtp.socketFactory.port","465");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.port","465");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.debug", "true");
        properties.put("mail.smtp.socketFactory.fallback", "false");

        //Toast.makeText(EnviarCorreo.this, "Correo enviado, checalo ;)", Toast.LENGTH_SHORT).show();

        try {
            session = javax.mail.Session.getDefaultInstance(properties, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(correo, contrasenia);
                }
            });

            if(session != null){
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(correo));
                message.setSubject(this.subject);
                message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(destinatario));
                message.setContent(mensaje,"text/html; charset=utf-8");

                Transport.send(message);


            }else{
                Log.e("SESSION","Session = nulo");
            }
        } catch (Exception e){
            Log.e("ERROR","Entra a catch "+e.toString());
            e.printStackTrace();
        }
        Log.e("AQUÍ!!","Termina de mandar correo");
    }

}
