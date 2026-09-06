package pe.civimoto.chofer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import org.json.JSONObject;

public class MainActivity extends Activity implements RealtimeBridge.Listener {
    private final int gold=Color.rgb(255,190,0), bg=Color.rgb(8,8,10), surface=Color.rgb(24,24,28), muted=Color.rgb(170,170,178);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Backend backend; private RealtimeBridge realtime; private LinearLayout root;
    private WebView map; private TextView connection,state,requestInfo,tripInfo,billingInfo;
    private Switch onlineSwitch; private Button acceptButton,rejectButton,arrivedButton,startButton,finishButton;
    private EditText yapeOperation;
    private String offeredTripId,activeTripId,tripStatus;
    private boolean online=false;
    private double lat=-12.0464,lng=-77.0428;
    private LocationManager locationManager; private LocationListener locationListener; private Runnable poller;

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView text(String s,int sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,dp(4),0,dp(4));if(b)v.setTypeface(null,1);return v;}
    Button button(String s){Button x=new Button(this);x.setText(s);x.setTextColor(Color.BLACK);x.setTypeface(null,1);x.setBackground(box(gold,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(6),0,dp(6));x.setLayoutParams(lp);return x;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(box(surface,20));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));c.setLayoutParams(lp);return c;}
    ImageView logo(int s){ImageView i=new ImageView(this);i.setImageResource(R.drawable.logo_civimoto);i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setLayoutParams(new LinearLayout.LayoutParams(dp(s),dp(s)));return i;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);backend=new Backend(this);requestLocationPermission();showSplash();
        backend.bootstrap(new Backend.Callback(){public void ok(Object v){if(backend.hasSession())backend.resume("chofer",resumeCb);else showLogin();}public void error(String m){showLogin();toast("Servidor: "+m);}});
    }
    private final Backend.Callback resumeCb=new Backend.Callback(){public void ok(Object v){showMain();}public void error(String m){showLogin();}};

    void showSplash(){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setGravity(Gravity.CENTER);s.setBackgroundColor(Color.BLACK);s.addView(logo(240));TextView b=text("CiviMoto",46,Color.WHITE,true);b.setGravity(Gravity.CENTER);s.addView(b);TextView t=text("Chofer · Conduce tu futuro",18,gold,true);t.setGravity(Gravity.CENTER);s.addView(t);setContentView(s);}

    void showLogin(){
        ScrollView sc=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(24),dp(36),dp(24),dp(36));r.setBackgroundColor(bg);sc.addView(r);setContentView(sc);
        LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.VERTICAL);h.setGravity(Gravity.CENTER);h.addView(logo(150));TextView title=text("CiviMoto",38,Color.WHITE,true);title.setGravity(Gravity.CENTER);h.addView(title);TextView role=text("Chofer",16,gold,true);role.setGravity(Gravity.CENTER);h.addView(role);r.addView(h);
        LinearLayout c=card();c.addView(text("Cuenta del chofer",22,Color.WHITE,true));EditText name=input("Nombre completo");EditText email=input("Correo electrónico");EditText pass=input("Contraseña");pass.setInputType(0x00000081);c.addView(name);c.addView(email);c.addView(pass);Button login=button("Iniciar sesión");Button reg=button("Registrarme como chofer");c.addView(login);c.addView(reg);r.addView(c);
        login.setOnClickListener(v->{login.setEnabled(false);backend.login(email.getText().toString(),pass.getText().toString(),"chofer",new Backend.Callback(){public void ok(Object x){showMain();}public void error(String m){login.setEnabled(true);toast(m);}});});
        reg.setOnClickListener(v->{reg.setEnabled(false);backend.register(name.getText().toString(),email.getText().toString(),pass.getText().toString(),"chofer",new Backend.Callback(){public void ok(Object x){reg.setEnabled(true);toast("Cuenta creada. Debe ser aprobada por el administrador.");backend.login(email.getText().toString(),pass.getText().toString(),"chofer",resumeCb);}public void error(String m){reg.setEnabled(true);toast(m);}});});
    }

    void showMain(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(80));root.setBackgroundColor(bg);sc.addView(root);setContentView(sc);
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.addView(logo(62));LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.setPadding(dp(12),0,0,0);ht.addView(text("CiviMoto",30,Color.WHITE,true));connection=text("Conectando con central…",12,muted,false);ht.addView(connection);h.addView(ht,new LinearLayout.LayoutParams(0,-2,1));Button logout=small("Salir");h.addView(logout);root.addView(h);logout.setOnClickListener(v->{setAvailability(false);backend.logout();stopRealtime();showLogin();});

        LinearLayout statusCard=card();statusCard.addView(text("Estado del chofer",21,Color.WHITE,true));onlineSwitch=new Switch(this);onlineSwitch.setText("Fuera de línea");onlineSwitch.setTextColor(Color.WHITE);statusCard.addView(onlineSwitch);state=text("Activa En línea para recibir viajes cercanos.",14,muted,false);statusCard.addView(state);root.addView(statusCard);
        map=createMap();root.addView(map,new LinearLayout.LayoutParams(-1,dp(300)));

        LinearLayout offer=card();offer.addView(text("Solicitud cercana",21,Color.WHITE,true));requestInfo=text("No hay solicitudes disponibles.",15,muted,false);offer.addView(requestInfo);LinearLayout row=new LinearLayout(this);rejectButton=button("Rechazar");acceptButton=button("Aceptar");row.addView(rejectButton,new LinearLayout.LayoutParams(0,dp(52),1));row.addView(acceptButton,new LinearLayout.LayoutParams(0,dp(52),1));offer.addView(row);root.addView(offer);

        LinearLayout trip=card();trip.addView(text("Viaje activo",21,Color.WHITE,true));tripInfo=text("Sin viaje activo.",15,muted,false);trip.addView(tripInfo);arrivedButton=button("Llegué al origen");startButton=button("Iniciar viaje");finishButton=button("Finalizar viaje");trip.addView(arrivedButton);trip.addView(startButton);trip.addView(finishButton);root.addView(trip);

        LinearLayout bill=card();bill.addView(text("Pago a Civimoto",21,Color.WHITE,true));billingInfo=text("Cargando plan semanal/mensual…",14,muted,false);bill.addView(billingInfo);yapeOperation=input("Número de operación Yape");bill.addView(yapeOperation);LinearLayout br=new LinearLayout(this);Button weekly=button("Pagar semanal"),monthly=button("Pagar mensual");br.addView(weekly,new LinearLayout.LayoutParams(0,dp(52),1));br.addView(monthly,new LinearLayout.LayoutParams(0,dp(52),1));bill.addView(br);root.addView(bill);

        acceptButton.setEnabled(false);rejectButton.setEnabled(false);arrivedButton.setEnabled(false);startButton.setEnabled(false);finishButton.setEnabled(false);
        onlineSwitch.setOnCheckedChangeListener((v,on)->{if(on){requestFreshLocation();setAvailability(true);}else setAvailability(false);});
        acceptButton.setOnClickListener(v->acceptTrip());
        rejectButton.setOnClickListener(v->{offeredTripId=null;requestInfo.setText("Solicitud rechazada. Buscando otra…");acceptButton.setEnabled(false);rejectButton.setEnabled(false);});
        arrivedButton.setOnClickListener(v->transition("chofer_llego"));
        startButton.setOnClickListener(v->transition("en_viaje"));
        finishButton.setOnClickListener(v->transition("completado"));
        weekly.setOnClickListener(v->submitPayment("weekly"));
        monthly.setOnClickListener(v->submitPayment("monthly"));

        requestFreshLocation();startRealtime();loadDriverState();loadActiveTrip();loadBilling();startPoller();
    }

    void loadDriverState(){
        backend.rest("GET","drivers?id=eq."+backend.getUserId()+"&select=status,is_available,rating",null,new Backend.Callback(){
            public void ok(Object v){JSONObject d=Backend.firstObject(v);if(d==null)return;String st=d.optString("status","pendiente");online=d.optBoolean("is_available",false);onlineSwitch.setOnCheckedChangeListener(null);onlineSwitch.setChecked(online);onlineSwitch.setText(online?"En línea":"Fuera de línea");onlineSwitch.setOnCheckedChangeListener((x,on)->{if(on){requestFreshLocation();setAvailability(true);}else setAvailability(false);});if(!"aprobado".equals(st)){onlineSwitch.setEnabled(false);state.setText("Tu cuenta está "+st+". El administrador debe aprobarla.");}else state.setText(online?"✓ Disponible para recibir viajes.":"Fuera de línea.");}
            public void error(String m){state.setText("No se pudo validar el estado del chofer.");}
        });
    }

    void setAvailability(boolean on){
        JSONObject b=new JSONObject();try{b.put("p_available",on).put("p_lat",lat).put("p_lng",lng);}catch(Exception ignored){}
        backend.rpc("driver_set_availability",b,new Backend.Callback(){
            public void ok(Object v){online=on;onlineSwitch.setText(on?"En línea":"Fuera de línea");state.setText(on?"✓ GPS activo · recibiendo solicitudes cercanas.":"Fuera de línea.");if(on)loadOffers();}
            public void error(String m){online=false;onlineSwitch.setChecked(false);toast(m);}
        });
    }

    void loadOffers(){
        if(!online||activeTripId!=null)return;
        JSONObject b=new JSONObject();try{b.put("p_radius_km",7);}catch(Exception ignored){}
        backend.rpc("driver_available_trips",b,new Backend.Callback(){
            public void ok(Object v){
                JSONObject t=Backend.firstObject(v);
                if(t==null){offeredTripId=null;requestInfo.setText("No hay solicitudes cercanas.");acceptButton.setEnabled(false);rejectButton.setEnabled(false);return;}
                offeredTripId=t.optString("id");requestInfo.setText("Origen: "+t.optString("origin_address","—")+"\nDestino: "+t.optString("destination_address","—")+"\nDistancia: "+t.optString("estimated_distance_km","—")+" km · Tarifa S/ "+t.optString("estimated_fare","—")+"\nPago: "+t.optString("payment_method","efectivo").toUpperCase());acceptButton.setEnabled(true);rejectButton.setEnabled(true);map.evaluateJavascript("showOffer("+t.optDouble("origin_lng")+","+t.optDouble("origin_lat")+","+t.optDouble("destination_lng")+","+t.optDouble("destination_lat")+")",null);
            }
            public void error(String m){connection.setText("Sincronizando…");}
        });
    }

    void acceptTrip(){
        if(offeredTripId==null)return;
        acceptButton.setEnabled(false);
        try{backend.rpc("accept_civimoto_trip",new JSONObject().put("p_trip_id",offeredTripId),new Backend.Callback(){public void ok(Object v){JSONObject t=Backend.firstObject(v);if(t!=null){activeTripId=t.optString("id");offeredTripId=null;updateTrip(t);transition("chofer_en_camino");}}public void error(String m){toast(m);loadOffers();}});}catch(Exception ignored){}
    }

    void loadActiveTrip(){
        if(activeTripId!=null){loadTrip(activeTripId);return;}
        backend.rest("GET","trips?driver_id=eq."+backend.getUserId()+"&status=in.(aceptado,chofer_en_camino,chofer_llego,en_viaje)&order=accepted_at.desc&limit=1&select=*",null,new Backend.Callback(){
            public void ok(Object v){JSONObject t=Backend.firstObject(v);if(t!=null){activeTripId=t.optString("id");updateTrip(t);}else{tripInfo.setText("Sin viaje activo.");setTripButtons(null);if(online)loadOffers();}}
            public void error(String m){}
        });
    }

    void loadTrip(String id){backend.rest("GET","trips?id=eq."+id+"&select=*",null,new Backend.Callback(){public void ok(Object v){JSONObject t=Backend.firstObject(v);if(t!=null)updateTrip(t);}public void error(String m){}});}

    void updateTrip(JSONObject t){
        tripStatus=t.optString("status");tripInfo.setText("Estado: "+friendly(tripStatus)+"\nOrigen: "+t.optString("origin_address","—")+"\nDestino: "+t.optString("destination_address","—")+"\nTarifa: S/ "+t.optString("estimated_fare","—")+" · "+t.optString("payment_method","efectivo").toUpperCase());setTripButtons(tripStatus);map.evaluateJavascript("showOffer("+t.optDouble("origin_lng")+","+t.optDouble("origin_lat")+","+t.optDouble("destination_lng")+","+t.optDouble("destination_lat")+")",null);
        if("completado".equals(tripStatus)||"cancelado".equals(tripStatus)){activeTripId=null;tripInfo.setText("Viaje "+friendly(tripStatus)+".");setTripButtons(null);if(online)loadOffers();}
    }

    void setTripButtons(String s){
        arrivedButton.setEnabled("chofer_en_camino".equals(s));
        startButton.setEnabled("chofer_llego".equals(s));
        finishButton.setEnabled("en_viaje".equals(s));
    }

    void transition(String target){
        if(activeTripId==null)return;
        try{backend.rpc("driver_transition_trip",new JSONObject().put("p_trip_id",activeTripId).put("p_target_status",target),new Backend.Callback(){public void ok(Object v){JSONObject t=Backend.firstObject(v);if(t!=null)updateTrip(t);}public void error(String m){toast(m);}});}catch(Exception ignored){}
    }

    void loadBilling(){
        backend.rest("GET","driver_billing_config?id=eq.true&select=weekly_amount,monthly_amount,yape_display_name,yape_number",null,new Backend.Callback(){
            public void ok(Object v){JSONObject c=Backend.firstObject(v);if(c==null)return;String y=c.optString("yape_number","");billingInfo.setText("Semanal: S/ "+c.optString("weekly_amount","—")+" · Mensual: S/ "+c.optString("monthly_amount","—")+(y.isEmpty()?"\nYape: configura el administrador.":"\nYape: "+y+" · "+c.optString("yape_display_name","")));}
            public void error(String m){billingInfo.setText("No se pudo cargar la configuración de pago.");}
        });
    }

    void submitPayment(String plan){
        String op=yapeOperation.getText().toString().trim();if(op.length()<3){toast("Ingresa el número de operación Yape.");return;}
        try{backend.rpc("submit_driver_membership_payment",new JSONObject().put("p_plan",plan).put("p_yape_operation",op).put("p_receipt_url",JSONObject.NULL),new Backend.Callback(){public void ok(Object v){toast("Pago enviado. El administrador debe aprobarlo.");yapeOperation.setText("");}public void error(String m){toast(m);}});}catch(Exception ignored){}
    }

    void requestLocationPermission(){if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},30);}
    void requestFreshLocation(){
        try{
            locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
            if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;
            Location last=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);if(last!=null)applyLocation(last);
            if(locationListener==null)locationListener=new LocationListener(){public void onLocationChanged(Location l){applyLocation(l);}public void onStatusChanged(String p,int s,Bundle e){}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}};
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,5000,5,locationListener);
        }catch(Exception ignored){}
    }
    void applyLocation(Location l){
        lat=l.getLatitude();lng=l.getLongitude();if(map!=null)map.evaluateJavascript("setMe("+lng+","+lat+")",null);
        if(backend.hasSession()&&(online||activeTripId!=null)){JSONObject b=new JSONObject();try{b.put("p_lat",lat).put("p_lng",lng).put("p_heading",l.hasBearing()?l.getBearing():JSONObject.NULL).put("p_speed_kmh",l.hasSpeed()?l.getSpeed()*3.6:JSONObject.NULL).put("p_trip_id",activeTripId==null?JSONObject.NULL:activeTripId);}catch(Exception ignored){}backend.rpc("driver_update_live_location",b,new Backend.Callback(){public void ok(Object v){}public void error(String m){}});}
    }

    void startRealtime(){realtime=new RealtimeBridge(this,backend,this);WebView rt=realtime.start("trips","driver_subscription_payments","driver_billing","driver_notifications");rt.setVisibility(View.INVISIBLE);root.addView(rt,new LinearLayout.LayoutParams(1,1));}
    void stopRealtime(){if(realtime!=null)realtime.stop();if(poller!=null)handler.removeCallbacks(poller);try{if(locationManager!=null&&locationListener!=null)locationManager.removeUpdates(locationListener);}catch(Exception ignored){}}
    void startPoller(){poller=new Runnable(){public void run(){if(backend.hasSession()){loadActiveTrip();if(online&&activeTripId==null)loadOffers();}handler.postDelayed(this,4000);}};handler.postDelayed(poller,4000);}
    @Override public void onRealtime(String table,String payload){connection.setText("● En tiempo real");if("trips".equals(table))loadActiveTrip();else if("driver_billing".equals(table)||"driver_subscription_payments".equals(table))loadBilling();}
    @Override public void onRealtimeStatus(String s){connection.setText("SUBSCRIBED".equals(s)?"● En tiempo real":"Sincronizando…");}

    WebView createMap(){WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);w.setWebViewClient(new WebViewClient());String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'><link href='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.css' rel='stylesheet'><style>html,body,#m{margin:0;width:100%;height:100%;background:#17171b}</style></head><body><div id='m'></div><script src='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.js'></script><script>let map=new maplibregl.Map({container:'m',style:'https://tiles.openfreemap.org/styles/liberty',center:[-77.0428,-12.0464],zoom:13});let me,a,b;function setMe(x,y){if(me)me.remove();me=new maplibregl.Marker({color:'#ffbe00'}).setLngLat([x,y]).addTo(map);map.flyTo({center:[x,y],zoom:15});}function showOffer(x1,y1,x2,y2){if(a)a.remove();if(b)b.remove();a=new maplibregl.Marker({color:'#19bd69'}).setLngLat([x1,y1]).addTo(map);b=new maplibregl.Marker({color:'#ff3344'}).setLngLat([x2,y2]).addTo(map);let d={type:'Feature',geometry:{type:'LineString',coordinates:[[x1,y1],[x2,y2]]}};if(map.getSource('r'))map.getSource('r').setData(d);else{map.addSource('r',{type:'geojson',data:d});map.addLayer({id:'r',type:'line',source:'r',paint:{'line-color':'#ffbe00','line-width':6}});}map.fitBounds([[x1,y1],[x2,y2]],{padding:45});}</script></body></html>";w.loadDataWithBaseURL("https://driver.civimoto.local/",html,"text/html","UTF-8",null);return w;}

    String friendly(String s){if("aceptado".equals(s))return "aceptado";if("chofer_en_camino".equals(s))return "en camino al pasajero";if("chofer_llego".equals(s))return "llegaste al origen";if("en_viaje".equals(s))return "en viaje";if("completado".equals(s))return "completado";if("cancelado".equals(s))return "cancelado";return s;}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(muted);e.setSingleLine(true);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));e.setPadding(dp(4),dp(10),dp(4),dp(10));return e;}
    Button small(String s){Button b=button(s);b.setLayoutParams(new LinearLayout.LayoutParams(dp(82),dp(48)));return b;}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){super.onDestroy();stopRealtime();}
}
