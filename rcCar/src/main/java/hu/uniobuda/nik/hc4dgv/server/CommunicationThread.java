package hu.uniobuda.nik.hc4dgv.server;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import hu.uniobuda.nik.hc4dgv.listener.InformationListener;
import hu.uniobuda.nik.hc4dgv.listener.NewDataListener;
import hu.uniobuda.nik.hc4dgv.util.rcCarUtil;
import hu.varadi.zoltan.rccar.R;

/**
 * Created by Zoltan Varadi on 2013.12.06..
 */
public class CommunicationThread extends Thread {

    private static final String LOG_TAG = "CommunicationThread";
    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean commThreadRun;
    private InformationListener info;
    private NewDataListener newDataListener;


    //letrehozasnal az input es output kiszedese a socket-bol
    public CommunicationThread(Socket clientSocket) {
        try {
            this.commThreadRun = true;
            this.clientSocket = clientSocket;
            this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.output = new PrintWriter(this.clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
            if (info != null) {
                info.information(e.getLocalizedMessage(), InformationListener.INPUT_ERROR);
            }
        }
    }

    public void run() {

        while (!Thread.currentThread().isInterrupted() && commThreadRun) {
            try {
//a szal inditasa utan ha van uj adat a akkor a newDataListener-en keresztul ertesites lesz
                String read = input.readLine();
                if (read != null) {

                    if (newDataListener != null) {
                        newDataListener.newData(read);
                    }
                } else {
                    commThreadRun = false;
                    if (newDataListener != null) {
                        newDataListener.newData(rcCarUtil.COMMAND_GAZ + ":" + rcCarUtil.BASE_GAS_VALUE);
                    }
                    if (info != null) {
                        info.information(R.string.clitenNull, InformationListener.INPUT_ERROR);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (info != null) {
                    info.information(e.getLocalizedMessage(), InformationListener.INPUT_ERROR);
                }
            }
        }
        try {
            input.close();
            this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeDataToOutputSream(String data) {
        //ha informactio kozulni kell a masik oldallal
        Log.e(LOG_TAG, data);

        if (clientSocket == null) {
            Log.e(LOG_TAG, "Socket is null");
            if (info != null) {
                info.information(R.string.socketNull, InformationListener.OUTPUT_ERROR);
            }
        } else {
            //Log.e(LOG_TAG, clientSocket.isConnected() + "");
            try {


                output.println(data);
                output.flush();
                // Log.e(LOG_TAG, output.checkError() + "");

                if (output.checkError()) {
                    clientSocket.close();
                    clientSocket = null;
                    //clientThread = null;
                    //bntConnect.setEnabled(true);
                    if (info != null) {
                        info.information(R.string.clientWriteError, InformationListener.OUTPUT_ERROR);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "sendDataToServer Exception " + e.getClass().toString());
                if (info != null) {
                    info.information(R.string.clientWriteError, InformationListener.OUTPUT_ERROR);
                }
            }
        }
    }


    //csak par metudus hogy el lehessen erni a beslo valtozokat
    public boolean isCommThreadRun() {
        return commThreadRun;
    }

    public void setCommThreadRun(boolean commThreadRun) {
        this.commThreadRun = commThreadRun;
    }

    public void setInformationListener(InformationListener i) {
        this.info = i;
    }

    public void removeInformationListener() {
        this.info = null;
    }

    public void setUSBWriteListener(NewDataListener usbWriteListener) {
        this.newDataListener = usbWriteListener;
    }

    public void removeUSBWriteListener() {
        this.newDataListener = null;
    }

    public void closeSocket() {
        if (clientSocket != null) {
            try {
                this.clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}