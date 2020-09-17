package com.example.namecardexchange;

import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;


import static com.example.namecardexchange.MainActivity.TAG;
import static com.example.namecardexchange.MainActivity.data_list;
import static com.example.namecardexchange.MainActivity.list_device;
import static com.example.namecardexchange.MainActivity.mBluetoothLeScanner;
import static com.example.namecardexchange.MainActivity.num_list;
import static com.example.namecardexchange.MainActivity.peripheralTextView;
import static com.example.namecardexchange.MainActivity.startScanningButton;
import static com.example.namecardexchange.MainActivity.stopScanningButton;

import static com.example.namecardexchange.Service_scan_function.leScanCallback;


public class Service_Scan extends Service {

    static int stop_int;
    static Handler handler;
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Service_Scan() {
        Log.e(TAG,"Service_Scan start");

        startScanning();
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
                stopSelf();
            }
        });
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void onClick(View v) {
                startScanning();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startScanning() {
        stop_int = 0;

        Log.e(TAG, "start scanning");

        list_device.clear();
        num_list.clear();
        data_list.clear();

        peripheralTextView.setText("");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);

        byte[] data_mask = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        ScanFilter Mau_filter_legacy = new ScanFilter.Builder().setManufacturerData(0xffff, data_mask, data_mask).build();

        ArrayList<ScanFilter> filters = new ArrayList<>();
        filters.add(Mau_filter_legacy);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setLegacy(false)
                .build();
        mBluetoothLeScanner.startScan(filters, settings, leScanCallback);

        handler= new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                int num_finish = 0;
                for (int count = 0; count < data_list.size(); count++) {
                    if (data_list.get(count).contains("finish")) {
                        num_finish = num_finish + 1;
                    }
                }
                if (data_list.size() == num_finish) {
                    Log.e(TAG,"the scan is over");
                    stopScanning();
                } else {
                    show_dialog();
                    Log.e(TAG,"keep scanning");
                }
            }
        }, 30*1000);   //30 seconds 自動結束
    }

    public static void stopScanning () {
            Log.e(TAG, "stopping scanning");

            peripheralTextView.setText("Stopped Scanning");
            startScanningButton.setVisibility(View.VISIBLE);
            stopScanningButton.setVisibility(View.INVISIBLE);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(leScanCallback);
                }
            });
            handler.removeCallbacksAndMessages(null);

        }

        private void show_dialog(){
            AlertDialog dialog = new AlertDialog.Builder(getApplicationContext()).setTitle("Warning")
                    .setMessage("There are some business card have not received completely!")
                    .setCancelable(false)
                    .setPositiveButton("Keep scanning", new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        public void onClick(DialogInterface dialog, int id) {
                            startScanning();
                        }
                    })
                    .setNegativeButton("End scanning", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            stopScanning();
                            stopSelf();
                        }
                    })

                    .create();

            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams attributes = window.getAttributes();
                if (attributes != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        attributes.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                    } else {
                        attributes.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                    }
                }
                window.setAttributes(attributes);
            }
            dialog.show();
        }

}
