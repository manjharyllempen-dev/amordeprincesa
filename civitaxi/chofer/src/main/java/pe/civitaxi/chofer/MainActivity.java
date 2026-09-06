package pe.civimoto.chofer;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

public class MainActivity extends Activity {
    private static final int OFFLINE=0, AVAILABLE=1, OFFERED=2, TO_PICKUP=3, ARRIVED=4, IN_RIDE=5, COMPLETED=6;
    private final int gold=Color.rgb(255,190,0), bg=Color.rgb(8,8,10), surface=Color.rgb(24,24,28), muted=Color.rgb(170,170,178);
    private SharedPreferences prefs;
    private TextView state, tripState, payStatus, earningsText;
    private WebView map;
    private Switch onlineSwitch;
    private Button accept,reject,arrived,start,finish;
    private int driverState=OFFLINE;
    private double earnings=65.50;
    private int rides=12;

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView text(String s,int sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,dp(4),0,dp(4));if(b)v.setTypeface(null,1);return v;}
    Button button(String s){Button x=new Button(this);x.setText(s);x.setTextColor(Color.BLACK);x.setTypeface(null,1);x.setBackground(box(gold,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(6),0,dp(6));x.setLayoutParams(lp);return x;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(box(surface,20));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));c.setLayoutParams(lp);return c;}
    ImageView logo(int size){ImageView i=new ImageView(this);i.setImageResource(R.drawable.logo_civimoto);i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return i;}

    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("civimoto_chofer",MODE_PRIVATE);driverState=prefs.getInt("driver_state",OFFLINE);earnings=Double.longBitsToDouble(prefs.getLong("earnings",Double.doubleToRawLongBits(65.50)));rides=prefs.getInt("rides",12);if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},30);showSplash();}

    void showSplash(){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setGravity(Gravity.CENTER);s.setBackgroundColor(Color.BLACK);s.addView(logo(230));TextView brand=text("CiviMoto",44,Color.WHITE,true);brand.setGravity(Gravity.CENTER);s.addView(brand);TextView sub=text("Chofer · Conduce tu futuro",17,gold,true);sub.setGravity(Gravity.CENTER);s.addView(sub);setContentView(s);new Handler(Looper.getMainLooper()).postDelayed(this::build,1100);}

    void build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(80));root.setBackgroundColor(bg);sc.addView(root);setContentView(sc);
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.addView(logo(62));LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.setPadding(dp(12),0,0,0);ht.addView(text("CiviMoto",30,Color.WHITE,true));ht.addView(text("Chofer · Tu esfuerzo, más oportunidades",13,muted,false));h.addView(ht);root.addView(h);

        LinearLayout onlineCard=card();onlineCard.addView(text("Estado del chofer",20,Color.WHITE,true));onlineSwitch=new Switch(this);onlineSwitch.setTextColor(Color.WHITE);onlineCard.addView(onlineSwitch);state=text("",14,muted,false);onlineCard.addView(state);root.addView(onlineCard);
        map=createMap();root.addView(map,new LinearLayout.LayoutParams(-1,dp(290)));

        LinearLayout service=card();service.addView(text("Solicitud disponible",21,Color.WHITE,true));service.addView(text("📍 Origen",13,muted,true));service.addView(text("Av. Central 123",18,Color.WHITE,true));service.addView(text("🏁 Destino",13,muted,true));service.addView(text("Plaza Mayor",18,Color.WHITE,true));service.addView(text("Distancia: 2.1 km · Tarifa: S/ 5.50",16,gold,true));LinearLayout row=new LinearLayout(this);reject=button("Rechazar");accept=button("Aceptar");row.addView(reject,new LinearLayout.LayoutParams(0,dp(52),1));row.addView(accept,new LinearLayout.LayoutParams(0,dp(52),1));service.addView(row);root.addView(service);

        LinearLayout trip=card();trip.addView(text("Viaje en curso",20,Color.WHITE,true));tripState=text("",14,muted,false);trip.addView(tripState);arrived=button("Llegué al origen");start=button("Iniciar viaje");finish=button("Finalizar viaje");trip.addView(arrived);trip.addView(start);trip.addView(finish);root.addView(trip);

        LinearLayout earningsCard=card();earningsCard.addView(text("Mis ganancias",20,Color.WHITE,true));earningsText=text("",28,gold,true);earningsCard.addView(earningsText);earningsCard.addView(text("Calificación 4.9 ⭐ · seguimiento local de prueba",14,muted,false));root.addView(earningsCard);

        LinearLayout payment=card();payment.addView(text("Pago a Civimoto",20,Color.WHITE,true));payment.addView(text("Pago operativo por Yape: semanal o mensual.",14,muted,false));LinearLayout prow=new LinearLayout(this);Button weekly=button("Semanal"),monthly=button("Mensual");prow.addView(weekly,new LinearLayout.LayoutParams(0,dp(52),1));prow.addView(monthly,new LinearLayout.LayoutParams(0,dp(52),1));payment.addView(prow);payStatus=text("",14,muted,false);payment.addView(payStatus);root.addView(payment);

        onlineSwitch.setOnCheckedChangeListener((v,on)->{if(on){if(driverState==OFFLINE||driverState==COMPLETED)setState(AVAILABLE);map.evaluateJavascript("locateMe()",null);}else{if(driverState==TO_PICKUP||driverState==ARRIVED||driverState==IN_RIDE){Toast.makeText(this,"Finaliza o cancela el viaje antes de desconectarte",Toast.LENGTH_SHORT).show();onlineSwitch.setChecked(true);return;}setState(OFFLINE);}});
        accept.setOnClickListener(v->{if(driverState!=AVAILABLE&&driverState!=OFFERED){Toast.makeText(this,"Debes estar disponible",Toast.LENGTH_SHORT).show();return;}setState(TO_PICKUP);map.evaluateJavascript("demoRoute()",null);});
        reject.setOnClickListener(v->{if(driverState==AVAILABLE||driverState==OFFERED){Toast.makeText(this,"Servicio rechazado",Toast.LENGTH_SHORT).show();setState(AVAILABLE);}});
        arrived.setOnClickListener(v->{if(driverState==TO_PICKUP)setState(ARRIVED);});
        start.setOnClickListener(v->{if(driverState==ARRIVED)setState(IN_RIDE);});
        finish.setOnClickListener(v->{if(driverState==IN_RIDE){earnings+=5.50;rides++;prefs.edit().putLong("earnings",Double.doubleToRawLongBits(earnings)).putInt("rides",rides).apply();setState(COMPLETED);new Handler(Looper.getMainLooper()).postDelayed(()->{if(driverState==COMPLETED)setState(AVAILABLE);},1200);}});
        weekly.setOnClickListener(v->{prefs.edit().putString("payment_plan","Semanal").apply();refresh();});
        monthly.setOnClickListener(v->{prefs.edit().putString("payment_plan","Mensual").apply();refresh();});
        refresh();
    }

    void setState(int s){driverState=s;prefs.edit().putInt("driver_state",s).apply();refresh();}
    String stateName(){switch(driverState){case AVAILABLE:return"DISPONIBLE";case OFFERED:return"NUEVA SOLICITUD";case TO_PICKUP:return"EN CAMINO AL PASAJERO";case ARRIVED:return"LLEGUÉ AL ORIGEN";case IN_RIDE:return"EN VIAJE";case COMPLETED:return"VIAJE COMPLETADO";default:return"FUERA DE LÍNEA";}}
    void refresh(){if(state==null)return;boolean online=driverState!=OFFLINE;onlineSwitch.setChecked(online);onlineSwitch.setText(online?"En línea":"Fuera de línea");state.setText("Estado: "+stateName()+" · GPS "+(online?"activo":"inactivo"));tripState.setText(driverState==TO_PICKUP?"Dirígete a Av. Central 123":driverState==ARRIVED?"Espera al pasajero y confirma inicio":driverState==IN_RIDE?"Destino: Plaza Mayor · tarifa S/ 5.50":driverState==COMPLETED?"✓ Viaje finalizado. Retomando disponibilidad…":"Sin viaje activo.");accept.setEnabled(driverState==AVAILABLE||driverState==OFFERED);reject.setEnabled(driverState==AVAILABLE||driverState==OFFERED);arrived.setEnabled(driverState==TO_PICKUP);start.setEnabled(driverState==ARRIVED);finish.setEnabled(driverState==IN_RIDE);earningsText.setText(String.format(java.util.Locale.US,"Hoy: S/ %.2f\nViajes: %d",earnings,rides));String plan=prefs.getString("payment_plan","Sin seleccionar");payStatus.setText("Plan elegido: "+plan+" · Método Yape · pendiente de aprobación administrativa.");}

    WebView createMap(){WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setGeolocationEnabled(true);w.setWebViewClient(new WebViewClient());w.setWebChromeClient(new WebChromeClient(){@Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb){cb.invoke(origin,true,false);}});String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'><link href='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.css' rel='stylesheet'><style>html,body,#map{margin:0;width:100%;height:100%;background:#17171b}</style></head><body><div id='map'></div><script src='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.js'></script><script>let map=new maplibregl.Map({container:'map',style:'https://tiles.openfreemap.org/styles/liberty',center:[-77.0428,-12.0464],zoom:13});let me;function locateMe(){navigator.geolocation.getCurrentPosition(p=>{let c=[p.coords.longitude,p.coords.latitude];if(me)me.remove();me=new maplibregl.Marker({color:'#ffbe00'}).setLngLat(c).addTo(map);map.flyTo({center:c,zoom:15});},()=>{});}function demoRoute(){let a=me?me.getLngLat().toArray():[-77.0428,-12.0464],b=[a[0]+0.018,a[1]+0.012];new maplibregl.Marker({color:'#20c878'}).setLngLat(b).addTo(map);let src=map.getSource('r');let d={type:'Feature',geometry:{type:'LineString',coordinates:[a,b]}};if(src)src.setData(d);else{map.addSource('r',{type:'geojson',data:d});map.addLayer({id:'r',type:'line',source:'r',paint:{'line-color':'#ffbe00','line-width':6}});}map.fitBounds([a,b],{padding:45});}map.on('load',locateMe);</script></body></html>";w.loadDataWithBaseURL("https://driver.civimoto.local/",html,"text/html","UTF-8",null);return w;}
}
