package pe.civimoto.chofer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import org.json.JSONObject;
import java.lang.reflect.Field;

public class LiveRatingActivity extends InteractiveFlowActivity {
    private final int GOLD=Color.rgb(255,190,0), GOLD2=Color.rgb(255,220,90), MUTED=Color.rgb(176,180,190);
    private String lastCompletedTripId;
    private boolean ratingOpen=false;

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private double dbl(String name,double fallback){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.getDouble(this);}catch(Exception e){return fallback;}}
    private Backend backend(){return (Backend)field("backend");}
    private LinearLayout body(){return (LinearLayout)field("body");}

    @Override WebView createMap(){
        WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);w.setWebViewClient(new WebViewClient());
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"+
                "<link href='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.css' rel='stylesheet'>"+
                "<style>html,body,#m{margin:0;width:100%;height:100%;background:#101216}.moto{font-size:34px;line-height:38px;filter:drop-shadow(0 3px 5px #000)}.pin{width:18px;height:18px;border-radius:50%;border:3px solid #111;box-shadow:0 2px 6px #000}</style></head>"+
                "<body><div id='m'></div><script src='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.js'></script><script>"+
                "let map=new maplibregl.Map({container:'m',style:'https://tiles.openfreemap.org/styles/liberty',center:[-77.0428,-12.0464],zoom:13});let me,a,b;"+
                "function dot(c){let e=document.createElement('div');e.className='pin';e.style.background=c;return e;}function moto(){let e=document.createElement('div');e.className='moto';e.textContent='🛺';return e;}"+
                "function setMe(x,y){if(!me)me=new maplibregl.Marker({element:moto(),anchor:'center'}).setLngLat([x,y]).addTo(map);else me.setLngLat([x,y]);map.easeTo({center:[x,y],duration:600});}"+
                "async function showOffer(x1,y1,x2,y2){if(a)a.remove();if(b)b.remove();a=new maplibregl.Marker({element:dot('#23be69')}).setLngLat([x1,y1]).addTo(map);b=new maplibregl.Marker({element:dot('#ef4040')}).setLngLat([x2,y2]).addTo(map);let coords=[[x1,y1],[x2,y2]];try{let r=await fetch('https://router.project-osrm.org/route/v1/driving/'+x1+','+y1+';'+x2+','+y2+'?overview=full&geometries=geojson');let j=await r.json();if(j.routes&&j.routes.length)coords=j.routes[0].geometry.coordinates;}catch(e){}let geo={type:'Feature',geometry:{type:'LineString',coordinates:coords}};if(map.getSource('route'))map.getSource('route').setData(geo);else{map.addSource('route',{type:'geojson',data:geo});map.addLayer({id:'route-shadow',type:'line',source:'route',paint:{'line-color':'#111','line-width':8,'line-opacity':.65}});map.addLayer({id:'route',type:'line',source:'route',paint:{'line-color':'#ffbe00','line-width':5,'line-opacity':.96}});}let bb=coords.reduce((z,c)=>z.extend(c),new maplibregl.LngLatBounds(coords[0],coords[0]));map.fitBounds(bb,{padding:55,maxZoom:16});}"+
                "</script></body></html>";
        w.loadDataWithBaseURL("https://driver.civimoto.local/",html,"text/html","UTF-8",null);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->w.evaluateJavascript("setMe("+dbl("lng",-77.0428)+","+dbl("lat",-12.0464)+")",null),800);
        return w;
    }

    @Override void transition(String target,Runnable after){
        if("completado".equals(target)){Object id=field("activeTripId");if(id instanceof String)lastCompletedTripId=(String)id;}
        super.transition(target,after);
    }

    @Override void screenEarnings(){
        if(lastCompletedTripId!=null&&!lastCompletedTripId.isEmpty()&&!ratingOpen){showPassengerRating();return;}
        ratingOpen=false;super.screenEarnings();
    }

    private void showPassengerRating(){
        ratingOpen=true;shell("","",0);LinearLayout c=card();c.addView(tx("Califica a tu pasajero",25,Color.WHITE,true));
        c.addView(tx("Selecciona de 1 a 5 estrellas según tu experiencia en este viaje.",13,MUTED,false));
        RatingBar stars=new RatingBar(this);stars.setNumStars(5);stars.setStepSize(1f);stars.setRating(5f);stars.setIsIndicator(false);
        if(Build.VERSION.SDK_INT>=21){stars.setProgressTintList(ColorStateList.valueOf(GOLD));stars.setSecondaryProgressTintList(ColorStateList.valueOf(GOLD2));}
        c.addView(stars,new LinearLayout.LayoutParams(-2,dp(58)));TextView score=tx("5 estrellas",17,GOLD2,true);c.addView(score);stars.setOnRatingBarChangeListener((bar,rating,from)->score.setText(((int)rating)+" estrella"+(rating==1?"":"s")));
        EditText note=input("Comentario opcional");c.addView(note);Button submit=btn("Enviar calificación",true),skip=btn("Continuar sin calificar",false);c.addView(submit);c.addView(skip);body().addView(c);
        submit.setOnClickListener(v->{int n=(int)stars.getRating();if(n<1){toast("Selecciona de 1 a 5 estrellas.");return;}submit.setEnabled(false);try{JSONObject p=new JSONObject().put("p_trip_id",lastCompletedTripId).put("p_score",n).put("p_comment",note.getText().toString().trim());backend().rpc("cm_submit_trip_rating",p,new Backend.Callback(){public void ok(Object x){toast("Calificación enviada al pasajero.");finishRating();}public void error(String m){submit.setEnabled(true);toast(m);}});}catch(Exception e){submit.setEnabled(true);}});
        skip.setOnClickListener(v->finishRating());
    }

    private void finishRating(){lastCompletedTripId=null;ratingOpen=false;super.screenEarnings();}
}
