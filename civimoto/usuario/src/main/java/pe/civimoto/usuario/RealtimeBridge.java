package pe.civimoto.usuario;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;

public class RealtimeBridge {
    public interface Listener { void onRealtime(String table,String payload); void onRealtimeStatus(String status); }
    private final Activity activity; private final Backend backend; private final Listener listener; private WebView webView;
    public RealtimeBridge(Activity a,Backend b,Listener l){activity=a;backend=b;listener=l;}
    public WebView start(String... tables){stop();webView=new WebView(activity);WebSettings s=webView.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);webView.setWebViewClient(new WebViewClient());webView.addJavascriptInterface(new Bridge(),"Android");String u=JSONObject.quote(backend.getSupabaseUrl()),k=JSONObject.quote(backend.getPublishableKey()),t=JSONObject.quote(backend.getAccessToken());StringBuilder a=new StringBuilder("[");for(int i=0;i<tables.length;i++){if(i>0)a.append(',');a.append(JSONObject.quote(tables[i]));}a.append(']');String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><script src='https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2/dist/umd/supabase.js'></script></head><body><script>const U="+u+",K="+k+",T="+t+",TABLES="+a+";function boot(){try{const c=supabase.createClient(U,K,{auth:{persistSession:false,autoRefreshToken:false}});c.realtime.setAuth(T);let ch=c.channel('civimoto-isolated-'+Math.random());TABLES.forEach(alias=>{const actual=alias.startsWith('cm_')?alias:'cm_'+alias;ch=ch.on('postgres_changes',{event:'*',schema:'public',table:actual},p=>Android.onRealtime(alias,JSON.stringify(p)));});ch.subscribe(st=>Android.onStatus(st));}catch(e){Android.onStatus('ERROR:'+e.message);}}if(document.readyState==='complete')boot();else window.addEventListener('load',boot);</script></body></html>";webView.loadDataWithBaseURL("https://app.civimoto.local/",html,"text/html","UTF-8",null);return webView;}
    public void stop(){if(webView!=null){try{webView.destroy();}catch(Exception ignored){}webView=null;}}
    private class Bridge{@JavascriptInterface public void onRealtime(String table,String payload){activity.runOnUiThread(()->listener.onRealtime(table,payload));}@JavascriptInterface public void onStatus(String status){activity.runOnUiThread(()->listener.onRealtimeStatus(status));}}
}
