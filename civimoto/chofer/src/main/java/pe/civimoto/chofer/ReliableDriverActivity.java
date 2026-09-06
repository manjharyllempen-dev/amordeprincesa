package pe.civimoto.chofer;

import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.*;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.HashSet;

/** Une el servicio de fondo con la interfaz usando el mismo trip_id pendiente. */
public class ReliableDriverActivity extends DocumentNavigationActivity {
    private final Handler ui=new Handler(Looper.getMainLooper());

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private void field(String name,Object value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}catch(Exception ignored){}}
    private boolean bool(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.getBoolean(this);}catch(Exception e){return false;}}
    private Backend backend(){return (Backend)field("backend");}
    private LinearLayout body(){return (LinearLayout)field("body");}

    @Override public void onCreate(Bundle b){super.onCreate(b);ui.postDelayed(this::openPendingIfReady,1800);}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);ui.postDelayed(this::openPendingIfReady,250);}
    @Override protected void onResume(){super.onResume();boolean online=getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE).getBoolean("online",false);if(online)DriverAlertService.start(this);ui.postDelayed(this::openPendingIfReady,500);}
    @Override protected void onPause(){if(getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE).getBoolean("online",false))DriverAlertService.start(this);super.onPause();}

    @Override void screenLogin(){DriverAlertService.stop(this);super.screenLogin();}
    @Override void screenRegister(){DriverAlertService.stop(this);super.screenRegister();}

    @Override void screenDashboard(){
        super.screenDashboard();
        Switch sw=findSwitch(findViewById(android.R.id.content));
        if(sw!=null){sw.setOnCheckedChangeListener((v,on)->{
            field("online",on);sw.setText(on?"En línea":"Fuera de línea");setAvailability(on);
            if(on){freshLocation();DriverAlertService.start(this);startRealtime();startPoller();requestBatteryExemption();ui.postDelayed(this::openPendingIfReady,500);}
            else DriverAlertService.stop(this);
        });}
        ui.postDelayed(()->{if(getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE).getBoolean("online",false)){DriverAlertService.start(this);requestBatteryExemption();openPendingIfReady();}},1200);
    }

    @Override void screenOffer(){super.screenOffer();ui.postDelayed(this::bindPendingOffer,220);}

    private void openPendingIfReady(){
        Backend b=backend();if(b==null||!b.hasSession())return;
        android.content.SharedPreferences p=getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE);
        String id=p.getString("pending_offer_id",null);if(id==null||id.isEmpty())return;
        Object active=field("activeTripId");if(active instanceof String&&!((String)active).isEmpty())return;
        if(!bool("online")&&!p.getBoolean("online",false))return;
        String current=field("offeredTripId") instanceof String?(String)field("offeredTripId"):null;
        if(!id.equals(current))screenOffer();else bindPendingOffer();
    }

    private void bindPendingOffer(){
        android.content.SharedPreferences p=getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE);
        String id=p.getString("pending_offer_id",null),json=p.getString("pending_offer_json",null);if(id==null||id.isEmpty())return;
        JSONObject trip=null;try{if(json!=null)trip=new JSONObject(json);}catch(Exception ignored){}
        field("offeredTripId",id);
        Button accept=findButton(body(),"Aceptar viaje"),reject=findButton(body(),"Rechazar");TextView offer=(TextView)field("offerText");
        if(trip!=null){
            if(offer!=null)offer.setText("Origen: "+trip.optString("origin_address","—")+"\nDestino: "+trip.optString("destination_address","—")+"\nDistancia: "+trip.optString("estimated_distance_km","—")+" km\nTarifa: S/ "+trip.optString("estimated_fare","—")+" · "+trip.optString("payment_method","efectivo").toUpperCase());
            WebView map=(WebView)field("map");if(map!=null)map.evaluateJavascript("showOffer("+trip.optDouble("origin_lng")+","+trip.optDouble("origin_lat")+","+trip.optDouble("destination_lng")+","+trip.optDouble("destination_lat")+")",null);
        }
        if(accept!=null){accept.setEnabled(true);accept.setOnClickListener(v->acceptPending(id,accept,reject));}
        if(reject!=null){reject.setEnabled(true);reject.setOnClickListener(v->rejectPending(id));}
    }

    private void acceptPending(String id,Button accept,Button reject){
        accept.setEnabled(false);if(reject!=null)reject.setEnabled(false);
        try{backend().rpc("accept_civimoto_trip",new JSONObject().put("p_trip_id",id),new Backend.Callback(){
            public void ok(Object x){JSONObject t=Backend.firstObject(x);if(t==null){accept.setEnabled(true);if(reject!=null)reject.setEnabled(true);toast("La solicitud ya no está disponible.");DriverAlertService.resolveOffer(ReliableDriverActivity.this,id);return;}DriverAlertService.resolveOffer(ReliableDriverActivity.this,id);field("activeTripId",t.optString("id",id));field("tripStatus",t.optString("status","aceptado"));field("offeredTripId",null);transition("chofer_en_camino",ReliableDriverActivity.this::screenTrip);}
            public void error(String m){accept.setEnabled(true);if(reject!=null)reject.setEnabled(true);toast("No se pudo aceptar: "+m);}
        });}catch(Exception e){accept.setEnabled(true);if(reject!=null)reject.setEnabled(true);toast("No se pudo aceptar el viaje.");}
    }

    @SuppressWarnings("unchecked") private void rejectPending(String id){
        try{Object r=field("rejected");if(r instanceof HashSet)((HashSet<String>)r).add(id);}catch(Exception ignored){}
        DriverAlertService.ignoreOffer(this,id);field("offeredTripId",null);toast("Solicitud rechazada.");screenDashboard();
    }

    private void requestBatteryExemption(){
        if(Build.VERSION.SDK_INT<23)return;
        try{PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);if(pm!=null&&pm.isIgnoringBatteryOptimizations(getPackageName()))return;android.content.SharedPreferences sp=getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE);if(sp.getBoolean("battery_prompted_v2",false))return;sp.edit().putBoolean("battery_prompted_v2",true).apply();Intent i=new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName()));startActivity(i);toast("Permite CiviMoto sin restricción de batería para recibir viajes con la pantalla apagada.");}catch(Exception ignored){}
    }

    private Switch findSwitch(View v){if(v instanceof Switch)return(Switch)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Switch s=findSwitch(g.getChildAt(i));if(s!=null)return s;}}return null;}
    private Button findButton(View v,String text){if(v==null)return null;if(v instanceof Button&&text.equals(((Button)v).getText().toString()))return(Button)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button b=findButton(g.getChildAt(i),text);if(b!=null)return b;}}return null;}
}
