package pe.civimoto.usuario;

import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Descarga únicamente imágenes de perfil autorizadas por las políticas CiviMoto. */
public class ProfileImageLoader {
    private final Backend backend;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    public ProfileImageLoader(Backend b){backend=b;}
    public void download(String path,Backend.Callback cb){io.execute(()->{try{
        if(path==null||path.trim().isEmpty())throw new Exception("Foto de perfil no disponible.");
        HttpURLConnection c=(HttpURLConnection)new URL(backend.getSupabaseUrl()+"/storage/v1/object/authenticated/civimoto-private/"+path).openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setRequestMethod("GET");
        c.setRequestProperty("apikey",backend.getPublishableKey());c.setRequestProperty("Authorization","Bearer "+backend.getAccessToken());
        int code=c.getResponseCode();if(code<200||code>=300)throw new Exception("Foto no disponible ("+code+").");
        try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);byte[] data=out.toByteArray();main.post(()->cb.ok(data));}
    }catch(Exception e){main.post(()->cb.error(e.getMessage()==null?"No se pudo abrir la foto.":e.getMessage()));}});}
}
