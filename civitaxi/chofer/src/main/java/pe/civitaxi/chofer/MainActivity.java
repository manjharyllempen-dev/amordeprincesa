package pe.civitaxi.chofer;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    int purple=Color.rgb(82,38,168), yellow=Color.rgb(255,196,0); TextView payStatus;
    GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);return g;}
    TextView t(String s,int sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,6,0,6);if(b)v.setTypeface(null,1);return v;}
    Button b(String s){Button x=new Button(this);x.setText(s);x.setTextColor(Color.rgb(30,20,0));x.setTypeface(null,1);x.setBackground(bg(yellow,28));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,8,0,8);x.setLayoutParams(lp);return x;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(28,24,28,24);c.setBackground(bg(Color.WHITE,34));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,10,0,10);c.setLayoutParams(lp);return c;}
    @Override public void onCreate(Bundle x){super.onCreate(x);ScrollView sc=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,30,28,90);r.setBackground(bg(purple,0));sc.addView(r);setContentView(sc);r.addView(t("🏍️  CiviTaxi",30,Color.WHITE,true));r.addView(t("Chofer · Mototaxi",14,Color.WHITE,false));
      LinearLayout s=card();s.addView(t("NUEVO SERVICIO",13,purple,true));s.addView(t("📍 Origen",14,Color.DKGRAY,true));s.addView(t("Av. Los Próceres 245",18,Color.rgb(35,22,66),true));s.addView(t("🏁 Destino",14,Color.DKGRAY,true));s.addView(t("Plaza de Armas",18,Color.rgb(35,22,66),true));s.addView(t("Tarifa estimada: S/ 8.00",16,purple,true));Button accept=b("✓ Aceptar servicio");s.addView(accept);r.addView(s);accept.setOnClickListener(v->Toast.makeText(this,"Servicio aceptado (demo)",Toast.LENGTH_SHORT).show());
      LinearLayout p=card();p.addView(t("Pago a CiviTaxi",20,Color.rgb(35,22,66),true));p.addView(t("Elige la periodicidad. Método: Yape.",13,Color.DKGRAY,false));LinearLayout row=new LinearLayout(this);Button weekly=b("Semanal"),monthly=b("Mensual");row.addView(weekly,new LinearLayout.LayoutParams(0,-2,1));row.addView(monthly,new LinearLayout.LayoutParams(0,-2,1));p.addView(row);payStatus=t("Sin pago seleccionado.",13,Color.DKGRAY,false);p.addView(payStatus);r.addView(p);weekly.setOnClickListener(v->payStatus.setText("✓ Pago semanal por Yape seleccionado. Pendiente de aprobación."));monthly.setOnClickListener(v->payStatus.setText("✓ Pago mensual por Yape seleccionado. Pendiente de aprobación."));
    }
}
