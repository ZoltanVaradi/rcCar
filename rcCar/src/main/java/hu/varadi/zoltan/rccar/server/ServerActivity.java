package hu.varadi.zoltan.rccar.server;

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

import java.net.Socket;

import hu.varadi.zoltan.rccar.R;
import hu.varadi.zoltan.rccar.listener.ConnectClientListener;
import hu.varadi.zoltan.rccar.listener.InformationListener;
import hu.varadi.zoltan.rccar.listener.NewDataListener;
import hu.varadi.zoltan.rccar.util.WifiUtil;
import hu.varadi.zoltan.rccar.util.rcCarUtil;


public class ServerActivity extends Activity {
    //TODO: A szerver socket létrehozát és indítását kirakni metódusokba. Ha lecsatlakozik a kliens akkor automatikusan egy friss ropogós szerver socketet indítani

    private static final String TAG = "rcCar_server";
    private int SERVERPORT = 6000;
    private Handler updateConversationHandler;
    private TextView textViewStatus;
    private TextView mStatusView;
    private TextView textViewGaz;
    private TextView textViewKormany;
    private Button btn;
    private WifiManager wifiManager;
    private ServerThread serverThread = null;
    private CommunicationThread communicationThread;
    private USBCommunication usbCommunication;

    //-------------------------------activity lifecycl----------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        usbCommunication = new USBCommunication(this);
        usbCommunication.setInformationListener(new InformationListener() {
            @Override
            public void information(final String info, int type) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
                    }
                });

                if (type == InformationListener.INFO) {
                    enableControls(true);
                } else {
                    enableControls(false);
                }
            }
        });

        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        mStatusView = (TextView) findViewById(R.id.textViewStatusConnect);
        textViewGaz = (TextView) findViewById(R.id.textViewGaz);
        textViewKormany = (TextView) findViewById(R.id.textViewKormany);
        btn = (Button) findViewById(R.id.buttonServer);
        wifiManager = (WifiManager) getSystemService(getApplicationContext().WIFI_SERVICE);

        WifiConfiguration wc = WifiUtil.getWifiApConfiguration(wifiManager);

        StringBuilder textViewSB = new StringBuilder();
        textViewSB.append("AP name: ").append(wc.SSID).append("\n");
        textViewSB.append("Ip address: ").append(WifiUtil.getMyIpAddress(wifiManager));
        textViewStatus.setText(textViewSB.toString());

        updateConversationHandler = new Handler();

        btn.setOnClickListener(onClickListener);

        enableControls(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        communicationThread = null;
        serverThread = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        usbCommunication.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        usbCommunication.onPause();
    }

    @Override
    public void onDestroy() {
        usbCommunication.onDestroy();
        super.onDestroy();
    }


    //-------------------------------Listener-------------------------------------------------------


    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                if (btn.getText().toString().equals(getString(R.string.startServer))) {

                    serverThread = new ServerThread(SERVERPORT);
                    serverThread.setPriority(Thread.NORM_PRIORITY - 1);
                    serverThread.setOnConnectClientListener(connectClientListener);
                    serverThread.start();

                    btn.setText(getString(R.string.stopServer));

                } else if ((btn.getText().toString().equals(getString(R.string.stopServer)))) {
                    // btn.setText(R.string.stopServer);

                    serverThread = null;

                    communicationThread = null;
                    btn.setText(R.string.startServer);
                    usbCommunication.sendDataToUSB(rcCarUtil.COMMAND_GAZ, rcCarUtil.BASE_GAS_VALUE);
                    Toast.makeText(getApplicationContext(), getString(R.string.stopServer), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {
                Log.e("ex bnt ", ex.getLocalizedMessage());
            }
        }
    };

    ConnectClientListener connectClientListener = new ConnectClientListener() {
        @Override
        public void connect(Socket clientSocket) {
            communicationThread = new CommunicationThread(clientSocket);
            communicationThread.setUSBWriteListener(newDataListener);
            communicationThread.setInformationListener(new InformationListener() {
                @Override
                public void information(String info, int type) {

                }
            });
            communicationThread.start();
        }
    };

    private NewDataListener newDataListener = new NewDataListener() {
        @Override
        public void newData(String data) {
            String[] sl = data.split(":");
            byte value = Byte.parseByte(sl[1]);
            byte command = 0x0;
            if (sl.length == 2) {

                if (sl[0].equalsIgnoreCase("k")) {
                    command = rcCarUtil.COMMAND_KORMANY;
                } else if (sl[0].equalsIgnoreCase("g")) {
                    command = rcCarUtil.COMMAND_GAZ;
                }
            }
            updateConversationHandler.post(new updateUIThread(data));
            usbCommunication.sendDataToUSB(command, value);
        }
    };

    //--------------------------------runnable------------------------------------------------------


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

    //--------------------------------method--------------------------------------------------------


    private void enableControls(boolean enable) {
        if (enable) {
            mStatusView.setText(R.string.usbConnectOK);
        } else {
            mStatusView.setText(R.string.usbConnectNO);
        }
    }
}
