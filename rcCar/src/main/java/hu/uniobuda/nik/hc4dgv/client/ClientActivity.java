package hu.uniobuda.nik.hc4dgv.client;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import hu.uniobuda.nik.hc4dgv.listener.InformationListener;
import hu.uniobuda.nik.hc4dgv.listener.NewDataListener;
import hu.uniobuda.nik.hc4dgv.util.WifiUtil;
import hu.uniobuda.nik.hc4dgv.util.rcCarUtil;
import hu.uniobuda.nik.hc4dgv.view.AccelerometerView;
import hu.uniobuda.nik.hc4dgv.view.BatteryView;

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
    private TextView tvLight;
    private CheckBox checkBoxDefaultGataway;
    private Button btnConnect;
    private SeekBar sbGaz;
    private SeekBar sbGazMax;
    private SeekBar sbKorm;
    private ClientCommunicationThread clientThread;
    private AccelerometerView accelerometerView;
    private BatteryView batteryView;
    private Chronometer chronometer;
    private SharedPreferences sharedPreferences;

    //-------------------------------activity lifecycl----------------------------------------------
    //kozvetlen kapcsolatnal a default gateway-t hasznlaja
    //egyebkent a beirt ip cimet
    // a port meg nem valtzok
    // ha leveszik a kapcsolat akkor alapalapotrol indul

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        wifiManager = (WifiManager) getSystemService(getApplicationContext().WIFI_SERVICE);
        sharedPreferences = getSharedPreferences(rcCarUtil.PrefFileName, 0);

        textViewGaz = (TextView) findViewById(R.id.textViewGaz);
        TextViewKormany = (TextView) findViewById(R.id.textViewKormany);
        tvGazMaxValue = (TextView) findViewById(R.id.textViewGazMax);
        tvLight = (TextView) findViewById(R.id.textViewLight);
        editTextIpAddress = (EditText) findViewById(R.id.editTextIP);
        checkBoxDefaultGataway = (CheckBox) findViewById(R.id.checkBoxGateway);
        btnConnect = (Button) findViewById(R.id.buttonConnect);
        sbKorm = (SeekBar) findViewById(R.id.seekBarKormany);
        sbGaz = (SeekBar) findViewById(R.id.seekBarGaz);
        sbGazMax = (SeekBar) findViewById(R.id.seekBarGazMax);
        chronometer = (Chronometer) findViewById(R.id.chronometer);

        TextViewKormany.setText(sbKorm.getProgress() + "");
        tvGazMaxValue.setText(getString(R.string.gazMaxValue) + "(" + sbGazMax.getProgress() + ")");

        accelerometerView = (AccelerometerView) findViewById(R.id.accelerometerView);
        batteryView = (BatteryView) findViewById(R.id.batteryView);

        checkBoxDefaultGataway.setOnClickListener(checkBoxClickListener);
        sbKorm.setOnSeekBarChangeListener(sbKormanyListener);
        btnConnect.setOnClickListener(buttonClickListener);
        sbGaz.setOnSeekBarChangeListener(sbGasChangeListener);
        sbGazMax.setOnSeekBarChangeListener(sbGazMaxChangeListener);

        sbGazMax.setProgress(rcCarUtil.getMaxSpeedSeekbar(sharedPreferences));
        editTextIpAddress.setText(rcCarUtil.getIpAddress(sharedPreferences));
        checkBoxDefaultGataway.setChecked(rcCarUtil.getUseGateway(sharedPreferences));
        editTextIpAddress.setEnabled(!checkBoxDefaultGataway.isChecked());

        sbGazMaxStopTrackingTouch(sbGazMax);
    }


    @Override
    protected void onStart() {
        super.onStart();

        SERVER_IP = WifiUtil.getGatewayIp(wifiManager);

        //wifimmanager-ből kinyerhető információ
        StringBuilder textViewSB = new StringBuilder();
        textViewSB.append(getString(R.string.wifiState)).append(wifiManager.isWifiEnabled()).append("\n");
        textViewSB.append(getString(R.string.ipAddress)).append(WifiUtil.getMyIpAddress(wifiManager)).append("\n");
        textViewSB.append(getString(R.string.defaultGataway)).append(SERVER_IP).append("\n");
        textViewSB.append(getString(R.string.ApName)).append(WifiUtil.getSSID(wifiManager));

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
        // egy szalat elokeszitve a communikcaciohoz
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
                        btnConnect.post(new Runnable() {
                            @Override
                            public void run() {
                                btnConnect.setEnabled(true);

                            }
                        });
                        chronometer.post(new Runnable() {
                            @Override
                            public void run() {
                                chronometer.stop();
                            }
                        });
                        break;
                }
            }

            @Override
            public void information(int stringResourcesID, int type) {
                this.information(getString(stringResourcesID), type);
            }
        });
        clientThread.setNewDataListener(new NewDataListener() {
            @Override
            public void newData(String data) {
                String[] s = data.split(";");

                if (rcCarUtil.COMMAND_SENSOR_ACC.equals(s[0])) {
                    float[] f = new float[3];
                    long time;
                    f[0] = Float.parseFloat(s[1]);
                    f[1] = Float.parseFloat(s[2]);
                    f[2] = Float.parseFloat(s[3]);
                    time = Long.parseLong(s[4]);
                    accelerometerView.addValue(f);
                } else if (rcCarUtil.COMMAND_SENSOR_AKKU_ANDROID.equals(s[0])) {
                    int level = Integer.parseInt(s[1]);
                    long time = Long.parseLong(s[2]);
                    batteryView.addValueAndroid(level);
                } else if (rcCarUtil.COMMAND_SENSOR_LIGHT.equals(s[0])) {
                    final String light = s[1];
                    long time = Long.parseLong(s[2]);
                    tvLight.post(new Runnable() {
                        @Override
                        public void run() {
                            tvLight.setText(light + " ");
                        }
                    });

                }


            }
        });
    }


    //------------------------------Listener--------------------------------------------------------


    private View.OnClickListener checkBoxClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            editTextIpAddress.setEnabled(!checkBoxDefaultGataway.isChecked());
            rcCarUtil.saveUseGateway(sharedPreferences, checkBoxDefaultGataway.isChecked());
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
        rcCarUtil.saveMaxSpeedSeekbar(sharedPreferences, seekBar.getProgress());
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
            if (clientThread == null) {
                creatClientTherad();
            }

            if (clientThread != null && clientThread.getState() == Thread.State.NEW) {
                clientThread.start();

                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.start();
                view.setEnabled(false);
                rcCarUtil.saveIpAddress(sharedPreferences, SERVER_IP);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.connectError), Toast.LENGTH_SHORT).show();
                clientThread = null;
            }

        }
    };

    private SeekBar.OnSeekBarChangeListener sbKormanyListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            TextViewKormany.setText(progress + "");
            if (clientThread != null) {
                int progress2 = 50 - progress; //megforditom az iranyt az uj servo miatt
                clientThread.sendDataToServer("k:" + progress2);
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
