package pe.civitaxi.usuario;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    int purple = Color.rgb(82,38,168), dark = Color.rgb(48,16,111), yellow = Color.rgb(255,196,0);
    LinearLayout root;
    TextView status;

    @Override public void onCreate(Bundle b){ super.onCreate(b); build(); }

    TextView txt(String s,int sp,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.WHITE); t.setPadding(0,8,0,8); if(bold)t.setTypeface(null,1); return t; }
    GradientDrawable bg(int color,int radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(radius); return g; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.rgb(33,22,0)); b.setTypeface(null,1); b.setBackground(bg(yellow,28)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,8,0,8); b.setLayoutParams(lp); return b; }
    LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(28,24,28,24); c.setBackground(bg(Color.WHITE,34)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,10,0,10); c.setLayoutParams(lp); return c; }
    TextView darkText(String s,int sp,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.rgb(35,22,66)); t.setPadding(0,5,0,5); if(bold)t.setTypeface(null,1); return t; }

    void build(){
        ScrollView sc=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,30,28,90); root.setBackground(bg(purple,0)); sc.addView(root); setContentView(sc);
        root.addView(txt("🏍️  CiviTaxi",30,true)); root.addView(txt("Usuario · Mototaxi",14,false));
        LinearLayout c=card(); c.addView(darkText("¿A dónde vamos?",23,true)); c.addView(darkText("📍 Origen",13,true)); EditText o=new EditText(this); o.setText("Mi ubicación actual"); c.addView(o); c.addView(darkText("🏁 Destino",13,true)); EditText d=new EditText(this); d.setText("Plaza de Armas"); c.addView(d); c.addView(darkText("Método de pago",16,true));
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); Button cash=btn("💵 Efectivo"), yape=btn("📱 Yape"); row.addView(cash,new LinearLayout.LayoutParams(0,-2,1)); row.addView(yape,new LinearLayout.LayoutParams(0,-2,1)); c.addView(row);
        Button request=btn("🚕 Solicitar viaje"); c.addView(request); status=darkText("Selecciona un método de pago.",13,false); c.addView(status); root.addView(c);
        LinearLayout trip=card(); trip.addView(darkText("Viaje activo",20,true)); trip.addView(darkText("Chofer: Juan · ⭐ 4.8",14,true)); trip.addView(darkText("Placa: ABC-123",13,false)); trip.addView(darkText("Ruta: Mi ubicación → Plaza de Armas",13,false)); Button share=btn("📤 Compartir viaje"); trip.addView(share); root.addView(trip);
        cash.setOnClickListener(v->status.setText("✓ Pago seleccionado: Efectivo")); yape.setOnClickListener(v->status.setText("✓ Pago seleccionado: Yape")); request.setOnClickListener(v->Toast.makeText(this,"Solicitud enviada (demo)",Toast.LENGTH_SHORT).show());
        share.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,"Estoy viajando con CiviTaxi. Destino: Plaza de Armas. Chofer: Juan, ABC-123."); startActivity(Intent.createChooser(i,"Compartir viaje")); });
    }
}
