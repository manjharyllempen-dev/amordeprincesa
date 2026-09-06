package pe.civimoto.chofer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Alarma persistente para nuevas solicitudes del conductor. */
public class CiviAlert {
    // Canal nuevo para no heredar configuraciones antiguas/silenciosas de Android.
    private static final String CHANNEL="civimoto_driver_trip_alerts_v3";
    private final Context context;
    private final NotificationManager nm;
    private final AudioManager audio;
    private MediaPlayer player;
    private boolean ringing=false;

    public CiviAlert(Context c){
        context=c.getApplicationContext();
        nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        createChannel();
    }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26&&nm!=null){
            Uri alarm=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if(alarm==null)alarm=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes aa=new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Solicitudes de viaje CiviMoto",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Alerta sonora persistente de nuevas solicitudes para conductores");
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0,650,250,650,250,900});
            ch.setSound(alarm,aa);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            ch.setBypassDnd(false);
            nm.createNotificationChannel(ch);
        }
    }

    public synchronized void startOffer(String title,String body){
        if(!ringing){
            ringing=true;
            startAudioLoop();
            startVibrationLoop();
        }
        notifyNow(title,body,4101,true);
    }

    public synchronized void stopOffer(){
        ringing=false;
        stopAudio();
        stopVibration();
        try{if(nm!=null)nm.cancel(4101);}catch(Exception ignored){}
    }

    public void event(String title,String body,int id){
        notifyNow(title,body,id,false);
    }

    private void startAudioLoop(){
        stopAudio();
        try{
            if(audio!=null)audio.requestAudioFocus(null,AudioManager.STREAM_ALARM,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            Uri u=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if(u==null)u=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if(u==null)u=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if(u==null)return;
            player=new MediaPlayer();
            if(Build.VERSION.SDK_INT>=21){
                player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            }else{
                player.setAudioStreamType(AudioManager.STREAM_ALARM);
            }
            player.setDataSource(context,u);
            player.setLooping(true);
            player.prepare();
            player.setVolume(1f,1f);
            player.start();
        }catch(Exception e){
            stopAudio();
        }
    }

    private void stopAudio(){
        try{if(player!=null){if(player.isPlaying())player.stop();player.release();}}catch(Exception ignored){}
        player=null;
        try{if(audio!=null)audio.abandonAudioFocus(null);}catch(Exception ignored){}
    }

    private void startVibrationLoop(){
        try{
            Vibrator v=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
            if(v==null)return;
            long[] pattern=new long[]{0,650,250,650,250,900};
            if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pattern,0));
            else v.vibrate(pattern,0);
        }catch(Exception ignored){}
    }

    private void stopVibration(){
        try{Vibrator v=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);if(v!=null)v.cancel();}catch(Exception ignored){}
    }

    private void notifyNow(String title,String body,int id,boolean ongoing){
        try{
            Intent launch=context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            PendingIntent pi=null;
            if(launch!=null){
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                pi=PendingIntent.getActivity(context,4101,launch,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
            }
            Notification.Builder n=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
            n.setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new Notification.BigTextStyle().bigText(body))
                    .setAutoCancel(!ongoing)
                    .setOngoing(ongoing)
                    .setOnlyAlertOnce(true)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setPriority(Notification.PRIORITY_MAX);
            if(pi!=null)n.setContentIntent(pi);
            if(Build.VERSION.SDK_INT<26){
                Uri u=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                n.setSound(u,AudioManager.STREAM_ALARM).setVibrate(new long[]{0,650,250,650,250,900});
            }
            if(nm!=null)nm.notify(id,n.build());
        }catch(SecurityException ignored){}catch(Exception ignored){}
    }

    public void release(){stopOffer();}
}
