package hu.varadi.zoltan.rccar.server;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import hu.varadi.zoltan.rccar.receiver.WifiReceiver;
import hu.varadi.zoltan.rccar.util.WifiUtil;
import hu.varadi.zoltan.rccar.util.rcCarUtil;


public class ServerActivity extends Activity {
    //TODO: A szerver socket létrehozát és indítását kirakni metódusokba. Ha lecsatlakozik a kliens akkor automatikusan egy friss ropogós szerver socketet indítani

    private static final String TAG = "rcCar_server";
    private final float ALPHA = 0.8f;
    private int SERVERPORT = 6000;
    private Handler updateConversationHandler;
    private TextView textViewStatus;
    private TextView mStatusView;
    private TextView textViewGaz;
    private TextView textViewKormany;
    private TextView textViewGPS;
    private Button btn;
    private ServerThread serverThread = null;
    private CommunicationThread communicationThread;
    private USBCommunication usbCommunication;
    private WifiReceiver wifiReceiver;
    private SensorManager sensorManager;
    private WifiManager wifiManager;
    private LocationManager locationManager;

    private float[] gravity = new float[3];
    private float[] acc = new float[3];

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
        textViewGPS = (TextView) findViewById(R.id.textViewSensorGPS);
        btn = (Button) findViewById(R.id.buttonServer);
        wifiManager = (WifiManager) getSystemService(getApplicationContext().WIFI_SERVICE);
        sensorManager = (SensorManager) getSystemService(getApplicationContext().SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        textViewGPS.setText("status:" + statusOfGPS);

        WifiConfiguration wc = WifiUtil.getWifiApConfiguration(wifiManager);

        StringBuilder textViewSB = new StringBuilder();
        if (wifiManager.isWifiEnabled()) {
            textViewSB.append("AP name: ").append(wc.SSID).append("\n");
            textViewSB.append("Ip address: ").append(WifiUtil.getMyIpAddress(wifiManager));
        } else {
            textViewSB.append("A wifi nincs bekapcsolva");
        }
        textViewStatus.setText(textViewSB.toString());

        updateConversationHandler = new Handler();

        btn.setOnClickListener(onClickListener);

        wifiReceiver = new WifiReceiver();
        wifiReceiver.setInformationListener(new InformationListener() {
            @Override
            public void information(String info, int type) {
                if (type == InformationListener.WIFI_CHANGE_ON) {
                    btn.setEnabled(true);
                }
                if (type == InformationListener.WIFI_CHANGE_OFF) {
                    btn.setEnabled(false);
                    if (usbCommunication != null) {
                        usbCommunication.sendDataToUSB(rcCarUtil.COMMAND_GAZ, rcCarUtil.BASE_GAS_VALUE);
                    }

                }
            }
        });


        enableControls(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        usbCommunication.onResume();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        registerReceiver(broadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        usbCommunication.onPause();
        unregisterReceiver(wifiReceiver);
        unregisterReceiver(broadcastReceiver);
        sensorManager.unregisterListener(sensorEventListener);
        locationManager.removeUpdates(locationListener);

    }

    @Override
    protected void onStop() {
        super.onStop();
        communicationThread = null;
        serverThread = null;
    }

    @Override
    public void onDestroy() {
        usbCommunication.onDestroy();
        super.onDestroy();
    }


    //-------------------------------BroadcastReceiver-------------------------------------------------------


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            int level = intent.getIntExtra("level", 0);
            if (communicationThread != null) {
                communicationThread.writeDataToOutput(rcCarUtil.COMMAND_SENSOR_AKKU_ANDROID
                        + ";" + level
                        + ";" + System.currentTimeMillis());
            }
        }
    };


    //-------------------------------Listener-------------------------------------------------------


    GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_STARTED:
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:

                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:

                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

                    break;
            }
        }
    };

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            communicationThread.writeDataToOutput(rcCarUtil.COMMAND_SENSOR_GPS
                    + ";" + location.getLongitude()
                    + ";" + location.getLatitude()
                    + ";" + location.hasAccuracy()
                    + ";" + location.getTime());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    SensorEventListener sensorEventListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (communicationThread == null) {
                return;
            }
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0];
                    gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1];
                    gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2];

                    acc[0] = event.values[0] - gravity[0];
                    acc[1] = event.values[1] - gravity[1];
                    acc[2] = event.values[2] - gravity[2];

                    communicationThread.writeDataToOutput(rcCarUtil.COMMAND_SENSOR_ACC
                            + ";" + acc[0]
                            + ";" + acc[1]
                            + ";" + acc[2]
                            + ";" + event.timestamp);
                    break;
                case Sensor.TYPE_LIGHT: {
                    communicationThread.writeDataToOutput(rcCarUtil.COMMAND_SENSOR_LIGHT
                            + ";" + event.values[0]
                            + ";" + event.timestamp);
                    break;
                }
                case Sensor.TYPE_GYROSCOPE: {
                    break;

                }
                case Sensor.TYPE_MAGNETIC_FIELD: {
                    break;

                }
            }
        }

        public void onAccuracyChanged(Sensor s, int a) {
        }
    };


    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                if (btn.getText().toString().equals(getString(R.string.startServer))) {

                    serverThread = new ServerThread(SERVERPORT);
                    serverThread.setPriority(Thread.NORM_PRIORITY - 1);
                    serverThread.setOnConnectClientListener(connectClientListener);
                    serverThread.setInformationListener(new InformationListener() {
                        @Override
                        public void information(String info, int type) {

                            switch (type) {
                                case InformationListener.INFO:
                                    break;
                                case InformationListener.ERROR:
                                case InformationListener.OUTPUT_ERROR:
                                    break;
                            }


                        }
                    });
                    serverThread.start();

                    sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
                    sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                    btn.setText(getString(R.string.stopServer));

                } else if ((btn.getText().toString().equals(getString(R.string.stopServer)))) {
                    // btn.setText(R.string.stopServer);
                    serverThread = null;
                    communicationThread = null;
                    btn.setText(R.string.startServer);

                    sensorManager.unregisterListener(sensorEventListener);
                    locationManager.removeUpdates(locationListener);

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
                    switch (type) {
                        case InformationListener.INFO:
                            break;
                        case InformationListener.ERROR:
                        case InformationListener.OUTPUT_ERROR:
                            btn.callOnClick();
                            break;
                    }

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
