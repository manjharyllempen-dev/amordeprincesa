package pe.civimoto.usuario;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

public class MainActivity extends Activity {
    private final int gold = Color.rgb(255,190,0);
    private final int bg = Color.rgb(8,8,10);
    private final int surface = Color.rgb(24,24,28);
    private final int muted = Color.rgb(170,170,178);
    private TextView rideStatus;
    private WebView map;
    private String payment = "Efectivo";

    int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density); }
    GradientDrawable box(int color,int radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    TextView text(String s,int sp,int color,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setPadding(0,dp(4),0,dp(4)); if(bold)t.setTypeface(null,1); return t; }
    Button button(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.BLACK); b.setTypeface(null,1); b.setBackground(box(gold,16)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52)); lp.setMargins(0,dp(6),0,dp(6)); b.setLayoutParams(lp); return b; }
    LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18),dp(16),dp(18),dp(16)); c.setBackground(box(surface,20)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(8),0,dp(8)); c.setLayoutParams(lp); return c; }
    ImageView logo(int size){ ImageView i=new ImageView(this); i.setImageResource(R.drawable.logo_civimoto); i.setScaleType(ImageView.ScaleType.CENTER_CROP); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(size),dp(size)); i.setLayoutParams(lp); return i; }

    @Override public void onCreate(Bundle b){ super.onCreate(b); if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},20); showSplash(); }

    void showSplash(){
        LinearLayout s=new LinearLayout(this); s.setOrientation(LinearLayout.VERTICAL); s.setGravity(Gravity.CENTER); s.setPadding(dp(30),dp(30),dp(30),dp(30)); s.setBackgroundColor(Color.BLACK);
        ImageView l=logo(230); s.addView(l);
        TextView brand=text("CiviMoto",44,Color.WHITE,true); brand.setGravity(Gravity.CENTER); s.addView(brand);
        TextView sub=text("Tu destino, nuestra ruta",17,gold,true); sub.setGravity(Gravity.CENTER); s.addView(sub);
        TextView role=text("PASAJERO",13,muted,true); role.setGravity(Gravity.CENTER); role.setPadding(0,dp(16),0,0); s.addView(role);
        setContentView(s); new Handler(Looper.getMainLooper()).postDelayed(this::build,1200);
    }

    void build(){
        ScrollView scroll=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(18),dp(16),dp(80)); root.setBackgroundColor(bg); scroll.addView(root); setContentView(scroll);
        LinearLayout h=new LinearLayout(this); h.setGravity(Gravity.CENTER_VERTICAL); h.addView(logo(62)); LinearLayout ht=new LinearLayout(this); ht.setOrientation(LinearLayout.VERTICAL); ht.setPadding(dp(12),0,0,0); ht.addView(text("CiviMoto",30,Color.WHITE,true)); ht.addView(text("Pasajero · Rápido · Seguro · Confiable",13,muted,false)); h.addView(ht); root.addView(h);

        TextView hello=text("Hola 👋 ¿A dónde vamos?",25,Color.WHITE,true); hello.setPadding(0,dp(18),0,dp(8)); root.addView(hello);
        map=createMap(); root.addView(map,new LinearLayout.LayoutParams(-1,dp(300)));

        LinearLayout form=card(); form.addView(text("Solicitar mototaxi",20,Color.WHITE,true));
        EditText origin=input("Origen", "Mi ubicación actual"); form.addView(origin);
        EditText dest=input("Destino", "Plaza de Armas"); form.addView(dest);
        LinearLayout payRow=new LinearLayout(this); payRow.setOrientation(LinearLayout.HORIZONTAL); Button cash=button("Efectivo"), yape=button("Yape"); payRow.addView(cash,new LinearLayout.LayoutParams(0,dp(52),1)); payRow.addView(yape,new LinearLayout.LayoutParams(0,dp(52),1)); form.addView(text("Método de pago",14,muted,true)); form.addView(payRow);
        TextView fare=text("Tarifa referencial: S/ 5.50",18,gold,true); form.addView(fare);
        Button locate=button("📍 Usar mi ubicación"); Button request=button("Solicitar CiviMoto"); form.addView(locate); form.addView(request); rideStatus=text("Listo para solicitar un viaje.",14,muted,false); form.addView(rideStatus); root.addView(form);

        LinearLayout active=card(); active.addView(text("Viaje / seguridad",20,Color.WHITE,true)); active.addView(text("Cuando un chofer acepte verás aquí su nombre, placa, calificación y ubicación.",14,muted,false)); Button share=button("Compartir viaje"); active.addView(share); Button cancel=button("Cancelar viaje"); active.addView(cancel); root.addView(active);

        cash.setOnClickListener(v->{payment="Efectivo"; rideStatus.setText("Pago seleccionado: Efectivo");});
        yape.setOnClickListener(v->{payment="Yape"; rideStatus.setText("Pago seleccionado: Yape");});
        locate.setOnClickListener(v->map.evaluateJavascript("locateMe()",null));
        request.setOnClickListener(v->{ rideStatus.setText("Buscando mototaxis cercanos… · Pago: "+payment); request.setEnabled(false); new Handler(Looper.getMainLooper()).postDelayed(()->{rideStatus.setText("✓ Chofer encontrado: Carlos M. · ⭐ 4.9 · Placa MOTO-123 · llega en 4 min"); request.setText("Viaje solicitado"); map.evaluateJavascript("demoRoute()",null);},1800); });
        share.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,"Estoy viajando con CiviMoto. Comparte mi viaje y ubicación por seguridad. Tu destino, nuestra ruta."); startActivity(Intent.createChooser(i,"Compartir viaje Civimoto")); });
        cancel.setOnClickListener(v->{rideStatus.setText("Viaje cancelado. Puedes solicitar otro."); request.setEnabled(true); request.setText("Solicitar CiviMoto");});
    }

    EditText input(String hint,String value){ EditText e=new EditText(this); e.setHint(hint); e.setText(value); e.setTextColor(Color.WHITE); e.setHintTextColor(muted); e.setSingleLine(true); e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold)); e.setPadding(dp(4),dp(8),dp(4),dp(8)); return e; }

    WebView createMap(){
        WebView w=new WebView(this); WebSettings s=w.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setGeolocationEnabled(true); w.setWebViewClient(new WebViewClient()); w.setWebChromeClient(new WebChromeClient(){ @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb){ cb.invoke(origin,true,false); } });
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'><link href='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.css' rel='stylesheet'><style>html,body,#map{margin:0;width:100%;height:100%;background:#17171b}.maplibregl-ctrl-attrib{font-size:8px}</style></head><body><div id='map'></div><script src='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.js'></script><script>let map=new maplibregl.Map({container:'map',style:'https://tiles.openfreemap.org/styles/liberty',center:[-77.0428,-12.0464],zoom:13});let me;function locateMe(){navigator.geolocation.getCurrentPosition(p=>{let c=[p.coords.longitude,p.coords.latitude];if(me)me.remove();me=new maplibregl.Marker({color:'#ffbe00'}).setLngLat(c).addTo(map);map.flyTo({center:c,zoom:15});},()=>{});}function demoRoute(){let a=me?me.getLngLat().toArray():[-77.0428,-12.0464],b=[a[0]+0.025,a[1]+0.016];new maplibregl.Marker({color:'#ff3344'}).setLngLat(b).addTo(map);let src=map.getSource('route');let data={type:'Feature',geometry:{type:'LineString',coordinates:[a,b]}};if(src)src.setData(data);else{map.addSource('route',{type:'geojson',data:data});map.addLayer({id:'route',type:'line',source:'route',paint:{'line-color':'#ffbe00','line-width':6}});}map.fitBounds([a,b],{padding:45});}map.on('load',locateMe);</script></body></html>";
        w.loadDataWithBaseURL("https://app.civimoto.local/",html,"text/html","UTF-8",null); return w;
    }
}
