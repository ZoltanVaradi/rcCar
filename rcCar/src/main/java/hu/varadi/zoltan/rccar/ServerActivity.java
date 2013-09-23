package hu.varadi.zoltan.rccar;

import android.app.Activity;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;


public class ServerActivity extends Activity {


    ServerSocket server;

    private ServerSocket serverSocket;
    public static final int SERVERPORT = 6000;
    Handler updateConversationHandler;
    private Thread serverThread = null;
    private TextView textViewStatus;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
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

}
