package pe.civimoto.usuario;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/** Registro de pasajero con DNI/fotos + geocodificación precisa por zonas de La Libertad. */
public class PassengerDocumentActivity extends PreciseLiveRatingActivity {
    private static final int REQ_PHOTO=9901;
    private final Map<String,byte[]> photos=new HashMap<>();
    private final Map<String,TextView> photoStatus=new HashMap<>();
    private String pendingKind;
    private Uri pendingUri;

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private Backend backend(){return (Backend)field("backend");}
    private LinearLayout body(){return (LinearLayout)field("body");}

    @Override void screenRegister(){
        shell("","",0);
        LinearLayout c=card();
        c.addView(tx("Crear cuenta",24,android.graphics.Color.WHITE,true));
        c.addView(tx("Completa tus datos, registra tu DNI y toma las 2 fotos de identificación.",13,android.graphics.Color.LTGRAY,false));
        EditText name=input("Nombre completo"),email=input("Correo electrónico"),phone=input("Teléfono"),dni=input("Número de DNI"),pass=input("Contraseña (8+ caracteres)");
        dni.setInputType(InputType.TYPE_CLASS_NUMBER);pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        c.addView(name);c.addView(email);c.addView(phone);c.addView(dni);c.addView(pass);
        CheckBox show=new CheckBox(this);show.setText("Mostrar contraseña");show.setTextColor(android.graphics.Color.rgb(255,220,90));
        show.setOnCheckedChangeListener((v,on)->{int p=pass.getSelectionStart();pass.setInputType(InputType.TYPE_CLASS_TEXT|(on?InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:InputType.TYPE_TEXT_VARIATION_PASSWORD));pass.setSelection(Math.max(0,Math.min(p,pass.length())));show.setText(on?"Ocultar contraseña":"Mostrar contraseña");});c.addView(show);
        addPhotoControl(c,"dni","📷 Tomar foto del DNI");addPhotoControl(c,"face","📷 Tomar foto de mi cara / perfil");
        Button create=btn("Crear cuenta y guardar identificación",true),back=btn("Ya tengo cuenta",false);c.addView(create);c.addView(back);body().addView(c);
        create.setOnClickListener(v->{String n=name.getText().toString().trim(),e=email.getText().toString().trim(),p=phone.getText().toString().trim(),d=dni.getText().toString().trim();if(n.isEmpty()||e.isEmpty()||p.isEmpty()||d.isEmpty()||pass.length()<8){toast("Completa nombre, correo, teléfono, DNI y contraseña.");return;}if(!photos.containsKey("dni")||!photos.containsKey("face")){toast("Toma la foto del DNI y la foto de tu cara/perfil.");return;}create.setEnabled(false);backend().register(n,e,pass.getText().toString(),"usuario",new Backend.Callback(){public void ok(Object x){backend().login(e,pass.getText().toString(),"usuario",new Backend.Callback(){public void ok(Object y){uploadDocuments(p,d,create);}public void error(String m){create.setEnabled(true);toast(m);}});}public void error(String m){create.setEnabled(true);toast(m);}});});
        back.setOnClickListener(v->screenLogin());
    }

    private void addPhotoControl(LinearLayout c,String kind,String title){Button b=btn(title,false);TextView status=tx("Pendiente",12,android.graphics.Color.rgb(255,175,45),true);status.setPadding(dp(4),0,0,dp(5));c.addView(b);c.addView(status);photoStatus.put(kind,status);b.setOnClickListener(v->takePhoto(kind));}
    private void takePhoto(String kind){try{pendingKind=kind;pendingUri=null;Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);if(i.resolveActivity(getPackageManager())==null){toast("No se encontró una cámara disponible.");return;}ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"civimoto_passenger_"+kind+"_"+System.currentTimeMillis()+".jpg");values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");if(Build.VERSION.SDK_INT>=29)values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/CiviMoto");try{pendingUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);}catch(Exception ignored){}if(pendingUri!=null){i.putExtra(MediaStore.EXTRA_OUTPUT,pendingUri);i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);}startActivityForResult(i,REQ_PHOTO);}catch(Exception e){toast("No se pudo abrir la cámara.");}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode!=REQ_PHOTO)return;if(resultCode!=RESULT_OK||pendingKind==null){toast("Foto cancelada.");return;}try{byte[] bytes=null;if(pendingUri!=null){try(InputStream in=getContentResolver().openInputStream(pendingUri)){bytes=readAndCompress(in);}}if((bytes==null||bytes.length==0)&&data!=null&&data.getExtras()!=null&&data.getExtras().get("data") instanceof Bitmap){Bitmap b=(Bitmap)data.getExtras().get("data");ByteArrayOutputStream os=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,88,os);bytes=os.toByteArray();}if(bytes==null||bytes.length==0)throw new Exception("sin imagen");photos.put(pendingKind,bytes);TextView s=photoStatus.get(pendingKind);if(s!=null){s.setText("✓ Foto capturada");s.setTextColor(android.graphics.Color.rgb(35,190,105));}toast("Foto guardada correctamente.");}catch(Exception e){toast("No se pudo leer la foto. Inténtalo nuevamente.");}}
    private byte[] readAndCompress(InputStream in)throws Exception{Bitmap b=BitmapFactory.decodeStream(in);if(b==null)throw new Exception("imagen inválida");int w=b.getWidth(),h=b.getHeight(),max=Math.max(w,h);if(max>1800){float f=1800f/max;b=Bitmap.createScaledBitmap(b,Math.max(1,(int)(w*f)),Math.max(1,(int)(h*f)),true);}ByteArrayOutputStream os=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,88,os);return os.toByteArray();}
    private void uploadDocuments(String phone,String dni,Button create){PassengerStorage st=new PassengerStorage(backend());String uid=backend().getUserId(),base="passengers/"+uid+"/";st.uploadJpeg(base+"dni.jpg",photos.get("dni"),new Backend.Callback(){public void ok(Object a){st.uploadJpeg(base+"face.jpg",photos.get("face"),new Backend.Callback(){public void ok(Object b){saveDocumentPaths(phone,dni,base,create);}public void error(String m){failUpload(create,m);}});}public void error(String m){failUpload(create,m);}});}
    private void saveDocumentPaths(String phone,String dni,String base,Button create){try{JSONObject p=new JSONObject().put("p_document_number",dni).put("p_dni_photo_path",base+"dni.jpg").put("p_face_photo_path",base+"face.jpg").put("p_phone",phone);backend().rpc("cm_passenger_save_documents",p,new Backend.Callback(){public void ok(Object x){toast("Cuenta creada correctamente.");screenHome();}public void error(String m){failUpload(create,m);}});}catch(Exception e){failUpload(create,e.getMessage());}}
    private void failUpload(Button create,String m){create.setEnabled(true);toast("No se pudo guardar la identificación: "+m);}
}
