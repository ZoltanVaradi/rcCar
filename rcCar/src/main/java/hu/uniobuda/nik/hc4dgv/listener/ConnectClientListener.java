package hu.uniobuda.nik.hc4dgv.listener;

import java.net.Socket;

/**
 * Created by Zoltan Varadi on 2013.12.07..
 */
public interface ConnectClientListener {
    public void connect(Socket clientSocket);
}
