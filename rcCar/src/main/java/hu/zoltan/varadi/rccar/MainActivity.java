package hu.zoltan.varadi.rccar;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ftdi.j2xx.D2xxManager;

import hu.varadi.zoltan.rccar.R;
import hu.zoltan.varadi.lib.ftdi.modem.FtdiInformationListener;
import hu.zoltan.varadi.lib.ftdi.modem.FtdiUartDeviceHelper;


public class MainActivity extends Activity {

    private static final int SEEKBAR_DEFAULT_VALUE = 25;
    private static final byte KORMANY = 0X01;
    private static final byte GAZ = 0X02;

    private Button connectFunction;
    private Button sendMessage;
    private TextView statusTextView;
    private ListView listView;
    private SeekBar sbGaz;
    private SeekBar sbkorm;
    private FtdiDeviceInputAdapter ftdiDeviceInputAdapter;


    FtdiUartDeviceHelper ftdiUartDeviceHelper;
    D2xxManager ftdid2xxContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectFunction = (Button) findViewById(R.id.connectFunctionButton);
        //sendMessage = (Button) findViewById(R.id.sendmessageButton);
        statusTextView = (TextView) findViewById(R.id.statusTextView);
        listView = (ListView) findViewById(R.id.listView);
        ftdiDeviceInputAdapter = new FtdiDeviceInputAdapter(this);
        listView.setAdapter(ftdiDeviceInputAdapter);
        sbGaz = (SeekBar) findViewById(R.id.sbGaz);
        sbkorm = (SeekBar) findViewById(R.id.sbkorm);
        sbkorm.setOnSeekBarChangeListener(seekBarChangeListener);
        sbGaz.setOnSeekBarChangeListener(seekBarChangeListener);


        try {
            ftdid2xxContext = D2xxManager.getInstance(getApplicationContext());
        } catch (D2xxManager.D2xxException e) {
            e.printStackTrace();
        }
        ftdiUartDeviceHelper = new FtdiUartDeviceHelper(this, ftdid2xxContext, informationListener);

        connectFunction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ftdiUartDeviceHelper.connectFunction();

            }
        });


    }

    FtdiInformationListener informationListener = new FtdiInformationListener() {
        @Override
        public void sendMessage(String message, int type) {

            if (type == FtdiInformationListener.DEVICE_CONNECTED) {
                statusTextView.setText(message);
                connectFunction.setEnabled(false);
            } else if (type == FtdiInformationListener.NEW_DATA) {

                ftdiDeviceInputAdapter.addItem(message);

                ftdiDeviceInputAdapter.notifyDataSetChanged();
            }

        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar.getId() == R.id.sbGaz) {
                ftdiUartDeviceHelper.sendMessage(GAZ, (byte) progress);
            } else if (seekBar.getId() == R.id.sbkorm) {
                ftdiUartDeviceHelper.sendMessage(KORMANY, (byte) progress);
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


    @Override
    protected void onStop() {
        super.onStop();
        try {
            ftdiUartDeviceHelper.disconnectFunction();
        } catch (Exception e) {
            Log.e("ex", e.getMessage());
        }

    }
}
