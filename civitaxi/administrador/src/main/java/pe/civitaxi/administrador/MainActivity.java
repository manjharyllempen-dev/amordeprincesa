package pe.civimoto.administrador;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

public class MainActivity extends Activity {
    private final int gold=Color.rgb(255,190,0), bg=Color.rgb(8,8,10), surface=Color.rgb(24,24,28), muted=Color.rgb(170,170,178);
    private TextView paymentState;
    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView text(String s,int sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,dp(4),0,dp(4));if(b)v.setTypeface(null,1);return v;}
    Button button(String s){Button x=new Button(this);x.setText(s);x.setTextColor(Color.BLACK);x.setTypeface(null,1);x.setBackground(box(gold,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(6),0,dp(6));x.setLayoutParams(lp);return x;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(box(surface,20));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));c.setLayoutParams(lp);return c;}
    ImageView logo(int size){ImageView i=new ImageView(this);i.setImageResource(R.drawable.logo_civimoto);i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return i;}
    @Override public void onCreate(Bundle b){super.onCreate(b);showSplash();}
    void showSplash(){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setGravity(Gravity.CENTER);s.setBackgroundColor(Color.BLACK);s.addView(logo(230));TextView brand=text("CiviMoto",44,Color.WHITE,true);brand.setGravity(Gravity.CENTER);s.addView(brand);TextView sub=text("Administrador · Control total",17,gold,true);sub.setGravity(Gravity.CENTER);s.addView(sub);setContentView(s);new Handler(Looper.getMainLooper()).postDelayed(this::build,1200);}
    void build(){ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(80));root.setBackgroundColor(bg);sc.addView(root);setContentView(sc);
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.addView(logo(62));LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.setPadding(dp(12),0,0,0);ht.addView(text("CiviMoto",30,Color.WHITE,true));ht.addView(text("Panel de administración · tiempo real",13,muted,false));h.addView(ht);root.addView(h);
        TextView title=text("Resumen operativo",25,Color.WHITE,true);title.setPadding(0,dp(18),0,dp(8));root.addView(title);
        LinearLayout metrics=new LinearLayout(this);metrics.setOrientation(LinearLayout.HORIZONTAL);metrics.addView(metric("1,245","Usuarios"),new LinearLayout.LayoutParams(0,-2,1));metrics.addView(metric("328","Choferes"),new LinearLayout.LayoutParams(0,-2,1));root.addView(metrics);
        LinearLayout metrics2=new LinearLayout(this);metrics2.setOrientation(LinearLayout.HORIZONTAL);metrics2.addView(metric("892","Viajes hoy"),new LinearLayout.LayoutParams(0,-2,1));metrics2.addView(metric("S/ 1,256","Ingresos"),new LinearLayout.LayoutParams(0,-2,1));root.addView(metrics2);
        WebView map=createMap();root.addView(map,new LinearLayout.LayoutParams(-1,dp(300)));
        LinearLayout active=card();active.addView(text("Viajes activos",20,Color.WHITE,true));active.addView(text("#CM-1042 · Carlos → Plaza Mayor · EN VIAJE",14,Color.WHITE,false));active.addView(text("#CM-1041 · Ana → Terminal · CHOFER EN CAMINO",14,Color.WHITE,false));active.addView(text("#CM-1040 · Luis → Centro · BUSCANDO CHOFER",14,Color.WHITE,false));root.addView(active);
        LinearLayout drivers=card();drivers.addView(text("Control de choferes",20,Color.WHITE,true));drivers.addView(text("✓ 24 conectados · 18 disponibles · 6 en viaje",15,gold,true));drivers.addView(text("Documentos pendientes: 3 · Bloqueados: 1",14,muted,false));Button review=button("Revisar documentos pendientes");drivers.addView(review);root.addView(drivers);
        LinearLayout payments=card();payments.addView(text("Pagos de choferes",20,Color.WHITE,true));payments.addView(text("Carlos R. · Yape · Semanal · S/ 50.00",14,Color.WHITE,true));LinearLayout r1=new LinearLayout(this);Button reject1=button("Rechazar"),approve1=button("Aprobar");r1.addView(reject1,new LinearLayout.LayoutParams(0,dp(52),1));r1.addView(approve1,new LinearLayout.LayoutParams(0,dp(52),1));payments.addView(r1);payments.addView(text("María L. · Yape · Mensual · S/ 200.00",14,Color.WHITE,true));LinearLayout r2=new LinearLayout(this);Button reject2=button("Rechazar"),approve2=button("Aprobar");r2.addView(reject2,new LinearLayout.LayoutParams(0,dp(52),1));r2.addView(approve2,new LinearLayout.LayoutParams(0,dp(52),1));payments.addView(r2);paymentState=text("Selecciona un comprobante para validarlo.",14,muted,false);payments.addView(paymentState);root.addView(payments);
        LinearLayout settings=card();settings.addView(text("Configuración operativa",20,Color.WHITE,true));settings.addView(text("Tarifa base: S/ 3.00 · Precio/km: S/ 1.20 · Tarifa mínima: S/ 5.00",14,muted,false));Button tariff=button("Editar tarifas y zonas");settings.addView(tariff);root.addView(settings);
        approve1.setOnClickListener(v->paymentState.setText("✓ Pago semanal de Carlos aprobado."));reject1.setOnClickListener(v->paymentState.setText("Pago de Carlos rechazado. Se solicitará corrección."));approve2.setOnClickListener(v->paymentState.setText("✓ Pago mensual de María aprobado."));reject2.setOnClickListener(v->paymentState.setText("Pago de María rechazado."));review.setOnClickListener(v->Toast.makeText(this,"Módulo de documentos preparado para backend",Toast.LENGTH_SHORT).show());tariff.setOnClickListener(v->Toast.makeText(this,"Motor de tarifas preparado para backend",Toast.LENGTH_SHORT).show());
    }
    LinearLayout metric(String value,String label){LinearLayout c=card();c.setPadding(dp(14),dp(12),dp(14),dp(12));c.addView(text(value,25,gold,true));c.addView(text(label,12,muted,true));return c;}
    WebView createMap(){WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);w.setWebViewClient(new WebViewClient());String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'><link href='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.css' rel='stylesheet'><style>html,body,#map{margin:0;width:100%;height:100%;background:#17171b}</style></head><body><div id='map'></div><script src='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.js'></script><script>let map=new maplibregl.Map({container:'map',style:'https://tiles.openfreemap.org/styles/liberty',center:[-77.0428,-12.0464],zoom:12.3});map.on('load',()=>{[[-77.04,-12.045],[-77.02,-12.055],[-77.06,-12.035],[-77.08,-12.05],[-77.01,-12.03]].forEach((c,i)=>new maplibregl.Marker({color:i<3?'#ffbe00':'#18b878'}).setLngLat(c).addTo(map));});</script></body></html>";w.loadDataWithBaseURL("https://admin.civimoto.local/",html,"text/html","UTF-8",null);return w;}
}
