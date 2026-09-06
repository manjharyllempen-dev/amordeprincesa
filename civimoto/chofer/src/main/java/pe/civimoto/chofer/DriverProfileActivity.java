package pe.civimoto.chofer;

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

/** Muestra la foto y datos esenciales del pasajero dentro de cada solicitud. */
public class DriverProfileActivity extends ReliableDriverActivity {
    private final Handler ui=new Handler(Looper.getMainLooper());
    private LinearLayout passengerCard;

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private Backend backend(){return (Backend)field("backend");}
    private LinearLayout body(){return (LinearLayout)field("body");}

    @Override void screenOffer(){
        super.screenOffer();
        ui.postDelayed(this::loadPassengerProfile,500);
    }

    private void loadPassengerProfile(){
        String tripId=getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE).getString("pending_offer_id",null);
        if((tripId==null||tripId.isEmpty())&&field("offeredTripId") instanceof String)tripId=(String)field("offeredTripId");
        if(tripId==null||tripId.isEmpty()||backend()==null)return;
        final String id=tripId;
        try{backend().rpc("cm_trip_passenger_profile",new JSONObject().put("p_trip_id",id),new Backend.Callback(){
            public void ok(Object x){JSONObject p=Backend.firstObject(x);if(p!=null)renderPassenger(p);}
            public void error(String m){}
        });}catch(Exception ignored){}
    }

    private void renderPassenger(JSONObject p){
        LinearLayout b=body();if(b==null)return;
        if(passengerCard!=null&&passengerCard.getParent()==b)b.removeView(passengerCard);
        passengerCard=card();passengerCard.addView(tx("Pasajero",19,Color.WHITE,true));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView face=photoBox();top.addView(face,new LinearLayout.LayoutParams(dp(96),dp(96)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);
        info.addView(tx(p.optString("full_name","Pasajero"),18,Color.WHITE,true));
        info.addView(tx("★ "+p.optString("rating","—"),13,Color.rgb(255,220,90),true));
        info.addView(tx("Teléfono: "+p.optString("phone","—"),13,Color.rgb(176,180,190),false));
        String last4=p.optString("document_last4","");if(!last4.isEmpty())info.addView(tx("DNI verificado: •••• "+last4,12,Color.rgb(176,180,190),false));
        top.addView(info,new LinearLayout.LayoutParams(0,-2,1));passengerCard.addView(top);
        passengerCard.addView(tx("La foto corresponde al perfil registrado del pasajero para esta solicitud.",12,Color.rgb(176,180,190),false));
        int pos=Math.min(1,b.getChildCount());b.addView(passengerCard,pos);
        loadPhoto(p.optString("face_photo_path"),face);
    }

    private ImageView photoBox(){ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);im.setImageResource(R.drawable.logo_civimoto);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(25,25,25));bg.setStroke(dp(2),Color.rgb(255,190,0));bg.setCornerRadius(dp(18));im.setBackground(bg);im.setClipToOutline(true);im.setPadding(dp(2),dp(2),dp(2),dp(2));return im;}
    private void loadPhoto(String path,ImageView target){if(path==null||path.trim().isEmpty())return;new ProfileImageLoader(backend()).download(path,new Backend.Callback(){public void ok(Object x){try{byte[] data=(byte[])x;Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);if(bm!=null)target.setImageBitmap(bm);}catch(Exception ignored){}}public void error(String m){}});}
    @Override protected void onDestroy(){ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
