package pe.civimoto.chofer;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.*;
import org.json.JSONObject;
import java.util.HashSet;

public class DriverAlertService extends Service {
    public static final String ACTION_NEW_OFFER="pe.civimoto.chofer.NEW_OFFER";
    private static final String CHANNEL="civimoto_driver_online_silent_v4";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final HashSet<String> ignored=new HashSet<>();
    private Backend backend; private CiviAlert alert; private PowerManager.WakeLock wakeLock; private WifiManager.WifiLock wifiLock;
    private LocationManager locationManager; private String lastOfferId; private boolean destroyed=false;

    public static void setAppVisible(boolean visible){}
    public static void start(Context c){c.getSharedPreferences("civimoto_driver_runtime",Context.MODE_PRIVATE).edit().putBoolean("online",true).apply();Intent i=new Intent(c,DriverAlertService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
    public static void stop(Context c){c.getSharedPreferences("civimoto_driver_runtime",Context.MODE_PRIVATE).edit().putBoolean("online",false).apply();c.stopService(new Intent(c,DriverAlertService.class));}
    public static void ignoreOffer(Context c,String id){if(id==null)return;Intent i=new Intent(c,DriverAlertService.class).setAction("IGNORE").putExtra("id",id);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}

    @Override public void onCreate(){
        super.onCreate();destroyed=false;backend=new Backend(this);alert=new CiviAlert(this);createSilentServiceChannel();acquireLocks();
        startForeground(7301,foregroundNotification("CiviMoto Conductor en línea","Esperando solicitudes de viaje"));
        startLocationTracking();handler.post(poller);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE).edit().putBoolean("online",true).apply();
        if(intent!=null&&"IGNORE".equals(intent.getAction())){String id=intent.getStringExtra("id");if(id!=null)ignored.add(id);stopOfferAlarm();if(id!=null&&id.equals(lastOfferId))lastOfferId=null;updateForeground("Solicitud rechazada. Esperando nuevos viajes");}
        return START_STICKY;
    }

    private void createSilentServiceChannel(){if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);NotificationChannel ch=new NotificationChannel(CHANNEL,"CiviMoto conductor en línea",NotificationManager.IMPORTANCE_LOW);ch.setDescription("Mantiene al conductor conectado incluso con la pantalla apagada");ch.enableVibration(false);ch.setSound(null,null);ch.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);nm.createNotificationChannel(ch);}}
    private Notification foregroundNotification(String title,String text){Intent open=getPackageManager().getLaunchIntentForPackage(getPackageName());if(open==null)open=new Intent(this,DocumentNavigationActivity.class);open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent pi=PendingIntent.getActivity(this,7301,open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);b.setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle(title).setContentText(text).setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).setPriority(Notification.PRIORITY_LOW).setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PRIVATE);if(Build.VERSION.SDK_INT<26)b.setSound(null).setVibrate(new long[]{0});return b.build();}
    private void updateForeground(String text){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)nm.notify(7301,foregroundNotification("CiviMoto Conductor en línea",text));}

    private void acquireLocks(){
        try{PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CiviMoto:DriverBackgroundLock");wakeLock.setReferenceCounted(false);wakeLock.acquire();}catch(Exception ignored){}
        try{WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);if(wm!=null){wifiLock=wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,"CiviMoto:DriverWifiLock");wifiLock.setReferenceCounted(false);wifiLock.acquire();}}catch(Exception ignored){}
    }

    private void startLocationTracking(){
        try{
            locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);if(locationManager==null)return;
            if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;
            Location last=null;try{last=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);}catch(Exception ignored){}if(last==null)try{last=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);}catch(Exception ignored){}if(last!=null)sendLocation(last);
            try{locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000L,5f,locationListener,Looper.getMainLooper());}catch(Exception ignored){}
            try{locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,8000L,10f,locationListener,Looper.getMainLooper());}catch(Exception ignored){}
        }catch(Exception ignored){}
    }

    private final LocationListener locationListener=new LocationListener(){public void onLocationChanged(Location l){if(l!=null)sendLocation(l);}public void onStatusChanged(String p,int s,Bundle b){}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}};
    private void sendLocation(Location l){try{if(backend!=null&&backend.hasSession())backend.rpc("driver_update_live_location",new JSONObject().put("p_lat",l.getLatitude()).put("p_lng",l.getLongitude()).put("p_heading",l.hasBearing()?l.getBearing():0).put("p_speed",l.hasSpeed()?l.getSpeed():0),new Backend.Callback(){public void ok(Object x){}public void error(String m){}});}catch(Exception ignored){}}
    private void stopOfferAlarm(){if(alert!=null)alert.stopOffer();}

    private final Runnable poller=new Runnable(){public void run(){
        if(destroyed)return;
        if(!getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE).getBoolean("online",false)){stopOfferAlarm();stopSelf();return;}
        if(backend==null||!backend.hasSession()){stopOfferAlarm();lastOfferId=null;updateForeground("Esperando inicio de sesión");handler.postDelayed(this,4000);return;}
        try{backend.rpc("driver_available_trips",new JSONObject().put("p_radius_km",20),new Backend.Callback(){
            public void ok(Object value){JSONObject trip=Backend.firstObject(value);if(trip==null){lastOfferId=null;stopOfferAlarm();updateForeground("Esperando solicitudes de viaje");return;}String id=trip.optString("id","");String status=trip.optString("status","");if(id.isEmpty()||!"solicitado".equals(status)||ignored.contains(id)){if(id.equals(lastOfferId))lastOfferId=null;stopOfferAlarm();updateForeground("Esperando solicitudes de viaje");return;}boolean fresh=!id.equals(lastOfferId);lastOfferId=id;String route=trip.optString("origin_address","Origen")+" → "+trip.optString("destination_address","Destino");updateForeground("Nueva solicitud: "+route);alert.startOffer("Nueva solicitud CiviMoto",route);if(fresh){Intent event=new Intent(ACTION_NEW_OFFER);event.setPackage(getPackageName());event.putExtra("trip_id",id);sendBroadcast(event);}}
            public void error(String message){if(lastOfferId==null||lastOfferId.isEmpty())updateForeground("Conectando con central…");}
        });}catch(Exception ignored){}
        handler.postDelayed(this,1800);
    }};

    private void scheduleRestart(){
        if(!getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE).getBoolean("online",false))return;
        try{AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);Intent restart=new Intent(this,DriverAlertService.class);int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);PendingIntent pi=Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(this,7302,restart,flags):PendingIntent.getService(this,7302,restart,flags);long at=SystemClock.elapsedRealtime()+2000;if(am!=null){if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,pi);else am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,pi);}}catch(Exception ignored){}
    }
    @Override public void onTaskRemoved(Intent rootIntent){scheduleRestart();super.onTaskRemoved(rootIntent);}
    @Override public void onDestroy(){destroyed=true;handler.removeCallbacksAndMessages(null);if(alert!=null)alert.release();try{if(locationManager!=null)locationManager.removeUpdates(locationListener);}catch(Exception ignored){}try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}try{if(wifiLock!=null&&wifiLock.isHeld())wifiLock.release();}catch(Exception ignored){}scheduleRestart();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}
