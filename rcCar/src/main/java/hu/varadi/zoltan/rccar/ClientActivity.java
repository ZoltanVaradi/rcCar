package hu.varadi.zoltan.rccar;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
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

    private Socket socket;
    private static final int SERVERPORT = 6000;
    private static final int SEEKBAR_DEFAULT_VALUE = 25;
    private static String SERVER_IP;
    EditText et;
    WifiManager wifii;
    private TextView tvGaz;
    private TextView tvKorm;
    private CheckBox cbGata;
    private Button bntConnect;
    private Thread clientThread;
    private Handler updateConversationHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        wifii = (WifiManager) getSystemService(getApplicationContext().WIFI_SERVICE);

        tvGaz = (TextView) findViewById(R.id.textViewGaz);
        tvKorm = (TextView) findViewById(R.id.textViewKormany);
        et = (EditText) findViewById(R.id.editTextInput);
        cbGata = (CheckBox) findViewById(R.id.checkBoxGateway);
        bntConnect = (Button) findViewById(R.id.buttonConnect);
        Button bnt = (Button) findViewById(R.id.button);
        SeekBar sbKorm = (SeekBar) findViewById(R.id.seekBarKormany);
        SeekBar sbGaz = (SeekBar) findViewById(R.id.seekBarGaz);

        clientThread = new Thread(new ClientThread());
        updateConversationHandler = new Handler();

        sbKorm.setOnSeekBarChangeListener(sbListener);
        sbGaz.setOnSeekBarChangeListener(sbListener);
        bnt.setOnClickListener(buttonOnClick);
    }

    private Button.OnClickListener buttonOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            if (cbGata.isChecked()) {
                int gw = wifii.getDhcpInfo().gateway;
                SERVER_IP = ipAddressToString(gw);
            } else {
                SERVER_IP = et.getText().toString();
            }

            if (clientThread.getState() == Thread.State.NEW) {
                clientThread.start();

            }else if(socket==null){
                clientThread = new Thread(new ClientThread());
                clientThread.start();
            }
            else {
                Toast.makeText(getApplicationContext(), "clientThread.state nem new", Toast.LENGTH_SHORT).show();
            }

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        TextView textStatus = (TextView) findViewById(R.id.textViewStat);
        int gw = wifii.getDhcpInfo().gateway;
        int ip = wifii.getConnectionInfo().getIpAddress();
        String textViewString = "";
        textViewString += "Wifi enabled:            " + wifii.isWifiEnabled() + "\n";
        textViewString += "My ip:            " + ipAddressToString(ip) + "\n";
        textViewString += "Default gateway:  " + ipAddressToString(gw) + "\n";
        textViewString += "Connected AP name: " + wifii.getConnectionInfo().getSSID();
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

    private void sendDataToServer(String data) {
        if (socket == null) {
            //Toast.makeText(getApplicationContext(), "Socket is null", Toast.LENGTH_SHORT).show();
        } else {
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())),
                        true);
                out.println(data);

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
                        tvGaz.setText(progress + "");
                        sendDataToServer("g:" + progress);
                        break;
                    case R.id.seekBarKormany:
                        tvKorm.setText(progress + "");
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
