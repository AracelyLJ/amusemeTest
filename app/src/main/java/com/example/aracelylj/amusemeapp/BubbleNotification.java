package com.example.aracelylj.amusemeapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.nex3z.notificationbadge.NotificationBadge;
import com.txusballesteros.bubbles.BubbleLayout;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.OnInitializedCallback;

import de.hdodenhof.circleimageview.CircleImageView;

public class BubbleNotification extends AppCompatActivity {

    // ** Bubble Notification variables
    private BubblesManager bubblesManager;
    private NotificationBadge nBadge;
    private AppCompatTextView notifName;
    private CircleImageView circle;
    private int MY_PERMISSION = 1000;
    public final static int REQUEST_CODE = 6341;
    private Context context;
    private int cont=0;
    private String mensaje;
    int bandCambio = 0;

    public BubbleNotification(Context context) {

        this.context = context;
        // Check premissions (para la burbuja)
        if (Build.VERSION.SDK_INT >= 23){
            if (!Settings.canDrawOverlays(context))
            {
                //Toast.makeText(context,"NO TIENE PERMISO ",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:"+getPackageName()));
                startActivityForResult(intent,MY_PERMISSION);
            }else{
                //Toast.makeText(context, "SI TIENE PERMISO", Toast.LENGTH_SHORT).show();
            }
        }else {
            Intent intent = new Intent(context,Service.class);
            startService(intent);

        }
        initBubble();
        //addNewBubble();
    }

    /**************************** Bubble Notification FUunciones****************************/
    public void initBubble(){

        bubblesManager = new BubblesManager.Builder(this.context)
                .setTrashLayout(R.layout.bubble_remove)
                .setInitializationCallback(new OnInitializedCallback() {
                    @Override
                    public void onInitialized() {
                        //addNewBubble(1);
                        BubbleLayout bubbleView = (BubbleLayout) LayoutInflater.from(context)
                                .inflate(R.layout.bubble_layout,null);

                        nBadge = (NotificationBadge) bubbleView.findViewById(R.id.count);
                        notifName = (AppCompatTextView) bubbleView.findViewById(R.id.notifName);
                        nBadge.setNumber(cont);
                        notifName.setText(mensaje);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                notifName.setText("");
                            }
                        }, 5000);

                        bubbleView.setOnBubbleRemoveListener(new BubbleLayout.OnBubbleRemoveListener() {
                            @Override
                            public void onBubbleRemoved(BubbleLayout bubble) {
                                Toast.makeText(context, "Removed", Toast.LENGTH_SHORT).show();
                            }
                        });
                        bubbleView.setOnBubbleClickListener(new BubbleLayout.OnBubbleClickListener() {
                            @Override
                            public void onBubbleClick(BubbleLayout bubble) {
                                Toast.makeText(context,"Clicked",Toast.LENGTH_SHORT).show();
                            }
                        });
                        bubbleView.setShouldStickToWall(true);
                    }
                }).build();

        bubblesManager.initialize();
    }
    public void addNewBubble(int cont, final String mensaje,String imagen){
        this.cont = cont;
        this.mensaje = mensaje;

        BubbleLayout bubbleView = (BubbleLayout) LayoutInflater.from(context)
                .inflate(R.layout.bubble_layout,null);

        nBadge = (NotificationBadge) bubbleView.findViewById(R.id.count);
        notifName = (AppCompatTextView) bubbleView.findViewById(R.id.notifName);
        circle = (CircleImageView) bubbleView.findViewById(R.id.avatar);

        nBadge.setNumber(cont);
        notifName.setText(mensaje);
        if (imagen.equals("fail")) circle.setImageResource(R.drawable.fail);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                notifName.setText("");
            }
        }, 5000);

        bubbleView.setOnBubbleRemoveListener(new BubbleLayout.OnBubbleRemoveListener() {
            @Override
            public void onBubbleRemoved(BubbleLayout bubble) {
                //Toast.makeText(context, "Removed", Toast.LENGTH_SHORT).show();
            }
        });
        bubbleView.setOnBubbleClickListener(new BubbleLayout.OnBubbleClickListener() {
            @Override
            public void onBubbleClick(BubbleLayout bubble) {
                if (bandCambio==0){
                    notifName.setText(mensaje);
                    bandCambio=1;
                }else{
                    notifName.setText("");
                    bandCambio=0;
                }
                //Toast.makeText(context,mensaje,Toast.LENGTH_SHORT).show();
            }
        });
        bubbleView.setShouldStickToWall(true);
        bubblesManager.addBubble(bubbleView,60,20);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        bubblesManager.recycle();
    }

}
