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

/** Mantiene visible el perfil del conductor asignado durante todo el viaje activo. */
public class PassengerProfileActivity extends PassengerDocumentActivity {
    private final Handler ui=new Handler(Looper.getMainLooper());
    private LinearLayout driverCard;
    private JSONObject cachedDriver;
    private Bitmap cachedFace;
    private String lastTripProfileId;
    private boolean finished=false;

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private Backend backend(){return (Backend)field("backend");}
    private LinearLayout body(){return (LinearLayout)field("body");}

    @Override void screenTracking(){
        finished=false;
        super.screenTracking();
        installDriverCard();
        ui.removeCallbacks(profileLoop);
        ui.postDelayed(profileLoop,350);
    }

    @Override void renderTrip(JSONObject t){
        super.renderTrip(t);
        String status=t.optString("status","solicitado");
        if("completado".equals(status)||"cancelado".equals(status)){
            finished=true;ui.removeCallbacks(profileLoop);return;
        }
        String driver=t.optString("driver_id","");
        if(driver!=null&&!driver.isEmpty()&&!"null".equals(driver)){
            installDriverCard();
            ui.removeCallbacks(profileLoop);
            ui.postDelayed(profileLoop,80);
        }
    }

    private final Runnable profileLoop=new Runnable(){public void run(){
        if(finished||isFinishing())return;
        loadDriverProfile();
        ui.postDelayed(this,cachedDriver==null?1200:4000);
    }};

    private void installDriverCard(){
        LinearLayout b=body();if(b==null)return;
        if(driverCard!=null&&driverCard.getParent()==b)return;
        driverCard=card();
        int pos=Math.min(1,b.getChildCount());
        b.addView(driverCard,pos);
        if(cachedDriver!=null)renderDriver(cachedDriver);else{
            driverCard.addView(tx("Conductor asignado",19,Color.WHITE,true));
            driverCard.addView(tx("Esperando que un conductor acepte tu solicitud…",13,Color.rgb(176,180,190),false));
        }
    }

    private void loadDriverProfile(){
        String tripId=field("tripId") instanceof String?(String)field("tripId"):null;
        if(tripId==null||tripId.isEmpty()||backend()==null)return;
        if(lastTripProfileId==null||!lastTripProfileId.equals(tripId)){
            cachedDriver=null;cachedFace=null;lastTripProfileId=tripId;
        }
        try{backend().rpc("cm_trip_driver_profile",new JSONObject().put("p_trip_id",tripId),new Backend.Callback(){
            public void ok(Object x){JSONObject d=Backend.firstObject(x);if(d==null)return;cachedDriver=d;installDriverCard();renderDriver(d);}
            public void error(String m){}
        });}catch(Exception ignored){}
    }

    private void renderDriver(JSONObject d){
        installDriverCard();if(driverCard==null)return;
        driverCard.removeAllViews();
        driverCard.addView(tx("Conductor asignado",19,Color.WHITE,true));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView face=photoBox(82);if(cachedFace!=null)face.setImageBitmap(cachedFace);top.addView(face,new LinearLayout.LayoutParams(dp(82),dp(82)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(10),0,0,0);
        info.addView(tx(d.optString("full_name","Conductor"),17,Color.WHITE,true));
        info.addView(tx("Placa: "+d.optString("plate","—"),14,Color.rgb(255,220,90),true));
        info.addView(tx("★ "+d.optString("rating","—")+" · "+d.optString("brand","Mototaxi")+" "+d.optString("model",""),12,Color.rgb(176,180,190),false));
        top.addView(info,new LinearLayout.LayoutParams(0,-2,1));driverCard.addView(top);
        if(cachedFace==null)loadFace(d.optString("face_photo_path"),face);
    }

    private ImageView photoBox(int h){ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);im.setImageResource(R.drawable.logo_civimoto);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(25,25,25));bg.setStroke(dp(2),Color.rgb(255,190,0));bg.setCornerRadius(dp(18));im.setBackground(bg);im.setClipToOutline(true);im.setPadding(dp(2),dp(2),dp(2),dp(2));return im;}

    private void loadFace(String path,ImageView target){
        if(path==null||path.trim().isEmpty())return;
        new ProfileImageLoader(backend()).download(path,new Backend.Callback(){public void ok(Object x){try{byte[] data=(byte[])x;Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);if(bm!=null){cachedFace=bm;target.setImageBitmap(bm);}}catch(Exception ignored){}}public void error(String m){}});
    }

    @Override protected void onDestroy(){ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
