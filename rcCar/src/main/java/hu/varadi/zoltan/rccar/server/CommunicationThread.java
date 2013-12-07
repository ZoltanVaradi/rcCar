package hu.varadi.zoltan.rccar.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import hu.varadi.zoltan.rccar.listener.InformationListener;
import hu.varadi.zoltan.rccar.listener.NewDataListener;
import hu.varadi.zoltan.rccar.util.rcCarUtil;

/**
 * Created by Zoltan Varadi on 2013.12.06..
 */
public class CommunicationThread extends Thread {

    private Socket clientSocket;
    private BufferedReader input;
    private boolean commThreadRun;
    private InformationListener info;
    private NewDataListener newDataListener;


    public CommunicationThread(Socket clientSocket) {
        this.commThreadRun = true;
        this.clientSocket = clientSocket;
        try {
            this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

        while (!Thread.currentThread().isInterrupted() && commThreadRun) {
            try {

                String read = input.readLine();
                if (read != null) {

                    if (newDataListener != null) {
                        newDataListener.newData(read);
                    }
                } else {
                    commThreadRun = false;
                    // commThread = null;
                    if (newDataListener != null) {
                        newDataListener.newData(rcCarUtil.COMMAND_GAZ + ":" + rcCarUtil.BASE_GAS_VALUE);
                    }
                    if (info != null) {
                        info.information("Cliens kilépett? mert a read==null volt", InformationListener.INPUT_ERROR);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            input.close();
            this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
}