package pe.civimoto.usuario;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Sube fotos privadas del pasajero al bucket exclusivo CiviMoto. */
public class PassengerStorage {
    private final Backend backend;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    public PassengerStorage(Backend backend){this.backend=backend;}

    public void uploadJpeg(String path,byte[] data,Backend.Callback cb){
        io.execute(()->{
            try{
                if(data==null||data.length==0)throw new Exception("La foto está vacía.");
                String base=backend.getSupabaseUrl(),key=backend.getPublishableKey(),token=backend.getAccessToken();
                if(base==null||key==null||token==null)throw new Exception("Inicia sesión para subir documentos.");
                HttpURLConnection c=(HttpURLConnection)new URL(base+"/storage/v1/object/civimoto-private/"+path).openConnection();
                c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setRequestMethod("POST");c.setDoOutput(true);
                c.setRequestProperty("apikey",key);c.setRequestProperty("Authorization","Bearer "+token);
                c.setRequestProperty("Content-Type","image/jpeg");c.setRequestProperty("x-upsert","true");
                c.setFixedLengthStreamingMode(data.length);
                try(OutputStream os=c.getOutputStream()){os.write(data);}
                int code=c.getResponseCode();String msg=read(c,code>=200&&code<300);
                if(code<200||code>=300)throw new Exception(msg==null||msg.isEmpty()?"No se pudo subir la foto.":msg);
                main.post(()->cb.ok(path));
            }catch(Exception e){main.post(()->cb.error(e.getMessage()==null?"No se pudo subir la foto.":e.getMessage()));}
        });
    }

    private String read(HttpURLConnection c,boolean ok)throws Exception{
        InputStream in=ok?c.getInputStream():c.getErrorStream();if(in==null)return"";
        BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();String line;
        while((line=br.readLine())!=null)s.append(line);br.close();return s.toString();
    }
}
