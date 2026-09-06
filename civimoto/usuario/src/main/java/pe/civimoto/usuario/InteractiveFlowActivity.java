package pe.civimoto.usuario;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

public class InteractiveFlowActivity extends FlowActivity {
    private final int GOLD_ON=Color.rgb(255,195,0);
    private final int GOLD_PRESS=Color.rgb(205,145,0);
    private final int GOLD_DISABLED=Color.rgb(120,92,20);
    private final int BLACK=Color.rgb(5,6,8), MUTED=Color.rgb(176,180,190), GOLD2=Color.rgb(255,220,90);
    private final Handler ui=new Handler(Looper.getMainLooper());

    private GradientDrawable round(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(16));g.setStroke(dp(1),Color.rgb(255,218,80));return g;}
    private StateListDrawable yellowSelector(){StateListDrawable s=new StateListDrawable();s.addState(new int[]{-android.R.attr.state_enabled},round(GOLD_DISABLED));s.addState(new int[]{android.R.attr.state_pressed},round(GOLD_PRESS));s.addState(new int[]{android.R.attr.state_selected},round(GOLD_PRESS));s.addState(new int[]{},round(GOLD_ON));return s;}
    @Override Button btn(String text,boolean primary){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(null,1);b.setTextColor(Color.BLACK);b.setBackground(yellowSelector());b.setElevation(dp(3));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(lp);return b;}

    private Object getField(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private void setField(String name,Object value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}catch(Exception ignored){}}
    private double getDouble(String name,double fallback){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.getDouble(this);}catch(Exception e){return fallback;}}
    private boolean getBoolean(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.getBoolean(this);}catch(Exception e){return false;}}
    private void setDouble(String name,double value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(this,value);}catch(Exception ignored){}}
    private void setBoolean(String name,boolean value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.setBoolean(this,value);}catch(Exception ignored){}}
    private Backend backend(){return (Backend)getField("backend");}
    private LinearLayout body(){return (LinearLayout)getField("body");}

    @Override void shell(String title,String subtitle,int step){
        stopPollingOnly();
        ScrollView sc=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(14),dp(14),dp(70));root.setBackgroundColor(BLACK);sc.addView(root);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(dp(4),dp(3),dp(4),dp(8));
        head.addView(logo(58));TextView brand=tx("CiviMoto Pasajero",27,Color.WHITE,true);brand.setPadding(dp(12),0,0,0);head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));root.addView(head);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,dp(8),0,0);root.addView(content);setField("body",content);setContentView(sc);
    }

    private void addPasswordToggle(LinearLayout c,EditText pass){
        CheckBox show=new CheckBox(this);show.setText("Mostrar contraseña");show.setTextColor(GOLD2);show.setPadding(dp(4),dp(2),0,dp(5));
        show.setOnCheckedChangeListener((v,on)->{int pos=pass.getSelectionStart();pass.setInputType(InputType.TYPE_CLASS_TEXT|(on?InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:InputType.TYPE_TEXT_VARIATION_PASSWORD));pass.setSelection(Math.max(0,Math.min(pos,pass.length())));show.setText(on?"Ocultar contraseña":"Mostrar contraseña");});
        c.addView(show);
    }

    @Override void screenLogin(){
        shell("","",0);LinearLayout c=card();c.addView(tx("Bienvenido",24,Color.WHITE,true));c.addView(tx("Ingresa a tu cuenta exclusiva de CiviMoto.",13,MUTED,false));
        EditText email=input("Correo electrónico"),pass=input("Contraseña");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);c.addView(email);c.addView(pass);addPasswordToggle(c,pass);
        Button login=btn("Iniciar sesión",true),register=btn("Crear una cuenta",false);c.addView(login);c.addView(register);body().addView(c);
        login.setOnClickListener(v->{login.setEnabled(false);backend().login(email.getText().toString(),pass.getText().toString(),"usuario",new Backend.Callback(){public void ok(Object x){screenHome();}public void error(String m){login.setEnabled(true);toast(m);}});});register.setOnClickListener(v->screenRegister());
    }

    @Override void screenRegister(){
        shell("","",0);LinearLayout c=card();c.addView(tx("Crear cuenta",24,Color.WHITE,true));EditText name=input("Nombre completo"),email=input("Correo electrónico"),phone=input("Teléfono"),pass=input("Contraseña (8+ caracteres)");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);c.addView(name);c.addView(email);c.addView(phone);c.addView(pass);addPasswordToggle(c,pass);
        Button create=btn("Crear cuenta y continuar",true),back=btn("Ya tengo cuenta",false);c.addView(create);c.addView(back);body().addView(c);
        create.setOnClickListener(v->{create.setEnabled(false);backend().register(name.getText().toString(),email.getText().toString(),pass.getText().toString(),"usuario",new Backend.Callback(){public void ok(Object x){backend().login(email.getText().toString(),pass.getText().toString(),"usuario",new Backend.Callback(){public void ok(Object y){screenHome();}public void error(String m){create.setEnabled(true);toast(m);}});}public void error(String m){create.setEnabled(true);toast(m);}});});back.setOnClickListener(v->screenLogin());
    }

    @Override WebView createMap(){
        WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);w.setWebViewClient(new WebViewClient());w.addJavascriptInterface(new MapBridge(),"AndroidMap");
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><link href='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.css' rel='stylesheet'><style>html,body,#m{margin:0;width:100%;height:100%;background:#101216}</style></head><body><div id='m'></div><script src='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.js'></script><script>let map=new maplibregl.Map({container:'m',style:'https://tiles.openfreemap.org/styles/liberty',center:[-77.0428,-12.0464],zoom:13});let me,dest,drv;function setMe(x,y){if(me)me.remove();me=new maplibregl.Marker({color:'#ffbe00'}).setLngLat([x,y]).addTo(map);map.flyTo({center:[x,y],zoom:16});}function setDriver(x,y){if(drv)drv.remove();drv=new maplibregl.Marker({color:'#111'}).setLngLat([x,y]).addTo(map);}async function drawRoute(x1,y1,x2,y2){if(dest)dest.remove();dest=new maplibregl.Marker({color:'#ef4040'}).setLngLat([x2,y2]).addTo(map);let coords=[[x1,y1],[x2,y2]];try{let r=await fetch('https://router.project-osrm.org/route/v1/driving/'+x1+','+y1+';'+x2+','+y2+'?overview=full&geometries=geojson');let j=await r.json();if(j.routes&&j.routes.length)coords=j.routes[0].geometry.coordinates;}catch(e){}let geo={type:'Feature',geometry:{type:'LineString',coordinates:coords}};if(map.getSource('route'))map.getSource('route').setData(geo);else{map.addSource('route',{type:'geojson',data:geo});map.addLayer({id:'route-shadow',type:'line',source:'route',paint:{'line-color':'#111','line-width':8,'line-opacity':0.65}});map.addLayer({id:'route',type:'line',source:'route',paint:{'line-color':'#ffbe00','line-width':5,'line-opacity':0.95}});}let b=coords.reduce((bb,c)=>bb.extend(c),new maplibregl.LngLatBounds(coords[0],coords[0]));map.fitBounds(b,{padding:55,maxZoom:16});}map.on('click',e=>{AndroidMap.destination(e.lngLat.lat,e.lngLat.lng);drawRoute(me?me.getLngLat().lng:-77.0428,me?me.getLngLat().lat:-12.0464,e.lngLat.lng,e.lngLat.lat);});</script></body></html>";
        w.loadDataWithBaseURL("https://passenger.civimoto.local/",html,"text/html","UTF-8",null);ui.postDelayed(()->w.evaluateJavascript("setMe("+getDouble("lng",-77.0428)+","+getDouble("lat",-12.0464)+")",null),900);return w;
    }

    @Override void screenHome(){
        shell("","",0);LinearLayout b=body();
        TextView connection=tx("Conectando con central…",12,MUTED,false);setField("connection",connection);b.addView(connection);
        freshLocation();WebView m=createMap();setField("map",m);b.addView(m,new LinearLayout.LayoutParams(-1,dp(340)));centerActual(m);
        LinearLayout c=card();c.addView(tx("Pedir un mototaxi",23,Color.WHITE,true));
        EditText origin=input("Origen: escribe una dirección o usa GPS");origin.setText("Mi ubicación actual");c.addView(origin);
        Button gps=btn("📍 Usar mi ubicación actual",false);c.addView(gps);
        EditText destination=input("Destino: calle, avenida o referencia");c.addView(destination);
        c.addView(tx("También puedes tocar el mapa para marcar el destino.",12,MUTED,false));
        TextView fare=tx("Tarifa calculada al solicitar",14,GOLD2,true);setField("fareState",fare);c.addView(fare);
        LinearLayout pay=new LinearLayout(this);Button cash=btn("Efectivo",false),yape=btn("Yape",false);cash.setSelected(true);pay.addView(cash,new LinearLayout.LayoutParams(0,dp(54),1));pay.addView(yape,new LinearLayout.LayoutParams(0,dp(54),1));c.addView(pay);
        Button request=btn("Solicitar mototaxi",true),history=btn("Historial y perfil",false);c.addView(request);c.addView(history);b.addView(c);
        cash.setOnClickListener(v->{setField("payment","efectivo");cash.setSelected(true);yape.setSelected(false);});
        yape.setOnClickListener(v->{setField("payment","yape");yape.setSelected(true);cash.setSelected(false);});
        gps.setOnClickListener(v->{freshLocation();origin.setText("Mi ubicación actual");centerActual(m);});
        history.setOnClickListener(v->screenHistory());request.setOnClickListener(v->{request.setEnabled(false);prepareAndRequest(origin,destination,m,request);});
        loadActiveAndRoute();
    }

    @Override void screenTracking(){
        shell("","",0);LinearLayout b=body();TextView connection=tx("Sincronizando viaje en tiempo real…",12,MUTED,false);setField("connection",connection);b.addView(connection);
        freshLocation();WebView m=createMap();setField("map",m);b.addView(m,new LinearLayout.LayoutParams(-1,dp(330)));centerActual(m);
        LinearLayout c=card();TextView trip=tx("Buscando chofer…",22,GOLD2,true),driver=tx("Estamos notificando a choferes cercanos.",14,MUTED,false),fare=tx("Tarifa: —",15,Color.WHITE,true);setField("tripState",trip);setField("driverState",driver);setField("fareState",fare);c.addView(trip);c.addView(driver);c.addView(fare);
        Button share=btn("Compartir viaje",false),cancel=btn("Cancelar viaje",false),again=btn("Volver a pedir viaje",true);c.addView(share);c.addView(cancel);c.addView(again);b.addView(c);
        share.setOnClickListener(v->shareTrip());cancel.setOnClickListener(v->cancelTrip());again.setOnClickListener(v->{String st=String.valueOf(getField("lastStatus"));if("completado".equals(st)||"cancelado".equals(st)||"null".equals(st)){setField("tripId",null);screenHome();}else toast("Finaliza o cancela el viaje actual antes de pedir otro.");});
        startRealtime();startPoller();loadTripById();
    }

    @Override void renderTrip(JSONObject t){super.renderTrip(t);WebView m=(WebView)getField("map");if(m!=null){double x1=t.optDouble("origin_lng",getDouble("lng",-77.0428)),y1=t.optDouble("origin_lat",getDouble("lat",-12.0464)),x2=t.optDouble("destination_lng",getDouble("dlng",-77.0228)),y2=t.optDouble("destination_lat",getDouble("dlat",-12.0564));m.evaluateJavascript("drawRoute("+x1+","+y1+","+x2+","+y2+")",null);}}

    @Override void screenPayment(){
        shell("","",0);LinearLayout b=body();LinearLayout c=card();c.addView(tx("Viaje finalizado",25,Color.WHITE,true));String payment=String.valueOf(getField("payment"));c.addView(tx("Método de pago: "+payment.toUpperCase(Locale.ROOT),17,GOLD2,true));c.addView(tx("Gracias por viajar con CiviMoto.",13,MUTED,false));Button again=btn("Volver a pedir viaje",true),history=btn("Ver historial",false);c.addView(again);c.addView(history);b.addView(c);again.setOnClickListener(v->{setField("tripId",null);setField("driverId",null);setField("lastStatus","");screenHome();});history.setOnClickListener(v->screenHistory());
    }

    private void centerActual(WebView m){ui.postDelayed(()->{double la=getDouble("lat",-12.0464),lo=getDouble("lng",-77.0428);if(m!=null)m.evaluateJavascript("setMe("+lo+","+la+")",null);},700);}
    private interface Done{void run();}
    private void resolveAddresses(EditText origin,EditText destination,WebView map,Done done){String o=origin.getText().toString().trim(),d=destination.getText().toString().trim();if(d.isEmpty()&&getBoolean("destReady")){d="Destino marcado en mapa";destination.setText(d);}if(d.isEmpty()){toast("Escribe una dirección de destino o toca un punto en el mapa.");done.run();return;}final String destText=d;new Thread(()->{try{double olat=getDouble("lat",-12.0464),olng=getDouble("lng",-77.0428);if(!o.isEmpty()&&!o.toLowerCase(Locale.ROOT).contains("mi ubicación")){double[] p=geocode(o);olat=p[0];olng=p[1];}double dlat=getDouble("dlat",-12.0564),dlng=getDouble("dlng",-77.0228);if(!"Destino marcado en mapa".equals(destText)){double[] q=geocode(destText);dlat=q[0];dlng=q[1];}else if(!getBoolean("destReady"))throw new Exception("Marca el destino");final double a=olat,b=olng,c=dlat,e=dlng;setDouble("lat",a);setDouble("lng",b);setDouble("dlat",c);setDouble("dlng",e);setBoolean("destReady",true);ui.post(()->{if(map!=null)map.evaluateJavascript("setMe("+b+","+a+");drawRoute("+b+","+a+","+e+","+c+")",null);done.run();});}catch(Exception ex){ui.post(()->{toast("No pude ubicar esa dirección. Escribe más detalle o marca el destino en el mapa.");done.run();});}}).start();}
    private double[] geocode(String text)throws Exception{if(!Geocoder.isPresent())throw new Exception("Geocoder no disponible");Geocoder g=new Geocoder(this,new Locale("es","PE"));List<Address> list=g.getFromLocationName(text+", Perú",1);if(list==null||list.isEmpty())throw new Exception("Dirección no encontrada");Address a=list.get(0);return new double[]{a.getLatitude(),a.getLongitude()};}
    private void prepareAndRequest(EditText origin,EditText destination,WebView map,Button request){resolveAddresses(origin,destination,map,()->{if(!getBoolean("destReady")){request.setEnabled(true);return;}try{String o=origin.getText().toString().trim();if(o.isEmpty())o="Mi ubicación actual";String d=destination.getText().toString().trim();if(d.isEmpty())d="Destino marcado en mapa";String payment=(String)getField("payment");if(payment==null)payment="efectivo";JSONObject data=new JSONObject().put("p_origin_address",o).put("p_origin_lat",getDouble("lat",-12.0464)).put("p_origin_lng",getDouble("lng",-77.0428)).put("p_destination_address",d).put("p_destination_lat",getDouble("dlat",-12.0564)).put("p_destination_lng",getDouble("dlng",-77.0228)).put("p_payment_method",payment);backend().rpc("request_civimoto_trip",data,new Backend.Callback(){public void ok(Object x){JSONObject t=Backend.firstObject(x);if(t!=null){setField("tripId",t.optString("id"));setField("lastStatus",t.optString("status",""));}screenTracking();}public void error(String m){request.setEnabled(true);toast(m);}});}catch(Exception e){request.setEnabled(true);toast("No se pudo preparar la solicitud.");}});}
}
