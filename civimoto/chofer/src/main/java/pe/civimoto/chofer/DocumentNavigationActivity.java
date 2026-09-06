package pe.civimoto.chofer;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/** Registro premium del conductor con fotos de DNI, rostro, tarjeta y mototaxi. */
public class DocumentNavigationActivity extends GoogleNavigationActivity {
    private static final int REQ_PHOTO=8801;
    private final Map<String,byte[]> photos=new HashMap<>();
    private final Map<String,TextView> photoStatus=new HashMap<>();
    private String pendingKind; private Uri pendingUri;

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private Backend backend(){return (Backend)field("backend");}
    private LinearLayout body(){return (LinearLayout)field("body");}

    @Override void screenRegister(){
        DriverAlertService.stop(this);shell("","",0);
        LinearLayout c=card();c.addView(tx("Registro de conductor",24,android.graphics.Color.WHITE,true));
        c.addView(tx("Completa tus datos y toma las 4 fotos obligatorias para que el administrador pueda validar tu cuenta.",13,android.graphics.Color.LTGRAY,false));
        EditText name=input("Nombre completo"),email=input("Correo electrónico"),phone=input("Teléfono"),dni=input("Número de DNI"),license=input("Número de licencia"),plate=input("Placa del mototaxi"),pass=input("Contraseña (8+ caracteres)");
        pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        c.addView(name);c.addView(email);c.addView(phone);c.addView(dni);c.addView(license);c.addView(plate);c.addView(pass);
        android.widget.CheckBox show=new android.widget.CheckBox(this);show.setText("Mostrar contraseña");show.setTextColor(android.graphics.Color.rgb(255,220,90));show.setOnCheckedChangeListener((v,on)->{int p=pass.getSelectionStart();pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT|(on?android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD));pass.setSelection(Math.max(0,Math.min(p,pass.length())));show.setText(on?"Ocultar contraseña":"Mostrar contraseña");});c.addView(show);
        addPhotoControl(c,"dni","Tomar foto del DNI");
        addPhotoControl(c,"property","Tomar foto de tarjeta de propiedad");
        addPhotoControl(c,"vehicle","Tomar foto del mototaxi");
        addPhotoControl(c,"face","Tomar foto de mi rostro / perfil");
        Button create=btn("Crear cuenta y enviar documentos",true),back=btn("Volver a iniciar sesión",false);c.addView(create);c.addView(back);body().addView(c);
        create.setOnClickListener(v->{
            if(name.getText().toString().trim().isEmpty()||email.getText().toString().trim().isEmpty()||phone.getText().toString().trim().isEmpty()||dni.getText().toString().trim().isEmpty()||license.getText().toString().trim().isEmpty()||plate.getText().toString().trim().isEmpty()||pass.length()<8){toast("Completa todos los datos personales y del vehículo.");return;}
            if(!photos.keySet().containsAll(java.util.Arrays.asList("dni","property","vehicle","face"))){toast("Faltan fotos: DNI, tarjeta de propiedad, mototaxi y rostro son obligatorias.");return;}
            create.setEnabled(false);
            backend().register(name.getText().toString(),email.getText().toString(),pass.getText().toString(),"chofer",new Backend.Callback(){
                public void ok(Object x){backend().login(email.getText().toString(),pass.getText().toString(),"chofer",new Backend.Callback(){
                    public void ok(Object y){uploadDocuments(phone.getText().toString(),dni.getText().toString(),license.getText().toString(),plate.getText().toString(),create);}
                    public void error(String m){create.setEnabled(true);toast(m);}
                });}
                public void error(String m){create.setEnabled(true);toast(m);}
            });
        });
        back.setOnClickListener(v->screenLogin());
    }

    private void addPhotoControl(LinearLayout c,String kind,String title){
        Button b=btn(title,false);TextView status=tx("Pendiente",12,android.graphics.Color.rgb(255,175,45),true);status.setPadding(dp(4),0,0,dp(5));c.addView(b);c.addView(status);photoStatus.put(kind,status);b.setOnClickListener(v->takePhoto(kind));
    }

    private void takePhoto(String kind){
        try{
            pendingKind=kind;pendingUri=null;
            Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(i.resolveActivity(getPackageManager())==null){toast("No se encontró una cámara disponible.");return;}
            ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"civimoto_"+kind+"_"+System.currentTimeMillis()+".jpg");values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
            if(Build.VERSION.SDK_INT>=29)values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/CiviMoto");
            try{pendingUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);}catch(Exception ignored){}
            if(pendingUri!=null){i.putExtra(MediaStore.EXTRA_OUTPUT,pendingUri);i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);}
            startActivityForResult(i,REQ_PHOTO);
        }catch(Exception e){toast("No se pudo abrir la cámara.");}
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=REQ_PHOTO)return;
        if(resultCode!=RESULT_OK||pendingKind==null){toast("Foto cancelada.");return;}
        try{
            byte[] bytes=null;
            if(pendingUri!=null){try(InputStream in=getContentResolver().openInputStream(pendingUri)){bytes=readAndCompress(in);}}
            if((bytes==null||bytes.length==0)&&data!=null&&data.getExtras()!=null&&data.getExtras().get("data") instanceof Bitmap){Bitmap b=(Bitmap)data.getExtras().get("data");ByteArrayOutputStream os=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,88,os);bytes=os.toByteArray();}
            if(bytes==null||bytes.length==0)throw new Exception("sin imagen");
            photos.put(pendingKind,bytes);TextView s=photoStatus.get(pendingKind);if(s!=null){s.setText("✓ Foto capturada");s.setTextColor(android.graphics.Color.rgb(35,190,105));}
            toast("Foto guardada correctamente.");
        }catch(Exception e){toast("No se pudo leer la foto. Inténtalo nuevamente.");}
    }

    private byte[] readAndCompress(InputStream in)throws Exception{
        Bitmap b=BitmapFactory.decodeStream(in);if(b==null)throw new Exception("imagen inválida");
        int w=b.getWidth(),h=b.getHeight(),max=Math.max(w,h);if(max>1800){float f=1800f/max;b=Bitmap.createScaledBitmap(b,Math.max(1,(int)(w*f)),Math.max(1,(int)(h*f)),true);}
        ByteArrayOutputStream os=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,88,os);return os.toByteArray();
    }

    private void uploadDocuments(String phone,String dni,String license,String plate,Button create){
        DriverStorage st=new DriverStorage(backend());String uid=backend().getUserId();String base="drivers/"+uid+"/";
        st.uploadJpeg(base+"dni.jpg",photos.get("dni"),new Backend.Callback(){public void ok(Object a){
            st.uploadJpeg(base+"property_card.jpg",photos.get("property"),new Backend.Callback(){public void ok(Object b){
                st.uploadJpeg(base+"vehicle.jpg",photos.get("vehicle"),new Backend.Callback(){public void ok(Object c){
                    st.uploadJpeg(base+"face.jpg",photos.get("face"),new Backend.Callback(){public void ok(Object d){saveDocumentPaths(phone,dni,license,plate,base,create);}public void error(String m){failUpload(create,m);}});
                }public void error(String m){failUpload(create,m);}});
            }public void error(String m){failUpload(create,m);}});
        }public void error(String m){failUpload(create,m);}});
    }

    private void saveDocumentPaths(String phone,String dni,String license,String plate,String base,Button create){
        try{
            JSONObject p=new JSONObject().put("p_document_number",dni).put("p_license_number",license).put("p_dni_photo_path",base+"dni.jpg").put("p_property_card_photo_path",base+"property_card.jpg").put("p_face_photo_path",base+"face.jpg").put("p_vehicle_photo_path",base+"vehicle.jpg").put("p_plate",plate).put("p_phone",phone);
            backend().rpc("cm_driver_save_documents",p,new Backend.Callback(){public void ok(Object x){toast("Cuenta y documentos enviados. Espera la aprobación del administrador.");screenDashboard();}public void error(String m){failUpload(create,m);}});
        }catch(Exception e){failUpload(create,e.getMessage());}
    }

    private void failUpload(Button create,String m){create.setEnabled(true);toast("No se pudieron guardar los documentos: "+m);}
}
