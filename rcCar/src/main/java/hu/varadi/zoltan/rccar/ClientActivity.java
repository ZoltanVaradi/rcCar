package hu.varadi.zoltan.rccar;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class ClientActivity extends Activity {

    private static final String LOG_TAG = "nyuszika";

    private int SERVERPORT = 6000;
    private int SEEKBAR_DEFAULT_VALUE = 25;
    private String SERVER_IP;
    private Socket socket;
    private EditText editTextIpAddress;
    private WifiManager wifiManager;
    private TextView textViewGaz;
    private TextView TextViewKormany;
    private CheckBox checkBoxDefaultGataway;
    private Button bntConnect;
    private Thread clientThread;
    private Handler updateConversationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        wifiManager = (WifiManager) getSystemService(getApplicationContext().WIFI_SERVICE);

        textViewGaz = (TextView) findViewById(R.id.textViewGaz);
        TextViewKormany = (TextView) findViewById(R.id.textViewKormany);
        editTextIpAddress = (EditText) findViewById(R.id.editTextIP);
        checkBoxDefaultGataway = (CheckBox) findViewById(R.id.checkBoxGateway);
        bntConnect = (Button) findViewById(R.id.buttonConnect);
        SeekBar sbKorm = (SeekBar) findViewById(R.id.seekBarKormany);
        SeekBar sbGaz = (SeekBar) findViewById(R.id.seekBarGaz);

        clientThread = new Thread(new ClientThread());
        updateConversationHandler = new Handler();

        sbKorm.setOnSeekBarChangeListener(sbListener);
        sbGaz.setOnSeekBarChangeListener(sbListener);

        try {


            Log.e(LOG_TAG, bntConnect == null ? "true" : "false");
            bntConnect.setOnClickListener(buttonOnClick);
        } catch (Exception ex) {
            Log.e(LOG_TAG, ex.getMessage());

        }

        checkBoxDefaultGataway.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBoxDefaultGataway.isChecked()) {
                    editTextIpAddress.setEnabled(false);
                } else {
                    editTextIpAddress.setEnabled(true);
                }
            }
        });
    }

    private Button.OnClickListener buttonOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            if (checkBoxDefaultGataway.isChecked()) {
                int gw = wifiManager.getDhcpInfo().gateway;
                SERVER_IP = ipAddressToString(gw);
            } else {
                SERVER_IP = editTextIpAddress.getText().toString();
            }

            if (clientThread.getState() == Thread.State.NEW) {
                clientThread.start();

            } else if (socket == null) {
                clientThread = new Thread(new ClientThread());
                clientThread.start();
            } else {
                Toast.makeText(getApplicationContext(), "clientThread.state nem new", Toast.LENGTH_SHORT).show();
            }

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        TextView textStatus = (TextView) findViewById(R.id.textViewStat);
        int gw = wifiManager.getDhcpInfo().gateway;
        int ip = wifiManager.getConnectionInfo().getIpAddress();
        String textViewString = "";
        textViewString += "Wifi enabled:            " + wifiManager.isWifiEnabled() + "\n";
        textViewString += "My ip:            " + ipAddressToString(ip) + "\n";
        textViewString += "Default gateway:  " + ipAddressToString(gw) + "\n";
        textViewString += "Connected AP name: " + wifiManager.getConnectionInfo().getSSID();
        textStatus.setText(textViewString);

        SERVER_IP = ipAddressToString(gw);
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

                socket = new Socket(serverAddr, SERVERPORT);


                updateConversationHandler.post(new showToastThread("Kapcsi papcsi talán él"));
            } catch (UnknownHostException e1) {
                Log.d("UnknownHostException", "e1");
                updateConversationHandler.post(new showToastThread(e1.getMessage()));
                e1.printStackTrace();
            } catch (IOException e1) {
                updateConversationHandler.post(new showToastThread(e1.getMessage()));
                e1.printStackTrace();
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

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendDataToServer(String data) {
        Log.e(LOG_TAG, "-----------------------------------------");

        if (socket == null) {
            //Toast.makeText(getApplicationContext(), "Socket is null", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Socket is null");
        } else {
            Log.e(LOG_TAG, socket.isConnected() + "");
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())),
                        true);
                out.println(data);
                Log.e(LOG_TAG, out.checkError() + "");


            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private SeekBar.OnSeekBarChangeListener sbListener;

    {
        sbListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (seekBar.getId()) {
                    case R.id.seekBarGaz:
                        textViewGaz.setText(progress + "");
                        sendDataToServer("g:" + progress);
                        break;
                    case R.id.seekBarKormany:
                        TextViewKormany.setText(progress + "");
                        sendDataToServer("k:" + progress);
                        break;
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(SEEKBAR_DEFAULT_VALUE);

            }
        };
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

}
