package pe.civimoto.administrador;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import java.lang.reflect.Field;

public class InteractiveFlowActivity extends FlowActivity {
    private final int GOLD_ON=Color.rgb(255,195,0),GOLD_PRESS=Color.rgb(205,145,0),GOLD_DISABLED=Color.rgb(120,92,20),BLACK=Color.rgb(5,6,8),MUTED=Color.rgb(176,180,190),GOLD2=Color.rgb(255,220,90);
    private GradientDrawable round(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(16));g.setStroke(dp(1),Color.rgb(255,218,80));return g;}
    private StateListDrawable selector(){StateListDrawable s=new StateListDrawable();s.addState(new int[]{-android.R.attr.state_enabled},round(GOLD_DISABLED));s.addState(new int[]{android.R.attr.state_pressed},round(GOLD_PRESS));s.addState(new int[]{android.R.attr.state_selected},round(GOLD_PRESS));s.addState(new int[]{},round(GOLD_ON));return s;}
    @Override Button btn(String text,boolean primary){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(null,1);b.setTextColor(Color.BLACK);b.setBackground(selector());b.setElevation(dp(3));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(lp);return b;}
    private void setBody(LinearLayout value){try{Field f=FlowActivity.class.getDeclaredField("body");f.setAccessible(true);f.set(this,value);}catch(Exception ignored){}}
    private LinearLayout body(){try{Field f=FlowActivity.class.getDeclaredField("body");f.setAccessible(true);return(LinearLayout)f.get(this);}catch(Exception e){return null;}}
    private Backend backend(){try{Field f=FlowActivity.class.getDeclaredField("backend");f.setAccessible(true);return(Backend)f.get(this);}catch(Exception e){return null;}}

    @Override void shell(String title,String subtitle,int step){stopPoller();ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(14),dp(14),dp(70));root.setBackgroundColor(BLACK);sc.addView(root);LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(dp(4),dp(3),dp(4),dp(8));head.addView(logo(58));TextView brand=tx("CiviMoto Administrador",27,Color.WHITE,true);brand.setPadding(dp(12),0,0,0);head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));root.addView(head);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,dp(8),0,0);root.addView(content);setBody(content);setContentView(sc);}

    private void addPasswordToggle(LinearLayout c,EditText pass){CheckBox show=new CheckBox(this);show.setText("Mostrar contraseña");show.setTextColor(GOLD2);show.setPadding(dp(4),dp(2),0,dp(5));show.setOnCheckedChangeListener((v,on)->{int pos=pass.getSelectionStart();pass.setInputType(InputType.TYPE_CLASS_TEXT|(on?InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:InputType.TYPE_TEXT_VARIATION_PASSWORD));pass.setSelection(Math.max(0,Math.min(pos,pass.length())));show.setText(on?"Ocultar contraseña":"Mostrar contraseña");});c.addView(show);}

    @Override void screenLogin(){shell("","",0);LinearLayout c=card();c.addView(tx("Acceso administrador",24,Color.WHITE,true));EditText email=input("Correo administrador"),pass=input("Contraseña");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);c.addView(email);c.addView(pass);addPasswordToggle(c,pass);Button login=btn("Ingresar al panel",true),setup=btn("Configurar primer administrador",false);c.addView(login);c.addView(setup);body().addView(c);login.setOnClickListener(v->{login.setEnabled(false);backend().login(email.getText().toString(),pass.getText().toString(),"admin",new Backend.Callback(){public void ok(Object x){screenDashboard();}public void error(String m){login.setEnabled(true);toast(m);}});});setup.setOnClickListener(v->screenRegisterAdmin());}

    @Override void screenRegisterAdmin(){shell("","",0);LinearLayout c=card();c.addView(tx("Administrador inicial",24,Color.WHITE,true));c.addView(tx("Crea o activa la cuenta principal de CiviMoto.",13,MUTED,false));EditText email=input("Correo administrador"),pass=input("Contraseña 8+ caracteres");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);c.addView(email);c.addView(pass);addPasswordToggle(c,pass);Button create=btn("Crear / activar administrador",true),back=btn("Volver",false);c.addView(create);c.addView(back);body().addView(c);create.setOnClickListener(v->{create.setEnabled(false);backend().login(email.getText().toString(),pass.getText().toString(),"admin",new Backend.Callback(){public void ok(Object x){screenDashboard();}public void error(String m){create.setEnabled(true);toast(m);}});});back.setOnClickListener(v->screenLogin());}
}
