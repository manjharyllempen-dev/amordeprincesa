package pe.civimoto.administrador;

import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Descarga documentos privados del conductor usando la sesión del administrador. */
public class AdminStorage {
    private final Backend backend; private final ExecutorService io=Executors.newSingleThreadExecutor(); private final Handler main=new Handler(Looper.getMainLooper());
    public AdminStorage(Backend b){backend=b;}
    public void download(String path,Backend.Callback cb){io.execute(()->{try{
        if(path==null||path.trim().isEmpty())throw new Exception("Documento no cargado.");
        HttpURLConnection c=(HttpURLConnection)new URL(backend.getSupabaseUrl()+"/storage/v1/object/authenticated/civimoto-private/"+path).openConnection();
        c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setRequestMethod("GET");c.setRequestProperty("apikey",backend.getPublishableKey());c.setRequestProperty("Authorization","Bearer "+backend.getAccessToken());
        int code=c.getResponseCode();if(code<200||code>=300)throw new Exception("No se pudo abrir el documento ("+code+").");
        try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[]buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);byte[]data=out.toByteArray();main.post(()->cb.ok(data));}
    }catch(Exception e){main.post(()->cb.error(e.getMessage()==null?"No se pudo abrir el documento.":e.getMessage()));}});}
}
