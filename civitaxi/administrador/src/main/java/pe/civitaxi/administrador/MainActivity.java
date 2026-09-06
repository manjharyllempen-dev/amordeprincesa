package pe.civimoto.administrador;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
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
    private SharedPreferences prefs;
    private TextView paymentState, tariffState, driverState, backendState;
    private double baseFare=3.00, perKm=1.20, minFare=5.00;
    private int approved=0,rejected=0;

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView text(String s,int sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,dp(4),0,dp(4));if(b)v.setTypeface(null,1);return v;}
    Button button(String s){Button x=new Button(this);x.setText(s);x.setTextColor(Color.BLACK);x.setTypeface(null,1);x.setBackground(box(gold,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(6),0,dp(6));x.setLayoutParams(lp);return x;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(box(surface,20));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));c.setLayoutParams(lp);return c;}
    ImageView logo(int size){ImageView i=new ImageView(this);i.setImageResource(R.drawable.logo_civimoto);i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return i;}

    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("civimoto_admin",MODE_PRIVATE);baseFare=Double.longBitsToDouble(prefs.getLong("base",Double.doubleToRawLongBits(3.00)));perKm=Double.longBitsToDouble(prefs.getLong("km",Double.doubleToRawLongBits(1.20)));minFare=Double.longBitsToDouble(prefs.getLong("min",Double.doubleToRawLongBits(5.00)));approved=prefs.getInt("approved",0);rejected=prefs.getInt("rejected",0);showSplash();}

    void showSplash(){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setGravity(Gravity.CENTER);s.setBackgroundColor(Color.BLACK);s.addView(logo(230));TextView brand=text("CiviMoto",44,Color.WHITE,true);brand.setGravity(Gravity.CENTER);s.addView(brand);TextView sub=text("Administrador · Control total",17,gold,true);sub.setGravity(Gravity.CENTER);s.addView(sub);setContentView(s);new Handler(Looper.getMainLooper()).postDelayed(this::build,1100);}

    void build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(80));root.setBackgroundColor(bg);sc.addView(root);setContentView(sc);
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.addView(logo(62));LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.setPadding(dp(12),0,0,0);ht.addView(text("CiviMoto",30,Color.WHITE,true));ht.addView(text("Panel de administración · operación y control",13,muted,false));h.addView(ht);root.addView(h);

        TextView title=text("Resumen operativo",25,Color.WHITE,true);title.setPadding(0,dp(18),0,dp(8));root.addView(title);
        LinearLayout metrics=new LinearLayout(this);metrics.setOrientation(LinearLayout.HORIZONTAL);metrics.addView(metric("1,245","Usuarios"),new LinearLayout.LayoutParams(0,-2,1));metrics.addView(metric("328","Choferes"),new LinearLayout.LayoutParams(0,-2,1));root.addView(metrics);
        LinearLayout metrics2=new LinearLayout(this);metrics2.setOrientation(LinearLayout.HORIZONTAL);metrics2.addView(metric("892","Viajes hoy"),new LinearLayout.LayoutParams(0,-2,1));metrics2.addView(metric("S/ 1,256","Ingresos"),new LinearLayout.LayoutParams(0,-2,1));root.addView(metrics2);

        WebView map=createMap();root.addView(map,new LinearLayout.LayoutParams(-1,dp(300)));

        LinearLayout backend=card();backend.addView(text("Estado de plataforma",20,Color.WHITE,true));backendState=text("Cliente Android preparado para backend Supabase/Realt ime. La conexión remota se activará en la build final estable.",14,muted,false);backend.addView(backendState);Button testBackend=button("Probar actualización local");backend.addView(testBackend);root.addView(backend);

        LinearLayout active=card();active.addView(text("Viajes activos",20,Color.WHITE,true));active.addView(text("#CM-1042 · Carlos → Plaza Mayor · EN VIAJE",14,Color.WHITE,false));active.addView(text("#CM-1041 · Ana → Terminal · CHOFER EN CAMINO",14,Color.WHITE,false));active.addView(text("#CM-1040 · Luis → Centro · BUSCANDO CHOFER",14,Color.WHITE,false));root.addView(active);

        LinearLayout drivers=card();drivers.addView(text("Control de choferes",20,Color.WHITE,true));driverState=text("24 conectados · 18 disponibles · 6 en viaje\nDocumentos pendientes: 3 · Bloqueados: 1",14,muted,false);drivers.addView(driverState);LinearLayout drow=new LinearLayout(this);Button approveDriver=button("Aprobar chofer"),blockDriver=button("Bloquear chofer");drow.addView(approveDriver,new LinearLayout.LayoutParams(0,dp(52),1));drow.addView(blockDriver,new LinearLayout.LayoutParams(0,dp(52),1));drivers.addView(drow);root.addView(drivers);

        LinearLayout payments=card();payments.addView(text("Pagos de choferes",20,Color.WHITE,true));payments.addView(text("Carlos R. · Yape · Semanal · S/ 50.00",14,Color.WHITE,true));LinearLayout r1=new LinearLayout(this);Button reject1=button("Rechazar"),approve1=button("Aprobar");r1.addView(reject1,new LinearLayout.LayoutParams(0,dp(52),1));r1.addView(approve1,new LinearLayout.LayoutParams(0,dp(52),1));payments.addView(r1);payments.addView(text("María L. · Yape · Mensual · S/ 200.00",14,Color.WHITE,true));LinearLayout r2=new LinearLayout(this);Button reject2=button("Rechazar"),approve2=button("Aprobar");r2.addView(reject2,new LinearLayout.LayoutParams(0,dp(52),1));r2.addView(approve2,new LinearLayout.LayoutParams(0,dp(52),1));payments.addView(r2);paymentState=text("",14,muted,false);payments.addView(paymentState);root.addView(payments);

        LinearLayout settings=card();settings.addView(text("Tarifas y zonas",20,Color.WHITE,true));tariffState=text("",14,muted,false);settings.addView(tariffState);Button tariff=button("Editar tarifas");settings.addView(tariff);root.addView(settings);

        approve1.setOnClickListener(v->{approved++;prefs.edit().putInt("approved",approved).apply();paymentState.setText("✓ Pago semanal de Carlos aprobado. Aprobados: "+approved);});
        reject1.setOnClickListener(v->{rejected++;prefs.edit().putInt("rejected",rejected).apply();paymentState.setText("Pago de Carlos rechazado. Rechazados: "+rejected);});
        approve2.setOnClickListener(v->{approved++;prefs.edit().putInt("approved",approved).apply();paymentState.setText("✓ Pago mensual de María aprobado. Aprobados: "+approved);});
        reject2.setOnClickListener(v->{rejected++;prefs.edit().putInt("rejected",rejected).apply();paymentState.setText("Pago de María rechazado. Rechazados: "+rejected);});
        approveDriver.setOnClickListener(v->driverState.setText("✓ Chofer aprobado · 25 conectados potenciales · documentos validados"));
        blockDriver.setOnClickListener(v->driverState.setText("Chofer bloqueado para nuevas solicitudes hasta revisión administrativa."));
        tariff.setOnClickListener(v->showTariffDialog());
        testBackend.setOnClickListener(v->{backendState.setText("✓ Interfaz operativa actualizada. Pendiente sincronización remota cuando Supabase responda establemente.");Toast.makeText(this,"Actualización local correcta",Toast.LENGTH_SHORT).show();});
        refresh();
    }

    LinearLayout metric(String value,String label){LinearLayout c=card();c.setPadding(dp(14),dp(12),dp(14),dp(12));c.addView(text(value,25,gold,true));c.addView(text(label,12,muted,true));return c;}
    void refresh(){if(tariffState!=null)tariffState.setText(String.format(java.util.Locale.US,"Tarifa base: S/ %.2f · Precio/km: S/ %.2f · Tarifa mínima: S/ %.2f",baseFare,perKm,minFare));if(paymentState!=null)paymentState.setText("Aprobados en este dispositivo: "+approved+" · Rechazados: "+rejected);}
    void showTariffDialog(){LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(20),dp(10),dp(20),0);EditText b=new EditText(this);b.setHint("Tarifa base");b.setText(String.valueOf(baseFare));EditText k=new EditText(this);k.setHint("Precio por km");k.setText(String.valueOf(perKm));EditText m=new EditText(this);m.setHint("Tarifa mínima");m.setText(String.valueOf(minFare));form.addView(b);form.addView(k);form.addView(m);new AlertDialog.Builder(this).setTitle("Tarifas Civimoto").setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{try{baseFare=Double.parseDouble(b.getText().toString());perKm=Double.parseDouble(k.getText().toString());minFare=Double.parseDouble(m.getText().toString());prefs.edit().putLong("base",Double.doubleToRawLongBits(baseFare)).putLong("km",Double.doubleToRawLongBits(perKm)).putLong("min",Double.doubleToRawLongBits(minFare)).apply();refresh();}catch(Exception e){Toast.makeText(this,"Valores inválidos",Toast.LENGTH_SHORT).show();}}).show();}

    WebView createMap(){WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);w.setWebViewClient(new WebViewClient());String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'><link href='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.css' rel='stylesheet'><style>html,body,#map{margin:0;width:100%;height:100%;background:#17171b}</style></head><body><div id='map'></div><script src='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.js'></script><script>let map=new maplibregl.Map({container:'map',style:'https://tiles.openfreemap.org/styles/liberty',center:[-77.0428,-12.0464],zoom:12.3});map.on('load',()=>{[[-77.04,-12.045],[-77.02,-12.055],[-77.06,-12.035],[-77.08,-12.05],[-77.01,-12.03]].forEach((c,i)=>new maplibregl.Marker({color:i<3?'#ffbe00':'#18b878'}).setLngLat(c).addTo(map));});</script></body></html>";w.loadDataWithBaseURL("https://admin.civimoto.local/",html,"text/html","UTF-8",null);return w;}
}
