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

/** Mantiene visible la foto y datos del pasajero desde la solicitud hasta finalizar el viaje. */
public class DriverProfileActivity extends ReliableDriverActivity {
    private final Handler ui=new Handler(Looper.getMainLooper());
    private LinearLayout passengerCard;
    private JSONObject cachedPassenger;
    private Bitmap cachedFace;
    private String profileTripId;
    private boolean finished=false;

    private Object field(String name){try{Field f=FlowActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private Backend backend(){return (Backend)field("backend");}
    private LinearLayout body(){return (LinearLayout)field("body");}

    @Override void screenOffer(){
        finished=false;super.screenOffer();installPassengerCard();ui.removeCallbacks(profileLoop);ui.postDelayed(profileLoop,250);
    }

    @Override void screenTrip(){
        finished=false;super.screenTrip();installPassengerCard();ui.removeCallbacks(profileLoop);ui.postDelayed(profileLoop,180);
    }

    @Override void loadTrip(){
        super.loadTrip();
        if(!finished){installPassengerCard();ui.removeCallbacks(profileLoop);ui.postDelayed(profileLoop,150);}
    }

    private final Runnable profileLoop=new Runnable(){public void run(){
        if(finished||isFinishing())return;
        loadPassengerProfile();
        ui.postDelayed(this,cachedPassenger==null?1000:4000);
    }};

    private String currentTripId(){
        Object active=field("activeTripId");if(active instanceof String&&!((String)active).isEmpty())return(String)active;
        String pending=getSharedPreferences("civimoto_driver_runtime",MODE_PRIVATE).getString("pending_offer_id",null);if(pending!=null&&!pending.isEmpty())return pending;
        Object offered=field("offeredTripId");return offered instanceof String?(String)offered:null;
    }

    private void installPassengerCard(){
        LinearLayout b=body();if(b==null)return;
        if(passengerCard!=null&&passengerCard.getParent()==b)return;
        passengerCard=card();
        int pos=Math.min(1,b.getChildCount());
        b.addView(passengerCard,pos);
        if(cachedPassenger!=null)renderPassenger(cachedPassenger);else{
            passengerCard.addView(tx("Pasajero",19,Color.WHITE,true));
            passengerCard.addView(tx("Cargando perfil del pasajero…",13,Color.rgb(176,180,190),false));
        }
    }

    private void loadPassengerProfile(){
        String tripId=currentTripId();if(tripId==null||tripId.isEmpty()||backend()==null)return;
        if(profileTripId==null||!profileTripId.equals(tripId)){profileTripId=tripId;cachedPassenger=null;cachedFace=null;}
        try{backend().rpc("cm_trip_passenger_profile",new JSONObject().put("p_trip_id",tripId),new Backend.Callback(){
            public void ok(Object x){JSONObject p=Backend.firstObject(x);if(p!=null){cachedPassenger=p;installPassengerCard();renderPassenger(p);}}
            public void error(String m){}
        });}catch(Exception ignored){}
    }

    private void renderPassenger(JSONObject p){
        installPassengerCard();if(passengerCard==null)return;
        passengerCard.removeAllViews();
        passengerCard.addView(tx("Pasajero",19,Color.WHITE,true));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView face=photoBox();if(cachedFace!=null)face.setImageBitmap(cachedFace);top.addView(face,new LinearLayout.LayoutParams(dp(82),dp(82)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(10),0,0,0);
        info.addView(tx(p.optString("full_name","Pasajero"),17,Color.WHITE,true));
        info.addView(tx("★ "+p.optString("rating","—"),13,Color.rgb(255,220,90),true));
        info.addView(tx("Tel: "+p.optString("phone","—"),12,Color.rgb(176,180,190),false));
        String last4=p.optString("document_last4","");if(!last4.isEmpty())info.addView(tx("DNI: •••• "+last4,12,Color.rgb(176,180,190),false));
        top.addView(info,new LinearLayout.LayoutParams(0,-2,1));passengerCard.addView(top);
        if(cachedFace==null)loadFace(p.optString("face_photo_path"),face);
    }

    private ImageView photoBox(){ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);im.setImageResource(R.drawable.logo_civimoto);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(25,25,25));bg.setStroke(dp(2),Color.rgb(255,190,0));bg.setCornerRadius(dp(18));im.setBackground(bg);im.setClipToOutline(true);im.setPadding(dp(2),dp(2),dp(2),dp(2));return im;}

    private void loadFace(String path,ImageView target){if(path==null||path.trim().isEmpty())return;new ProfileImageLoader(backend()).download(path,new Backend.Callback(){public void ok(Object x){try{byte[] data=(byte[])x;Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);if(bm!=null){cachedFace=bm;target.setImageBitmap(bm);}}catch(Exception ignored){}}public void error(String m){}});}

    @Override void screenEarnings(){finished=true;ui.removeCallbacks(profileLoop);super.screenEarnings();}
    @Override void screenDashboard(){if(field("activeTripId")==null){finished=true;ui.removeCallbacks(profileLoop);}super.screenDashboard();}
    @Override protected void onDestroy(){ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
