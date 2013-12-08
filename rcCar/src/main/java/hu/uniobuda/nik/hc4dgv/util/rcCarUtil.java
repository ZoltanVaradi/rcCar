package hu.uniobuda.nik.hc4dgv.util;


import android.content.SharedPreferences;

/**
 * Created by Zoltan Varadi on 2013.12.06..
 */
public class rcCarUtil {
    public static final byte COMMAND_KORMANY = 0x3;
    public static final byte COMMAND_GAZ = 0x2;
    public static final byte BASE_GAS_VALUE = 25;
    public static final String COMMAND_SENSOR_LIGHT = "light";
    public static final String COMMAND_SENSOR_ACC = "acc";
    public static final String COMMAND_SENSOR_GPS = "gps";
    public static final String COMMAND_SENSOR_AKKU_AUTO = "akkuauto";
    public static final String COMMAND_SENSOR_AKKU_ANDROID = "akkuandroid";
    public static final String PrefFileName = "rcCar";


    //a lokalis taroloba mentes es vissazallitas view specifikusan

    public static void saveIpAddress(SharedPreferences settings, String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("clientipaddress", value);
        editor.commit();
    }

    public static String getIpAddress(SharedPreferences settings) {
        return settings.getString("clientipaddress", "192.168.1.100");
    }


    public static void saveUseGateway(SharedPreferences settings, boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("clientusegateway", value);
        editor.commit();
    }

    public static boolean getUseGateway(SharedPreferences settings) {
        return settings.getBoolean("clientusegateway", false);
    }

    public static void saveMaxSpeedSeekbar(SharedPreferences settings, int value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("clientmaxseekbar", value);
        editor.commit();
    }

    public static int getMaxSpeedSeekbar(SharedPreferences settings) {
        return settings.getInt("clientmaxseekbar", 8);
    }
}
