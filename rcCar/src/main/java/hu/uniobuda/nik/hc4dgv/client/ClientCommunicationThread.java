package hu.uniobuda.nik.hc4dgv.client;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import hu.uniobuda.nik.hc4dgv.listener.InformationListener;
import hu.uniobuda.nik.hc4dgv.listener.NewDataListener;
import hu.varadi.zoltan.rccar.R;

/**
 * Created by Zoltan Varadi on 2013.12.06.
 */
class ClientCommunicationThread extends Thread {

    private static final String LOG_TAG = "ClientCommunicationThread.java";

    private Socket socket;
    private PrintWriter out;
    private BufferedReader input;
    private String serverIP;
    private int serverPort;
    private InformationListener informationListener;
    private NewDataListener newDataListener;

    //szal letrehozasakor az input output kiszedes
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
                informationListener.information(R.string.connected, InformationListener.INFO);
            }
            this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            while (!Thread.currentThread().isInterrupted()) {
                try {
//folyamat figyeles hogy jott egy uj adat
                    String read = input.readLine();
                    if (read != null) {

                        if (newDataListener != null) {
                            newDataListener.newData(read);
                        }
                    } else {
                        Log.e(LOG_TAG, "read az egy null");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                input.close();
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (UnknownHostException e1) {
            if (informationListener != null) {
                informationListener.information(e1.getMessage(), InformationListener.ERROR);
            }
            e1.printStackTrace();
        } catch (IOException e1) {
            if (informationListener != null) {
                informationListener.information(e1.getMessage(), InformationListener.ERROR);
            }
        }

    }

    public void sendDataToServer(String data) {
        Log.e(LOG_TAG, "-----------------------------------------");
//ha valaki uzzeni akar a masik oldalnak
        if (socket == null) {
            Log.e(LOG_TAG, "Socket is null");
            if (informationListener != null) {
                informationListener.information(R.string.socketNull, InformationListener.OUTPUT_ERROR);
            }
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
                        informationListener.information(R.string.serverWriteError, InformationListener.OUTPUT_ERROR);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "sendDataToServer Exception " + e.getClass().toString());
                if (informationListener != null) {
                    informationListener.information(R.string.serverWriteError, InformationListener.OUTPUT_ERROR);
                }
            }
        }
    }


    // a belso valtzok eleresehez metodusok
    public void setInformationListener(InformationListener i) {
        this.informationListener = i;
    }

    public void setNewDataListener(NewDataListener n) {
        this.newDataListener = n;
    }

    public void removeInformationListener() {
        this.informationListener = null;
    }

    public void removeNewDataListener() {
        this.newDataListener = null;
    }
}
