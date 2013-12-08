package hu.uniobuda.nik.hc4dgv.server;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import hu.uniobuda.nik.hc4dgv.listener.InformationListener;
import hu.uniobuda.nik.hc4dgv.util.rcCarUtil;
import hu.varadi.zoltan.rccar.R;

/**
 * Created by Zoltan Varadi on 2013.12.06..
 */
public class USBCommunication implements Runnable {

    private static final String ACTION_USB_PERMISSION = "com.example.usbteszt1.USB_PERMISSION";
    private static final String TAG = "USBCommunication";

    private boolean permissionRequestPending;
    private PendingIntent pendingIntent;
    private UsbManager usbManager;
    private UsbAccessory mAccessory;
    private ParcelFileDescriptor parcelFileDescriptor;
    private FileInputStream fileInputStream;
    private FileOutputStream fileOutputStream;
    private Activity activity;
    private InformationListener informationListener;


    public USBCommunication(Activity activity) {
        this.activity = activity;
        usbManager = (UsbManager) this.activity.getSystemService(Context.USB_SERVICE);

        pendingIntent = PendingIntent.getBroadcast(this.activity, 0, new Intent(ACTION_USB_PERMISSION), 0);


        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        this.activity.registerReceiver(usbReceiver, filter);

        if (this.activity.getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) this.activity.getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }

    }

    //usb eszkozok lekerdezese es a nulladik megnyitasa
    private void openAccessory(UsbAccessory accessory) {
        parcelFileDescriptor = usbManager.openAccessory(accessory);

        if (parcelFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = parcelFileDescriptor.getFileDescriptor();

            fileInputStream = new FileInputStream(fd);
            fileOutputStream = new FileOutputStream(fd);

            // communication thread start
            Thread thread = new Thread(null, this, "DemoKit");
            thread.start();
            sendDataToUSB(rcCarUtil.COMMAND_GAZ, rcCarUtil.BASE_GAS_VALUE);
            Log.d(TAG, "acc opened");
            if (informationListener != null) {

                informationListener.information(R.string.accessoryOpen, InformationListener.INFO);
            }

        } else {
            Log.d(TAG, "acc open fail");
            if (informationListener != null) {
                informationListener.information(R.string.accessoryOpenFail, InformationListener.ERROR);
            }
        }
    }

    //megnyitott usb kapcsolat lezarasa, de elott meg kuldunk egy utolsot rajta
    private void closeAccessory() {
        try {
            if (parcelFileDescriptor != null) {
                sendDataToUSB(rcCarUtil.COMMAND_GAZ, rcCarUtil.BASE_GAS_VALUE);
                parcelFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            parcelFileDescriptor = null;
            mAccessory = null;
        }
    }

    //usb-be iras
    public void sendDataToUSB(byte command, byte value) {
        byte[] buffer = new byte[2];
        buffer[0] = command;
        buffer[1] = value;
        if (fileOutputStream != null) {
            try {
                fileOutputStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "write failed", e);
                if (informationListener != null) {
                    informationListener.information(R.string.usbWriteError, InformationListener.OUTPUT_ERROR);
                }
            }
        }
    }

    //ha csatalkoztattak egy usb eszkozt akkor arrol jo ha jon ertesites
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

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
                        if (informationListener != null) {
                            informationListener.information("permission denied for accessory", InformationListener.ERROR);
                        }
                    }
                    permissionRequestPending = false;
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
                ret = fileInputStream.read(buffer);
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


    public void onResume() {
        if (fileInputStream != null && fileOutputStream != null) {
            return;
        }

        UsbAccessory[] accessories = usbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (usbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                synchronized (usbReceiver) {
                    if (!permissionRequestPending) {
                        usbManager.requestPermission(accessory, pendingIntent);
                        permissionRequestPending = true;
                    }
                }
            }
        } else {
            Log.d(TAG, "mAccessory is null1");
            if (informationListener != null) {

                informationListener.information(R.string.accessoryEmpty, InformationListener.ERROR);
            }
        }
    }


    public void onPause() {
        closeAccessory();
    }

    public void onDestroy() {
        this.activity.unregisterReceiver(usbReceiver);

    }

    public void setInformationListener(InformationListener i) {
        this.informationListener = i;
    }

    public void removeInformationListener() {
        this.informationListener = null;
    }
}
