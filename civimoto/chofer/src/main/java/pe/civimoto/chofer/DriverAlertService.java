package pe.civimoto.chofer;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.JSONObject;
import java.util.HashSet;

public class DriverAlertService extends Service {
    public static final String ACTION_NEW_OFFER="pe.civimoto.chofer.NEW_OFFER";
    private static final String CHANNEL="civimoto_driver_online_silent_v3";
    private static volatile boolean appVisible=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final HashSet<String> ignored=new HashSet<>();
    private Backend backend; private CiviAlert alert; private PowerManager.WakeLock wakeLock; private String lastOfferId;

    public static void setAppVisible(boolean visible){appVisible=visible;}
    public static void start(Context c){Intent i=new Intent(c,DriverAlertService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
    public static void stop(Context c){c.stopService(new Intent(c,DriverAlertService.class));}
    public static void ignoreOffer(Context c,String id){if(id==null)return;Intent i=new Intent(c,DriverAlertService.class).setAction("IGNORE").putExtra("id",id);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}

    @Override public void onCreate(){
        super.onCreate();
        backend=new Backend(this);
        alert=new CiviAlert(this);
        createSilentServiceChannel();
        acquireWakeLock();
        startForeground(7301,foregroundNotification("CiviMoto Conductor en línea","Esperando solicitudes de viaje"));
        handler.post(poller);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&"IGNORE".equals(intent.getAction())){
            String id=intent.getStringExtra("id");
            if(id!=null)ignored.add(id);
            stopOfferAlarm();
            if(id!=null&&id.equals(lastOfferId))lastOfferId=null;
            updateForeground("Solicitud rechazada. Esperando nuevos viajes");
        }
        return START_STICKY;
    }

    private void createSilentServiceChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel ch=new NotificationChannel(CHANNEL,"CiviMoto conductor en línea",NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Mantiene al conductor conectado para recibir solicitudes");
            ch.enableVibration(false);ch.setSound(null,null);ch.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            nm.createNotificationChannel(ch);
        }
    }

    private Notification foregroundNotification(String title,String text){
        Intent open=getPackageManager().getLaunchIntentForPackage(getPackageName());
        if(open==null)open=new Intent(this,LiveRatingActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,7301,open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle(title).setContentText(text).setContentIntent(pi)
                .setOngoing(true).setOnlyAlertOnce(true).setPriority(Notification.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PRIVATE);
        if(Build.VERSION.SDK_INT<26)b.setSound(null).setVibrate(new long[]{0});
        return b.build();
    }

    private void updateForeground(String text){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)nm.notify(7301,foregroundNotification("CiviMoto Conductor en línea",text));}

    private void acquireWakeLock(){
        try{PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CiviMoto:DriverOfferLock");wakeLock.setReferenceCounted(false);wakeLock.acquire();}catch(Exception ignored){}
    }

    private void stopOfferAlarm(){if(alert!=null)alert.stopOffer();}

    private final Runnable poller=new Runnable(){
        public void run(){
            if(backend==null||!backend.hasSession()){
                stopOfferAlarm();lastOfferId=null;updateForeground("Esperando inicio de sesión");handler.postDelayed(this,4000);return;
            }
            try{
                backend.rpc("driver_available_trips",new JSONObject().put("p_radius_km",12),new Backend.Callback(){
                    public void ok(Object value){
                        JSONObject trip=Backend.firstObject(value);
                        if(trip==null){lastOfferId=null;stopOfferAlarm();updateForeground("Esperando solicitudes de viaje");return;}
                        String id=trip.optString("id","");String status=trip.optString("status","");
                        if(id.isEmpty()||!"solicitado".equals(status)||ignored.contains(id)){
                            if(id.equals(lastOfferId))lastOfferId=null;stopOfferAlarm();updateForeground("Esperando solicitudes de viaje");return;
                        }
                        boolean fresh=!id.equals(lastOfferId);lastOfferId=id;
                        String route=trip.optString("origin_address","Origen")+" → "+trip.optString("destination_address","Destino");
                        updateForeground("Nueva solicitud: "+route);

                        // Siempre mantener la alarma mientras la solicitud siga pendiente.
                        // Así funciona con pantalla apagada, app minimizada o app abierta.
                        alert.startOffer("Nueva solicitud CiviMoto",route);

                        if(fresh){Intent event=new Intent(ACTION_NEW_OFFER);event.setPackage(getPackageName());event.putExtra("trip_id",id);sendBroadcast(event);}
                    }
                    public void error(String message){
                        // No cortamos una alarma pendiente por un fallo de red momentáneo.
                        // Si ya había una oferta, conservamos la alerta y reintentamos pronto.
                        if(lastOfferId==null||lastOfferId.isEmpty())updateForeground("Conectando con central…");
                    }
                });
            }catch(Exception ignored){}
            handler.postDelayed(this,2000);
        }
    };

    @Override public void onTaskRemoved(Intent rootIntent){
        try{
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);Intent restart=new Intent(this,DriverAlertService.class);
            int flags=PendingIntent.FLAG_ONE_SHOT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
            PendingIntent pi=Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(this,7302,restart,flags):PendingIntent.getService(this,7302,restart,flags);
            if(am!=null){long at=SystemClock.elapsedRealtime()+1500;if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,pi);else am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,pi);}
        }catch(Exception ignored){}
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy(){handler.removeCallbacks(poller);if(alert!=null)alert.release();try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}
