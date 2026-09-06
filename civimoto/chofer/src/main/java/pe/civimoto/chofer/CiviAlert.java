package pe.civimoto.chofer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class CiviAlert {
    private static final String CHANNEL="civimoto_driver_alerts";
    private final Context context;
    private final NotificationManager nm;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ToneGenerator tone;
    private boolean ringing=false;
    private String title="Nuevo viaje disponible",body="Tienes una nueva solicitud CiviMoto";

    public CiviAlert(Context c){
        context=c.getApplicationContext();
        nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        try{tone=new ToneGenerator(AudioManager.STREAM_ALARM,100);}catch(Exception ignored){}
        createChannel();
    }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26&&nm!=null){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Servicios CiviMoto",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Solicitudes y cambios del viaje para choferes");
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0,450,180,450});
            nm.createNotificationChannel(ch);
        }
    }

    public void startOffer(String t,String b){
        title=t;body=b;
        if(!ringing){ringing=true;handler.post(alarmLoop);}
        notifyNow(title,body,4101,true);
    }

    public void stopOffer(){ringing=false;handler.removeCallbacks(alarmLoop);try{if(nm!=null)nm.cancel(4101);}catch(Exception ignored){}}

    public void event(String t,String b,int id){beepShort();vibrate(260);notifyNow(t,b,id,false);}

    private final Runnable alarmLoop=new Runnable(){public void run(){if(!ringing)return;try{if(tone!=null)tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,650);}catch(Exception ignored){}vibrate(500);handler.postDelayed(this,1300);}};

    private void notifyNow(String t,String b,int id,boolean ongoing){
        try{
            android.app.Notification.Builder n=Build.VERSION.SDK_INT>=26?new android.app.Notification.Builder(context,CHANNEL):new android.app.Notification.Builder(context);
            n.setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle(t).setContentText(b).setAutoCancel(!ongoing).setOngoing(ongoing).setPriority(android.app.Notification.PRIORITY_MAX);
            if(Build.VERSION.SDK_INT<26)n.setDefaults(android.app.Notification.DEFAULT_ALL);
            if(nm!=null)nm.notify(id,n.build());
        }catch(SecurityException ignored){}catch(Exception ignored){}
    }

    private void beepShort(){try{if(tone!=null)tone.startTone(ToneGenerator.TONE_PROP_BEEP2,420);}catch(Exception ignored){}}
    private void vibrate(long ms){try{Vibrator v=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);if(v==null)return;if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(ms,VibrationEffect.DEFAULT_AMPLITUDE));else v.vibrate(ms);}catch(Exception ignored){}}
    public void release(){stopOffer();try{if(tone!=null){tone.release();tone=null;}}catch(Exception ignored){}}
}
