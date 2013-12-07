package hu.varadi.zoltan.rccar.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import hu.varadi.zoltan.rccar.listener.ConnectClientListener;
import hu.varadi.zoltan.rccar.listener.InformationListener;

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

    public void run() {
        Socket socket;
        try {
            serverSocket = new ServerSocket(serverPort);
            if (informationListener != null) {
                informationListener.information("A szerver elindult", InformationListener.SERVER_SOCKET_START);
            }

            try {
                socket = serverSocket.accept();

                if (informationListener != null) {
                    informationListener.information(socket.getRemoteSocketAddress().toString(), InformationListener.INFO);
                }
                if (connectClientListener != null) {
                    connectClientListener.connect(socket);
                }
                //commRunnable= new CommunicationThread(socket,usbWrite);
                //commThread = new Thread(new CommunicationThread(socket,usbWrite));
                //commThread.start();

                //Az első bejövő kapcsolat után több nem lehet
                serverSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

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
