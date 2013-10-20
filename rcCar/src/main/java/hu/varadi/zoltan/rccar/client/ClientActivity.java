package hu.varadi.zoltan.rccar.client;

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import hu.varadi.zoltan.rccar.R;

public class ClientActivity extends Activity {

    private static final String LOG_TAG = "nyuszika";

    private int SERVERPORT = 6000;
    private int SEEKBAR_DEFAULT_VALUE = 25;
    private int SEEKBAR_BASE_VALUE = 25;
    private int SEEKBAR_GAZ_MAX_VALUE = 50;
    private String SERVER_IP;
    private Socket socket;
    private EditText editTextIpAddress;
    private WifiManager wifiManager;
    private TextView textViewGaz;
    private TextView TextViewKormany;
    private TextView tvGazMaxValue;
    private CheckBox checkBoxDefaultGataway;
    private Button bntConnect;
    private Thread clientThread;
    private Handler updateConversationHandler;
    private SeekBar sbGaz;
    private SeekBar sbGazMax;
    private PrintWriter out;

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
        sbGaz = (SeekBar) findViewById(R.id.seekBarGaz);
        sbGazMax = (SeekBar) findViewById(R.id.seekBarGazMax);
        clientThread = new Thread(new ClientThread());
        updateConversationHandler = new Handler();
        tvGazMaxValue = (TextView) findViewById(R.id.textViewGazMax);
        tvGazMaxValue.setText(getString(R.string.gazMaxValue) + "(" + sbGazMax.getProgress() + ")");

        sbKorm.setOnSeekBarChangeListener(sbListener);

        try {

            Log.e(LOG_TAG, bntConnect == null ? "bntConnect true" : "false");
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


        sbGaz.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int progressValue = SEEKBAR_DEFAULT_VALUE;
                progressValue += progress - sbGazMax.getProgress();
                sendDataToServer("g:" + progressValue);
                textViewGaz.setText(progress + "|" + progressValue + "|" + sbGazMax.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                sbGazMax.setEnabled(false);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sbGazMax.setEnabled(true);
                seekBar.setProgress(sbGazMax.getProgress());

            }
        });

        sbGazMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvGazMaxValue.setText(getString(R.string.gazMaxValue) + "(" + progress + ")");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                sbGaz.setEnabled(false);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 1) {
                    seekBar.setProgress(1);
                }
                sbGazMaxStopTrackingTouch(seekBar);
            }
        });


        sbGazMaxStopTrackingTouch(sbGazMax);
    }

    private void sbGazMaxStopTrackingTouch(SeekBar seekBar) {
        sbGaz.setEnabled(true);
        sbGaz.setMax(seekBar.getProgress() * 2);
        sbGaz.setProgress(seekBar.getProgress());
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

            if (clientThread != null && clientThread.getState() == Thread.State.NEW) {
                clientThread.start();
                view.setEnabled(false);
            } else if (socket == null) {
                clientThread = new Thread(new ClientThread());
                clientThread.start();
                view.setEnabled(false);
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

            boolean ex = false;

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);
                out = new PrintWriter(socket.getOutputStream(), true);

                updateConversationHandler.post(new showToastThread("Kapcsi papcsi talán él"));

            } catch (UnknownHostException e1) {
                Log.d("UnknownHostException", "e1");
                updateConversationHandler.post(new showToastThread(e1.getMessage()));
                e1.printStackTrace();
                ex = true;
            } catch (IOException e1) {
                updateConversationHandler.post(new showToastThread(e1.getMessage()));
                e1.printStackTrace();
                ex = true;
            }

            if (ex) {
                bntConnect.post(new Runnable() {
                    @Override
                    public void run() {
                        bntConnect.setEnabled(true);
                    }
                });
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

                out.println(data);
                out.flush();
                Log.e(LOG_TAG, out.checkError() + "");

                if (out.checkError()) {
                    socket.close();
                    socket = null;
                    clientThread = null;
                    bntConnect.setEnabled(true);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "sendDataToServer Exception " + e.getClass().toString());
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

    private String ipAddressToString(int ipAddress) {

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