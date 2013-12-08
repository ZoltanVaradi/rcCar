package hu.uniobuda.nik.hc4dgv.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import hu.uniobuda.nik.hc4dgv.listener.InformationListener;
import hu.varadi.zoltan.rccar.R;
import hu.uniobuda.nik.hc4dgv.listener.ConnectClientListener;

/**
 * Created by Zoltan Varadi on 2013.12.06.
 */
public class ServerThread extends Thread {

    private int serverPort;
    private ServerSocket serverSocket;
    private InformationListener informationListener;
    private ConnectClientListener connectClientListener;

    public ServerThread(int serverPort) {
        this.serverPort = serverPort;
    }

    //csak egy bejovo kapcsolatig var vagyis max egy kliens lehet
    public void run() {
        Socket socket;
        try {
            serverSocket = new ServerSocket(serverPort);
            if (informationListener != null) {

                informationListener.information(R.string.serverStarted, InformationListener.SERVER_SOCKET_START);
            }

            socket = serverSocket.accept();

            if (connectClientListener != null) {
                connectClientListener.connect(socket);
            }

            //Az első bejövő kapcsolat után több nem lehet
            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
            if (informationListener != null) {
                informationListener.information(e.getLocalizedMessage(), InformationListener.ERROR);
            }
        }
    }

    public void setInformationListener(InformationListener i) {
        this.informationListener = i;
    }

    public void removeInformationListener() {
        this.informationListener = null;
    }


    public void setOnConnectClientListener(ConnectClientListener connectClientListener) {
        this.connectClientListener = connectClientListener;
    }
}
