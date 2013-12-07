package hu.varadi.zoltan.rccar.client;

import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import hu.varadi.zoltan.rccar.listener.InformationListener;

/**
 * Created by Zoltan Varadi on 2013.12.06.
 */
class ClientCommunicationThread extends Thread {

    private static final String LOG_TAG= "ClientCommunicationThread.java";

    private Socket socket;
    private PrintWriter out;
    private String serverIP;
    private int serverPort;
    private InformationListener informationListener;

    ClientCommunicationThread(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try {

            InetAddress serverAddr = InetAddress.getByName(serverIP);
            socket = new Socket(serverAddr, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);

            if (informationListener != null) {
                informationListener.information("Kapcsi papcsi talán él", InformationListener.INFO);
            }
        } catch (UnknownHostException e1) {
            if (informationListener != null) {
                informationListener.information(e1.getMessage(),InformationListener.ERROR);
            }
            e1.printStackTrace();
        } catch (IOException e1) {
            if (informationListener != null) {
                informationListener.information(e1.getMessage(),InformationListener.ERROR);
            }
        }

    }

    public void sendDataToServer(String data) {
        Log.e(LOG_TAG, "-----------------------------------------");

        if (socket == null) {
            Log.e(LOG_TAG, "Socket is null");
        } else {
            Log.e(LOG_TAG, socket.isConnected() + "");
            try {

                out.println(data);
                out.flush();
                Log.e(LOG_TAG, out.checkError() + "");

                if (out.checkError()) {
                    socket.close();
                    socket = null;
                    //clientThread = null;
                    //bntConnect.setEnabled(true);
                    if (informationListener != null) {
                        informationListener.information("output.checkerror is true",InformationListener.OUTPUT_ERROR);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "sendDataToServer Exception " + e.getClass().toString());
            }
        }
    }

    public void setInformationListener(InformationListener i) {
        this.informationListener = i;
    }

    public void removeInformationListener() {
        this.informationListener = null;
    }
}
