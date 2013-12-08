package hu.zoltan.varadi.rccar.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import hu.zoltan.varadi.rccar.listener.InformationListener;

/**
 * Created by Zoltan Varadi on 2013.12.07..
 */
public class WifiReceiver extends BroadcastReceiver {

    private InformationListener info;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (info != null) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            int type= 0;
            int type2=0;
            if (netInfo != null) {
                 type = netInfo.getType();
                 type2 = ConnectivityManager.TYPE_WIFI;
            }
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                info.information("wifi on", InformationListener.WIFI_CHANGE_ON);
                Log.d("WifiReceiver", "Have Wifi Connection");
            } else {
                Log.d("WifiReceiver", "Don't have Wifi Connection");
                info.information("wifi off", InformationListener.WIFI_CHANGE_OFF);
            }
        }
    }

    public void setInformationListener(InformationListener info) {
        this.info = info;
    }
}
