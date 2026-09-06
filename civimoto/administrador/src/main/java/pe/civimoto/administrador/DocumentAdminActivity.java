package pe.civimoto.administrador;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Field;

/** Panel de choferes con documentos privados, aprobación y eliminación corregida. */
public class DocumentAdminActivity extends InteractiveFlowActivity {
    private final int MUTED=Color.rgb(176,180,190),GREEN=Color.rgb(35,190,105),RED=Color.rgb(235,65,65),ORANGE=Color.rgb(255,175,45);
    private Backend backend(){try{Field f=FlowActivity.class.getDeclaredField("backend");f.setAccessible(true);return(Backend)f.get(this);}catch(Exception e){return null;}}
    private LinearLayout body(){try{Field f=FlowActivity.class.getDeclaredField("body");f.setAccessible(true);return(LinearLayout)f.get(this);}catch(Exception e){return null;}}
    private void tab(String v){try{Field f=InteractiveFlowActivity.class.getDeclaredField("currentTab");f.setAccessible(true);f.set(this,v);}catch(Exception ignored){}}

    @Override void screenDrivers(){
        tab("choferes");shell("Choferes","",4);
        LinearLayout intro=card();intro.addView(tx("Choferes registrados",22,Color.WHITE,true));intro.addView(tx("Revisa identidad, DNI, tarjeta de propiedad, foto del mototaxi y rostro antes de aprobar. Los choferes eliminados dejan de aparecer en el directorio activo.",13,MUTED,false));body().addView(intro);
        LinearLayout list=card();list.addView(tx("Cargando choferes…",14,MUTED,false));body().addView(list);loadDriverDirectory(list);
    }

    private void loadDriverDirectory(LinearLayout box){
        box.removeAllViews();box.addView(tx("Directorio activo",20,Color.WHITE,true));
        backend().rpc("admin_driver_directory",new JSONObject(),new Backend.Callback(){public void ok(Object x){
            if(!(x instanceof JSONArray)){box.addView(tx("Sin choferes registrados.",14,MUTED,false));return;}JSONArray a=(JSONArray)x;int shown=0;
            for(int i=0;i<a.length();i++){JSONObject d=a.optJSONObject(i);if(d==null)continue;if("suspendido".equals(d.optString("account_status")))continue;shown++;addDriverCard(box,d);}if(shown==0)box.addView(tx("Sin choferes activos.",14,MUTED,false));
        }public void error(String m){box.addView(tx("Error: "+m,14,RED,false));}});
    }

    private void addDriverCard(LinearLayout box,JSONObject d){
        String id=d.optString("id"),name=d.optString("full_name","Chofer"),status=d.optString("status","pendiente");boolean verified=d.optBoolean("identity_verified",false);
        LinearLayout c=card();c.addView(tx(name+" · "+d.optString("plate","Sin placa"),17,Color.WHITE,true));c.addView(tx("Estado: "+status+" · "+(verified?"✓ documentos verificados":"documentos pendientes"),12,verified?GREEN:ORANGE,true));
        c.addView(tx("DNI: "+d.optString("document_number","—")+" · Licencia: "+d.optString("license_number","—"),12,MUTED,false));c.addView(tx("Teléfono: "+d.optString("phone","—")+" · Email: "+d.optString("email","—"),12,MUTED,false));
        Button docs=btn("Ver fotos y documentos",false);c.addView(docs);docs.setOnClickListener(v->showDocumentMenu(d,name));
        if("pendiente".equals(status)||"rechazado".equals(status)){Button approve=btn("Aprobar chofer",true);c.addView(approve);approve.setOnClickListener(v->setStatus(id,"aprobado",box));}
        if("pendiente".equals(status)||"aprobado".equals(status)){Button reject=btn("Rechazar / suspender",false);c.addView(reject);reject.setOnClickListener(v->setStatus(id,"rechazado",box));}
        Button verify=btn(verified?"Quitar verificación documental":"Marcar documentos como verificados",false);c.addView(verify);verify.setOnClickListener(v->verify(id,!verified,box));
        Button delete=btn("Eliminar chofer",false);delete.setTextColor(RED);c.addView(delete);delete.setOnClickListener(v->confirmDelete(id,name,box));
        box.addView(c);
    }

    private void showDocumentMenu(JSONObject d,String name){
        String[] labels={"Foto DNI","Tarjeta de propiedad","Foto del mototaxi","Foto de rostro / perfil"};
        String[] paths={d.optString("dni_photo_path"),d.optString("property_card_photo_path"),d.optString("vehicle_photo_path"),d.optString("face_photo_path")};
        new AlertDialog.Builder(this).setTitle("Documentos de "+name).setItems(labels,(dialog,which)->openImage(labels[which],paths[which])).setNegativeButton("Cerrar",null).show();
    }

    private void openImage(String title,String path){
        if(path==null||path.trim().isEmpty()){toast("El conductor todavía no cargó este documento.");return;}
        toast("Abriendo documento…");new AdminStorage(backend()).download(path,new Backend.Callback(){public void ok(Object x){
            byte[] data=(byte[])x;Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);if(bm==null){toast("No se pudo leer la imagen.");return;}
            ScrollView sc=new ScrollView(DocumentAdminActivity.this);LinearLayout root=new LinearLayout(DocumentAdminActivity.this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(10),dp(10),dp(10),dp(10));ImageView im=new ImageView(DocumentAdminActivity.this);im.setAdjustViewBounds(true);im.setScaleType(ImageView.ScaleType.FIT_CENTER);im.setImageBitmap(bm);root.addView(im,new LinearLayout.LayoutParams(-1,-2));sc.addView(root);new AlertDialog.Builder(DocumentAdminActivity.this).setTitle(title).setView(sc).setPositiveButton("Cerrar",null).show();
        }public void error(String m){toast(m);}});
    }

    private void setStatus(String id,String status,LinearLayout box){try{backend().rpc("admin_set_driver_status",new JSONObject().put("p_driver_id",id).put("p_status",status),new Backend.Callback(){public void ok(Object x){toast("Estado actualizado: "+status);loadDriverDirectory(box);}public void error(String m){toast(m);}});}catch(Exception e){toast(e.getMessage());}}
    private void verify(String id,boolean value,LinearLayout box){try{backend().rpc("cm_admin_verify_driver",new JSONObject().put("p_driver_id",id).put("p_verified",value).put("p_notes",value?"Documentos revisados desde panel CiviMoto":"Verificación retirada"),new Backend.Callback(){public void ok(Object x){toast(value?"Documentos verificados":"Verificación retirada");loadDriverDirectory(box);}public void error(String m){toast(m);}});}catch(Exception e){toast(e.getMessage());}}
    private void confirmDelete(String id,String name,LinearLayout box){new AlertDialog.Builder(this).setTitle("Eliminar chofer").setMessage("¿Eliminar a "+name+"? Se desactivará su cuenta y su vehículo, conservando el historial de viajes.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{try{backend().rpc("cm_admin_delete_driver",new JSONObject().put("p_driver_id",id),new Backend.Callback(){public void ok(Object x){toast("Chofer eliminado correctamente");loadDriverDirectory(box);}public void error(String m){toast("No se pudo eliminar: "+m);}});}catch(Exception e){toast(e.getMessage());}}).show();}
}
