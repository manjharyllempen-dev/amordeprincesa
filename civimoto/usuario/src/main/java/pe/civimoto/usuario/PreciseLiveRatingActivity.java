package pe.civimoto.usuario;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Geocodificación zonificada para La Libertad: ciudad/distrito + provincia. */
public class PreciseLiveRatingActivity extends LiveRatingActivity {
    private static final double LLAT=-9.10, LLNG=-79.85, ULAT=-6.75, ULNG=-77.25;
    private static final double TRU_LAT=-8.1116, TRU_LNG=-79.0288;
    private final Handler zoneUi=new Handler(Looper.getMainLooper());
    private TextView originZone,destinationZone;
    private Runnable originPreview,destinationPreview;
    private AddressPick lastOriginPick,lastDestinationPick;

    private static final String[] TRUJILLO_DISTRICTS={"Trujillo","El Porvenir","Florencia de Mora","Huanchaco","La Esperanza","Laredo","Moche","Poroto","Salaverry","Simbal","Víctor Larco Herrera","Alto Trujillo"};
    private static final String[] PROVINCES={"Trujillo","Ascope","Bolívar","Chepén","Gran Chimú","Julcán","Otuzco","Pacasmayo","Pataz","Sánchez Carrión","Santiago de Chuco","Virú"};

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private void field(String name,Object value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}catch(Exception ignored){}}
    private double dbl(String name,double fallback){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.getDouble(this);}catch(Exception e){return fallback;}}
    private void setDbl(String name,double value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(this,value);}catch(Exception ignored){}}
    private void bool(String name,boolean value){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);f.setBoolean(this,value);}catch(Exception ignored){}}
    private boolean getBool(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.getBoolean(this);}catch(Exception e){return false;}}
    private Backend backend(){return (Backend)field("backend");}

    @Override void screenHome(){
        super.screenHome();
        View root=findViewById(android.R.id.content);
        EditText origin=findEdit(root,"Origen:");EditText destination=findEdit(root,"Destino:");Button request=findButton(root,"Solicitar mototaxi");
        if(origin==null||destination==null||request==null)return;
        originZone=attachZoneLabel(origin,"Zona de origen: detectando…");
        destinationZone=attachZoneLabel(destination,"Zona de destino: escribe calle + distrito o ciudad");
        installPreview(origin,originZone,true);installPreview(destination,destinationZone,false);
        previewGpsZone();
        request.setOnClickListener(v->{request.setEnabled(false);resolveAndRequest(origin,destination,request);});
    }

    private TextView attachZoneLabel(EditText edit,String initial){
        TextView t=new TextView(this);t.setText(initial);t.setTextColor(Color.rgb(255,220,90));t.setTextSize(13);t.setPadding(dp(4),0,dp(4),dp(7));
        if(edit.getParent() instanceof ViewGroup){ViewGroup p=(ViewGroup)edit.getParent();int i=p.indexOfChild(edit);p.addView(t,Math.min(i+1,p.getChildCount()));}
        return t;
    }

    private void installPreview(EditText edit,TextView label,boolean isOrigin){
        edit.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){}
            public void afterTextChanged(Editable e){
                String text=e.toString().trim();Runnable old=isOrigin?originPreview:destinationPreview;if(old!=null)zoneUi.removeCallbacks(old);
                Runnable r=()->previewAddress(text,label,isOrigin);if(isOrigin)originPreview=r;else destinationPreview=r;zoneUi.postDelayed(r,850);
            }
        });
    }

    private void previewAddress(String text,TextView label,boolean isOrigin){
        if(text.isEmpty()){label.setText(isOrigin?"Zona de origen: usa GPS o escribe una dirección":"Zona de destino: escribe calle + distrito o ciudad");return;}
        if(isOrigin&&text.toLowerCase(Locale.ROOT).contains("mi ubicación")){previewGpsZone();return;}
        if(text.length()<3)return;
        label.setText((isOrigin?"Zona de origen: ":"Zona de destino: ")+"buscando…");
        new Thread(()->{try{AddressPick p=preciseGeocode(text,isOrigin?dbl("lat",TRU_LAT):dbl("dlat",TRU_LAT),isOrigin?dbl("lng",TRU_LNG):dbl("dlng",TRU_LNG),false);runOnUiThread(()->{if(isOrigin)lastOriginPick=p;else lastDestinationPick=p;label.setText(zoneText(isOrigin,p));});}catch(Exception ex){runOnUiThread(()->label.setText((isOrigin?"Zona de origen: ":"Zona de destino: ")+"no definida — agrega distrito o ciudad"));}}).start();
    }

    private void previewGpsZone(){
        if(originZone==null)return;new Thread(()->{try{Geocoder g=new Geocoder(this,new Locale("es","PE"));List<Address> x=g.getFromLocation(dbl("lat",TRU_LAT),dbl("lng",TRU_LNG),3);if(x!=null&&!x.isEmpty()){AddressPick p=pickFromAddress(x.get(0),"Ubicación GPS");runOnUiThread(()->{lastOriginPick=p;originZone.setText(zoneText(true,p));});}}catch(Exception ignored){}}).start();
    }

    private String zoneText(boolean origin,AddressPick p){return (origin?"Zona de origen: ":"Zona de destino: ")+"Distrito/Ciudad: "+p.district+" · Provincia: "+p.province;}

    private void resolveAndRequest(EditText origin,EditText destination,Button request){
        final String originText=origin.getText().toString().trim(),destinationText=destination.getText().toString().trim();
        if(destinationText.isEmpty()&&!getBool("destReady")){toast("Escribe el destino o marca un punto en el mapa.");request.setEnabled(true);return;}
        new Thread(()->{
            try{
                double oLat=dbl("lat",TRU_LAT),oLng=dbl("lng",TRU_LNG);AddressPick op=lastOriginPick;
                if(!originText.isEmpty()&&!originText.toLowerCase(Locale.ROOT).contains("mi ubicación")){op=preciseGeocode(originText,oLat,oLng,true);oLat=op.lat;oLng=op.lng;}
                else if(op==null){op=reversePick(oLat,oLng,"Mi ubicación actual");}
                double dLat=dbl("dlat",TRU_LAT),dLng=dbl("dlng",TRU_LNG);AddressPick dp=lastDestinationPick;
                if(!destinationText.isEmpty()){dp=preciseGeocode(destinationText,oLat,oLng,true);dLat=dp.lat;dLng=dp.lng;}
                else if(getBool("destReady")){dp=reversePick(dLat,dLng,"Destino marcado en mapa");}
                else throw new Exception("Destino no definido");

                final AddressPick fop=op,fdp=dp;final double foLat=oLat,foLng=oLng,fdLat=dLat,fdLng=dLng;
                setDbl("lat",foLat);setDbl("lng",foLng);setDbl("dlat",fdLat);setDbl("dlng",fdLng);bool("destReady",true);
                runOnUiThread(()->{
                    lastOriginPick=fop;lastDestinationPick=fdp;if(originZone!=null)originZone.setText(zoneText(true,fop));if(destinationZone!=null)destinationZone.setText(zoneText(false,fdp));
                    WebView map=(WebView)field("map");if(map!=null)map.evaluateJavascript("setMe("+foLng+","+foLat+");drawRoute("+foLng+","+foLat+","+fdLng+","+fdLat+")",null);
                    sendTrip(originText,destinationText,fop,fdp,request);
                });
            }catch(Exception e){runOnUiThread(()->{request.setEnabled(true);toast(e.getMessage()!=null&&e.getMessage().startsWith("AMBIGUA")?e.getMessage().substring(8):"No pude ubicar esa dirección en La Libertad. Agrega calle, número y distrito/ciudad.");});}
        }).start();
    }

    private void sendTrip(String originText,String destinationText,AddressPick op,AddressPick dp,Button request){
        try{
            String o=classifiedAddress(originText.isEmpty()?"Mi ubicación actual":originText,op),d=classifiedAddress(destinationText.isEmpty()?"Destino marcado en mapa":destinationText,dp);
            String payment=String.valueOf(field("payment"));if(payment==null||"null".equals(payment))payment="efectivo";
            JSONObject data=new JSONObject().put("p_origin_address",o).put("p_origin_lat",dbl("lat",TRU_LAT)).put("p_origin_lng",dbl("lng",TRU_LNG))
                    .put("p_destination_address",d).put("p_destination_lat",dbl("dlat",TRU_LAT)).put("p_destination_lng",dbl("dlng",TRU_LNG)).put("p_payment_method",payment);
            backend().rpc("request_civimoto_trip",data,new Backend.Callback(){public void ok(Object x){JSONObject t=Backend.firstObject(x);if(t!=null){field("tripId",t.optString("id"));field("lastStatus",t.optString("status",""));}screenTracking();}public void error(String m){request.setEnabled(true);toast(m);}});
        }catch(Exception e){request.setEnabled(true);toast("No se pudo preparar la solicitud.");}
    }

    private String classifiedAddress(String raw,AddressPick p){return raw+" · Distrito/Ciudad: "+p.district+" · Provincia: "+p.province;}

    private AddressPick preciseGeocode(String raw,double nearLat,double nearLng,boolean strict)throws Exception{
        if(!Geocoder.isPresent())throw new Exception("Geocoder no disponible");
        String q=raw.trim();String low=norm(q);if(!low.contains("la libertad"))q+=", La Libertad";if(!low.contains("peru"))q+=", Perú";
        Geocoder g=new Geocoder(this,new Locale("es","PE"));List<Address> list=g.getFromLocationName(q,15,LLAT,LLNG,ULAT,ULNG);if(list==null||list.isEmpty())list=g.getFromLocationName(q,15);if(list==null||list.isEmpty())throw new Exception("Dirección no encontrada");
        String explicitDistrict=matchKnown(raw,TRUJILLO_DISTRICTS),explicitProvince=matchKnown(raw,PROVINCES),number=extractNumber(raw);List<String> toks=tokens(raw);
        ArrayList<Scored> candidates=new ArrayList<>();
        for(Address a:list){double la=a.getLatitude(),lo=a.getLongitude();if(la<LLAT||la>ULAT||lo<LLNG||lo>ULNG)continue;AddressPick p=pickFromAddress(a,raw);String all=norm(addressLabel(a)+" "+p.district+" "+p.province+" "+safe(a.getAdminArea()));double s=0;if(all.contains("la libertad"))s+=70;if(number!=null&&all.contains(number))s+=160;for(String t:toks)if(all.contains(norm(t)))s+=28;if(explicitDistrict!=null&&norm(p.district).contains(norm(explicitDistrict)))s+=320;if(explicitProvince!=null&&norm(p.province).contains(norm(explicitProvince)))s+=120;s-=distanceKm(nearLat,nearLng,la,lo)*0.08;candidates.add(new Scored(p,s));}
        if(candidates.isEmpty())throw new Exception("Fuera de La Libertad");Collections.sort(candidates,(a,b)->Double.compare(b.score,a.score));
        if(strict&&explicitDistrict==null&&candidates.size()>1){Scored a=candidates.get(0),b=candidates.get(1);if(!norm(a.p.district).equals(norm(b.p.district))&&a.score-b.score<45)throw new Exception("AMBIGUAHay varias coincidencias. Agrega el distrito o ciudad, por ejemplo: Laredo, Trujillo, Moche o Huanchaco.");}
        return candidates.get(0).p;
    }

    private AddressPick reversePick(double lat,double lng,String fallback)throws Exception{Geocoder g=new Geocoder(this,new Locale("es","PE"));List<Address> x=g.getFromLocation(lat,lng,5);if(x==null||x.isEmpty())return new AddressPick(lat,lng,fallback,"No definido","La Libertad");return pickFromAddress(x.get(0),fallback);}
    private AddressPick pickFromAddress(Address a,String raw){String explicit=matchKnown(raw,TRUJILLO_DISTRICTS);String district=explicit!=null?explicit:firstNonEmpty(a.getSubLocality(),a.getLocality(),a.getFeatureName(),"No definido");String province=firstNonEmpty(a.getSubAdminArea(),"No definido");if(isTrujilloDistrict(district))province="Trujillo";return new AddressPick(a.getLatitude(),a.getLongitude(),addressLabel(a),district,province);}
    private static boolean isTrujilloDistrict(String d){for(String x:TRUJILLO_DISTRICTS)if(norm(x).equals(norm(d)))return true;return false;}
    private static String matchKnown(String raw,String[] known){String n=norm(raw);String best=null;for(String x:known)if(n.contains(norm(x))&&(best==null||x.length()>best.length()))best=x;return best;}
    private static String firstNonEmpty(String... x){for(String s:x)if(s!=null&&!s.trim().isEmpty())return s.trim();return "No definido";}
    private static String safe(String s){return s==null?"":s;}
    private static String norm(String s){if(s==null)return"";String n=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);return n.replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();}
    private static String addressLabel(Address a){String x=a.getAddressLine(0);if(x!=null&&!x.trim().isEmpty())return x;return firstNonEmpty(a.getThoroughfare(),a.getFeatureName(),a.getLocality(),"Punto seleccionado");}
    private static String extractNumber(String s){Matcher m=Pattern.compile("\\b\\d{1,5}\\b").matcher(s);return m.find()?m.group():null;}
    private static List<String> tokens(String s){String[] p=norm(s).split("\\s+");List<String> r=new ArrayList<>();for(String x:p)if(x.length()>=3&&!x.equals("calle")&&!x.equals("avenida")&&!x.equals("libertad")&&!x.equals("peru"))r.add(x);return r;}
    private static double distanceKm(double a,double b,double c,double d){double R=6371,dp=Math.toRadians(c-a),dl=Math.toRadians(d-b),x=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.sin(dl/2)*Math.sin(dl/2);return 2*R*Math.atan2(Math.sqrt(x),Math.sqrt(1-x));}

    private EditText findEdit(View v,String hint){if(v instanceof EditText){CharSequence h=((EditText)v).getHint();if(h!=null&&h.toString().contains(hint))return(EditText)v;}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){EditText e=findEdit(g.getChildAt(i),hint);if(e!=null)return e;}}return null;}
    private Button findButton(View v,String text){if(v instanceof Button&&text.equals(((Button)v).getText().toString()))return(Button)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button b=findButton(g.getChildAt(i),text);if(b!=null)return b;}}return null;}
    private static class AddressPick{final double lat,lng;final String label,district,province;AddressPick(double a,double b,String c,String d,String p){lat=a;lng=b;label=c;district=d;province=p;}}
    private static class Scored{final AddressPick p;final double score;Scored(AddressPick p,double s){this.p=p;score=s;}}
}
