package hu.varadi.zoltan.rccar;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;


public class ServerActivity extends Activity implements Runnable {

    private static final String TAG = "rcCar_server";

    private static final String ACTION_USB_PERMISSION = "com.example.usbteszt1.USB_PERMISSION";
    private static final byte COMMAND_KORMANY = 0x3;
    private static final byte COMMAND_GAZ = 0x2;

    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;

    private UsbManager mUsbManager;
    private UsbAccessory mAccessory;

    ParcelFileDescriptor mFileDescriptor;

    FileInputStream mInputStream;
    FileOutputStream mOutputStream;

    ServerSocket server;

    private ServerSocket serverSocket;
    public static final int SERVERPORT = 6000;
    Handler updateConversationHandler;
    private Thread serverThread = null;
    private TextView textViewStatus;
    private TextView mStatusView;
    private TextView textViewGaz;
    private TextView textViewKormany;
    private Button btn;
    private WifiManager wifii;
    private Thread commThread;
    private boolean commThreadRun = true;

    public WifiConfiguration getWifiApConfiguration() {
        try {
            Method method = wifii.getClass().getMethod("getWifiApConfiguration");
            return (WifiConfiguration) method.invoke(wifii);
        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);


        // Broadcast Intent for myPermission
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        // Register Intent myPermission and remove accessory
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);

/////////////////////////////////////////////////////////////////////////////
        if (getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }
/////////////////////////////////////////////////////////////////////////////


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        mStatusView = (TextView) findViewById(R.id.textViewStatusConnect);
        textViewGaz = (TextView) findViewById(R.id.textViewGaz);
        textViewKormany = (TextView) findViewById(R.id.textViewKormany);
        btn = (Button) findViewById(R.id.buttonServer);
        wifii = (WifiManager) getSystemService(getApplicationContext().WIFI_SERVICE);

        WifiConfiguration wc = getWifiApConfiguration();


        String textViewString = "";

        textViewString += "AP name: " + wc.SSID + "\n";
        textViewString += "Ip address: " + ipAddressToString(wifii.getDhcpInfo().ipAddress);
        textViewStatus.setText(textViewString);

        updateConversationHandler = new Handler();
        this.serverThread = new Thread(new ServerThread());

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (btn.getText().toString().equals(getString(R.string.startServer))) {

                        if (serverThread.getState() == Thread.State.NEW) {
                            serverThread.start();

                        } else {
                            Toast.makeText(getApplicationContext(), "serverThread.getState nem NEW", Toast.LENGTH_SHORT).show();

                        }

                    } else if ((btn.getText().toString().equals(getString(R.string.stopServer)))) {
                        btn.setText("nem lehet leállítani");
                    }
                } catch (Exception ex) {
                    Log.e("ex bnt ", ex.getLocalizedMessage());
                }
            }
        });

        enableControls(false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            commThreadRun = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mInputStream != null && mOutputStream != null) {
            return;
        }

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            Log.d(TAG, "mAccessory is nullq1");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeAccessory();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    protected String ipAddressToString(int ipAddress) {
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFI_IP", "Unable to get host address.");
            ipAddressString = "NaN";
        }

        return ipAddressString;
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(SERVERPORT);
                updateConversationHandler.post(new showToastThread("A szerver elindult"));
                textViewStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewStatus.append("\nPort:" + SERVERPORT);
                    }
                });
                btn.post(new Runnable() {
                    @Override
                    public void run() {
                        btn.setText(R.string.stopServer);
                    }
                });


                try {

                    socket = serverSocket.accept();

                    updateConversationHandler.post(new showToastThread(socket.getRemoteSocketAddress().toString()));

                    commThread = new Thread(new CommunicationThread(socket));
                    commThread.start();

                    serverSocket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
                updateConversationHandler.post(new showToastThread(e.getLocalizedMessage()));
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {

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

                    updateConversationHandler.post(new updateUIThread(read));

                    String[] sl = read.split(":");
                    if (sl.length == 2) {
                        byte value =Byte.parseByte(sl[1]);
                        byte command = 0x0;
                        if (sl[0].equalsIgnoreCase("k")) {
                            command = COMMAND_KORMANY;
                        } else   if (sl[0].equalsIgnoreCase("g")) {
                            command = COMMAND_GAZ;
                        }
                        sendCommand(command, value);
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
    }

    class updateUIThread implements Runnable {
        private String msg;

        public updateUIThread(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            if (msg != null) {
                if (msg.startsWith("k")) {
                    textViewKormany.setText(msg);
                } else if (msg.startsWith("g")) {
                    textViewGaz.setText(msg);
                }
            }
        }
    }

    class showToastThread implements Runnable {
        private String msg;

        public showToastThread(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            if (msg != null) {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);

        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();

            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

            // communication thread start
            Thread thread = new Thread(null, this, "DemoKit");
            thread.start();
            Log.d(TAG, "accessory opened");

            enableControls(true);
        } else {
            Log.d(TAG, "accessory open fail");
        }
    }

    private void closeAccessory() {
        enableControls(false);

        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    private void enableControls(boolean enable) {
        if (enable) {
            mStatusView.setText(R.string.usbConnectOK);
        } else {
            mStatusView.setText(R.string.usbConnectNO);
        }
    }

    public void sendCommand(byte command, byte value) {
        byte[] buffer = new byte[2];
        buffer[0] = command;
        buffer[1] = value;
        if (mOutputStream != null) {
            try {
                mOutputStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "write failed", e);
            }
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action " + action);
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        Log.d(TAG, "permission denied for accessory " + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
            }
        }
    };

    // USB read thread
    @Override
    public void run() {
        int ret = 0;
        byte[] buffer = new byte[16384];
        int i;

        // Accessory -> Android
        while (ret >= 0) {
            try {
                ret = mInputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            i = 0;
            Log.d(TAG, "---------- read usb data begin ----------");
            while (i < ret) {
                Log.d(TAG, "read usb" + buffer[i]);
                i++;
            }
            Log.d(TAG, "---------- read usb data end ----------");

        }
    }
}
