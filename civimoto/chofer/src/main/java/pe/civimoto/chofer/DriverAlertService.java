package pe.civimoto.chofer;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.JSONObject;
import java.util.HashSet;

public class DriverAlertService extends Service {
    public static final String ACTION_NEW_OFFER="pe.civimoto.chofer.NEW_OFFER";
    private static final String CHANNEL="civimoto_driver_online";
    private static volatile boolean appVisible=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final HashSet<String> ignored=new HashSet<>();
    private Backend backend; private CiviAlert alert; private PowerManager.WakeLock wakeLock; private String lastOfferId;

    public static void setAppVisible(boolean visible){appVisible=visible;}
    public static void start(Context c){Intent i=new Intent(c,DriverAlertService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
    public static void stop(Context c){c.stopService(new Intent(c,DriverAlertService.class));}
    public static void ignoreOffer(Context c,String id){if(id==null)return;Intent i=new Intent(c,DriverAlertService.class).setAction("IGNORE").putExtra("id",id);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}

    @Override public void onCreate(){super.onCreate();backend=new Backend(this);alert=new CiviAlert(this);createChannel();acquireWakeLock();startForeground(7301,foregroundNotification("CiviMoto Conductor en línea","Esperando solicitudes de viaje"));handler.post(poller);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){if(intent!=null&&"IGNORE".equals(intent.getAction())){String id=intent.getStringExtra("id");if(id!=null)ignored.add(id);alert.stopOffer();lastOfferId=null;}return START_STICKY;}

    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);NotificationChannel ch=new NotificationChannel(CHANNEL,"CiviMoto conductor en línea",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("Mantiene activas las solicitudes de viaje incluso con la pantalla apagada");ch.enableVibration(true);nm.createNotificationChannel(ch);}}
    private Notification foregroundNotification(String title,String text){Intent open=new Intent(this,InteractiveFlowActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent pi=PendingIntent.getActivity(this,7301,open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);b.setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle(title).setContentText(text).setContentIntent(pi).setOngoing(true).setPriority(Notification.PRIORITY_HIGH).setCategory(Notification.CATEGORY_SERVICE);return b.build();}
    private void updateForeground(String text){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)nm.notify(7301,foregroundNotification("CiviMoto Conductor en línea",text));}
    private void acquireWakeLock(){try{PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CiviMoto:DriverOfferLock");wakeLock.setReferenceCounted(false);wakeLock.acquire();}catch(Exception ignored){}}

    private final Runnable poller=new Runnable(){public void run(){if(backend==null||!backend.hasSession()){alert.stopOffer();handler.postDelayed(this,7000);return;}try{backend.rpc("driver_available_trips",new JSONObject().put("p_radius_km",7),new Backend.Callback(){public void ok(Object value){JSONObject trip=Backend.firstObject(value);if(trip==null){lastOfferId=null;alert.stopOffer();updateForeground("Esperando solicitudes de viaje");return;}String id=trip.optString("id","");if(id.isEmpty()||ignored.contains(id)){alert.stopOffer();return;}boolean fresh=!id.equals(lastOfferId);lastOfferId=id;String route=trip.optString("origin_address","Origen")+" → "+trip.optString("destination_address","Destino");updateForeground("Nueva solicitud: "+route);if(!appVisible)alert.startOffer("Nueva solicitud CiviMoto",route);else alert.stopOffer();if(fresh){Intent event=new Intent(ACTION_NEW_OFFER);event.setPackage(getPackageName());event.putExtra("trip_id",id);sendBroadcast(event);}}public void error(String message){updateForeground("Conectando con central…");}});}catch(Exception ignored){}handler.postDelayed(this,4000);}};

    @Override public void onDestroy(){handler.removeCallbacks(poller);if(alert!=null)alert.release();try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}
