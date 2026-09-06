package pe.civimoto.chofer;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio dedicado a escuchar solicitudes aunque la pantalla esté apagada.
 * Mantiene un foreground service + PARTIAL_WAKE_LOCK y consulta viajes sin
 * depender del ciclo de vida de la Activity.
 */
public class ReliableDriverAlertService extends Service {
    public static final String ACTION_NEW_OFFER = DriverAlertService.ACTION_NEW_OFFER;
    private static final String CHANNEL = "civimoto_driver_reliable_online_v1";
    private static final String PREF = "civimoto_driver_runtime";
    private static final String PREF_IGNORED = "reliable_ignored";

    private Backend backend;
    private CiviAlert alert;
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean checking = new AtomicBoolean(false);
    private PowerManager.WakeLock wakeLock;
    private String lastOfferId;
    private volatile boolean destroyed;
    private BroadcastReceiver screenReceiver;

    public static void start(Context c) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("online", true).apply();
        Intent i = new Intent(c, ReliableDriverAlertService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
        } catch (Exception ignored) {}
    }

    public static void stop(Context c) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("online", false).apply();
        c.stopService(new Intent(c, ReliableDriverAlertService.class));
    }

    public static void ignoreOffer(Context c, String id) {
        if (id == null || id.isEmpty()) return;
        android.content.SharedPreferences p=c.getSharedPreferences(PREF_IGNORED,Context.MODE_PRIVATE);
        java.util.Set<String> s=new java.util.HashSet<>(p.getStringSet("ids",java.util.Collections.emptySet()));
        s.add(id);p.edit().putStringSet("ids",s).apply();
        Intent i=new Intent(c,ReliableDriverAlertService.class).setAction("IGNORE").putExtra("id",id);
        try{if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}catch(Exception ignored){}
    }

    @Override public void onCreate() {
        super.onCreate();
        destroyed=false;
        backend=new Backend(this);
        alert=new CiviAlert(this);
        createChannel();
        acquireWakeLock();
        startForeground(7701, serviceNotification("Esperando solicitudes de viaje"));
        registerScreenReceiver();
        scheduler=Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::checkOffers,0,2,TimeUnit.SECONDS);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        getSharedPreferences(PREF,MODE_PRIVATE).edit().putBoolean("online",true).apply();
        if(intent!=null&&"IGNORE".equals(intent.getAction())) {
            String id=intent.getStringExtra("id");
            if(id!=null&&id.equals(lastOfferId)) lastOfferId=null;
            if(alert!=null) alert.stopOffer();
            updateForeground("Solicitud rechazada. Esperando nuevos viajes");
        }
        checkOffers();
        return START_STICKY;
    }

    private void checkOffers() {
        if(destroyed||checking.getAndSet(true)) return;
        if(!getSharedPreferences(PREF,MODE_PRIVATE).getBoolean("online",false)) {
            checking.set(false); stopSelf(); return;
        }
        if(backend==null||!backend.hasSession()) {
            if(alert!=null) alert.stopOffer(); lastOfferId=null; checking.set(false);
            updateForeground("Esperando inicio de sesión"); return;
        }
        try {
            backend.rpc("driver_available_trips",new JSONObject().put("p_radius_km",25),new Backend.Callback(){
                public void ok(Object value){
                    try {
                        JSONObject t=Backend.firstObject(value);
                        if(t==null){clearOffer("Esperando solicitudes de viaje");return;}
                        String id=t.optString("id","");
                        String status=t.optString("status","");
                        if(id.isEmpty()||!"solicitado".equals(status)||isIgnored(id)){clearOffer("Esperando solicitudes de viaje");return;}
                        boolean fresh=!id.equals(lastOfferId);
                        lastOfferId=id;
                        String route=t.optString("origin_address","Origen")+" → "+t.optString("destination_address","Destino");
                        updateForeground("Nueva solicitud: "+route);
                        if(alert!=null) alert.startOffer("Nueva solicitud CiviMoto",route+". Acepta o rechaza para detener la alerta.");
                        if(fresh){Intent e=new Intent(ACTION_NEW_OFFER);e.setPackage(getPackageName());e.putExtra("trip_id",id);sendBroadcast(e);}
                    } finally {checking.set(false);}
                }
                public void error(String m){checking.set(false);updateForeground("Conectando con central…");}
            });
        } catch(Exception e){checking.set(false);}
    }

    private boolean isIgnored(String id){
        return getSharedPreferences(PREF_IGNORED,MODE_PRIVATE).getStringSet("ids",java.util.Collections.emptySet()).contains(id);
    }

    private void clearOffer(String text){lastOfferId=null;if(alert!=null)alert.stopOffer();updateForeground(text);}

    private void acquireWakeLock(){
        try{
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CiviMoto:ReliableOfferLock");
            wakeLock.setReferenceCounted(false);
            if(!wakeLock.isHeld())wakeLock.acquire();
        }catch(Exception ignored){}
    }

    private void registerScreenReceiver(){
        screenReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
            String a=i.getAction();
            if(Intent.ACTION_SCREEN_OFF.equals(a)||Intent.ACTION_USER_PRESENT.equals(a)){acquireWakeLock();checkOffers();}
        }};
        try{IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_USER_PRESENT);registerReceiver(screenReceiver,f);}catch(Exception ignored){}
    }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel ch=new NotificationChannel(CHANNEL,"CiviMoto conductor conectado",NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Mantiene activa la recepción de solicitudes con pantalla apagada");ch.setSound(null,null);ch.enableVibration(false);
            if(nm!=null)nm.createNotificationChannel(ch);
        }
    }

    private Notification serviceNotification(String text){
        Intent open=getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pi=null;
        if(open!=null){open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);pi=PendingIntent.getActivity(this,7701,open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));}
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle("CiviMoto Conductor en línea").setContentText(text).setOngoing(true).setOnlyAlertOnce(true).setPriority(Notification.PRIORITY_LOW).setCategory(Notification.CATEGORY_SERVICE);
        if(Build.VERSION.SDK_INT>=31)b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        if(pi!=null)b.setContentIntent(pi);
        return b.build();
    }

    private void updateForeground(String text){try{NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)nm.notify(7701,serviceNotification(text));}catch(Exception ignored){}}

    private void scheduleRestart(){
        if(!getSharedPreferences(PREF,MODE_PRIVATE).getBoolean("online",false))return;
        try{
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            Intent i=new Intent(this,ReliableDriverAlertService.class);
            PendingIntent pi=Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(this,7702,i,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0)):PendingIntent.getService(this,7702,i,PendingIntent.FLAG_UPDATE_CURRENT);
            if(am!=null)am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+4000,pi);
        }catch(Exception ignored){}
    }

    @Override public void onTaskRemoved(Intent rootIntent){scheduleRestart();super.onTaskRemoved(rootIntent);}
    @Override public void onDestroy(){
        destroyed=true;
        if(scheduler!=null)scheduler.shutdownNow();
        try{if(screenReceiver!=null)unregisterReceiver(screenReceiver);}catch(Exception ignored){}
        if(alert!=null)alert.release();
        try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}
        scheduleRestart();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){return null;}
}
