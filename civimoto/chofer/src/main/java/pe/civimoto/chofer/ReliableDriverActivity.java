package pe.civimoto.chofer;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import java.lang.reflect.Field;
import java.util.HashSet;

/** Activa recepción fiable de viajes cuando el teléfono queda en segundo plano. */
public class ReliableDriverActivity extends DocumentNavigationActivity {
    private boolean askedBattery=false;

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private void field(String name,Object value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}catch(Exception ignored){}}

    @Override public void onCreate(Bundle b){super.onCreate(b);}

    @Override protected void onResume(){
        super.onResume();
        boolean online=getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE).getBoolean("online",false);
        if(online)ReliableDriverAlertService.start(this);
    }

    @Override protected void onPause(){
        boolean online=getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE).getBoolean("online",false);
        if(online)ReliableDriverAlertService.start(this);
        super.onPause();
    }

    @Override void screenLogin(){ReliableDriverAlertService.stop(this);super.screenLogin();}
    @Override void screenRegister(){ReliableDriverAlertService.stop(this);super.screenRegister();}

    @Override void screenDashboard(){
        super.screenDashboard();
        Switch sw=findSwitch(findViewById(android.R.id.content));
        if(sw!=null){
            sw.setOnCheckedChangeListener((v,on)->{
                field("online",on);sw.setText(on?"En línea":"Fuera de línea");setAvailability(on);
                if(on){freshLocation();startRealtime();startPoller();ReliableDriverAlertService.start(this);requestBatteryExemptionOnce();}
                else{ReliableDriverAlertService.stop(this);DriverAlertService.stop(this);}
            });
        }
    }

    @Override void screenOffer(){
        super.screenOffer();
        Button reject=findButton(findViewById(android.R.id.content),"Rechazar");
        Button accept=findButton(findViewById(android.R.id.content),"Aceptar viaje");
        if(reject!=null){
            reject.setOnClickListener(v->{
                String id=(String)field("offeredTripId");
                if(id!=null){Object r=field("rejected");if(r instanceof HashSet)((HashSet<String>)r).add(id);ReliableDriverAlertService.ignoreOffer(this,id);DriverAlertService.ignoreOffer(this,id);}
                Object a=field("alerts");if(a instanceof CiviAlert)((CiviAlert)a).stopOffer();
                field("offeredTripId",null);
                Object txt=field("offerText");if(txt instanceof android.widget.TextView)((android.widget.TextView)txt).setText("Solicitud rechazada. Esperando otra solicitud…");
                if(accept!=null)accept.setEnabled(false);reject.setEnabled(false);
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{if(accept!=null)loadOffers(accept,reject);},900);
            });
        }
    }

    private void requestBatteryExemptionOnce(){
        if(askedBattery)return;askedBattery=true;
        try{
            if(Build.VERSION.SDK_INT>=23){
                PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
                if(pm!=null&&!pm.isIgnoringBatteryOptimizations(getPackageName())){
                    Intent i=new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:"+getPackageName()));
                    startActivity(i);
                }
            }
        }catch(Exception ignored){}
    }

    private Switch findSwitch(View v){
        if(v instanceof Switch)return(Switch)v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Switch s=findSwitch(g.getChildAt(i));if(s!=null)return s;}}
        return null;
    }
    private Button findButton(View v,String text){
        if(v instanceof Button&&text.equals(((Button)v).getText().toString()))return(Button)v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button b=findButton(g.getChildAt(i),text);if(b!=null)return b;}}
        return null;
    }
}
