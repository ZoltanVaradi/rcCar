package hu.varadi.zoltan.rccar.client;

import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import hu.varadi.zoltan.rccar.listener.InformationListener;
import hu.varadi.zoltan.rccar.util.WifiUtil;

import hu.varadi.zoltan.rccar.R;

public class ClientActivity extends Activity {

    private static final String LOG_TAG = "ClientActivity";

    private int SEEKBAR_DEFAULT_VALUE = 25;
    private int SEEKBAR_GAZ_MAX_VALUE = 50;
    private int SERVERPORT = 6000;
    private String SERVER_IP;
    private WifiManager wifiManager;
    private EditText editTextIpAddress;
    private TextView textViewGaz;
    private TextView TextViewKormany;
    private TextView tvGazMaxValue;
    private CheckBox checkBoxDefaultGataway;
    private Button bntConnect;
    private SeekBar sbGaz;
    private SeekBar sbGazMax;
    private SeekBar sbKorm;
    private ClientCommunicationThread clientThread;

    //-------------------------------activity lifecycl----------------------------------------------
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
        sbKorm = (SeekBar) findViewById(R.id.seekBarKormany);
        sbGaz = (SeekBar) findViewById(R.id.seekBarGaz);
        sbGazMax = (SeekBar) findViewById(R.id.seekBarGazMax);

        tvGazMaxValue = (TextView) findViewById(R.id.textViewGazMax);
        tvGazMaxValue.setText(getString(R.string.gazMaxValue) + "(" + sbGazMax.getProgress() + ")");


        sbKorm.setOnSeekBarChangeListener(sbKormanyListener);
        bntConnect.setOnClickListener(buttonClickListener);
        checkBoxDefaultGataway.setOnClickListener(checkBoxClickListener);
        sbGaz.setOnSeekBarChangeListener(sbGasChangeListener);
        sbGazMax.setOnSeekBarChangeListener(sbGazMaxChangeListener);

        sbGazMaxStopTrackingTouch(sbGazMax);
    }


    @Override
    protected void onStart() {
        super.onStart();

        SERVER_IP = WifiUtil.getGatewayIp(wifiManager);

        StringBuilder textViewSB = new StringBuilder();
        textViewSB.append("Wifi enabled:            ").append(wifiManager.isWifiEnabled()).append("\n");
        textViewSB.append("My ip:                   ").append(WifiUtil.getMyIpAddress(wifiManager)).append("\n");
        textViewSB.append("Default gateway:         ").append(SERVER_IP).append("\n");
        textViewSB.append("Connected AP name:       ").append(WifiUtil.getSSID(wifiManager)).append("\n");

        TextView textStatus = (TextView) findViewById(R.id.textViewStat);
        textStatus.setText(textViewSB.toString());
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (clientThread != null) {
            clientThread = null;
        }
    }


    //------------------------------private method--------------------------------------------------


    private void creatClientTherad() {
        clientThread = new ClientCommunicationThread(SERVER_IP, SERVERPORT);
        clientThread.setPriority(Thread.NORM_PRIORITY - 1);
        clientThread.setInformationListener(new InformationListener() {
            @Override
            public void information(final String info, int type) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
                    }
                });

                switch (type) {
                    case InformationListener.INFO:
                        break;
                    case InformationListener.ERROR:
                    case InformationListener.OUTPUT_ERROR:
                        bntConnect.post(new Runnable() {
                            @Override
                            public void run() {
                                bntConnect.setEnabled(true);
                            }
                        });
                        break;
                }
            }
        });
    }


    //------------------------------Listener--------------------------------------------------------


    private View.OnClickListener checkBoxClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (checkBoxDefaultGataway.isChecked()) {
                editTextIpAddress.setEnabled(false);
            } else {
                editTextIpAddress.setEnabled(true);
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener sbGasChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int progressValue = SEEKBAR_DEFAULT_VALUE;
            progressValue += progress - sbGazMax.getProgress();
            if (clientThread != null) {
                clientThread.sendDataToServer("g:" + progressValue);
            }
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
    };

    private SeekBar.OnSeekBarChangeListener sbGazMaxChangeListener = new SeekBar.OnSeekBarChangeListener() {
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
    };

    private void sbGazMaxStopTrackingTouch(SeekBar seekBar) {
        sbGaz.setEnabled(true);
        sbGaz.setMax(seekBar.getProgress() * 2);
        sbGaz.setProgress(seekBar.getProgress());
    }


    private Button.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (checkBoxDefaultGataway.isChecked()) {
                int gw = wifiManager.getDhcpInfo().gateway;
                SERVER_IP = WifiUtil.ipAddressToString(gw);
            } else {
                SERVER_IP = editTextIpAddress.getText().toString();
            }


            if (clientThread != null && clientThread.getState() == Thread.State.NEW) {
                creatClientTherad();
                clientThread.start();
                view.setEnabled(false);
            } else {
                Toast.makeText(getApplicationContext(), "clientThread.state nem new", Toast.LENGTH_SHORT).show();
            }

        }
    };

    private SeekBar.OnSeekBarChangeListener sbKormanyListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (clientThread != null) {
                switch (seekBar.getId()) {
                    case R.id.seekBarGaz:
                        textViewGaz.setText(progress + "");
                        clientThread.sendDataToServer("g:" + progress);

                        break;
                    case R.id.seekBarKormany:
                        TextViewKormany.setText(progress + "");
                        int progress2 = 50 - progress; //megforditom az iranyt az uj servo miatt
                        clientThread.sendDataToServer("k:" + progress2);
                        break;
                }
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
