package pe.civimoto.chofer;

import android.os.Handler;
import android.os.Looper;
import okhttp3.*;
import okio.ByteString;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

/** WebSocket Realtime nativo. Mantiene la escucha fuera de la Activity y despierta el poll inmediato. */
public class RealtimeOfferSocket {
    public interface Listener { void onTripSignal(); void onState(String state); }
    private final Backend backend;
    private final Listener listener;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final OkHttpClient client=new OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).retryOnConnectionFailure(true).build();
    private WebSocket ws; private boolean stopped=false; private int ref=1;

    public RealtimeOfferSocket(Backend backend,Listener listener){this.backend=backend;this.listener=listener;}

    public synchronized void start(){
        stopped=false;
        if(ws!=null)return;
        String base=backend.getSupabaseUrl(),key=backend.getPublishableKey(),token=backend.getAccessToken();
        if(base==null||key==null||token==null){scheduleReconnect();return;}
        String url=base.replace("https://","wss://").replace("http://","ws://")+"/realtime/v1/websocket?apikey="+key+"&vsn=1.0.0";
        Request req=new Request.Builder().url(url).header("Authorization","Bearer "+token).build();
        ws=client.newWebSocket(req,new WebSocketListener(){
            @Override public void onOpen(WebSocket webSocket,Response response){listener.onState("CONNECTED");join(webSocket,token);}
            @Override public void onMessage(WebSocket webSocket,String text){
                if(text.contains("postgres_changes")||text.contains("cm_trips")||text.contains("phx_reply"))main.post(listener::onTripSignal);
            }
            @Override public void onMessage(WebSocket webSocket,ByteString bytes){main.post(listener::onTripSignal);}
            @Override public void onFailure(WebSocket webSocket,Throwable t,Response response){synchronized(RealtimeOfferSocket.this){ws=null;}listener.onState("ERROR");scheduleReconnect();}
            @Override public void onClosed(WebSocket webSocket,int code,String reason){synchronized(RealtimeOfferSocket.this){ws=null;}listener.onState("CLOSED");scheduleReconnect();}
        });
    }

    private void join(WebSocket s,String token){
        try{
            JSONObject broadcast=new JSONObject().put("ack",false).put("self",false);
            JSONObject presence=new JSONObject().put("key","");
            JSONArray changes=new JSONArray().put(new JSONObject().put("event","*").put("schema","public").put("table","cm_trips"));
            JSONObject config=new JSONObject().put("broadcast",broadcast).put("presence",presence).put("postgres_changes",changes);
            JSONObject payload=new JSONObject().put("config",config).put("access_token",token);
            JSONObject msg=new JSONObject().put("topic","realtime:civimoto-driver-background").put("event","phx_join").put("payload",payload).put("ref",String.valueOf(ref++));
            s.send(msg.toString());
        }catch(Exception ignored){}
    }

    private void scheduleReconnect(){if(stopped)return;main.removeCallbacks(reconnect);main.postDelayed(reconnect,3500);}
    private final Runnable reconnect=()->{if(!stopped)start();};

    public synchronized void stop(){stopped=true;main.removeCallbacksAndMessages(null);if(ws!=null){try{ws.close(1000,"stop");}catch(Exception ignored){}ws=null;}}
}
