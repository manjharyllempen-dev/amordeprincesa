package pe.civimoto.chofer;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.net.wifi.WifiManager;
import android.os.*;
import org.json.JSONObject;
import java.util.HashSet;

/**
 * Servicio único de recepción del conductor. Mantiene una solicitud pendiente
 * persistida para que Activity y servicio usen exactamente el mismo viaje.
 */
public class DriverAlertService extends Service {
    public static final String ACTION_NEW_OFFER="pe.civimoto.chofer.NEW_OFFER";
    private static final String ACTION_IGNORE="pe.civimoto.chofer.IGNORE_OFFER";
    private static final String ACTION_RESOLVE="pe.civimoto.chofer.RESOLVE_OFFER";
    private static final String CHANNEL="civimoto_driver_online_silent_v6";
    private static final String PREF="civimoto_driver_runtime";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final HashSet<String> ignored=new HashSet<>();
    private Backend backend; private CiviAlert alert; private RealtimeOfferSocket socket;
    private PowerManager.WakeLock wakeLock; private WifiManager.WifiLock wifiLock;
    private LocationManager locationManager; private String lastOfferId; private boolean destroyed=false,polling=false;
    private int missCount=0;

    public static void setAppVisible(boolean visible){}
    public static void start(Context c){
        c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putBoolean("online",true).apply();
        Intent i=new Intent(c,DriverAlertService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);
    }
    public static void stop(Context c){
        c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putBoolean("online",false).remove("pending_offer_id").remove("pending_offer_json").apply();
        c.stopService(new Intent(c,DriverAlertService.class));
    }
    public static void ignoreOffer(Context c,String id){sendAction(c,ACTION_IGNORE,id);}
    public static void resolveOffer(Context c,String id){sendAction(c,ACTION_RESOLVE,id);}
    private static void sendAction(Context c,String action,String id){Intent i=new Intent(c,DriverAlertService.class).setAction(action).putExtra("id",id);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}

    @Override public void onCreate(){
        super.onCreate();destroyed=false;backend=new Backend(this);alert=new CiviAlert(this);createSilentServiceChannel();acquireLocks();
        startForeground(7301,foregroundNotification("CiviMoto Conductor en línea","Escuchando solicitudes en segundo plano",null));
        startLocationTracking();registerScreenReceiver();
        socket=new RealtimeOfferSocket(backend,new RealtimeOfferSocket.Listener(){public void onTripSignal(){pollNow();}public void onState(String s){if("CONNECTED".equals(s))pollNow();}});socket.start();
        restorePendingOffer();handler.post(poller);scheduleWatchdog();
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        getSharedPreferences(PREF,MODE_PRIVATE).edit().putBoolean("online",true).apply();
        if(intent!=null&&(ACTION_IGNORE.equals(intent.getAction())||ACTION_RESOLVE.equals(intent.getAction()))){
            String id=intent.getStringExtra("id");if(ACTION_IGNORE.equals(intent.getAction())&&id!=null)ignored.add(id);
            clearPending(id);stopOfferAlarm();lastOfferId=null;missCount=0;updateForeground(ACTION_IGNORE.equals(intent.getAction())?"Solicitud rechazada. Esperando nuevos viajes":"Viaje aceptado. Alerta detenida",null);
        }
        if(socket!=null)socket.start();pollNow();scheduleWatchdog();return START_STICKY;
    }

    private void createSilentServiceChannel(){if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);NotificationChannel ch=new NotificationChannel(CHANNEL,"CiviMoto conductor en línea",NotificationManager.IMPORTANCE_LOW);ch.setDescription("Mantiene al conductor conectado con pantalla apagada");ch.enableVibration(false);ch.setSound(null,null);ch.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);nm.createNotificationChannel(ch);}}
    private Notification foregroundNotification(String title,String text,String tripId){
        Intent open=new Intent(this,ReliableDriverActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if(tripId!=null){open.putExtra("show_offer",true).putExtra("trip_id",tripId);}
        PendingIntent pi=PendingIntent.getActivity(this,7301,open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle(title).setContentText(text).setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).setPriority(Notification.PRIORITY_LOW).setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PRIVATE);
        if(Build.VERSION.SDK_INT<26)b.setSound(null).setVibrate(new long[]{0});return b.build();
    }
    private void updateForeground(String text,String tripId){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)nm.notify(7301,foregroundNotification("CiviMoto Conductor en línea",text,tripId));}

    private void acquireLocks(){
        try{PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CiviMoto:DriverAlwaysOn");wakeLock.setReferenceCounted(false);wakeLock.acquire();}catch(Exception ignored){}
        try{WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);if(wm!=null){wifiLock=wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,"CiviMoto:DriverWifiAlwaysOn");wifiLock.setReferenceCounted(false);wifiLock.acquire();}}catch(Exception ignored){}
    }

    private void registerScreenReceiver(){try{IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_USER_PRESENT);registerReceiver(screenReceiver,f);}catch(Exception ignored){}}
    private final BroadcastReceiver screenReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(socket!=null)socket.start();pollNow();scheduleWatchdog();}};

    private void startLocationTracking(){
        try{locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);if(locationManager==null)return;if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;
            Location last=null;try{last=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);}catch(Exception ignored){}if(last==null)try{last=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);}catch(Exception ignored){}if(last!=null)sendLocation(last);
            try{locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000L,5f,locationListener,Looper.getMainLooper());}catch(Exception ignored){}try{locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,8000L,10f,locationListener,Looper.getMainLooper());}catch(Exception ignored){}
        }catch(Exception ignored){}
    }
    private final LocationListener locationListener=new LocationListener(){public void onLocationChanged(Location l){if(l!=null)sendLocation(l);}public void onStatusChanged(String p,int s,Bundle b){}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}};
    private void sendLocation(Location l){try{if(backend!=null&&backend.hasSession())backend.rpc("driver_update_live_location",new JSONObject().put("p_lat",l.getLatitude()).put("p_lng",l.getLongitude()).put("p_heading",l.hasBearing()?l.getBearing():0).put("p_speed",l.hasSpeed()?l.getSpeed():0),new Backend.Callback(){public void ok(Object x){}public void error(String m){}});}catch(Exception ignored){}}
    private void stopOfferAlarm(){if(alert!=null)alert.stopOffer();}

    private final Runnable poller=new Runnable(){public void run(){if(destroyed)return;pollNow();handler.postDelayed(this,2200);}};
    private void pollNow(){
        if(destroyed||polling)return;
        if(!getSharedPreferences(PREF,MODE_PRIVATE).getBoolean("online",false)){stopOfferAlarm();stopSelf();return;}
        if(backend==null||!backend.hasSession()){updateForeground("Esperando inicio de sesión",null);return;}
        polling=true;
        try{backend.rpc("driver_available_trips",new JSONObject().put("p_radius_km",20),new Backend.Callback(){
            public void ok(Object value){polling=false;JSONObject trip=Backend.firstObject(value);
                if(trip==null){if(lastOfferId!=null&&++missCount<2)return;missCount=0;lastOfferId=null;clearPending(null);stopOfferAlarm();updateForeground("Esperando solicitudes de viaje",null);return;}
                String id=trip.optString("id","");String status=trip.optString("status","");
                if(id.isEmpty()||!"solicitado".equals(status)||ignored.contains(id)){if(id.equals(lastOfferId))lastOfferId=null;clearPending(id);stopOfferAlarm();updateForeground("Esperando solicitudes de viaje",null);return;}
                missCount=0;boolean fresh=!id.equals(lastOfferId);lastOfferId=id;persistPending(trip);
                String route=trip.optString("origin_address","Origen")+" → "+trip.optString("destination_address","Destino");
                updateForeground("Nueva solicitud pendiente",id);alert.startOffer("Nueva solicitud CiviMoto",route+". Acepta o rechaza para detener la alerta.");
                if(fresh){Intent event=new Intent(ACTION_NEW_OFFER).setPackage(getPackageName()).putExtra("trip_id",id);sendBroadcast(event);}
            }
            public void error(String message){polling=false;if(lastOfferId!=null){String json=getSharedPreferences(PREF,MODE_PRIVATE).getString("pending_offer_json",null);try{JSONObject t=json==null?null:new JSONObject(json);if(t!=null)alert.startOffer("Solicitud CiviMoto pendiente",t.optString("origin_address","Origen")+" → "+t.optString("destination_address","Destino"));}catch(Exception ignored){}}else updateForeground("Reconectando con central…",null);}
        });}catch(Exception e){polling=false;}
    }

    private void persistPending(JSONObject trip){getSharedPreferences(PREF,MODE_PRIVATE).edit().putString("pending_offer_id",trip.optString("id","")).putString("pending_offer_json",trip.toString()).apply();}
    private void restorePendingOffer(){String json=getSharedPreferences(PREF,MODE_PRIVATE).getString("pending_offer_json",null);if(json==null)return;try{JSONObject t=new JSONObject(json);String id=t.optString("id","");if(!id.isEmpty()){lastOfferId=id;alert.startOffer("Solicitud CiviMoto pendiente",t.optString("origin_address","Origen")+" → "+t.optString("destination_address","Destino"));}}catch(Exception ignored){}}
    private void clearPending(String id){String current=getSharedPreferences(PREF,MODE_PRIVATE).getString("pending_offer_id",null);if(id==null||current==null||id.equals(current))getSharedPreferences(PREF,MODE_PRIVATE).edit().remove("pending_offer_id").remove("pending_offer_json").apply();}

    private void scheduleWatchdog(){
        if(!getSharedPreferences(PREF,MODE_PRIVATE).getBoolean("online",false))return;
        try{AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);Intent restart=new Intent(this,DriverAlertService.class);int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);PendingIntent pi=Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(this,7302,restart,flags):PendingIntent.getService(this,7302,restart,flags);long at=SystemClock.elapsedRealtime()+45000;if(am!=null){if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,pi);else am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,pi);}}catch(Exception ignored){}
    }
    @Override public void onTaskRemoved(Intent rootIntent){scheduleWatchdog();super.onTaskRemoved(rootIntent);}
    @Override public void onDestroy(){destroyed=true;handler.removeCallbacksAndMessages(null);if(socket!=null)socket.stop();if(alert!=null)alert.release();try{unregisterReceiver(screenReceiver);}catch(Exception ignored){}try{if(locationManager!=null)locationManager.removeUpdates(locationListener);}catch(Exception ignored){}try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}try{if(wifiLock!=null&&wifiLock.isHeld())wifiLock.release();}catch(Exception ignored){}scheduleWatchdog();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}
