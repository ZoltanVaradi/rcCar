package hu.zoltan.varadi.lib.ftdi.modem;

/**
 * Created by zoltan on 2014.06.01..
 */
public interface FtdiInformationListener {
    public static int INFO = 0;

    public static int ERROR = 1;
    public static int OUTPUT_ERROR = 2;
    public static int INPUT_ERROR = 3;

    public static int NEW_DATA = 4;

    public static int DEVICE_CONNECTED = 10;
    public static int DEVICE_DISCONNECTED = 11;


    public void sendMessage(String message, int type);
}
