package pe.civimoto.usuario;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.json.JSONObject;
import java.lang.reflect.Field;

/** Muestra foto y datos compactos del conductor apenas acepta el viaje. */
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
        ui.postDelayed(this::loadDriverProfile,500);
    }

    private void loadDriverProfile(){
        String tripId=field("tripId") instanceof String?(String)field("tripId"):null;
        if(tripId==null||tripId.isEmpty()||backend()==null)return;
        try{backend().rpc("cm_trip_driver_profile",new JSONObject().put("p_trip_id",tripId),new Backend.Callback(){
            public void ok(Object x){JSONObject d=Backend.firstObject(x);if(d==null){retry();return;}renderDriver(d);}
            public void error(String m){retry();}
        });}catch(Exception e){retry();}
    }

    private void retry(){if(++profileAttempts<90)ui.postDelayed(this::loadDriverProfile,1500);}

    private void renderDriver(JSONObject d){
        if(driverCard==null)return;driverCard.removeAllViews();
        driverCard.addView(tx("Conductor asignado",19,Color.WHITE,true));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView face=photoBox();top.addView(face,new LinearLayout.LayoutParams(dp(84),dp(84)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);
        info.addView(tx(d.optString("full_name","Conductor"),17,Color.WHITE,true));
        info.addView(tx("Placa: "+d.optString("plate","—"),14,Color.rgb(255,220,90),true));
        info.addView(tx("★ "+d.optString("rating","—")+" · "+d.optString("phone","—"),12,Color.rgb(176,180,190),false));
        String vehicle=(d.optString("brand","")+" "+d.optString("model","")+" "+d.optString("color","")).trim();
        if(!vehicle.isEmpty())info.addView(tx(vehicle,12,Color.rgb(176,180,190),false));
        top.addView(info,new LinearLayout.LayoutParams(0,-2,1));driverCard.addView(top);
        loadPhoto(d.optString("face_photo_path"),face);
    }

    private ImageView photoBox(){
        ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);im.setImageResource(R.drawable.logo_civimoto);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(25,25,25));bg.setStroke(dp(2),Color.rgb(255,190,0));bg.setCornerRadius(dp(42));im.setBackground(bg);im.setClipToOutline(true);im.setPadding(dp(2),dp(2),dp(2),dp(2));return im;
    }

    private void loadPhoto(String path,ImageView target){
        if(path==null||path.trim().isEmpty())return;
        new ProfileImageLoader(backend()).download(path,new Backend.Callback(){public void ok(Object x){try{byte[] data=(byte[])x;Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);if(bm!=null)target.setImageBitmap(bm);}catch(Exception ignored){}}public void error(String m){}});
    }

    @Override protected void onDestroy(){ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
