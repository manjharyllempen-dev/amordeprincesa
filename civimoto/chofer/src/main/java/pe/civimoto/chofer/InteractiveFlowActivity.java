package pe.civimoto.chofer;

import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.*;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.HashSet;

public class InteractiveFlowActivity extends FlowActivity {
    private final int GOLD_ON=Color.rgb(255,195,0),GOLD_PRESS=Color.rgb(205,145,0),GOLD_DISABLED=Color.rgb(120,92,20),BLACK=Color.rgb(5,6,8),MUTED=Color.rgb(176,180,190),GOLD2=Color.rgb(255,220,90);
    private final Handler autoHandler=new Handler(Looper.getMainLooper());
    private boolean receiverRegistered=false,offerScreenOpen=false,checkingOffer=false;

    private GradientDrawable round(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(16));g.setStroke(dp(1),Color.rgb(255,218,80));return g;}
    private StateListDrawable selector(){StateListDrawable s=new StateListDrawable();s.addState(new int[]{-android.R.attr.state_enabled},round(GOLD_DISABLED));s.addState(new int[]{android.R.attr.state_pressed},round(GOLD_PRESS));s.addState(new int[]{android.R.attr.state_selected},round(GOLD_PRESS));s.addState(new int[]{},round(GOLD_ON));return s;}
    @Override Button btn(String text,boolean primary){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(null,1);b.setTextColor(Color.BLACK);b.setBackground(selector());b.setElevation(dp(3));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(lp);return b;}

    private Object getField(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private void setField(String name,Object value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}catch(Exception ignored){}}
    private boolean getBool(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.getBoolean(this);}catch(Exception e){return false;}}
    private double getDouble(String name,double fallback){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.getDouble(this);}catch(Exception e){return fallback;}}
    private Backend backend(){return (Backend)getField("backend");}
    private LinearLayout body(){return (LinearLayout)getField("body");}

    @Override public void onCreate(Bundle b){super.onCreate(b);}
    @Override protected void onStart(){super.onStart();if(!receiverRegistered){IntentFilter f=new IntentFilter(DriverAlertService.ACTION_NEW_OFFER);if(Build.VERSION.SDK_INT>=33)registerReceiver(offerReceiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(offerReceiver,f);receiverRegistered=true;}}
    @Override protected void onStop(){if(receiverRegistered){try{unregisterReceiver(offerReceiver);}catch(Exception ignored){}receiverRegistered=false;}super.onStop();}
    @Override protected void onResume(){super.onResume();DriverAlertService.setAppVisible(true);}
    @Override protected void onPause(){DriverAlertService.setAppVisible(false);super.onPause();}
    @Override protected void onDestroy(){autoHandler.removeCallbacks(autoWatcher);super.onDestroy();}

    private final BroadcastReceiver offerReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(DriverAlertService.ACTION_NEW_OFFER.equals(i.getAction())&&getBool("online")&&!offerScreenOpen&&getField("activeTripId")==null)screenOffer();}};

    @Override void shell(String title,String subtitle,int step){
        stopPoller();ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(14),dp(14),dp(70));root.setBackgroundColor(BLACK);sc.addView(root);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(dp(4),dp(3),dp(4),dp(8));head.addView(logo(58));TextView brand=tx("CiviMoto Conductor",27,Color.WHITE,true);brand.setPadding(dp(12),0,0,0);head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));root.addView(head);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,dp(8),0,0);root.addView(content);setField("body",content);setContentView(sc);
    }

    @Override void screenDashboard(){
        offerScreenOpen=false;super.screenDashboard();LinearLayout b=body();if(b==null)return;
        Button services=findButton(b,"Ver solicitudes disponibles");if(services!=null){View parent=(View)services.getParent();if(parent instanceof LinearLayout)((LinearLayout)parent).removeView(services);}
        LinearLayout info=card();info.addView(tx("Solicitudes automáticas",20,Color.WHITE,true));info.addView(tx("Cuando estés en línea, CiviMoto mostrará los viajes automáticamente. Con la pantalla apagada, el servicio seguirá escuchando y sonará la alerta.",13,MUTED,false));b.addView(info,Math.min(2,b.getChildCount()));
        Switch sw=findSwitch(b);if(sw!=null){sw.setOnCheckedChangeListener((v,on)->{setField("online",on);sw.setText(on?"En línea":"Fuera de línea");setAvailability(on);if(on){freshLocation();DriverAlertService.start(this);startRealtime();startPoller();autoHandler.removeCallbacks(autoWatcher);autoHandler.post(autoWatcher);}else{DriverAlertService.stop(this);autoHandler.removeCallbacks(autoWatcher);}});}
        freshLocation();centerCurrentMap();autoHandler.removeCallbacks(autoWatcher);autoHandler.post(autoWatcher);
    }

    @Override void screenOffer(){offerScreenOpen=true;super.screenOffer();Button accept=findButton(body(),"Aceptar viaje"),reject=findButton(body(),"Rechazar");if(reject!=null){reject.setOnClickListener(v->{String id=(String)getField("offeredTripId");if(id!=null){Object r=getField("rejected");if(r instanceof HashSet)((HashSet<String>)r).add(id);DriverAlertService.ignoreOffer(this,id);}CiviAlert a=(CiviAlert)getField("alerts");if(a!=null)a.stopOffer();setField("offeredTripId",null);TextView txt=(TextView)getField("offerText");if(txt!=null)txt.setText("Solicitud rechazada. Esperando otra solicitud…");if(accept!=null)accept.setEnabled(false);reject.setEnabled(false);autoHandler.postDelayed(()->{if(accept!=null)loadOffers(accept,reject);},900);});}freshLocation();centerCurrentMap();}
    @Override void screenTrip(){offerScreenOpen=false;super.screenTrip();freshLocation();centerCurrentMap();}
    @Override void screenEarnings(){offerScreenOpen=false;super.screenEarnings();}
    @Override void screenLogin(){offerScreenOpen=false;DriverAlertService.stop(this);autoHandler.removeCallbacks(autoWatcher);super.screenLogin();}

    private final Runnable autoWatcher=new Runnable(){public void run(){if(getBool("online")&&!offerScreenOpen&&getField("activeTripId")==null&&!checkingOffer&&backend()!=null&&backend().hasSession()){checkingOffer=true;try{backend().rpc("driver_available_trips",new JSONObject().put("p_radius_km",7),new Backend.Callback(){public void ok(Object x){checkingOffer=false;JSONObject t=Backend.firstObject(x);if(t!=null&&!offerScreenOpen&&getBool("online"))screenOffer();}public void error(String m){checkingOffer=false;}});}catch(Exception e){checkingOffer=false;}}autoHandler.postDelayed(this,3500);}};

    private Button findButton(View root,String text){if(root instanceof Button&&text.equals(((Button)root).getText().toString()))return(Button)root;if(root instanceof LinearLayout){LinearLayout l=(LinearLayout)root;for(int i=0;i<l.getChildCount();i++){Button b=findButton(l.getChildAt(i),text);if(b!=null)return b;}}if(root instanceof ScrollView&&((ScrollView)root).getChildCount()>0)return findButton(((ScrollView)root).getChildAt(0),text);return null;}
    private Switch findSwitch(View root){if(root instanceof Switch)return(Switch)root;if(root instanceof LinearLayout){LinearLayout l=(LinearLayout)root;for(int i=0;i<l.getChildCount();i++){Switch s=findSwitch(l.getChildAt(i));if(s!=null)return s;}}return null;}
    private void centerCurrentMap(){autoHandler.postDelayed(()->{WebView m=(WebView)getField("map");if(m!=null)m.evaluateJavascript("setMe("+getDouble("lng",-77.0428)+","+getDouble("lat",-12.0464)+")",null);},700);}
}
