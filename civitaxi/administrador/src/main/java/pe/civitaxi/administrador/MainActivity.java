package pe.civitaxi.administrador;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;

public class MainActivity extends Activity {
    int purple=Color.rgb(82,38,168), yellow=Color.rgb(255,196,0); TextView status;
    GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);return g;}
    TextView t(String s,int sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,6,0,6);if(b)v.setTypeface(null,1);return v;}
    Button b(String s){Button x=new Button(this);x.setText(s);x.setTextColor(Color.rgb(30,20,0));x.setTypeface(null,1);x.setBackground(bg(yellow,28));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,8,0,8);x.setLayoutParams(lp);return x;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(28,24,28,24);c.setBackground(bg(Color.WHITE,34));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,10,0,10);c.setLayoutParams(lp);return c;}
    @Override public void onCreate(Bundle x){super.onCreate(x);ScrollView sc=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,30,28,90);r.setBackground(bg(purple,0));sc.addView(r);setContentView(sc);r.addView(t("🏍️  CiviTaxi",30,Color.WHITE,true));r.addView(t("Administrador · Mototaxi",14,Color.WHITE,false));
      LinearLayout dash=card();dash.addView(t("Panel administrativo",22,Color.rgb(35,22,66),true));dash.addView(t("Viajes activos: 12",16,purple,true));dash.addView(t("Choferes conectados: 24",16,purple,true));dash.addView(t("Usuarios registrados: 1,248",16,purple,true));r.addView(dash);
      LinearLayout p=card();p.addView(t("Pagos de choferes",20,Color.rgb(35,22,66),true));p.addView(t("Juan · Yape · Semanal",15,Color.DKGRAY,true));Button w=b("Confirmar pago semanal");p.addView(w);p.addView(t("Pedro · Yape · Mensual",15,Color.DKGRAY,true));Button m=b("Confirmar pago mensual");p.addView(m);status=t("Selecciona un pago para aprobar.",13,Color.DKGRAY,false);p.addView(status);r.addView(p);w.setOnClickListener(v->status.setText("✓ Pago semanal aprobado."));m.setOnClickListener(v->status.setText("✓ Pago mensual aprobado."));
    }
}
