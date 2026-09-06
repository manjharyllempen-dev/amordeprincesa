package pe.civimoto.usuario;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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
    private static final int IDLE=0, SEARCHING=1, DRIVER_ASSIGNED=2, DRIVER_ARRIVING=3, IN_RIDE=4, COMPLETED=5, CANCELLED=6;
    private final int gold=Color.rgb(255,190,0), bg=Color.rgb(8,8,10), surface=Color.rgb(24,24,28), muted=Color.rgb(170,170,178), green=Color.rgb(45,205,120);
    private SharedPreferences prefs;
    private TextView rideStatus, driverInfo, historyText;
    private Button requestBtn, advanceBtn, cancelBtn;
    private WebView map;
    private String payment="Efectivo";
    private int rideState=IDLE;

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView text(String s,int sp,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setPadding(0,dp(4),0,dp(4));if(b)t.setTypeface(null,1);return t;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.BLACK);b.setTypeface(null,1);b.setBackground(box(gold,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(6),0,dp(6));b.setLayoutParams(lp);return b;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(box(surface,20));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));c.setLayoutParams(lp);return c;}
    ImageView logo(int size){ImageView i=new ImageView(this);i.setImageResource(R.drawable.logo_civimoto);i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return i;}

    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("civimoto_pasajero",MODE_PRIVATE);payment=prefs.getString("payment","Efectivo");rideState=prefs.getInt("ride_state",IDLE);if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},20);showSplash();}

    void showSplash(){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setGravity(Gravity.CENTER);s.setPadding(dp(30),dp(30),dp(30),dp(30));s.setBackgroundColor(Color.BLACK);s.addView(logo(230));TextView brand=text("CiviMoto",44,Color.WHITE,true);brand.setGravity(Gravity.CENTER);s.addView(brand);TextView sub=text("Tu destino, nuestra ruta",17,gold,true);sub.setGravity(Gravity.CENTER);s.addView(sub);TextView role=text("PASAJERO",13,muted,true);role.setGravity(Gravity.CENTER);role.setPadding(0,dp(16),0,0);s.addView(role);setContentView(s);new Handler(Looper.getMainLooper()).postDelayed(this::build,1100);}

    void build(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(80));root.setBackgroundColor(bg);scroll.addView(root);setContentView(scroll);
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.addView(logo(62));LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.setPadding(dp(12),0,0,0);ht.addView(text("CiviMoto",30,Color.WHITE,true));ht.addView(text("Pasajero · Rápido · Seguro · Confiable",13,muted,false));h.addView(ht);root.addView(h);
        TextView hello=text("Hola 👋 ¿A dónde vamos?",25,Color.WHITE,true);hello.setPadding(0,dp(18),0,dp(8));root.addView(hello);
        map=createMap();root.addView(map,new LinearLayout.LayoutParams(-1,dp(300)));

        LinearLayout form=card();form.addView(text("Solicitar mototaxi",20,Color.WHITE,true));EditText origin=input("Origen",prefs.getString("origin","Mi ubicación actual"));EditText dest=input("Destino",prefs.getString("dest","Plaza de Armas"));form.addView(origin);form.addView(dest);form.addView(text("Método de pago",14,muted,true));LinearLayout payRow=new LinearLayout(this);Button cash=button("Efectivo"),yape=button("Yape");payRow.addView(cash,new LinearLayout.LayoutParams(0,dp(52),1));payRow.addView(yape,new LinearLayout.LayoutParams(0,dp(52),1));form.addView(payRow);form.addView(text("Tarifa referencial: S/ 5.50",18,gold,true));Button locate=button("📍 Usar mi ubicación");requestBtn=button("Solicitar CiviMoto");form.addView(locate);form.addView(requestBtn);rideStatus=text("",14,muted,false);form.addView(rideStatus);root.addView(form);

        LinearLayout active=card();active.addView(text("Seguimiento del viaje",20,Color.WHITE,true));driverInfo=text("",14,Color.WHITE,false);active.addView(driverInfo);advanceBtn=button("Avanzar estado de prueba");cancelBtn=button("Cancelar viaje");Button share=button("Compartir viaje");active.addView(advanceBtn);active.addView(share);active.addView(cancelBtn);root.addView(active);

        LinearLayout history=card();history.addView(text("Historial",20,Color.WHITE,true));historyText=text("",14,muted,false);history.addView(historyText);root.addView(history);

        cash.setOnClickListener(v->{payment="Efectivo";prefs.edit().putString("payment",payment).apply();refresh();});
        yape.setOnClickListener(v->{payment="Yape";prefs.edit().putString("payment",payment).apply();refresh();});
        locate.setOnClickListener(v->map.evaluateJavascript("locateMe()",null));
        requestBtn.setOnClickListener(v->{if(rideState!=IDLE&&rideState!=CANCELLED&&rideState!=COMPLETED)return;prefs.edit().putString("origin",origin.getText().toString()).putString("dest",dest.getText().toString()).apply();setState(SEARCHING);requestBtn.setEnabled(false);new Handler(Looper.getMainLooper()).postDelayed(()->{if(rideState==SEARCHING){setState(DRIVER_ASSIGNED);map.evaluateJavascript("demoRoute()",null);}},1600);new Handler(Looper.getMainLooper()).postDelayed(()->{if(rideState==DRIVER_ASSIGNED)setState(DRIVER_ARRIVING);},3400);});
        advanceBtn.setOnClickListener(v->{if(rideState==DRIVER_ASSIGNED)setState(DRIVER_ARRIVING);else if(rideState==DRIVER_ARRIVING)setState(IN_RIDE);else if(rideState==IN_RIDE){int trips=prefs.getInt("trips",0)+1;prefs.edit().putInt("trips",trips).apply();setState(COMPLETED);}else Toast.makeText(this,"No hay un viaje activo para avanzar",Toast.LENGTH_SHORT).show();});
        cancelBtn.setOnClickListener(v->{if(rideState==SEARCHING||rideState==DRIVER_ASSIGNED||rideState==DRIVER_ARRIVING||rideState==IN_RIDE)setState(CANCELLED);});
        share.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,"Estoy usando CiviMoto. Estado: "+stateName(rideState)+". Destino: "+prefs.getString("dest","Plaza de Armas")+". Comparte mi viaje por seguridad.");startActivity(Intent.createChooser(i,"Compartir viaje Civimoto"));});
        refresh();
    }

    void setState(int s){rideState=s;prefs.edit().putInt("ride_state",s).apply();refresh();}
    String stateName(int s){switch(s){case SEARCHING:return"BUSCANDO CHOFER";case DRIVER_ASSIGNED:return"CHOFER ASIGNADO";case DRIVER_ARRIVING:return"CHOFER EN CAMINO";case IN_RIDE:return"EN VIAJE";case COMPLETED:return"COMPLETADO";case CANCELLED:return"CANCELADO";default:return"LISTO";}}
    void refresh(){if(rideStatus==null)return;String msg;switch(rideState){case SEARCHING:msg="Buscando mototaxis cercanos… · Pago: "+payment;break;case DRIVER_ASSIGNED:msg="✓ Chofer encontrado. Preparando ruta…";break;case DRIVER_ARRIVING:msg="Carlos M. está en camino · llega aprox. en 4 min";break;case IN_RIDE:msg="Viaje en curso · comparte tu viaje si deseas";break;case COMPLETED:msg="✓ Viaje completado · Total S/ 5.50 · "+payment;break;case CANCELLED:msg="Viaje cancelado. Puedes solicitar otro.";break;default:msg="Listo para solicitar un viaje · Pago: "+payment;}
        rideStatus.setText(msg);driverInfo.setText(rideState>=DRIVER_ASSIGNED&&rideState<=IN_RIDE?"Chofer: Carlos Mendoza · ⭐ 4.9\nPlaca: MOTO-123 · CiviMoto amarillo/negro\nEstado: "+stateName(rideState):"Aún no hay chofer asignado.");
        boolean active=rideState==SEARCHING||rideState==DRIVER_ASSIGNED||rideState==DRIVER_ARRIVING||rideState==IN_RIDE;requestBtn.setEnabled(!active);requestBtn.setText(active?"Viaje activo":"Solicitar CiviMoto");advanceBtn.setEnabled(rideState==DRIVER_ASSIGNED||rideState==DRIVER_ARRIVING||rideState==IN_RIDE);cancelBtn.setEnabled(active);historyText.setText("Viajes completados en este dispositivo: "+prefs.getInt("trips",0)+"\nÚltimo estado: "+stateName(rideState));}

    EditText input(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(Color.WHITE);e.setHintTextColor(muted);e.setSingleLine(true);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));e.setPadding(dp(4),dp(8),dp(4),dp(8));return e;}

    WebView createMap(){WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setGeolocationEnabled(true);w.setWebViewClient(new WebViewClient());w.setWebChromeClient(new WebChromeClient(){@Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb){cb.invoke(origin,true,false);}});String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'><link href='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.css' rel='stylesheet'><style>html,body,#map{margin:0;width:100%;height:100%;background:#17171b}.maplibregl-ctrl-attrib{font-size:8px}</style></head><body><div id='map'></div><script src='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.js'></script><script>let map=new maplibregl.Map({container:'map',style:'https://tiles.openfreemap.org/styles/liberty',center:[-77.0428,-12.0464],zoom:13});let me;function locateMe(){navigator.geolocation.getCurrentPosition(p=>{let c=[p.coords.longitude,p.coords.latitude];if(me)me.remove();me=new maplibregl.Marker({color:'#ffbe00'}).setLngLat(c).addTo(map);map.flyTo({center:c,zoom:15});},()=>{});}function demoRoute(){let a=me?me.getLngLat().toArray():[-77.0428,-12.0464],b=[a[0]+0.025,a[1]+0.016];new maplibregl.Marker({color:'#ff3344'}).setLngLat(b).addTo(map);let src=map.getSource('route');let data={type:'Feature',geometry:{type:'LineString',coordinates:[a,b]}};if(src)src.setData(data);else{map.addSource('route',{type:'geojson',data:data});map.addLayer({id:'route',type:'line',source:'route',paint:{'line-color':'#ffbe00','line-width':6}});}map.fitBounds([a,b],{padding:45});}map.on('load',locateMe);</script></body></html>";w.loadDataWithBaseURL("https://app.civimoto.local/",html,"text/html","UTF-8",null);return w;}
}
