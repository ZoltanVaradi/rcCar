package hu.zoltan.varadi.rccar.util;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

/**
 * Created by Zoltan Varadi on 2013.12.06..
 */
public class WifiUtil {

    public static String ipAddressToString(int ipAddress) {

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFI_IP", "Unable to get host address.");
            ipAddressString = "NaN";
        }

        return ipAddressString;
    }

    public static WifiConfiguration getWifiApConfiguration(WifiManager w) {
        try {
            Method method = w.getClass().getMethod("getWifiApConfiguration");
            return (WifiConfiguration) method.invoke(w);
        } catch (Exception e) {
            Log.e(WifiUtil.class.toString(), "", e);
            return null;
        }
    }


    public static String getGatewayIp(WifiManager w) {
        return ipAddressToString(w.getDhcpInfo().gateway);
    }

    public static String getMyIpAddress(WifiManager w) {
        return ipAddressToString(w.getConnectionInfo().getIpAddress());
    }
    public static String getSSID(WifiManager w) {
        return w.getConnectionInfo().getSSID();
    }
}
