package pe.civimoto.chofer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Backend {
    public interface Callback { void ok(Object value); void error(String message); }
    private static final String CONFIG_URL="https://civimoto-backend-not-created.invalid/functions/v1/client-config";
    private final SharedPreferences prefs; private final ExecutorService io=Executors.newSingleThreadExecutor(); private final Handler main=new Handler(Looper.getMainLooper());
    private String supabaseUrl,publishableKey,accessToken,refreshToken,userId,role; private long expiresAtMs;
    public Backend(Context c){prefs=c.getSharedPreferences("civimoto_chofer_session_v1",Context.MODE_PRIVATE);supabaseUrl=prefs.getString("url",null);publishableKey=prefs.getString("key",null);accessToken=prefs.getString("access",null);refreshToken=prefs.getString("refresh",null);userId=prefs.getString("uid",null);role=prefs.getString("role",null);expiresAtMs=prefs.getLong("expires",0L);}
    public boolean hasSession(){return accessToken!=null&&refreshToken!=null&&userId!=null;} public String getUserId(){return userId;} public String getRole(){return role;} public String getAccessToken(){return accessToken;} public String getSupabaseUrl(){return supabaseUrl;} public String getPublishableKey(){return publishableKey;}
    public void bootstrap(Callback cb){io.execute(()->{try{ensureConfigSync();postOk(cb,new JSONObject().put("connected",true));}catch(Exception e){postError(cb,friendly(e));}});}
    public void login(String email,String password,String expectedRole,Callback cb){io.execute(()->{try{ensureConfigSync();JSONObject body=new JSONObject().put("email",email.trim().toLowerCase()).put("password",password);JSONObject session=asObject(requestSync("POST",supabaseUrl+"/auth/v1/token?grant_type=password",body,false));storeSession(session);JSONObject profile=getProfileSync();String actual=profile.optString("role","");if(!expectedRole.equals(actual)){clearSession();throw new Exception("Esta cuenta pertenece al rol "+actual+". Abre la app correcta.");}role=actual;prefs.edit().putString("role",role).apply();postOk(cb,profile);}catch(Exception e){postError(cb,friendly(e));}});}
    public void register(String fullName,String email,String password,String requestedRole,Callback cb){io.execute(()->{try{ensureConfigSync();JSONObject body=new JSONObject().put("full_name",fullName.trim()).put("email",email.trim().toLowerCase()).put("password",password).put("role",requestedRole);postOk(cb,requestSync("POST",supabaseUrl+"/functions/v1/register-account",body,false));}catch(Exception e){postError(cb,friendly(e));}});}
    public void resume(String expectedRole,Callback cb){io.execute(()->{try{ensureConfigSync();ensureFreshTokenSync();JSONObject p=getProfileSync();String actual=p.optString("role","");if(!expectedRole.equals(actual))throw new Exception("Sesión de otro rol.");role=actual;prefs.edit().putString("role",role).apply();postOk(cb,p);}catch(Exception e){clearSession();postError(cb,friendly(e));}});}
    public void rest(String method,String path,JSONObject body,Callback cb){io.execute(()->{try{ensureConfigSync();ensureFreshTokenSync();postOk(cb,requestSync(method,supabaseUrl+"/rest/v1/"+path,body,true));}catch(Exception e){postError(cb,friendly(e));}});}
    public void rpc(String fn,JSONObject body,Callback cb){io.execute(()->{try{ensureConfigSync();ensureFreshTokenSync();postOk(cb,requestSync("POST",supabaseUrl+"/rest/v1/rpc/"+fn,body==null?new JSONObject():body,true));}catch(Exception e){postError(cb,friendly(e));}});}
    public void logout(){clearSession();}
    private void ensureConfigSync()throws Exception{if(supabaseUrl!=null&&publishableKey!=null)return;HttpURLConnection c=open("GET",CONFIG_URL,false);int code=c.getResponseCode();String txt=read(c,code>=200&&code<300);if(code<200||code>=300)throw new Exception(extractMessage(txt));JSONObject o=new JSONObject(txt);supabaseUrl=o.getString("supabase_url");publishableKey=o.getString("publishable_key");prefs.edit().putString("url",supabaseUrl).putString("key",publishableKey).apply();}
    private void ensureFreshTokenSync()throws Exception{if(accessToken==null||refreshToken==null)throw new Exception("Inicia sesión.");if(expiresAtMs==0L||System.currentTimeMillis()<expiresAtMs-60000L)return;JSONObject body=new JSONObject().put("refresh_token",refreshToken);storeSession(asObject(requestSync("POST",supabaseUrl+"/auth/v1/token?grant_type=refresh_token",body,false)));}
    private JSONObject getProfileSync()throws Exception{Object raw=requestSync("GET",supabaseUrl+"/rest/v1/profiles?id=eq."+userId+"&select=id,role,full_name,phone,email,account_status,rating",null,true);JSONArray a=raw instanceof JSONArray?(JSONArray)raw:new JSONArray();if(a.length()==0)throw new Exception("No se encontró el perfil CiviMoto.");return a.getJSONObject(0);}
    private void storeSession(JSONObject s)throws Exception{accessToken=s.getString("access_token");refreshToken=s.getString("refresh_token");userId=s.getJSONObject("user").getString("id");expiresAtMs=System.currentTimeMillis()+s.optLong("expires_in",3600L)*1000L;prefs.edit().putString("access",accessToken).putString("refresh",refreshToken).putString("uid",userId).putLong("expires",expiresAtMs).apply();}
    private Object requestSync(String method,String absoluteUrl,JSONObject body,boolean auth)throws Exception{HttpURLConnection c=open(method,absoluteUrl,auth);if(body!=null&&!"GET".equals(method)){byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setDoOutput(true);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream os=c.getOutputStream()){os.write(bytes);}}int code=c.getResponseCode();String txt=read(c,code>=200&&code<300);if(code<200||code>=300){if(code==401&&auth&&refreshToken!=null){expiresAtMs=1L;ensureFreshTokenSync();return requestSync(method,absoluteUrl,body,true);}throw new Exception(extractMessage(txt));}if(txt==null||txt.trim().isEmpty())return new JSONObject().put("ok",true);String s=txt.trim();if(s.startsWith("["))return new JSONArray(s);if(s.startsWith("{"))return new JSONObject(s);return s;}
    private HttpURLConnection open(String method,String absoluteUrl,boolean auth)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(absoluteUrl).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestMethod(method);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Accept","application/json");if(publishableKey!=null)c.setRequestProperty("apikey",publishableKey);if(auth&&accessToken!=null)c.setRequestProperty("Authorization","Bearer "+accessToken);if(!"GET".equals(method))c.setRequestProperty("Prefer","return=representation");return c;}
    private String read(HttpURLConnection c,boolean success)throws Exception{InputStream in=success?c.getInputStream():c.getErrorStream();if(in==null)return"";BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();return sb.toString();}
    private static JSONObject asObject(Object v)throws Exception{if(v instanceof JSONObject)return(JSONObject)v;if(v instanceof JSONArray&&((JSONArray)v).length()>0)return((JSONArray)v).getJSONObject(0);throw new Exception("Respuesta inesperada del servidor.");}
    public static JSONObject firstObject(Object v){try{if(v instanceof JSONObject)return(JSONObject)v;if(v instanceof JSONArray&&((JSONArray)v).length()>0)return((JSONArray)v).getJSONObject(0);}catch(Exception ignored){}return null;}
    private String extractMessage(String txt){try{JSONObject o=new JSONObject(txt);String m=o.optString("message","");if(m.isEmpty())m=o.optString("msg","");if(m.isEmpty())m=o.optString("error_description","");if(m.isEmpty())m=o.optString("error","");if(!m.isEmpty())return m;}catch(Exception ignored){}return(txt==null||txt.trim().isEmpty())?"Backend CiviMoto aún no está activado.":txt;}
    private String friendly(Exception e){String m=e.getMessage();if(m==null||m.trim().isEmpty())return"No se pudo conectar con el backend exclusivo de CiviMoto.";if(m.contains("Invalid login credentials"))return"Correo o contraseña incorrectos.";if(m.contains("Email not confirmed"))return"Confirma tu correo antes de ingresar.";return m;}
    private void clearSession(){accessToken=null;refreshToken=null;userId=null;role=null;expiresAtMs=0L;prefs.edit().clear().apply();}
    private void postOk(Callback cb,Object value){main.post(()->cb.ok(value));}private void postError(Callback cb,String message){main.post(()->cb.error(message));}
}
