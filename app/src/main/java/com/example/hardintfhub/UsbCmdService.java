package com.example.hardintfhub;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.intfdefine.HardGpio;
import com.example.intfdefine.HardIntf;
import com.example.intfdefine.HardSerial;
import com.example.intfdefine.InterfaceFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

interface UsbEventIntf {
    public void callBack(byte[] data);
}

public class UsbCmdService extends Service {
    private final static String TAG = "USB_HOST";
    private final static int VENDOR_ID = 0x1f00;
    private final static int PRODUCT_ID = 0x2012;
    private HardGpio PC13, PA0;
    private HardSerial Serial2;
    private final HashMap<Integer, UsbEventIntf> mEventMap = new HashMap<Integer, UsbEventIntf>();
    private final IBinder mUsbBinder = new UsbCmdBinder();
    private boolean mReadThreadRun = true;

    public static final int BUTT_CHANGE_EVENT = 0;
    public static final int STR_READ_EVENT = 1;

    private class usbReadThread extends Thread {
        private byte mLastState = 1;

        private void callEntry(int eventType, byte[] param) {
            UsbEventIntf entry = mEventMap.get(eventType);
            if (entry != null) {
                entry.callBack(param);
            }
        }

        @Override
        public void run() {
            while (mReadThreadRun) {
                byte[] value = new byte[1];

                value[0] = PA0.read();
                if (mLastState != value[0]) {
                    callEntry(BUTT_CHANGE_EVENT, value);
                    mLastState = value[0];
                }

                byte[] buf = new byte[10];
                int readLen = Serial2.read(buf);
                if (readLen > 0) {
                    callEntry(STR_READ_EVENT, Arrays.copyOf(buf, readLen));
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Log.e(TAG, "thread sleep interrupt");
                }
            }
        }
    }

    public class UsbCmdBinder extends Binder {
        UsbCmdService getService(){
            return UsbCmdService.this;
        }
    }

    public UsbCmdService() {
    }

    /* Service onCreate is called after Activity onCreate */
    @Override
    public void onCreate() {
        super.onCreate();

        UsbManager usbManager = (UsbManager)getSystemService(USB_SERVICE);
        InterfaceFactory intfFactory = new InterfaceFactory(usbManager);
        intfFactory.connect(VENDOR_ID, PRODUCT_ID);

        try {
            intfFactory.start();
        } catch (Exception e) {
            Log.e(TAG, "Usb start failed\n");
            return;
        };

        PC13 = (HardGpio) intfFactory.createHardIntf(InterfaceFactory.IntfType.GPIO);
        PC13.setPort(HardIntf.Group.C, 13);

        PA0 = (HardGpio) intfFactory.createHardIntf(InterfaceFactory.IntfType.GPIO);
        PA0.setPort(HardIntf.Group.A, 0);

        Serial2 = (HardSerial) intfFactory.createHardIntf(InterfaceFactory.IntfType.Serial);
        Serial2.setTx(HardIntf.Group.A, 2);
        Serial2.setRx(HardIntf.Group.A, 3);
        Serial2.setBuadRate(HardSerial.BuadRate.BUAD_115200);

        try {
            PC13.config();
            PA0.config();
            Serial2.config();
        } catch (IOException e) {
            e.printStackTrace();
        }

        usbReadThread readThread = new usbReadThread();
        readThread.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mUsbBinder;
    }

    @Override
    public void onDestroy() {
        mReadThreadRun = false;
        super.onDestroy();
    }

    public void setLedState(boolean on) {
        PC13.write(!on);
    }

    public void sendString(String str) { Serial2.write(str.getBytes()); }

    public void registerEvent(int type, UsbEventIntf entry) {
        mEventMap.put(type, entry);
    }
}