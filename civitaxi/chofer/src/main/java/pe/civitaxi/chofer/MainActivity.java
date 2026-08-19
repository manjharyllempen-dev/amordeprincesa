package pe.civitaxi.chofer;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    int purple=Color.rgb(82,38,168), dark=Color.rgb(48,16,111), yellow=Color.rgb(255,196,0); TextView payStatus;
    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView t(String s,int sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,dp(4),0,dp(4));if(b)v.setTypeface(null,1);return v;}
    Button b(String s){Button x=new Button(this);x.setText(s);x.setTextColor(Color.rgb(30,20,0));x.setTypeface(null,1);x.setBackground(bg(yellow,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),0,dp(5));x.setLayoutParams(lp);return x;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(bg(Color.WHITE,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(7),0,dp(7));c.setLayoutParams(lp);return c;}
    ImageView logo(int w,int h){ImageView i=new ImageView(this);i.setImageResource(R.drawable.mototaxi_logo);i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);i.setLayoutParams(new LinearLayout.LayoutParams(dp(w),dp(h)));return i;}
    @Override public void onCreate(Bundle x){super.onCreate(x);showSplash();}
    void showSplash(){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setGravity(Gravity.CENTER);s.setPadding(dp(30),dp(30),dp(30),dp(30));s.setBackground(bg(dark,0));s.addView(logo(260,210));TextView brand=t("CiviTaxi",42,Color.WHITE,true);brand.setGravity(Gravity.CENTER);s.addView(brand);LinearLayout mr=new LinearLayout(this);mr.setGravity(Gravity.CENTER);mr.addView(t("Mototaxi",22,Color.WHITE,true));mr.addView(logo(58,44));s.addView(mr);TextView sub=t("Chofer · Tu ruta, tu trabajo",15,Color.WHITE,false);sub.setGravity(Gravity.CENTER);s.addView(sub);setContentView(s);new Handler(Looper.getMainLooper()).postDelayed(this::build,1400);}
    void addHeader(LinearLayout r){LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.addView(logo(74,58));LinearLayout text=new LinearLayout(this);text.setOrientation(LinearLayout.VERTICAL);text.addView(t("CiviTaxi",29,Color.WHITE,true));LinearLayout sub=new LinearLayout(this);sub.setGravity(Gravity.CENTER_VERTICAL);sub.addView(t("Chofer · Mototaxi",14,Color.WHITE,false));sub.addView(logo(42,32));text.addView(sub);h.addView(text);r.addView(h);}
    void build(){ScrollView sc=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(18),dp(18),dp(70));r.setBackground(bg(purple,0));sc.addView(r);setContentView(sc);addHeader(r);
      LinearLayout s=card();s.addView(t("NUEVO SERVICIO",13,purple,true));s.addView(t("📍 Origen",14,Color.DKGRAY,true));s.addView(t("Av. Los Próceres 245",18,Color.rgb(35,22,66),true));s.addView(t("🏁 Destino",14,Color.DKGRAY,true));s.addView(t("Plaza de Armas",18,Color.rgb(35,22,66),true));s.addView(t("Tarifa estimada: S/ 8.00",16,purple,true));Button accept=b("✓ Aceptar servicio");s.addView(accept);r.addView(s);accept.setOnClickListener(v->Toast.makeText(this,"Servicio aceptado (demo)",Toast.LENGTH_SHORT).show());
      LinearLayout p=card();p.addView(t("Pago a CiviTaxi",20,Color.rgb(35,22,66),true));p.addView(t("Elige la periodicidad. Método: Yape.",13,Color.DKGRAY,false));LinearLayout row=new LinearLayout(this);Button weekly=b("Semanal"),monthly=b("Mensual");row.addView(weekly,new LinearLayout.LayoutParams(0,-2,1));row.addView(monthly,new LinearLayout.LayoutParams(0,-2,1));p.addView(row);payStatus=t("Sin pago seleccionado.",13,Color.DKGRAY,false);p.addView(payStatus);r.addView(p);weekly.setOnClickListener(v->payStatus.setText("✓ Pago semanal por Yape seleccionado. Pendiente de aprobación."));monthly.setOnClickListener(v->payStatus.setText("✓ Pago mensual por Yape seleccionado. Pendiente de aprobación."));
    }
}
