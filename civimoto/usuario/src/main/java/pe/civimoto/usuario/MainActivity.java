package pe.civimoto.usuario;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity implements RealtimeBridge.Listener {
    private final int gold=Color.rgb(255,190,0), bg=Color.rgb(8,8,10), surface=Color.rgb(24,24,28), muted=Color.rgb(170,170,178);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Backend backend;
    private RealtimeBridge realtime;
    private LinearLayout root;
    private WebView map;
    private TextView connection, rideStatus, driverInfo, fareInfo;
    private EditText destinationAddress;
    private Button requestButton, cancelButton;
    private String activeTripId, activeDriverId, payment="efectivo";
    private double myLat=-12.0464, myLng=-77.0428, destLat=-12.0564, destLng=-77.0228;
    private boolean destinationSelected=false;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Runnable poller;

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView text(String s,int sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,dp(4),0,dp(4));if(b)v.setTypeface(null,1);return v;}
    Button button(String s){Button x=new Button(this);x.setText(s);x.setTextColor(Color.BLACK);x.setTypeface(null,1);x.setBackground(box(gold,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(6),0,dp(6));x.setLayoutParams(lp);return x;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(box(surface,20));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));c.setLayoutParams(lp);return c;}
    ImageView logo(int size){ImageView i=new ImageView(this);i.setImageResource(R.drawable.logo_civimoto);i.setScaleType(ImageView.ScaleType.CENTER_CROP);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(size),dp(size));i.setLayoutParams(lp);return i;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        backend=new Backend(this);
        requestLocationPermission();
        showSplash();
        backend.bootstrap(new Backend.Callback(){
            public void ok(Object v){ if(backend.hasSession()) backend.resume("usuario", resumeCb); else showLogin(); }
            public void error(String m){ showLogin(); toast("Servidor: "+m); }
        });
    }

    private final Backend.Callback resumeCb=new Backend.Callback(){
        public void ok(Object v){showMain();}
        public void error(String m){showLogin();}
    };

    void showSplash(){
        LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setGravity(Gravity.CENTER);s.setPadding(dp(28),dp(28),dp(28),dp(28));s.setBackgroundColor(Color.BLACK);
        s.addView(logo(240));TextView b=text("CiviMoto",46,Color.WHITE,true);b.setGravity(Gravity.CENTER);s.addView(b);TextView sub=text("Tu destino, nuestra ruta",18,gold,true);sub.setGravity(Gravity.CENTER);s.addView(sub);TextView r=text("PASAJERO · PRODUCCIÓN",12,muted,true);r.setGravity(Gravity.CENTER);r.setPadding(0,dp(18),0,0);s.addView(r);setContentView(s);
    }

    void showLogin(){
        ScrollView sc=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(24),dp(36),dp(24),dp(36));r.setBackgroundColor(bg);sc.addView(r);setContentView(sc);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER);head.setOrientation(LinearLayout.VERTICAL);head.addView(logo(150));TextView title=text("CiviMoto",38,Color.WHITE,true);title.setGravity(Gravity.CENTER);head.addView(title);TextView st=text("Pasajero",16,gold,true);st.setGravity(Gravity.CENTER);head.addView(st);r.addView(head);
        LinearLayout c=card();c.addView(text("Ingresa a tu cuenta",22,Color.WHITE,true));
        EditText name=input("Nombre completo");EditText email=input("Correo electrónico");EditText pass=input("Contraseña");pass.setInputType(0x00000081);
        c.addView(name);c.addView(email);c.addView(pass);
        Button login=button("Iniciar sesión");Button register=button("Crear cuenta de pasajero");c.addView(login);c.addView(register);r.addView(c);
        login.setOnClickListener(v->{login.setEnabled(false);backend.login(email.getText().toString(),pass.getText().toString(),"usuario",new Backend.Callback(){public void ok(Object x){showMain();}public void error(String m){login.setEnabled(true);toast(m);}});});
        register.setOnClickListener(v->{register.setEnabled(false);backend.register(name.getText().toString(),email.getText().toString(),pass.getText().toString(),"usuario",new Backend.Callback(){public void ok(Object x){register.setEnabled(true);toast("Cuenta creada. Iniciando sesión…");backend.login(email.getText().toString(),pass.getText().toString(),"usuario",resumeCb);}public void error(String m){register.setEnabled(true);toast(m);}});});
    }

    void showMain(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(80));root.setBackgroundColor(bg);sc.addView(root);setContentView(sc);
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.addView(logo(62));LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.setPadding(dp(12),0,0,0);ht.addView(text("CiviMoto",30,Color.WHITE,true));connection=text("Conectando con central…",12,muted,false);ht.addView(connection);h.addView(ht,new LinearLayout.LayoutParams(0,-2,1));Button logout=small("Salir");h.addView(logout);root.addView(h);logout.setOnClickListener(v->{backend.logout();stopRealtime();showLogin();});

        TextView hello=text("¿A dónde vamos?",26,Color.WHITE,true);hello.setPadding(0,dp(16),0,dp(8));root.addView(hello);
        map=createMap();root.addView(map,new LinearLayout.LayoutParams(-1,dp(320)));

        LinearLayout form=card();form.addView(text("Solicitar mototaxi",21,Color.WHITE,true));form.addView(text("1. Usa tu ubicación · 2. Toca el mapa para elegir destino",13,muted,false));
        destinationAddress=input("Nombre o referencia del destino");destinationAddress.setText("Destino seleccionado en el mapa");form.addView(destinationAddress);
        TextView payLabel=text("Método de pago",14,muted,true);form.addView(payLabel);
        LinearLayout payRow=new LinearLayout(this);Button cash=button("Efectivo"),yape=button("Yape");payRow.addView(cash,new LinearLayout.LayoutParams(0,dp(52),1));payRow.addView(yape,new LinearLayout.LayoutParams(0,dp(52),1));form.addView(payRow);
        fareInfo=text("La tarifa se calcula en el servidor al solicitar.",15,gold,true);form.addView(fareInfo);
        Button locate=button("📍 Centrar en mi ubicación");requestButton=button("Solicitar CiviMoto");form.addView(locate);form.addView(requestButton);root.addView(form);

        LinearLayout trip=card();trip.addView(text("Estado del viaje",21,Color.WHITE,true));rideStatus=text("Sin viaje activo.",15,Color.WHITE,false);driverInfo=text("Cuando un chofer acepte, verás aquí sus datos.",14,muted,false);trip.addView(rideStatus);trip.addView(driverInfo);Button share=button("Compartir viaje");cancelButton=button("Cancelar viaje");trip.addView(share);trip.addView(cancelButton);root.addView(trip);

        LinearLayout hist=card();hist.addView(text("Últimos viajes",20,Color.WHITE,true));TextView history=text("Cargando…",14,muted,false);hist.addView(history);root.addView(hist);

        cash.setOnClickListener(v->{payment="efectivo";toast("Pago: efectivo");});
        yape.setOnClickListener(v->{payment="yape";toast("Pago: Yape");});
        locate.setOnClickListener(v->{requestFreshLocation();map.evaluateJavascript("setMe("+myLng+","+myLat+")",null);});
        requestButton.setOnClickListener(v->requestTrip());
        cancelButton.setOnClickListener(v->cancelTrip());
        share.setOnClickListener(v->shareTrip());
        cancelButton.setEnabled(false);

        startRealtime();
        requestFreshLocation();
        loadCurrentTrip();
        loadHistory(history);
        startPoller();
    }

    void requestTrip(){
        if(!destinationSelected){toast("Toca el mapa para marcar el destino.");return;}
        requestButton.setEnabled(false);rideStatus.setText("Enviando solicitud a choferes cercanos…");
        JSONObject b=new JSONObject();
        try{
            b.put("p_origin_address","Mi ubicación actual").put("p_origin_lat",myLat).put("p_origin_lng",myLng)
             .put("p_destination_address",destinationAddress.getText().toString().trim())
             .put("p_destination_lat",destLat).put("p_destination_lng",destLng).put("p_payment_method",payment);
        }catch(Exception ignored){}
        backend.rpc("request_civimoto_trip",b,new Backend.Callback(){
            public void ok(Object v){JSONObject t=Backend.firstObject(v);if(t!=null){activeTripId=t.optString("id");updateTripUi(t);}requestButton.setEnabled(false);}
            public void error(String m){requestButton.setEnabled(true);rideStatus.setText("No se pudo solicitar.");toast(m);}
        });
    }

    void cancelTrip(){
        if(activeTripId==null)return;
        try{
            backend.rpc("passenger_cancel_civimoto_trip",new JSONObject().put("p_trip_id",activeTripId).put("p_reason","Cancelado por pasajero"),new Backend.Callback(){
                public void ok(Object v){JSONObject t=Backend.firstObject(v);if(t!=null)updateTripUi(t);}
                public void error(String m){toast(m);}
            });
        }catch(Exception ignored){}
    }

    void loadCurrentTrip(){
        if(activeTripId!=null){loadTripById(activeTripId);return;}
        String p="trips?passenger_id=eq."+backend.getUserId()+"&status=in.(solicitado,aceptado,chofer_en_camino,chofer_llego,en_viaje)&order=requested_at.desc&limit=1&select=*";
        backend.rest("GET",p,null,new Backend.Callback(){
            public void ok(Object v){JSONObject t=Backend.firstObject(v);if(t!=null){activeTripId=t.optString("id");updateTripUi(t);}else resetTripUi();}
            public void error(String m){connection.setText("Conexión degradada · reintentando");}
        });
    }

    void loadTripById(String id){
        backend.rest("GET","trips?id=eq."+id+"&select=*",null,new Backend.Callback(){
            public void ok(Object v){JSONObject t=Backend.firstObject(v);if(t!=null)updateTripUi(t);}
            public void error(String m){}
        });
    }

    void updateTripUi(JSONObject t){
        String st=t.optString("status","solicitado");
        String label=friendlyStatus(st);
        double fare=t.optDouble("final_fare",t.optDouble("estimated_fare",0));
        rideStatus.setText(label+" · "+(fare>0?String.format("S/ %.2f",fare):"calculando tarifa"));
        fareInfo.setText("Tarifa estimada: "+(fare>0?String.format("S/ %.2f",fare):"—")+" · "+t.optString("payment_method","efectivo").toUpperCase());
        activeDriverId=t.optString("driver_id","");
        if(activeDriverId!=null && !activeDriverId.isEmpty() && !"null".equals(activeDriverId)){loadDriver(t.optString("id"));loadDriverLocation(activeDriverId);}
        else driverInfo.setText("Buscando choferes disponibles cerca de ti…");
        cancelButton.setEnabled(st.equals("solicitado")||st.equals("aceptado")||st.equals("chofer_en_camino")||st.equals("chofer_llego"));
        requestButton.setEnabled(st.equals("cancelado")||st.equals("completado"));
        requestButton.setText(st.equals("cancelado")||st.equals("completado")?"Solicitar otro CiviMoto":"Viaje activo");
        if(st.equals("completado")||st.equals("cancelado")){activeTripId=null;activeDriverId=null;driverInfo.setText(st.equals("completado")?"Viaje completado. Gracias por viajar con Civimoto.":"Viaje cancelado.");}
    }

    void loadDriver(String tripId){
        try{
            backend.rpc("trip_party_public",new JSONObject().put("p_trip_id",tripId),new Backend.Callback(){
                public void ok(Object v){
                    JSONObject o=Backend.firstObject(v);if(o==null && v instanceof JSONObject)o=(JSONObject)v;if(o==null)return;
                    JSONObject d=o.optJSONObject("driver");if(d!=null)driverInfo.setText("Chofer: "+d.optString("name","—")+" · ⭐ "+d.optString("rating","5.0")+" · Placa "+d.optString("plate","—"));
                }
                public void error(String m){}
            });
        }catch(Exception ignored){}
    }

    void loadDriverLocation(String driverId){
        backend.rest("GET","driver_live_locations?driver_id=eq."+driverId+"&select=lat,lng,updated_at",null,new Backend.Callback(){
            public void ok(Object v){JSONObject l=Backend.firstObject(v);if(l!=null && map!=null)map.evaluateJavascript("setDriver("+l.optDouble("lng")+","+l.optDouble("lat")+")",null);}
            public void error(String m){}
        });
    }

    void loadHistory(TextView out){
        backend.rest("GET","trips?passenger_id=eq."+backend.getUserId()+"&order=requested_at.desc&limit=5&select=id,status,destination_address,estimated_fare,final_fare,requested_at",null,new Backend.Callback(){
            public void ok(Object v){
                if(!(v instanceof JSONArray)){out.setText("Sin viajes.");return;}JSONArray a=(JSONArray)v;StringBuilder s=new StringBuilder();
                for(int i=0;i<a.length();i++){JSONObject t=a.optJSONObject(i);if(t!=null)s.append("• ").append(t.optString("destination_address","Destino")).append(" · ").append(friendlyStatus(t.optString("status"))).append("\n");}
                out.setText(s.length()==0?"Aún no tienes viajes.":s.toString().trim());
            }
            public void error(String m){out.setText("No se pudo cargar el historial.");}
        });
    }

    void resetTripUi(){rideStatus.setText("Sin viaje activo.");driverInfo.setText("Listo para solicitar.");cancelButton.setEnabled(false);requestButton.setEnabled(true);requestButton.setText("Solicitar CiviMoto");}

    void startRealtime(){
        realtime=new RealtimeBridge(this,backend,this);
        WebView rt=realtime.start("trips","driver_live_locations","passenger_notifications");
        rt.setVisibility(View.INVISIBLE);root.addView(rt,new LinearLayout.LayoutParams(1,1));
    }
    void stopRealtime(){if(realtime!=null)realtime.stop();if(poller!=null)handler.removeCallbacks(poller);try{if(locationManager!=null&&locationListener!=null)locationManager.removeUpdates(locationListener);}catch(Exception ignored){}}

    void startPoller(){
        poller=new Runnable(){public void run(){if(backend.hasSession())loadCurrentTrip();handler.postDelayed(this,5000);}};
        handler.postDelayed(poller,5000);
    }

    @Override public void onRealtime(String table,String payload){
        connection.setText("● En tiempo real");
        if("trips".equals(table)){try{JSONObject p=new JSONObject(payload);JSONObject rec=p.optJSONObject("new");if(rec!=null){String id=rec.optString("id");if(activeTripId==null||activeTripId.equals(id)){activeTripId=id;updateTripUi(rec);}}}catch(Exception e){loadCurrentTrip();}}
        else if("driver_live_locations".equals(table) && activeDriverId!=null) loadDriverLocation(activeDriverId);
    }
    @Override public void onRealtimeStatus(String status){connection.setText("SUBSCRIBED".equals(status)?"● En tiempo real":"Sincronizando…");}

    void shareTrip(){
        if(activeTripId==null){toast("No hay viaje activo.");return;}
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,"Estoy viajando con CiviMoto. Viaje "+activeTripId+". Tu destino, nuestra ruta.");startActivity(Intent.createChooser(i,"Compartir viaje Civimoto"));
    }

    String friendlyStatus(String s){
        if("solicitado".equals(s))return "Buscando chofer";
        if("aceptado".equals(s))return "Chofer asignado";
        if("chofer_en_camino".equals(s))return "Chofer en camino";
        if("chofer_llego".equals(s))return "Tu chofer llegó";
        if("en_viaje".equals(s))return "Viaje en curso";
        if("completado".equals(s))return "Viaje completado";
        if("cancelado".equals(s))return "Viaje cancelado";
        return s;
    }

    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(muted);e.setSingleLine(true);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));e.setPadding(dp(4),dp(10),dp(4),dp(10));return e;}
    Button small(String s){Button b=button(s);b.setLayoutParams(new LinearLayout.LayoutParams(dp(82),dp(48)));return b;}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    void requestLocationPermission(){if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},20);}
    void requestFreshLocation(){
        try{
            locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
            if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;
            Location last=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);if(last!=null)applyLocation(last);
            if(locationListener==null)locationListener=new LocationListener(){public void onLocationChanged(Location l){applyLocation(l);}public void onStatusChanged(String p,int s,Bundle e){}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}};
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,4000,5,locationListener);
        }catch(Exception ignored){}
    }
    void applyLocation(Location l){myLat=l.getLatitude();myLng=l.getLongitude();if(map!=null)map.evaluateJavascript("setMe("+myLng+","+myLat+")",null);}

    WebView createMap(){
        WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);w.setWebViewClient(new WebViewClient());w.addJavascriptInterface(new MapBridge(),"AndroidMap");
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'><link href='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.css' rel='stylesheet'><style>html,body,#m{margin:0;width:100%;height:100%;background:#17171b}.maplibregl-ctrl-attrib{font-size:8px}</style></head><body><div id='m'></div><script src='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.js'></script><script>let map=new maplibregl.Map({container:'m',style:'https://tiles.openfreemap.org/styles/liberty',center:[-77.0428,-12.0464],zoom:13});let me,dest,drv;function setMe(x,y){if(me)me.remove();me=new maplibregl.Marker({color:'#ffbe00'}).setLngLat([x,y]).addTo(map);map.flyTo({center:[x,y],zoom:15});}function setDest(x,y){if(dest)dest.remove();dest=new maplibregl.Marker({color:'#ff3344'}).setLngLat([x,y]).addTo(map);}function setDriver(x,y){if(drv)drv.remove();drv=new maplibregl.Marker({color:'#111111'}).setLngLat([x,y]).addTo(map);}map.on('click',e=>{setDest(e.lngLat.lng,e.lngLat.lat);AndroidMap.destination(e.lngLat.lat,e.lngLat.lng);});</script></body></html>";
        w.loadDataWithBaseURL("https://map.civimoto.local/",html,"text/html","UTF-8",null);return w;
    }
    class MapBridge{@JavascriptInterface public void destination(double lat,double lng){runOnUiThread(()->{destLat=lat;destLng=lng;destinationSelected=true;destinationAddress.setText(String.format("Destino %.5f, %.5f",lat,lng));});}}

    @Override protected void onDestroy(){super.onDestroy();stopRealtime();}
}
