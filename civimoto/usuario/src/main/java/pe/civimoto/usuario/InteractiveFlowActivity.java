package pe.civimoto.usuario;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

public class InteractiveFlowActivity extends FlowActivity {
    private final int GOLD_ON=Color.rgb(255,195,0);
    private final int GOLD_PRESS=Color.rgb(205,145,0);
    private final int GOLD_DISABLED=Color.rgb(120,92,20);
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

    @Override void screenHome(){
        shell("Solicitar CiviMoto","Pantalla 3 · Origen, destino y mapa",3);
        LinearLayout b=body();
        TextView connection=tx("Conectando con central…",12,Color.rgb(176,180,190),false);setField("connection",connection);b.addView(connection);
        WebView m=createMap();setField("map",m);b.addView(m,new LinearLayout.LayoutParams(-1,dp(330)));

        LinearLayout c=card();
        c.addView(tx("¿De dónde salimos?",21,Color.WHITE,true));
        EditText origin=input("Origen: escribe una dirección o usa GPS");origin.setText("Mi ubicación actual");c.addView(origin);
        Button gps=btn("📍 Usar mi ubicación actual",false);c.addView(gps);
        c.addView(tx("¿A dónde vamos?",21,Color.WHITE,true));
        EditText destination=input("Destino: escribe calle, avenida o referencia");c.addView(destination);
        Button locate=btn("Buscar origen y destino en el mapa",false);c.addView(locate);

        TextView fare=tx("Tarifa calculada por el servidor al solicitar",14,Color.rgb(255,220,90),true);setField("fareState",fare);c.addView(fare);
        LinearLayout pay=new LinearLayout(this);Button cash=btn("Efectivo",false),yape=btn("Yape",false);cash.setSelected(true);pay.addView(cash,new LinearLayout.LayoutParams(0,dp(54),1));pay.addView(yape,new LinearLayout.LayoutParams(0,dp(54),1));c.addView(pay);
        Button request=btn("Solicitar mototaxi",true),history=btn("Historial y perfil",false);c.addView(request);c.addView(history);b.addView(c);

        cash.setOnClickListener(v->{setField("payment","efectivo");cash.setSelected(true);yape.setSelected(false);toast("Pago: efectivo");});
        yape.setOnClickListener(v->{setField("payment","yape");yape.setSelected(true);cash.setSelected(false);toast("Pago: Yape");});
        gps.setOnClickListener(v->{freshLocation();origin.setText("Mi ubicación actual");ui.postDelayed(()->{double lat=getDouble("lat",-12.0464),lng=getDouble("lng",-77.0428);if(m!=null)m.evaluateJavascript("setMe("+lng+","+lat+")",null);},500);});
        history.setOnClickListener(v->screenHistory());
        locate.setOnClickListener(v->{locate.setEnabled(false);resolveAddresses(origin,destination,m,()->locate.setEnabled(true));});
        request.setOnClickListener(v->{request.setEnabled(false);prepareAndRequest(origin,destination,m,request);});
        freshLocation();loadActiveAndRoute();
    }

    private interface Done{void run();}
    private void resolveAddresses(EditText origin,EditText destination,WebView map,Done done){
        String o=origin.getText().toString().trim(),d=destination.getText().toString().trim();
        if(d.isEmpty()&&getBoolean("destReady")){d="Destino marcado en mapa";destination.setText(d);}
        if(d.isEmpty()){toast("Escribe una dirección de destino o toca un punto en el mapa.");done.run();return;}
        final String destText=d;
        new Thread(()->{
            try{
                double olat=getDouble("lat",-12.0464),olng=getDouble("lng",-77.0428);
                if(!o.isEmpty()&&!o.toLowerCase(Locale.ROOT).contains("mi ubicación")){
                    double[] p=geocode(o);olat=p[0];olng=p[1];
                }
                double dlat=getDouble("dlat",-12.0564),dlng=getDouble("dlng",-77.0228);
                if(!"Destino marcado en mapa".equals(destText)){
                    double[] q=geocode(destText);dlat=q[0];dlng=q[1];
                }else if(!getBoolean("destReady")){throw new Exception("Marca el destino en el mapa");}
                final double foLat=olat,foLng=olng,fdLat=dlat,fdLng=dlng;
                setDouble("lat",foLat);setDouble("lng",foLng);setDouble("dlat",fdLat);setDouble("dlng",fdLng);setBoolean("destReady",true);
                ui.post(()->{if(map!=null)map.evaluateJavascript("setMe("+foLng+","+foLat+");if(dest)dest.remove();dest=new maplibregl.Marker({color:'#ef4040'}).setLngLat(["+fdLng+","+fdLat+"]).addTo(map);map.fitBounds([["+foLng+","+foLat+"],["+fdLng+","+fdLat+"]],{padding:50});",null);toast("Origen y destino ubicados.");done.run();});
            }catch(Exception e){ui.post(()->{toast("No pude ubicar esa dirección. Escribe más detalle o marca el destino en el mapa.");done.run();});}
        }).start();
    }

    private double[] geocode(String text)throws Exception{
        if(!Geocoder.isPresent())throw new Exception("Geocoder no disponible");
        Geocoder g=new Geocoder(this,new Locale("es","PE"));List<Address> list=g.getFromLocationName(text+", Perú",1);if(list==null||list.isEmpty())throw new Exception("Dirección no encontrada");Address a=list.get(0);return new double[]{a.getLatitude(),a.getLongitude()};
    }

    private void prepareAndRequest(EditText origin,EditText destination,WebView map,Button request){
        resolveAddresses(origin,destination,map,()->{
            if(!getBoolean("destReady")){request.setEnabled(true);return;}
            try{
                String o=origin.getText().toString().trim();if(o.isEmpty())o="Mi ubicación actual";
                String d=destination.getText().toString().trim();if(d.isEmpty())d="Destino marcado en mapa";
                String payment=(String)getField("payment");if(payment==null)payment="efectivo";
                JSONObject data=new JSONObject().put("p_origin_address",o).put("p_origin_lat",getDouble("lat",-12.0464)).put("p_origin_lng",getDouble("lng",-77.0428)).put("p_destination_address",d).put("p_destination_lat",getDouble("dlat",-12.0564)).put("p_destination_lng",getDouble("dlng",-77.0228)).put("p_payment_method",payment);
                backend().rpc("request_civimoto_trip",data,new Backend.Callback(){public void ok(Object x){JSONObject t=Backend.firstObject(x);if(t!=null){setField("tripId",t.optString("id"));setField("lastStatus",t.optString("status",""));}screenTracking();}public void error(String m){request.setEnabled(true);toast(m);}});
            }catch(Exception e){request.setEnabled(true);toast("No se pudo preparar la solicitud.");}
        });
    }
}
