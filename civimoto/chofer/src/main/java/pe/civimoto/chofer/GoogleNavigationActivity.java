package pe.civimoto.chofer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import org.json.JSONObject;
import java.lang.reflect.Field;

/**
 * Añade navegación externa con Google Maps usando las coordenadas exactas
 * guardadas en el viaje. Antes de recoger al pasajero navega al punto A;
 * una vez iniciado el viaje navega al punto B.
 */
public class GoogleNavigationActivity extends LiveRatingActivity {
    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private Backend backend(){return (Backend)field("backend");}
    private LinearLayout body(){return (LinearLayout)field("body");}

    @Override void screenTrip(){
        super.screenTrip();
        LinearLayout b=body();if(b==null)return;
        Button nav=btn("🧭 Abrir ruta en Google Maps",true);
        b.addView(nav);
        nav.setOnClickListener(v->openCurrentNavigation(nav));
    }

    private void openCurrentNavigation(Button button){
        Object id=field("activeTripId");
        if(!(id instanceof String)||((String)id).isEmpty()){toast("No hay un viaje activo.");return;}
        button.setEnabled(false);
        backend().rest("GET","trips?id=eq."+id+"&select=status,origin_lat,origin_lng,destination_lat,destination_lng,origin_address,destination_address",null,new Backend.Callback(){
            public void ok(Object value){
                button.setEnabled(true);JSONObject t=Backend.firstObject(value);if(t==null){toast("No se encontró el viaje.");return;}
                String status=t.optString("status","");
                boolean toDestination="en_viaje".equals(status);
                double lat=toDestination?t.optDouble("destination_lat"):t.optDouble("origin_lat");
                double lng=toDestination?t.optDouble("destination_lng"):t.optDouble("origin_lng");
                String label=toDestination?t.optString("destination_address","Destino"):t.optString("origin_address","Origen");
                if(lat==0||lng==0){toast("El viaje no tiene coordenadas válidas.");return;}
                launchGoogleMaps(lat,lng,label,toDestination);
            }
            public void error(String m){button.setEnabled(true);toast(m);}
        });
    }

    private void launchGoogleMaps(double lat,double lng,String label,boolean destination){
        try{
            Uri nav=Uri.parse("google.navigation:q="+lat+","+lng+"&mode=d");
            Intent i=new Intent(Intent.ACTION_VIEW,nav);i.setPackage("com.google.android.apps.maps");startActivity(i);
            toast(destination?"Navegando al destino en Google Maps":"Navegando al origen en Google Maps");
        }catch(ActivityNotFoundException e){
            Uri web=Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+lat+","+lng+"&travelmode=driving");
            startActivity(new Intent(Intent.ACTION_VIEW,web));
        }catch(Exception e){toast("No se pudo abrir Google Maps.");}
    }
}
