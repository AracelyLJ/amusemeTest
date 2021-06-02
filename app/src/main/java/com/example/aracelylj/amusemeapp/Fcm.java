package com.example.aracelylj.amusemeapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebase.client.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;


public class Fcm extends FirebaseMessagingService {

    private Firebase ref;
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("token", "mi token es: "+s);

        guradarToken(s);
    }

    private void guradarToken(String s) {
        ref = new Firebase("https://amusebd-8797e.firebaseio.com/").child("tokens");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("token");
        ref.child(user.getUid()).setValue(s);
        Log.e("token","se guarda el token");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String from = remoteMessage.getFrom();
        if (remoteMessage.getData().size() > 0){
            String titulo = remoteMessage.getData().get("titulo");
            String detalle = remoteMessage.getData().get("detalle");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                mayorQueOreo(titulo, detalle);
            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//                mayorQueOreo(titulo, detalle);
//            }


        }
    }

    private void mayorQueOreo(String titulo, String detalle) {

        String id = "mensaje";
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, id);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(id, "nuevo", NotificationManager.IMPORTANCE_HIGH);
            nc.setShowBadge(true);
            assert nm != null;
            nm.createNotificationChannel(nc);
        }
        builder.setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(titulo)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(detalle)
                .setContentIntent(clicknoti())
                .setContentInfo("nuevo");
        Random random = new Random();
        int idNotify = random.nextInt(8000);

        assert nm != null;
        nm.notify(idNotify, builder.build());
    }
    public PendingIntent clicknoti(){
        Intent nf = new Intent(getApplicationContext(), MainActivity.class);
        nf.putExtra("color", "rojo");
        nf.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, 0, nf, 0);
    }
}
