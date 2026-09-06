package pe.civimoto.administrador;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class InteractiveFlowActivity extends FlowActivity {
    private final int GOLD_ON=Color.rgb(255,195,0),GOLD_PRESS=Color.rgb(205,145,0),GOLD_DISABLED=Color.rgb(120,92,20),BLACK=Color.rgb(5,6,8),PANEL=Color.rgb(18,20,24),PANEL2=Color.rgb(29,31,37),MUTED=Color.rgb(176,180,190),GOLD2=Color.rgb(255,220,90),RED=Color.rgb(235,65,65),GREEN=Color.rgb(35,190,105),PURPLE=Color.rgb(132,67,230),BLUE=Color.rgb(40,126,230);
    private String currentTab="inicio";

    private GradientDrawable round(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(16));g.setStroke(dp(1),Color.rgb(255,218,80));return g;}
    private StateListDrawable selector(){StateListDrawable s=new StateListDrawable();s.addState(new int[]{-android.R.attr.state_enabled},round(GOLD_DISABLED));s.addState(new int[]{android.R.attr.state_pressed},round(GOLD_PRESS));s.addState(new int[]{android.R.attr.state_selected},round(GOLD_PRESS));s.addState(new int[]{},round(GOLD_ON));return s;}
    @Override Button btn(String text,boolean primary){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(null,1);b.setTextColor(Color.BLACK);b.setBackground(selector());b.setElevation(dp(3));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(lp);return b;}

    private void setBody(LinearLayout value){try{Field f=FlowActivity.class.getDeclaredField("body");f.setAccessible(true);f.set(this,value);}catch(Exception ignored){}}
    private LinearLayout body(){try{Field f=FlowActivity.class.getDeclaredField("body");f.setAccessible(true);return(LinearLayout)f.get(this);}catch(Exception e){return null;}}
    private Backend backend(){try{Field f=FlowActivity.class.getDeclaredField("backend");f.setAccessible(true);return(Backend)f.get(this);}catch(Exception e){return null;}}

    @Override void shell(String title,String subtitle,int step){
        stopPoller();
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(BLACK);
        ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(14),dp(14),step>0?dp(108):dp(26));root.setBackgroundColor(BLACK);sc.addView(root);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(dp(4),dp(3),dp(4),dp(8));head.addView(logo(58));TextView brand=tx("CiviMoto Administrador",27,Color.WHITE,true);brand.setPadding(dp(12),0,0,0);head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));root.addView(head);
        if(title!=null&&!title.trim().isEmpty()){TextView section=tx(title,18,GOLD2,true);section.setPadding(dp(3),dp(7),0,dp(2));root.addView(section);}
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,dp(6),0,0);root.addView(content);setBody(content);
        frame.addView(sc,new FrameLayout.LayoutParams(-1,-1));
        if(step>0)frame.addView(bottomNav(),new FrameLayout.LayoutParams(-1,dp(84),Gravity.BOTTOM));
        setContentView(frame);
    }

    private LinearLayout bottomNav(){
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER);bar.setPadding(dp(5),dp(6),dp(5),dp(7));bar.setBackground(round(Color.rgb(9,10,13)));
        bar.addView(navItem("Inicio",android.R.drawable.ic_menu_view,"inicio",()->screenDashboard()),new LinearLayout.LayoutParams(0,-1,1));
        bar.addView(navItem("Viajes",android.R.drawable.ic_menu_recent_history,"viajes",()->screenTrips()),new LinearLayout.LayoutParams(0,-1,1));
        bar.addView(navItem("Chofer",android.R.drawable.ic_menu_myplaces,"choferes",()->screenDrivers()),new LinearLayout.LayoutParams(0,-1,1));
        bar.addView(navItem("Más",android.R.drawable.ic_menu_more,"mas",()->screenMore()),new LinearLayout.LayoutParams(0,-1,1));
        return bar;
    }

    private LinearLayout navItem(String label,int iconRes,String key,Runnable action){
        boolean active=key.equals(currentTab);LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setPadding(dp(3),dp(5),dp(3),dp(4));item.setBackground(active?round(GOLD_ON):round(Color.TRANSPARENT));
        ImageView icon=new ImageView(this);icon.setImageResource(iconRes);icon.setColorFilter(active?Color.BLACK:GOLD2);item.addView(icon,new LinearLayout.LayoutParams(dp(25),dp(25)));
        TextView t=tx(label,11,active?Color.BLACK:Color.WHITE,true);t.setGravity(Gravity.CENTER);item.addView(t);item.setOnClickListener(v->action.run());return item;
    }

    private void addPasswordToggle(LinearLayout c,EditText pass){CheckBox show=new CheckBox(this);show.setText("Mostrar contraseña");show.setTextColor(GOLD2);show.setPadding(dp(4),dp(2),0,dp(5));show.setOnCheckedChangeListener((v,on)->{int pos=pass.getSelectionStart();pass.setInputType(InputType.TYPE_CLASS_TEXT|(on?InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:InputType.TYPE_TEXT_VARIATION_PASSWORD));pass.setSelection(Math.max(0,Math.min(pos,pass.length())));show.setText(on?"Ocultar contraseña":"Mostrar contraseña");});c.addView(show);}

    @Override void screenLogin(){currentTab="inicio";shell("","",0);LinearLayout c=card();c.addView(tx("Acceso administrador",24,Color.WHITE,true));EditText email=input("Correo administrador"),pass=input("Contraseña");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);c.addView(email);c.addView(pass);addPasswordToggle(c,pass);Button login=btn("Ingresar al panel",true),setup=btn("Configurar primer administrador",false);c.addView(login);c.addView(setup);body().addView(c);login.setOnClickListener(v->{login.setEnabled(false);backend().login(email.getText().toString(),pass.getText().toString(),"admin",new Backend.Callback(){public void ok(Object x){screenDashboard();}public void error(String m){login.setEnabled(true);toast(m);}});});setup.setOnClickListener(v->screenRegisterAdmin());}

    @Override void screenRegisterAdmin(){currentTab="inicio";shell("","",0);LinearLayout c=card();c.addView(tx("Administrador inicial",24,Color.WHITE,true));c.addView(tx("Crea o activa la cuenta principal de CiviMoto.",13,MUTED,false));EditText email=input("Correo administrador"),pass=input("Contraseña 8+ caracteres");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);c.addView(email);c.addView(pass);addPasswordToggle(c,pass);Button create=btn("Crear / activar administrador",true),back=btn("Volver",false);c.addView(create);c.addView(back);body().addView(c);create.setOnClickListener(v->{create.setEnabled(false);backend().login(email.getText().toString(),pass.getText().toString(),"admin",new Backend.Callback(){public void ok(Object x){screenDashboard();}public void error(String m){create.setEnabled(true);toast(m);}});});back.setOnClickListener(v->screenLogin());}

    @Override void screenDashboard(){
        currentTab="inicio";shell("Inicio","",3);
        LinearLayout hero=card();hero.addView(tx("Centro de control CiviMoto",23,Color.WHITE,true));hero.addView(tx("Operación, choferes, usuarios, viajes, archivos diarios y tarifas en un solo panel.",13,MUTED,false));body().addView(hero);
        LinearLayout row1=new LinearLayout(this),row2=new LinearLayout(this);TextView users=metric("—","Usuarios",BLUE),drivers=metric("—","Choferes",Color.rgb(215,160,0)),trips=metric("—","Viajes",GREEN),gross=metric("—","Facturado",PURPLE);row1.addView(users,new LinearLayout.LayoutParams(0,dp(92),1));row1.addView(drivers,new LinearLayout.LayoutParams(0,dp(92),1));row2.addView(trips,new LinearLayout.LayoutParams(0,dp(92),1));row2.addView(gross,new LinearLayout.LayoutParams(0,dp(92),1));body().addView(row1);body().addView(row2);
        LinearLayout quick=card();quick.addView(tx("Accesos rápidos",19,Color.WHITE,true));Button today=btn("Ver viajes de hoy",true),pending=btn("Revisar choferes",false),archives=btn("Abrir archivos diarios",false);quick.addView(today);quick.addView(pending);quick.addView(archives);body().addView(quick);today.setOnClickListener(v->screenTrips());pending.setOnClickListener(v->screenDrivers());archives.setOnClickListener(v->screenDailyArchive());
        loadMetrics(users,drivers,trips,gross);startRealtime();startPoller(()->loadMetrics(users,drivers,trips,gross));
    }

    @Override void screenTrips(){
        currentTab="viajes";shell("Viajes de hoy","",5);
        LinearLayout info=card();info.addView(tx("Todos los viajes generados hoy",22,Color.WHITE,true));info.addView(tx("Ordenados por chofer y hora. Se actualizan automáticamente y quedan disponibles en Archivos diarios.",13,MUTED,false));body().addView(info);
        LinearLayout list=card();list.addView(tx("Cargando viajes…",14,MUTED,false));body().addView(list);String day=todayLima();loadDailyTrips(list,day,true);startRealtime();startPoller(()->loadDailyTrips(list,day,true));
    }

    @Override void screenDrivers(){
        currentTab="choferes";shell("Choferes","",4);
        LinearLayout intro=card();intro.addView(tx("Choferes registrados",22,Color.WHITE,true));intro.addView(tx("Aprueba, rechaza, verifica documentos o elimina/desactiva un chofer sin perder su historial de viajes.",13,MUTED,false));body().addView(intro);
        LinearLayout list=card();list.addView(tx("Cargando choferes…",14,MUTED,false));body().addView(list);loadAllDrivers(list);startRealtime();
    }

    private void loadAllDrivers(LinearLayout box){
        box.removeAllViews();box.addView(tx("Directorio de choferes",20,Color.WHITE,true));backend().rpc("admin_driver_directory",new JSONObject(),new Backend.Callback(){public void ok(Object x){if(!(x instanceof JSONArray)){box.addView(tx("Sin choferes registrados.",14,MUTED,false));return;}JSONArray a=(JSONArray)x;if(a.length()==0){box.addView(tx("Sin choferes registrados.",14,MUTED,false));return;}for(int i=0;i<a.length();i++){JSONObject d=a.optJSONObject(i);if(d==null)continue;String id=d.optString("id");boolean verified=d.optBoolean("identity_verified",false);LinearLayout c=subCard();c.addView(tx(d.optString("full_name","Chofer")+" · "+d.optString("plate","Sin placa"),16,Color.WHITE,true));c.addView(tx("Estado: "+d.optString("status","—")+" · Cuenta: "+d.optString("account_status","activo"),12,GOLD2,true));c.addView(tx("DNI: "+d.optString("document_number","—")+" · Licencia: "+d.optString("license_number","—"),12,MUTED,false));c.addView(tx("Vehículo: "+d.optString("brand","—")+" · "+d.optString("color","—")+" · Tel: "+d.optString("phone","—"),12,MUTED,false));c.addView(tx(verified?"✓ Identidad/documentos verificados por administrador":"⚠ Verificación documental pendiente",12,verified?GREEN:Color.rgb(255,175,45),true));
            if("pendiente".equals(d.optString("status"))){LinearLayout r=new LinearLayout(InteractiveFlowActivity.this);Button reject=smallAction("Rechazar",RED),approve=smallAction("Aprobar",GREEN);r.addView(reject,new LinearLayout.LayoutParams(0,dp(48),1));r.addView(approve,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(r);approve.setOnClickListener(v->setDriverStatus(id,"aprobado",box));reject.setOnClickListener(v->setDriverStatus(id,"rechazado",box));}
            LinearLayout r2=new LinearLayout(InteractiveFlowActivity.this);Button verify=smallAction(verified?"Quitar verificación":"Verificar autenticidad",verified?Color.rgb(145,105,30):BLUE),delete=smallAction("Eliminar",RED);r2.addView(verify,new LinearLayout.LayoutParams(0,dp(48),2));r2.addView(delete,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(r2);verify.setOnClickListener(v->verifyDriverDialog(id,verified,box));delete.setOnClickListener(v->confirmDeleteDriver(id,d.optString("full_name","Chofer"),box));box.addView(c);} }public void error(String m){box.addView(tx("Error: "+m,14,RED,false));}});
    }

    private Button smallAction(String text,int color){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(12);b.setTypeface(null,1);b.setTextColor(Color.WHITE);GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(12));b.setBackground(g);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(46));lp.setMargins(dp(3),dp(5),dp(3),dp(5));b.setLayoutParams(lp);return b;}
    private void setDriverStatus(String id,String status,LinearLayout box){try{backend().rpc("admin_set_driver_status",new JSONObject().put("p_driver_id",id).put("p_status",status),new Backend.Callback(){public void ok(Object x){toast("Chofer "+status);loadAllDrivers(box);}public void error(String m){toast(m);}});}catch(Exception e){toast(e.getMessage());}}
    private void verifyDriverDialog(String id,boolean verified,LinearLayout box){EditText notes=input("Notas de verificación (opcional)");new AlertDialog.Builder(this).setTitle(verified?"Quitar verificación":"Verificar autenticidad").setMessage(verified?"Se marcará nuevamente como pendiente de revisión documental.":"Confirma solo después de revisar DNI, licencia y datos del vehículo.").setView(notes).setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w)->{try{backend().rpc("cm_admin_verify_driver",new JSONObject().put("p_driver_id",id).put("p_verified",!verified).put("p_notes",notes.getText().toString()),new Backend.Callback(){public void ok(Object x){toast(!verified?"Chofer verificado":"Verificación retirada");loadAllDrivers(box);}public void error(String m){toast(m);}});}catch(Exception e){toast(e.getMessage());}}).show();}
    private void confirmDeleteDriver(String id,String name,LinearLayout box){new AlertDialog.Builder(this).setTitle("Eliminar chofer").setMessage("Se desactivará a "+name+" y su vehículo. El historial de viajes se conservará.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{try{backend().rpc("cm_admin_delete_driver",new JSONObject().put("p_driver_id",id),new Backend.Callback(){public void ok(Object x){toast("Chofer eliminado/desactivado");loadAllDrivers(box);}public void error(String m){toast(m);}});}catch(Exception e){toast(e.getMessage());}}).show();}

    private void screenMore(){
        currentTab="mas";shell("Más","",6);
        LinearLayout c=card();c.addView(tx("Administración",22,Color.WHITE,true));Button archive=btn("Archivos diarios de viajes",true),users=btn("Usuarios registrados",false),payments=btn("Pagos y membresías",false),fares=btn("Tarifas",false),logout=btn("Cerrar sesión",false);c.addView(archive);c.addView(users);c.addView(payments);c.addView(fares);c.addView(logout);body().addView(c);archive.setOnClickListener(v->screenDailyArchive());users.setOnClickListener(v->screenUsers());payments.setOnClickListener(v->screenPayments());fares.setOnClickListener(v->screenFareManager());logout.setOnClickListener(v->{backend().logout();screenLogin();});
    }

    @Override void screenPayments(){currentTab="mas";super.screenPayments();}

    private void screenDailyArchive(){
        currentTab="mas";shell("Archivos diarios","",7);
        LinearLayout filter=card();filter.addView(tx("Archivo diario de viajes",22,Color.WHITE,true));filter.addView(tx("Selecciona un día para ver cada viaje agrupado por chofer y ordenado por hora.",13,MUTED,false));EditText day=input("AAAA-MM-DD");day.setText(todayLima());day.setFocusable(false);Button pick=btn("Seleccionar fecha",false),load=btn("Abrir archivo del día",true);filter.addView(day);filter.addView(pick);filter.addView(load);body().addView(filter);LinearLayout list=card();list.addView(tx("Archivo "+day.getText(),18,Color.WHITE,true));body().addView(list);pick.setOnClickListener(v->openDatePicker(day));load.setOnClickListener(v->loadDailyTrips(list,day.getText().toString().trim(),false));loadDailyTrips(list,day.getText().toString().trim(),false);
    }

    private void openDatePicker(EditText target){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));DatePickerDialog dlg=new DatePickerDialog(this,(view,y,m,d)->target.setText(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)),c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));dlg.show();}

    private void loadDailyTrips(LinearLayout box,String day,boolean compact){
        box.removeAllViews();box.addView(tx((compact?"Operación de hoy · ":"Archivo diario · ")+day,20,Color.WHITE,true));try{backend().rpc("cm_admin_daily_trip_archive",new JSONObject().put("p_day",day),new Backend.Callback(){public void ok(Object x){if(!(x instanceof JSONArray)){box.addView(tx("Sin viajes para esta fecha.",14,MUTED,false));return;}JSONArray a=(JSONArray)x;if(a.length()==0){box.addView(tx("No se registraron viajes en esta fecha.",14,MUTED,false));return;}String lastDriver="";for(int i=0;i<a.length();i++){JSONObject t=a.optJSONObject(i);if(t==null)continue;String driver=t.optString("driver_name","Sin chofer");if(!driver.equals(lastDriver)){TextView h=tx("🛺 "+driver,17,GOLD2,true);h.setPadding(0,dp(12),0,dp(5));box.addView(h);lastDriver=driver;}LinearLayout c=subCard();c.addView(tx(t.optString("trip_hour","--:--")+" · "+t.optString("status","—")+" · S/ "+String.format(Locale.US,"%.2f",t.optDouble("fare",0)),14,Color.WHITE,true));c.addView(tx(t.optString("origin_address","Origen")+" → "+t.optString("destination_address","Destino"),12,MUTED,false));if(!compact)c.addView(tx("Pasajero: "+t.optString("passenger_name","Usuario")+" · ID viaje: "+shortId(t.optString("trip_id","")),11,MUTED,false));box.addView(c);}}public void error(String m){box.addView(tx("No se pudo cargar el archivo: "+m,13,RED,false));}});}catch(Exception e){box.addView(tx("Fecha inválida.",13,RED,false));}
    }

    private void screenUsers(){
        currentTab="mas";shell("Usuarios","",8);LinearLayout intro=card();intro.addView(tx("Usuarios registrados",22,Color.WHITE,true));intro.addView(tx("Consulta cuentas, envía notificaciones o elimina/desactiva usuarios sin borrar su historial de viajes.",13,MUTED,false));body().addView(intro);LinearLayout list=card();list.addView(tx("Cargando usuarios…",14,MUTED,false));body().addView(list);loadUsers(list);
    }

    private void loadUsers(LinearLayout box){box.removeAllViews();box.addView(tx("Directorio de usuarios",20,Color.WHITE,true));backend().rpc("cm_admin_user_directory",new JSONObject(),new Backend.Callback(){public void ok(Object x){if(!(x instanceof JSONArray)){box.addView(tx("Sin usuarios.",14,MUTED,false));return;}JSONArray a=(JSONArray)x;if(a.length()==0){box.addView(tx("Sin usuarios registrados.",14,MUTED,false));return;}for(int i=0;i<a.length();i++){JSONObject u=a.optJSONObject(i);if(u==null)continue;String id=u.optString("id"),name=u.optString("full_name","Usuario");LinearLayout c=subCard();c.addView(tx(name,16,Color.WHITE,true));c.addView(tx(u.optString("email","—")+" · "+u.optString("phone","Sin teléfono"),12,MUTED,false));c.addView(tx("Estado: "+u.optString("account_status","activo")+" · Calificación: "+u.optString("rating","—"),12,GOLD2,true));LinearLayout r=new LinearLayout(InteractiveFlowActivity.this);Button notify=smallAction("Notificar",PURPLE),delete=smallAction("Eliminar",RED);r.addView(notify,new LinearLayout.LayoutParams(0,dp(48),1));r.addView(delete,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(r);notify.setOnClickListener(v->notificationDialog(id,name));delete.setOnClickListener(v->confirmDeleteUser(id,name,box));box.addView(c);}}public void error(String m){box.addView(tx("Error: "+m,13,RED,false));}});}
    private void notificationDialog(String id,String name){LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),0,dp(16),0);EditText title=input("Título"),msg=input("Mensaje");msg.setSingleLine(false);msg.setMinLines(3);form.addView(title);form.addView(msg);new AlertDialog.Builder(this).setTitle("Notificar a "+name).setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Enviar",(d,w)->{try{backend().rpc("cm_admin_send_user_notification",new JSONObject().put("p_user_id",id).put("p_title",title.getText().toString()).put("p_body",msg.getText().toString()),new Backend.Callback(){public void ok(Object x){toast("Notificación enviada");}public void error(String m){toast(m);}});}catch(Exception e){toast(e.getMessage());}}).show();}
    private void confirmDeleteUser(String id,String name,LinearLayout box){new AlertDialog.Builder(this).setTitle("Eliminar usuario").setMessage("Se desactivará a "+name+". Sus viajes históricos permanecerán en los archivos diarios.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{try{backend().rpc("cm_admin_delete_user",new JSONObject().put("p_user_id",id),new Backend.Callback(){public void ok(Object x){toast("Usuario eliminado/desactivado");loadUsers(box);}public void error(String m){toast(m);}});}catch(Exception e){toast(e.getMessage());}}).show();}

    private void screenFareManager(){
        currentTab="mas";shell("Tarifas","",9);LinearLayout c=card();c.addView(tx("Tarifa CiviMoto",22,Color.WHITE,true));c.addView(tx("Comisión de plataforma: 0%. Puedes modificar la tarifa activa o desactivarla para reemplazarla.",13,GOLD2,true));EditText base=input("Tarifa base"),km=input("Precio por km"),minimum=input("Tarifa mínima");base.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);km.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);minimum.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);c.addView(base);c.addView(km);c.addView(minimum);Button save=btn("Guardar / cambiar tarifa",true),disable=smallAction("Desactivar / eliminar tarifa activa",RED);c.addView(save);c.addView(disable);body().addView(c);loadFareFields(base,km,minimum);save.setOnClickListener(v->saveFareFields(base,km,minimum));disable.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Desactivar tarifa").setMessage("No habrá una tarifa activa hasta que guardes una nueva. ¿Continuar?").setNegativeButton("Cancelar",null).setPositiveButton("Desactivar",(d,w)->backend().rpc("cm_admin_deactivate_fare",new JSONObject(),new Backend.Callback(){public void ok(Object x){toast("Tarifa desactivada");base.setText("");km.setText("");minimum.setText("");}public void error(String m){toast(m);}})).show());
    }
    private void loadFareFields(EditText base,EditText km,EditText minimum){backend().rest("GET","fares?active=eq.true&order=created_at.desc&limit=1&select=base_fare,per_km,minimum_fare",null,new Backend.Callback(){public void ok(Object x){if(x instanceof JSONArray&&((JSONArray)x).length()>0){JSONObject f=((JSONArray)x).optJSONObject(0);if(f!=null){base.setText(f.optString("base_fare",""));km.setText(f.optString("per_km",""));minimum.setText(f.optString("minimum_fare",""));}}}public void error(String m){}});}
    private void saveFareFields(EditText base,EditText km,EditText minimum){try{double b=Double.parseDouble(base.getText().toString().trim()),k=Double.parseDouble(km.getText().toString().trim()),m=Double.parseDouble(minimum.getText().toString().trim());backend().rpc("cm_admin_update_fare",new JSONObject().put("p_base",b).put("p_per_km",k).put("p_minimum",m),new Backend.Callback(){public void ok(Object x){toast("Tarifa actualizada");}public void error(String msg){toast(msg);}});}catch(Exception e){toast("Completa los tres importes correctamente.");}}

    private String todayLima(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setTimeZone(TimeZone.getTimeZone("America/Lima"));return f.format(new Date());}
    private String shortId(String id){return id==null?"—":id.substring(0,Math.min(8,id.length()));}
}
