package pe.civimoto.usuario;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.lang.reflect.Field;

/** Muestra el perfil real del conductor asignado durante el seguimiento del viaje. */
public class PassengerProfileActivity extends PassengerDocumentActivity {
    private final Handler ui=new Handler(Looper.getMainLooper());
    private LinearLayout driverCard;
    private int profileAttempts=0;

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private Backend backend(){return (Backend)field("backend");}
    private LinearLayout body(){return (LinearLayout)field("body");}

    @Override void screenTracking(){
        ui.removeCallbacksAndMessages(null);profileAttempts=0;
        super.screenTracking();
        LinearLayout b=body();if(b==null)return;
        driverCard=card();
        driverCard.addView(tx("Conductor asignado",19,Color.WHITE,true));
        driverCard.addView(tx("Esperando que un conductor acepte tu solicitud…",13,Color.rgb(176,180,190),false));
        int pos=Math.min(1,b.getChildCount());b.addView(driverCard,pos);
        ui.postDelayed(this::loadDriverProfile,700);
    }

    private void loadDriverProfile(){
        String tripId=field("tripId") instanceof String?(String)field("tripId"):null;
        if(tripId==null||tripId.isEmpty()||backend()==null)return;
        try{backend().rpc("cm_trip_driver_profile",new JSONObject().put("p_trip_id",tripId),new Backend.Callback(){
            public void ok(Object x){
                JSONObject d=Backend.firstObject(x);
                if(d==null){retry();return;}
                renderDriver(d);
            }
            public void error(String m){retry();}
        });}catch(Exception e){retry();}
    }

    private void retry(){if(++profileAttempts<45)ui.postDelayed(this::loadDriverProfile,2000);}

    private void renderDriver(JSONObject d){
        if(driverCard==null)return;driverCard.removeAllViews();
        driverCard.addView(tx("Tu conductor",20,Color.WHITE,true));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView face=photoBox(96);top.addView(face,new LinearLayout.LayoutParams(dp(96),dp(96)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);
        info.addView(tx(d.optString("full_name","Conductor"),18,Color.WHITE,true));
        info.addView(tx("★ "+d.optString("rating","—")+" · Tel: "+d.optString("phone","—"),13,Color.rgb(255,220,90),true));
        info.addView(tx("Licencia: "+d.optString("license_number","—"),12,Color.rgb(176,180,190),false));
        top.addView(info,new LinearLayout.LayoutParams(0,-2,1));driverCard.addView(top);
        driverCard.addView(tx("Mototaxi: "+d.optString("brand","—")+" "+d.optString("model","—")+" · "+d.optString("color","—")+" · Placa: "+d.optString("plate","—"),13,Color.WHITE,true));
        ImageView vehicle=photoBox(150);driverCard.addView(vehicle,new LinearLayout.LayoutParams(-1,dp(150)));
        loadPhoto(d.optString("face_photo_path"),face);
        loadPhoto(d.optString("vehicle_photo_path"),vehicle);
    }

    private ImageView photoBox(int h){
        ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);im.setImageResource(R.drawable.logo_civimoto);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(25,25,25));bg.setStroke(dp(2),Color.rgb(255,190,0));bg.setCornerRadius(dp(18));im.setBackground(bg);im.setClipToOutline(true);im.setPadding(dp(2),dp(2),dp(2),dp(2));return im;
    }

    private void loadPhoto(String path,ImageView target){
        if(path==null||path.trim().isEmpty())return;
        new ProfileImageLoader(backend()).download(path,new Backend.Callback(){public void ok(Object x){try{byte[] data=(byte[])x;Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);if(bm!=null)target.setImageBitmap(bm);}catch(Exception ignored){}}public void error(String m){}});
    }

    @Override protected void onDestroy(){ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
