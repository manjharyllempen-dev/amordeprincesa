package pe.civimoto.administrador;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity implements RealtimeBridge.Listener {
    private final int gold=Color.rgb(255,190,0), bg=Color.rgb(8,8,10), surface=Color.rgb(24,24,28), muted=Color.rgb(170,170,178);
    private Backend backend; private RealtimeBridge realtime; private LinearLayout root,paymentsBox,driversBox;
    private TextView connection,usersMetric,driversMetric,tripsMetric,revenueMetric;
    private WebView map;
    private EditText weeklyAmount,monthlyAmount,yapeName,yapeNumber,baseFare,perKm,minFare;

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView text(String s,int sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,dp(4),0,dp(4));if(b)v.setTypeface(null,1);return v;}
    Button button(String s){Button x=new Button(this);x.setText(s);x.setTextColor(Color.BLACK);x.setTypeface(null,1);x.setBackground(box(gold,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));lp.setMargins(0,dp(5),0,dp(5));x.setLayoutParams(lp);return x;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(box(surface,20));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));c.setLayoutParams(lp);return c;}
    ImageView logo(int s){ImageView i=new ImageView(this);i.setImageResource(R.drawable.logo_civimoto);i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setLayoutParams(new LinearLayout.LayoutParams(dp(s),dp(s)));return i;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);backend=new Backend(this);showSplash();
        backend.bootstrap(new Backend.Callback(){public void ok(Object v){if(backend.hasSession())backend.resume("admin",resumeCb);else showLogin();}public void error(String m){showLogin();toast("Servidor: "+m);}});
    }
    private final Backend.Callback resumeCb=new Backend.Callback(){public void ok(Object v){showMain();}public void error(String m){showLogin();}};

    void showSplash(){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setGravity(Gravity.CENTER);s.setBackgroundColor(Color.BLACK);s.addView(logo(240));TextView b=text("CiviMoto",46,Color.WHITE,true);b.setGravity(Gravity.CENTER);s.addView(b);TextView t=text("Administrador · Control en tiempo real",18,gold,true);t.setGravity(Gravity.CENTER);s.addView(t);setContentView(s);}

    void showLogin(){
        ScrollView sc=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(24),dp(36),dp(24),dp(36));r.setBackgroundColor(bg);sc.addView(r);setContentView(sc);
        LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.VERTICAL);h.setGravity(Gravity.CENTER);h.addView(logo(150));TextView title=text("CiviMoto",38,Color.WHITE,true);title.setGravity(Gravity.CENTER);h.addView(title);TextView role=text("Administrador",16,gold,true);role.setGravity(Gravity.CENTER);h.addView(role);r.addView(h);
        LinearLayout c=card();c.addView(text("Acceso administrativo",22,Color.WHITE,true));EditText email=input("Correo electrónico");EditText pass=input("Contraseña");pass.setInputType(0x00000081);c.addView(email);c.addView(pass);Button login=button("Ingresar al panel");c.addView(login);r.addView(c);
        login.setOnClickListener(v->{login.setEnabled(false);backend.login(email.getText().toString(),pass.getText().toString(),"admin",new Backend.Callback(){public void ok(Object x){showMain();}public void error(String m){login.setEnabled(true);toast(m);}});});
    }

    void showMain(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(80));root.setBackgroundColor(bg);sc.addView(root);setContentView(sc);
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.addView(logo(62));LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.setPadding(dp(12),0,0,0);ht.addView(text("CiviMoto",30,Color.WHITE,true));connection=text("Conectando con operación…",12,muted,false);ht.addView(connection);h.addView(ht,new LinearLayout.LayoutParams(0,-2,1));Button logout=small("Salir");h.addView(logout);root.addView(h);logout.setOnClickListener(v->{backend.logout();if(realtime!=null)realtime.stop();showLogin();});

        root.addView(text("Panel operativo",26,Color.WHITE,true));
        LinearLayout m1=new LinearLayout(this);usersMetric=metricValue("—","Pasajeros");driversMetric=metricValue("—","Choferes");m1.addView(usersMetric,new LinearLayout.LayoutParams(0,dp(86),1));m1.addView(driversMetric,new LinearLayout.LayoutParams(0,dp(86),1));root.addView(m1);
        LinearLayout m2=new LinearLayout(this);tripsMetric=metricValue("—","Viajes");revenueMetric=metricValue("—","Facturado");m2.addView(tripsMetric,new LinearLayout.LayoutParams(0,dp(86),1));m2.addView(revenueMetric,new LinearLayout.LayoutParams(0,dp(86),1));root.addView(m2);

        LinearLayout live=card();live.addView(text("Mapa operativo en vivo",21,Color.WHITE,true));map=createMap();live.addView(map,new LinearLayout.LayoutParams(-1,dp(310)));root.addView(live);

        LinearLayout drivers=card();drivers.addView(text("Choferes pendientes",21,Color.WHITE,true));driversBox=new LinearLayout(this);driversBox.setOrientation(LinearLayout.VERTICAL);drivers.addView(driversBox);root.addView(drivers);

        LinearLayout payments=card();payments.addView(text("Pagos semanal / mensual",21,Color.WHITE,true));payments.addView(text("El administrador aprueba o rechaza transferencias Yape del chofer.",13,muted,false));paymentsBox=new LinearLayout(this);paymentsBox.setOrientation(LinearLayout.VERTICAL);payments.addView(paymentsBox);root.addView(payments);

        LinearLayout bill=card();bill.addView(text("Configuración Yape y membresías",21,Color.WHITE,true));weeklyAmount=input("Monto semanal");monthlyAmount=input("Monto mensual");yapeName=input("Nombre que aparece en Yape");yapeNumber=input("Número Yape");bill.addView(weeklyAmount);bill.addView(monthlyAmount);bill.addView(yapeName);bill.addView(yapeNumber);Button saveBill=button("Guardar configuración de pagos");bill.addView(saveBill);root.addView(bill);saveBill.setOnClickListener(v->saveBilling());

        LinearLayout fare=card();fare.addView(text("Tarifas de viajes",21,Color.WHITE,true));fare.addView(text("Sin comisión porcentual. La plataforma cobra solo membresía semanal/mensual.",13,gold,true));baseFare=input("Tarifa base");perKm=input("Precio por km");minFare=input("Tarifa mínima");fare.addView(baseFare);fare.addView(perKm);fare.addView(minFare);Button saveFare=button("Guardar tarifas");fare.addView(saveFare);root.addView(fare);saveFare.setOnClickListener(v->saveFare());

        startRealtime();refreshAll();
    }

    TextView metricValue(String value,String label){TextView t=text(value+"\n"+label,19,Color.WHITE,true);t.setGravity(Gravity.CENTER);t.setBackground(box(surface,18));return t;}

    void refreshAll(){loadMetrics();loadDrivers();loadPayments();loadBillingConfig();loadFareConfig();loadMap();}

    void loadMetrics(){
        backend.rest("GET","profiles?select=id,role,account_status",null,new Backend.Callback(){public void ok(Object v){if(v instanceof JSONArray){JSONArray a=(JSONArray)v;int users=0;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&"usuario".equals(o.optString("role")))users++;}usersMetric.setText(users+"\nPasajeros");}}public void error(String m){}});
        backend.rest("GET","drivers?select=id,status,is_available",null,new Backend.Callback(){public void ok(Object v){if(v instanceof JSONArray){JSONArray a=(JSONArray)v;int online=0;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optBoolean("is_available"))online++;}driversMetric.setText(a.length()+"\nChoferes · "+online+" online");}}public void error(String m){}});
        backend.rest("GET","trips?order=requested_at.desc&limit=500&select=id,status,estimated_fare,final_fare",null,new Backend.Callback(){public void ok(Object v){if(v instanceof JSONArray){JSONArray a=(JSONArray)v;int completed=0;double gross=0;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&"completado".equals(o.optString("status"))){completed++;gross+=o.optDouble("final_fare",o.optDouble("estimated_fare",0));}}tripsMetric.setText(a.length()+"\nViajes · "+completed+" completados");revenueMetric.setText(String.format("S/ %.2f\nFacturado",gross));}}public void error(String m){}});
    }

    void loadDrivers(){
        driversBox.removeAllViews();
        backend.rpc("admin_driver_directory",new JSONObject(),new Backend.Callback(){
            public void ok(Object v){
                if(!(v instanceof JSONArray)){driversBox.addView(text("Sin datos.",14,muted,false));return;}JSONArray a=(JSONArray)v;int pending=0;
                for(int i=0;i<a.length();i++){JSONObject d=a.optJSONObject(i);if(d==null||!"pendiente".equals(d.optString("status")))continue;pending++;LinearLayout c=subCard();c.addView(text(d.optString("full_name","Chofer")+" · "+d.optString("document_number","sin DNI"),15,Color.WHITE,true));c.addView(text("Licencia: "+d.optString("license_number","—")+" · Placa: "+d.optString("plate","—"),13,muted,false));LinearLayout row=new LinearLayout(MainActivity.this);Button reject=button("Rechazar"),approve=button("Aprobar");row.addView(reject,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(approve,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(row);driversBox.addView(c);String id=d.optString("id");approve.setOnClickListener(x->reviewDriver(id,"aprobado"));reject.setOnClickListener(x->reviewDriver(id,"rechazado"));}
                if(pending==0)driversBox.addView(text("No hay choferes pendientes.",14,muted,false));
            }
            public void error(String m){driversBox.addView(text("Error: "+m,14,Color.RED,false));}
        });
    }

    void reviewDriver(String id,String status){
        try{backend.rpc("admin_set_driver_status",new JSONObject().put("p_driver_id",id).put("p_status",status),new Backend.Callback(){public void ok(Object v){toast("Chofer "+status+".");loadDrivers();loadMetrics();}public void error(String m){toast(m);}});}catch(Exception ignored){}
    }

    void loadPayments(){
        paymentsBox.removeAllViews();
        backend.rest("GET","driver_subscription_payments?status=eq.pending&order=submitted_at.asc&limit=30&select=*",null,new Backend.Callback(){
            public void ok(Object v){
                if(!(v instanceof JSONArray)){paymentsBox.addView(text("Sin pagos pendientes.",14,muted,false));return;}JSONArray a=(JSONArray)v;
                if(a.length()==0){paymentsBox.addView(text("No hay pagos pendientes.",14,muted,false));return;}
                for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;LinearLayout c=subCard();c.addView(text(p.optString("plan","weekly").toUpperCase()+" · S/ "+p.optString("amount","0")+" · Op. "+p.optString("yape_operation","—"),15,Color.WHITE,true));c.addView(text("Chofer: "+p.optString("driver_id","—"),12,muted,false));LinearLayout row=new LinearLayout(MainActivity.this);Button reject=button("Rechazar"),approve=button("Aprobar");row.addView(reject,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(approve,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(row);paymentsBox.addView(c);String id=p.optString("id");approve.setOnClickListener(x->reviewPayment(id,true));reject.setOnClickListener(x->reviewPayment(id,false));}
            }
            public void error(String m){paymentsBox.addView(text("Error: "+m,14,Color.RED,false));}
        });
    }

    void reviewPayment(String id,boolean approve){
        try{backend.rpc("admin_review_driver_membership_payment",new JSONObject().put("p_payment_id",id).put("p_approve",approve).put("p_note","Revisado desde CiviMoto Admin"),new Backend.Callback(){public void ok(Object v){toast(approve?"Pago aprobado.":"Pago rechazado.");loadPayments();}public void error(String m){toast(m);}});}catch(Exception ignored){}
    }

    void loadBillingConfig(){
        backend.rest("GET","driver_billing_config?id=eq.true&select=*",null,new Backend.Callback(){public void ok(Object v){JSONObject c=Backend.firstObject(v);if(c!=null){weeklyAmount.setText(c.optString("weekly_amount","50.00"));monthlyAmount.setText(c.optString("monthly_amount","200.00"));yapeName.setText(c.optString("yape_display_name",""));yapeNumber.setText(c.optString("yape_number",""));}}public void error(String m){}});
    }

    void saveBilling(){
        try{JSONObject b=new JSONObject().put("weekly_amount",Double.parseDouble(weeklyAmount.getText().toString())).put("monthly_amount",Double.parseDouble(monthlyAmount.getText().toString())).put("yape_display_name",yapeName.getText().toString().trim()).put("yape_number",yapeNumber.getText().toString().trim());backend.rest("PATCH","driver_billing_config?id=eq.true",b,new Backend.Callback(){public void ok(Object v){toast("Configuración de Yape guardada.");}public void error(String m){toast(m);}});}catch(Exception e){toast("Revisa los montos.");}
    }

    void loadFareConfig(){
        backend.rest("GET","fares?active=eq.true&order=created_at.desc&limit=1&select=id,base_fare,per_km,minimum_fare,platform_commission_percent",null,new Backend.Callback(){public void ok(Object v){JSONObject f=Backend.firstObject(v);if(f!=null){baseFare.setText(f.optString("base_fare","3.00"));perKm.setText(f.optString("per_km","1.50"));minFare.setText(f.optString("minimum_fare","5.00"));}}public void error(String m){}});
    }

    void saveFare(){
        try{JSONObject b=new JSONObject().put("base_fare",Double.parseDouble(baseFare.getText().toString())).put("per_km",Double.parseDouble(perKm.getText().toString())).put("minimum_fare",Double.parseDouble(minFare.getText().toString())).put("platform_commission_percent",0);backend.rest("PATCH","fares?active=eq.true",b,new Backend.Callback(){public void ok(Object v){toast("Tarifas actualizadas sin comisión porcentual.");}public void error(String m){toast(m);}});}catch(Exception e){toast("Revisa las tarifas.");}
    }

    void loadMap(){
        backend.rest("GET","driver_live_locations?select=driver_id,lat,lng,updated_at",null,new Backend.Callback(){public void ok(Object v){if(v instanceof JSONArray&&map!=null)map.evaluateJavascript("setDrivers("+JSONObject.quote(v.toString())+")",null);}public void error(String m){}});
    }

    void startRealtime(){realtime=new RealtimeBridge(this,backend,this);WebView rt=realtime.start("trips","driver_live_locations","driver_subscription_payments","driver_billing","drivers","profiles");rt.setVisibility(View.INVISIBLE);root.addView(rt,new LinearLayout.LayoutParams(1,1));}
    @Override public void onRealtime(String table,String payload){connection.setText("● En tiempo real");if("driver_live_locations".equals(table))loadMap();else if("driver_subscription_payments".equals(table)||"driver_billing".equals(table))loadPayments();else{loadMetrics();if("drivers".equals(table))loadDrivers();}}
    @Override public void onRealtimeStatus(String s){connection.setText("SUBSCRIBED".equals(s)?"● En tiempo real":"Sincronizando…");}

    WebView createMap(){WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);w.setWebViewClient(new WebViewClient());String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'><link href='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.css' rel='stylesheet'><style>html,body,#m{margin:0;width:100%;height:100%;background:#17171b}</style></head><body><div id='m'></div><script src='https://unpkg.com/maplibre-gl@5.6.2/dist/maplibre-gl.js'></script><script>let map=new maplibregl.Map({container:'m',style:'https://tiles.openfreemap.org/styles/liberty',center:[-77.0428,-12.0464],zoom:12.5});let marks=[];function setDrivers(s){marks.forEach(x=>x.remove());marks=[];try{JSON.parse(s).forEach(d=>{marks.push(new maplibregl.Marker({color:'#ffbe00'}).setLngLat([d.lng,d.lat]).addTo(map));});}catch(e){}}</script></body></html>";w.loadDataWithBaseURL("https://admin.civimoto.local/",html,"text/html","UTF-8",null);return w;}

    LinearLayout subCard(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(10),dp(12),dp(10));c.setBackground(box(Color.rgb(35,35,40),14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(4),0,dp(4));c.setLayoutParams(lp);return c;}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(muted);e.setSingleLine(true);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));e.setPadding(dp(4),dp(10),dp(4),dp(10));return e;}
    Button small(String s){Button b=button(s);b.setLayoutParams(new LinearLayout.LayoutParams(dp(82),dp(48)));return b;}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){super.onDestroy();if(realtime!=null)realtime.stop();}
}
