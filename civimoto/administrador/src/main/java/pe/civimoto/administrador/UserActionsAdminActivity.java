package pe.civimoto.administrador;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Field;

/** Panel de usuarios con foto de perfil y cuatro acciones independientes. */
public class UserActionsAdminActivity extends DocumentAdminActivity {
    private final int GREEN=Color.rgb(35,190,105), ORANGE=Color.rgb(235,145,35), RED=Color.rgb(235,65,65), PURPLE=Color.rgb(132,67,230), MUTED=Color.rgb(176,180,190), GOLD2=Color.rgb(255,220,90);
    private final Handler ui=new Handler(Looper.getMainLooper());

    private Backend backend(){try{Field f=FlowActivity.class.getDeclaredField("backend");f.setAccessible(true);return(Backend)f.get(this);}catch(Exception e){return null;}}
    private LinearLayout body(){try{Field f=FlowActivity.class.getDeclaredField("body");f.setAccessible(true);return(LinearLayout)f.get(this);}catch(Exception e){return null;}}
    private void tab(String v){try{Field f=InteractiveFlowActivity.class.getDeclaredField("currentTab");f.setAccessible(true);f.set(this,v);}catch(Exception ignored){}}

    @Override void shell(String title,String subtitle,int step){super.shell(title,subtitle,step);ui.post(this::hookUsersButton);}
    private void hookUsersButton(){View root=findViewById(android.R.id.content);Button users=findButton(root,"Usuarios registrados");if(users!=null)users.setOnClickListener(v->showUsers());}
    private Button findButton(View v,String text){if(v instanceof Button&&text.equals(((Button)v).getText().toString()))return(Button)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button b=findButton(g.getChildAt(i),text);if(b!=null)return b;}}return null;}

    private void showUsers(){tab("mas");shell("Usuarios","",8);LinearLayout intro=card();intro.addView(tx("Usuarios registrados",22,Color.WHITE,true));intro.addView(tx("Foto de perfil, identificación y acciones: Aprobar, Suspender, Eliminar y Notificar.",13,MUTED,false));body().addView(intro);LinearLayout list=card();list.addView(tx("Cargando usuarios…",14,MUTED,false));body().addView(list);loadUsersActions(list);}

    private void loadUsersActions(LinearLayout box){
        box.removeAllViews();box.addView(tx("Directorio de usuarios",20,Color.WHITE,true));
        backend().rpc("cm_admin_user_directory",new JSONObject(),new Backend.Callback(){
            public void ok(Object x){
                if(!(x instanceof JSONArray)||((JSONArray)x).length()==0){box.addView(tx("Sin usuarios registrados.",14,MUTED,false));return;}
                JSONArray a=(JSONArray)x;
                for(int i=0;i<a.length();i++){
                    JSONObject u=a.optJSONObject(i);if(u==null)continue;
                    String id=u.optString("id"),name=u.optString("full_name","Usuario"),status=u.optString("account_status","activo");
                    LinearLayout c=card();
                    ImageView face=profileImage();c.addView(face,new LinearLayout.LayoutParams(dp(112),dp(112)));loadProfilePhoto(u.optString("face_photo_path"),face);
                    c.addView(tx(name,17,Color.WHITE,true));
                    c.addView(tx(u.optString("email","—")+" · "+u.optString("phone","Sin teléfono"),12,MUTED,false));
                    c.addView(tx("DNI: "+u.optString("document_number","—"),12,MUTED,false));
                    c.addView(tx("Estado: "+status+" · Calificación: "+u.optString("rating","—"),12,GOLD2,true));
                    Button dni=btn("Ver foto del DNI",false);c.addView(dni);dni.setOnClickListener(v->openUserDni(name,u.optString("dni_photo_path")));
                    LinearLayout row1=new LinearLayout(UserActionsAdminActivity.this);row1.setOrientation(LinearLayout.HORIZONTAL);Button approve=userAction("Aprobar",GREEN),suspend=userAction("Suspender",ORANGE);row1.addView(approve,new LinearLayout.LayoutParams(0,dp(48),1));row1.addView(suspend,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(row1);
                    LinearLayout row2=new LinearLayout(UserActionsAdminActivity.this);row2.setOrientation(LinearLayout.HORIZONTAL);Button delete=userAction("Eliminar",RED),notify=userAction("Notificar",PURPLE);row2.addView(delete,new LinearLayout.LayoutParams(0,dp(48),1));row2.addView(notify,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(row2);
                    approve.setEnabled(!"activo".equals(status));suspend.setEnabled(!"suspendido".equals(status));
                    approve.setOnClickListener(v->setUserStatus(id,name,"activo",box));suspend.setOnClickListener(v->confirmSuspend(id,name,box));delete.setOnClickListener(v->confirmDelete(id,name,box));notify.setOnClickListener(v->notifyUser(id,name));box.addView(c);
                }
            }
            public void error(String m){box.addView(tx("Error: "+m,13,RED,false));}
        });
    }

    private ImageView profileImage(){ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);im.setImageResource(R.drawable.logo_civimoto);GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(22,22,22));g.setStroke(dp(2),Color.rgb(255,190,0));g.setCornerRadius(dp(20));im.setBackground(g);im.setClipToOutline(true);return im;}
    private void loadProfilePhoto(String path,ImageView im){if(path==null||path.trim().isEmpty())return;new AdminStorage(backend()).download(path,new Backend.Callback(){public void ok(Object x){try{byte[] data=(byte[])x;Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);if(bm!=null)im.setImageBitmap(bm);}catch(Exception ignored){}}public void error(String m){}});}
    private void openUserDni(String name,String path){if(path==null||path.trim().isEmpty()){toast("El usuario no cargó foto del DNI.");return;}new AdminStorage(backend()).download(path,new Backend.Callback(){public void ok(Object x){try{byte[] data=(byte[])x;Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);if(bm==null){toast("No se pudo leer la foto.");return;}ImageView im=new ImageView(UserActionsAdminActivity.this);im.setAdjustViewBounds(true);im.setImageBitmap(bm);new AlertDialog.Builder(UserActionsAdminActivity.this).setTitle("DNI · "+name).setView(im).setPositiveButton("Cerrar",null).show();}catch(Exception e){toast("No se pudo abrir la foto.");}}public void error(String m){toast(m);}});}

    private Button userAction(String text,int color){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(13);b.setTypeface(null,1);b.setTextColor(Color.WHITE);GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(12));b.setBackground(g);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(46));lp.setMargins(dp(3),dp(4),dp(3),dp(4));b.setLayoutParams(lp);return b;}
    private void setUserStatus(String id,String name,String status,LinearLayout box){try{backend().rpc("cm_admin_set_user_status",new JSONObject().put("p_user_id",id).put("p_status",status),new Backend.Callback(){public void ok(Object x){toast("activo".equals(status)?name+" aprobado y activado":name+" suspendido");loadUsersActions(box);}public void error(String m){toast("No se pudo actualizar: "+m);}});}catch(Exception e){toast(e.getMessage());}}
    private void confirmSuspend(String id,String name,LinearLayout box){new AlertDialog.Builder(this).setTitle("Suspender usuario").setMessage("¿Suspender temporalmente a "+name+"? La cuenta podrá volver a aprobarse después.").setNegativeButton("Cancelar",null).setPositiveButton("Suspender",(d,w)->setUserStatus(id,name,"suspendido",box)).show();}
    private void confirmDelete(String id,String name,LinearLayout box){new AlertDialog.Builder(this).setTitle("Eliminar usuario").setMessage("¿Eliminar a "+name+" del directorio CiviMoto?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{try{backend().rpc("cm_admin_delete_user",new JSONObject().put("p_user_id",id),new Backend.Callback(){public void ok(Object x){toast("Usuario eliminado correctamente");loadUsersActions(box);}public void error(String m){toast("No se pudo eliminar: "+m);}});}catch(Exception e){toast(e.getMessage());}}).show();}
    private void notifyUser(String id,String name){LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),0,dp(16),0);EditText title=input("Título"),msg=input("Mensaje");msg.setSingleLine(false);msg.setMinLines(3);form.addView(title);form.addView(msg);new AlertDialog.Builder(this).setTitle("Notificar a "+name).setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Enviar",(d,w)->{String t=title.getText().toString().trim(),m=msg.getText().toString().trim();if(t.isEmpty()||m.isEmpty()){toast("Escribe título y mensaje.");return;}try{backend().rpc("cm_admin_send_user_notification",new JSONObject().put("p_user_id",id).put("p_title",t).put("p_body",m),new Backend.Callback(){public void ok(Object x){toast("Notificación enviada");}public void error(String e){toast("No se pudo notificar: "+e);}});}catch(Exception e){toast(e.getMessage());}}).show();}
}
