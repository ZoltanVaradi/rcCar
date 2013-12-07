package hu.varadi.zoltan.rccar.listener;

/**
 * Created by Zoltan Varadi on 2013.12.06..
 */
public interface InformationListener {
    public static int INFO = 0;
    public static int ERROR = 1;
    public static int OUTPUT_ERROR = 2;
    public static int INPUT_ERROR = 3;
    public static int SERVER_SOCKET_START = 4;

    // you can define any parameter as per your requirement
    public void information(String info, int type);
}

