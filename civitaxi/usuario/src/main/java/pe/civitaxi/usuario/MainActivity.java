package pe.civitaxi.usuario;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    int purple = Color.rgb(82,38,168), dark = Color.rgb(48,16,111), yellow = Color.rgb(255,196,0);
    LinearLayout root; TextView status;
    int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle b){ super.onCreate(b); showSplash(); }

    GradientDrawable bg(int color,int radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    TextView txt(String s,int sp,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.WHITE); t.setPadding(0,dp(4),0,dp(4)); if(bold)t.setTypeface(null,1); return t; }
    TextView darkText(String s,int sp,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.rgb(35,22,66)); t.setPadding(0,dp(4),0,dp(4)); if(bold)t.setTypeface(null,1); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.rgb(33,22,0)); b.setTypeface(null,1); b.setBackground(bg(yellow,14)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(5),0,dp(5)); b.setLayoutParams(lp); return b; }
    LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18),dp(16),dp(18),dp(16)); c.setBackground(bg(Color.WHITE,18)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(7),0,dp(7)); c.setLayoutParams(lp); return c; }
    ImageView logo(int w,int h){ ImageView i=new ImageView(this); i.setImageResource(R.drawable.mototaxi_logo); i.setScaleType(ImageView.ScaleType.CENTER_INSIDE); i.setLayoutParams(new LinearLayout.LayoutParams(dp(w),dp(h))); return i; }

    void showSplash(){
        LinearLayout s=new LinearLayout(this); s.setOrientation(LinearLayout.VERTICAL); s.setGravity(Gravity.CENTER); s.setPadding(dp(30),dp(30),dp(30),dp(30)); s.setBackground(bg(dark,0));
        s.addView(logo(260,210));
        TextView brand=txt("CiviTaxi",42,true); brand.setGravity(Gravity.CENTER); s.addView(brand);
        LinearLayout mr=new LinearLayout(this); mr.setGravity(Gravity.CENTER); TextView mt=txt("Mototaxi",22,true); mr.addView(mt); mr.addView(logo(58,44)); s.addView(mr);
        TextView sub=txt("Tu destino, nuestra ruta",15,false); sub.setGravity(Gravity.CENTER); s.addView(sub);
        setContentView(s); new Handler(Looper.getMainLooper()).postDelayed(this::build,1400);
    }

    void addHeader(LinearLayout r,String role){
        LinearLayout h=new LinearLayout(this); h.setGravity(Gravity.CENTER_VERTICAL); h.addView(logo(74,58));
        LinearLayout text=new LinearLayout(this); text.setOrientation(LinearLayout.VERTICAL); text.addView(txt("CiviTaxi",29,true));
        LinearLayout sub=new LinearLayout(this); sub.setGravity(Gravity.CENTER_VERTICAL); sub.addView(txt(role+" · Mototaxi",14,false)); sub.addView(logo(42,32)); text.addView(sub); h.addView(text); r.addView(h);
    }

    void build(){
        ScrollView sc=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(70)); root.setBackground(bg(purple,0)); sc.addView(root); setContentView(sc); addHeader(root,"Usuario");
        LinearLayout c=card(); c.addView(darkText("¿A dónde vamos?",23,true)); c.addView(darkText("📍 Origen",13,true)); EditText o=new EditText(this); o.setText("Mi ubicación actual"); c.addView(o); c.addView(darkText("🏁 Destino",13,true)); EditText d=new EditText(this); d.setText("Plaza de Armas"); c.addView(d); c.addView(darkText("Método de pago",16,true));
        LinearLayout row=new LinearLayout(this); Button cash=btn("💵 Efectivo"), yape=btn("📱 Yape"); row.addView(cash,new LinearLayout.LayoutParams(0,-2,1)); row.addView(yape,new LinearLayout.LayoutParams(0,-2,1)); c.addView(row);
        Button request=btn("Solicitar Mototaxi"); c.addView(request); status=darkText("Selecciona un método de pago.",13,false); c.addView(status); root.addView(c);
        LinearLayout trip=card(); trip.addView(darkText("Viaje activo",20,true)); trip.addView(darkText("Chofer: Juan · ⭐ 4.8",14,true)); trip.addView(darkText("Placa: ABC-123",13,false)); trip.addView(darkText("Ruta: Mi ubicación → Plaza de Armas",13,false)); Button share=btn("📤 Compartir viaje"); trip.addView(share); root.addView(trip);
        cash.setOnClickListener(v->status.setText("✓ Pago seleccionado: Efectivo")); yape.setOnClickListener(v->status.setText("✓ Pago seleccionado: Yape")); request.setOnClickListener(v->Toast.makeText(this,"Solicitud de mototaxi enviada (demo)",Toast.LENGTH_SHORT).show());
        share.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,"Estoy viajando con CiviTaxi Mototaxi. Destino: Plaza de Armas. Chofer: Juan, ABC-123."); startActivity(Intent.createChooser(i,"Compartir viaje")); });
    }
}
