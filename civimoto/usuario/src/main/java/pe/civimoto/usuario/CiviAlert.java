package pe.civimoto.usuario;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class CiviAlert {
    private static final String CHANNEL="civimoto_passenger_alerts";
    private final Context context;
    private final NotificationManager nm;
    private ToneGenerator tone;

    public CiviAlert(Context c){
        context=c.getApplicationContext();
        nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        try{tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,100);}catch(Exception ignored){}
        createChannel();
    }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26&&nm!=null){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Alertas de viaje CiviMoto",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Cambios importantes del viaje del pasajero");
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0,220,120,220});
            nm.createNotificationChannel(ch);
        }
    }

    public void event(String title,String body,int id){
        beep(); vibrate();
        try{
            android.app.Notification.Builder b=Build.VERSION.SDK_INT>=26?new android.app.Notification.Builder(context,CHANNEL):new android.app.Notification.Builder(context);
            b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setAutoCancel(true).setPriority(android.app.Notification.PRIORITY_HIGH);
            if(Build.VERSION.SDK_INT<26)b.setDefaults(android.app.Notification.DEFAULT_ALL);
            if(nm!=null)nm.notify(id,b.build());
        }catch(SecurityException ignored){}catch(Exception ignored){}
    }

    private void beep(){try{if(tone!=null)tone.startTone(ToneGenerator.TONE_PROP_BEEP2,420);}catch(Exception ignored){}}
    private void vibrate(){try{Vibrator v=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);if(v==null)return;if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(320,VibrationEffect.DEFAULT_AMPLITUDE));else v.vibrate(320);}catch(Exception ignored){}}
    public void release(){try{if(tone!=null){tone.release();tone=null;}}catch(Exception ignored){}}
}
