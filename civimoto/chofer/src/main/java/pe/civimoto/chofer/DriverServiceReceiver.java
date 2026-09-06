package pe.civimoto.chofer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Reinicia el servicio si el conductor dejó guardado el estado EN LÍNEA. */
public class DriverServiceReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        boolean online=context.getSharedPreferences("civimoto_driver_runtime",Context.MODE_PRIVATE).getBoolean("online",false);
        if(!online)return;
        try{Intent s=new Intent(context,DriverAlertService.class);if(Build.VERSION.SDK_INT>=26)context.startForegroundService(s);else context.startService(s);}catch(Exception ignored){}
    }
}
