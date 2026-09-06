package pe.civimoto.usuario;

import android.location.Address;
import android.location.Geocoder;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Refuerza la resolución de direcciones del pasajero para evitar coincidencias
 * lejanas. Las búsquedas manuales quedan sesgadas al área metropolitana de
 * Trujillo, La Libertad, y el punto elegido se guarda como coordenada real A/B.
 */
public class PreciseLiveRatingActivity extends LiveRatingActivity {
    private static final double TRU_LLAT=-8.30, TRU_LLNG=-79.22, TRU_ULAT=-7.92, TRU_ULNG=-78.82;
    private static final double TRU_CENTER_LAT=-8.1116, TRU_CENTER_LNG=-79.0288;

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private void field(String name,Object value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}catch(Exception ignored){}}
    private double dbl(String name,double fallback){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.getDouble(this);}catch(Exception e){return fallback;}}
    private void dbl(String name,double value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(this,value);}catch(Exception ignored){}}
    private void bool(String name,boolean value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.setBoolean(this,value);}catch(Exception ignored){}}
    private Backend backend(){return (Backend)field("backend");}

    @Override void screenHome(){
        super.screenHome();
        View root=findViewById(android.R.id.content);
        EditText origin=findEdit(root,"Origen:");
        EditText destination=findEdit(root,"Destino:");
        Button request=findButton(root,"Solicitar mototaxi");
        if(origin==null||destination==null||request==null)return;
        request.setOnClickListener(v->{
            request.setEnabled(false);
            resolveAndRequest(origin,destination,request);
        });
    }

    private void resolveAndRequest(EditText origin,EditText destination,Button request){
        final String originText=origin.getText().toString().trim();
        final String destinationText=destination.getText().toString().trim();
        if(destinationText.isEmpty()&&!getBool("destReady")){
            toast("Escribe una dirección de destino o marca el destino en el mapa.");request.setEnabled(true);return;
        }
        new Thread(()->{
            try{
                double oLat=dbl("lat",TRU_CENTER_LAT),oLng=dbl("lng",TRU_CENTER_LNG);
                if(!originText.isEmpty()&&!originText.toLowerCase(Locale.ROOT).contains("mi ubicación")){
                    AddressPick p=preciseGeocode(originText,oLat,oLng);oLat=p.lat;oLng=p.lng;
                }
                double dLat=dbl("dlat",TRU_CENTER_LAT),dLng=dbl("dlng",TRU_CENTER_LNG);
                String resolvedDestination=destinationText;
                if(!destinationText.isEmpty()){
                    AddressPick p=preciseGeocode(destinationText,oLat,oLng);dLat=p.lat;dLng=p.lng;resolvedDestination=p.label;
                }else if(!getBool("destReady"))throw new Exception("Destino no definido");

                final double foLat=oLat,foLng=oLng,fdLat=dLat,fdLng=dLng;
                final String displayDestination=resolvedDestination;
                dbl("lat",foLat);dbl("lng",foLng);dbl("dlat",fdLat);dbl("dlng",fdLng);bool("destReady",true);
                runOnUiThread(()->{
                    WebView map=(WebView)field("map");
                    if(map!=null)map.evaluateJavascript("setMe("+foLng+","+foLat+");drawRoute("+foLng+","+foLat+","+fdLng+","+fdLat+")",null);
                    if(displayDestination!=null&&!displayDestination.isEmpty())toast("Destino ubicado en Trujillo: "+displayDestination);
                    sendTrip(originText,destinationText,request);
                });
            }catch(Exception e){runOnUiThread(()->{request.setEnabled(true);toast("No pude ubicar esa dirección con precisión en Trujillo. Agrega calle, número y Trujillo, o marca el punto en el mapa.");});}
        }).start();
    }

    private void sendTrip(String originText,String destinationText,Button request){
        try{
            String o=originText.isEmpty()?"Mi ubicación actual":originText;
            String d=destinationText.isEmpty()?"Destino marcado en mapa":destinationText;
            String payment=String.valueOf(field("payment"));if(payment==null||"null".equals(payment))payment="efectivo";
            JSONObject data=new JSONObject()
                    .put("p_origin_address",o).put("p_origin_lat",dbl("lat",TRU_CENTER_LAT)).put("p_origin_lng",dbl("lng",TRU_CENTER_LNG))
                    .put("p_destination_address",d).put("p_destination_lat",dbl("dlat",TRU_CENTER_LAT)).put("p_destination_lng",dbl("dlng",TRU_CENTER_LNG))
                    .put("p_payment_method",payment);
            backend().rpc("request_civimoto_trip",data,new Backend.Callback(){
                public void ok(Object x){JSONObject t=Backend.firstObject(x);if(t!=null){field("tripId",t.optString("id"));field("lastStatus",t.optString("status",""));}screenTracking();}
                public void error(String m){request.setEnabled(true);toast(m);}
            });
        }catch(Exception e){request.setEnabled(true);toast("No se pudo preparar la solicitud.");}
    }

    private AddressPick preciseGeocode(String raw,double nearLat,double nearLng)throws Exception{
        if(!Geocoder.isPresent())throw new Exception("Geocoder no disponible");
        String q=raw.trim();String low=q.toLowerCase(Locale.ROOT);
        if(!low.contains("trujillo"))q=q+", Trujillo";
        if(!low.contains("la libertad"))q=q+", La Libertad";
        if(!low.contains("perú")&&!low.contains("peru"))q=q+", Perú";
        Geocoder g=new Geocoder(this,new Locale("es","PE"));
        List<Address> list=g.getFromLocationName(q,10,TRU_LLAT,TRU_LLNG,TRU_ULAT,TRU_ULNG);
        if(list==null||list.isEmpty()){
            list=g.getFromLocationName(raw+", Trujillo, La Libertad, Perú",10);
        }
        if(list==null||list.isEmpty())throw new Exception("Dirección no encontrada");
        Address best=null;double bestScore=-1e9;String number=extractNumber(raw);List<String> tokens=tokens(raw);
        for(Address a:list){
            double la=a.getLatitude(),lo=a.getLongitude();
            if(la<TRU_LLAT||la>TRU_ULAT||lo<TRU_LLNG||lo>TRU_ULNG)continue;
            String label=addressLabel(a).toLowerCase(Locale.ROOT);double score=0;
            if(label.contains("trujillo"))score+=80;if(label.contains("la libertad"))score+=35;
            if(number!=null&&label.contains(number))score+=120;
            for(String t:tokens)if(label.contains(t))score+=12;
            score-=distanceKm(nearLat,nearLng,la,lo)*0.8;
            score-=distanceKm(TRU_CENTER_LAT,TRU_CENTER_LNG,la,lo)*0.15;
            if(score>bestScore){bestScore=score;best=a;}
        }
        if(best==null)throw new Exception("Fuera de Trujillo");
        return new AddressPick(best.getLatitude(),best.getLongitude(),addressLabel(best));
    }

    private static String addressLabel(Address a){String x=a.getAddressLine(0);if(x!=null&&!x.trim().isEmpty())return x;StringBuilder s=new StringBuilder();if(a.getThoroughfare()!=null)s.append(a.getThoroughfare());if(a.getSubThoroughfare()!=null)s.append(" ").append(a.getSubThoroughfare());if(a.getLocality()!=null)s.append(", ").append(a.getLocality());return s.length()==0?"Punto seleccionado":s.toString();}
    private static String extractNumber(String s){Matcher m=Pattern.compile("\\b\\d{1,5}\\b").matcher(s);return m.find()?m.group():null;}
    private static List<String> tokens(String s){String n=s.toLowerCase(Locale.ROOT).replaceAll("[^a-záéíóúñ0-9 ]"," ");String[] p=n.split("\\s+");List<String> r=new ArrayList<>();for(String x:p)if(x.length()>=4&&!x.equals("calle")&&!x.equals("avenida")&&!x.equals("trujillo")&&!x.equals("libertad"))r.add(x);return r;}
    private static double distanceKm(double a,double b,double c,double d){double R=6371.0,dp=Math.toRadians(c-a),dl=Math.toRadians(d-b),x=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.sin(dl/2)*Math.sin(dl/2);return 2*R*Math.atan2(Math.sqrt(x),Math.sqrt(1-x));}
    private boolean getBool(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.getBoolean(this);}catch(Exception e){return false;}}

    private EditText findEdit(View v,String hint){if(v instanceof EditText){CharSequence h=((EditText)v).getHint();if(h!=null&&h.toString().contains(hint))return(EditText)v;}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){EditText e=findEdit(g.getChildAt(i),hint);if(e!=null)return e;}}return null;}
    private Button findButton(View v,String text){if(v instanceof Button&&text.equals(((Button)v).getText().toString()))return(Button)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button b=findButton(g.getChildAt(i),text);if(b!=null)return b;}}return null;}
    private static class AddressPick{final double lat,lng;final String label;AddressPick(double a,double b,String c){lat=a;lng=b;label=c;}}
}
