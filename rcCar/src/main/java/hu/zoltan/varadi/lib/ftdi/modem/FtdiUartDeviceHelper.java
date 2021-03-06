package hu.zoltan.varadi.lib.ftdi.modem;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import hu.varadi.zoltan.rccar.R;


public class FtdiUartDeviceHelper {

    private static final String TAG = FtdiUartDeviceHelper.class.getSimpleName();
    private static final int FT_BAUD_RATE_57600 = 57600;

    private Context deviceUARTContext;
    private D2xxManager d2xxManager;
    private FT_Device ftDevice = null;
    private int deviceCount = -1;
    private int currentIndex = -1;
    private int openIndex = 0;

    private int baudRate;
    private byte stopBit;
    private byte dataBit;
    private byte parity;
    private short flowControl;

    private byte[] readData;
    private char[] readDataToText;

    private boolean uart_configured = false;
    private static final int readLength = 512;
    private int iavailable = 0;
    private int readcount = 0;
    private boolean bReadThreadGoing = false;

    private ReadThread readThread;

    private FtdiInformationListener informationListener;


    public FtdiUartDeviceHelper(Context parentContext, D2xxManager ftdid2xxContext) {
        init(parentContext, ftdid2xxContext);
    }

    public FtdiUartDeviceHelper(Context parentContext, D2xxManager ftdid2xxContext, FtdiInformationListener listener) {
        this.informationListener = listener;
        init(parentContext, ftdid2xxContext);
    }

    public void init(Context parentContext, D2xxManager ftdid2xxContext) {
        deviceUARTContext = parentContext;
        d2xxManager = ftdid2xxContext;

        readData = new byte[readLength];
        readDataToText = new char[readLength];

        baudRate = FT_BAUD_RATE_57600;
        stopBit = D2xxManager.FT_STOP_BITS_1;
        dataBit = D2xxManager.FT_DATA_BITS_8;
        parity = D2xxManager.FT_PARITY_NONE;
        flowControl = D2xxManager.FT_FLOW_RTS_CTS;

        connectFunction();
    }

    public void disconnectFunction() {
        deviceCount = -1;
        currentIndex = -1;
        bReadThreadGoing = false;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (ftDevice != null) {
            synchronized (ftDevice) {
                if (true == ftDevice.isOpen()) {
                    ftDevice.close();
                }
            }
        }
    }

    public void connectFunction() {
        createDeviceList();
        if (deviceCount < 0) {
            Log.e(TAG, "connectFunction: deviceCount<0");
            return;
        }
        int tmpProtNumber = openIndex + 1;

        if (currentIndex != openIndex) {
            if (null == ftDevice) {
                ftDevice = d2xxManager.openByIndex(deviceUARTContext, openIndex);
            } else {
                synchronized (ftDevice) {
                    ftDevice = d2xxManager.openByIndex(deviceUARTContext, openIndex);

                }
            }
            uart_configured = false;
        } else {
            Toast.makeText(deviceUARTContext, "Device port " + tmpProtNumber + " is already opened", Toast.LENGTH_LONG).show();
            return;
        }

        if (ftDevice == null) {
            Toast.makeText(deviceUARTContext, "open device port(" + tmpProtNumber + ") NG, ftDevice == null", Toast.LENGTH_LONG).show();
            return;
        }

        if (true == ftDevice.isOpen()) {
            currentIndex = openIndex;
            Log.i(TAG, "open device port(" + tmpProtNumber + ") OK");

            if (false == bReadThreadGoing) {
                readThread = new ReadThread(handler);
                readThread.start();
                bReadThreadGoing = true;
            }
        } else {
            Toast.makeText(deviceUARTContext, "open device port(" + tmpProtNumber + ") NG", Toast.LENGTH_LONG).show();
        }

        setConfig(baudRate, dataBit, stopBit, parity, flowControl);

    }

    public void sendMessage(byte... outData) {
        if (outData.length == 0) {
            return;
        }
        if (ftDevice.isOpen() == false) {
            Log.e(TAG, "sendMessage: device not open");
            disconnectFunction();
            connectFunction();
        }

        ftDevice.setLatencyTimer((byte) 16);

        ftDevice.write(outData, outData.length);
    }


    private void createDeviceList() {
        int tempDevCount = d2xxManager.createDeviceInfoList(deviceUARTContext);

        if (tempDevCount > 0) {
            if (deviceCount != tempDevCount) {
                deviceCount = tempDevCount;
                updatePortNumberSelector();
            }
        } else {
            deviceCount = -1;
            currentIndex = -1;
        }
    }

    private void updatePortNumberSelector() {

        if (deviceCount == 2) {

            Toast.makeText(deviceUARTContext, "2 port device attached", Toast.LENGTH_SHORT).show();
        } else if (deviceCount == 4) {

            Toast.makeText(deviceUARTContext, "4 port device attached", Toast.LENGTH_SHORT).show();
        } else {

            Log.i(TAG, "1 port device attached");
        }

    }


    private void setConfig(int baud, byte dataBits, byte stopBits, byte parity, short flowControl) {
        if (ftDevice.isOpen() == false) {
            Log.e(TAG, "setConfig: device not open");
            return;
        }

        // configure our port
        // reset to UART mode for 232 devices
        ftDevice.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
        ftDevice.setBaudRate(baud);
        ftDevice.setDataCharacteristics(dataBits, stopBits, parity);
        ftDevice.setFlowControl(flowControl, (byte) 0x0b, (byte) 0x0d);

        uart_configured = true;
        String toastMSG = "Config done\nbaud: " + baud + ", dataBits: " + dataBits + ", stopBits: " + stopBits + ", parity: " + parity + ", flowControl: " + flowControl;
        Resources res = deviceUARTContext.getResources();
        String text = String.format(res.getString(R.string.config_done), baud, dataBits, stopBits, parity, flowControl);
        triggerInformationListener(text, FtdiInformationListener.DEVICE_CONNECTED);
        //Toast.makeText(deviceUARTContext, toastMSG, Toast.LENGTH_LONG).show();
    }


    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (iavailable > 0) {
                StringBuilder sb = new StringBuilder(iavailable);
                for (int i = 0; i < iavailable; i++) {
                    sb.append(readDataToText[i]);
                }
//              String ss =  String.copyValueOf(readDataToText, 0, iavailable)
                triggerInformationListener(sb.toString(), FtdiInformationListener.NEW_DATA);
            }
        }
    };

    public void setInformationListener(FtdiInformationListener listener) {
        this.informationListener = listener;
    }

    private void triggerInformationListener(String message, int type) {
        if (informationListener != null) {
            informationListener.sendMessage(message, type);
        }
    }


    private class ReadThread extends Thread {
        Handler mHandler;

        ReadThread(Handler h) {
            mHandler = h;
            this.setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            int i;

            while (true == bReadThreadGoing) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }

                synchronized (ftDevice) {
                    iavailable = ftDevice.getQueueStatus();
                    if (iavailable > 0) {

                        if (iavailable > readLength) {
                            iavailable = readLength;
                        }

                        ftDevice.read(readData, iavailable);
                        for (i = 0; i < iavailable; i++) {
                            readDataToText[i] = (char) readData[i];
                        }
                        Message msg = mHandler.obtainMessage();
                        mHandler.sendMessage(msg);
                    }
                }
            }
        }

    }
}

