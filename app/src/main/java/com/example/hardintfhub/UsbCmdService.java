package com.example.hardintfhub;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.intfdefine.HardAdc;
import com.example.intfdefine.HardGpio;
import com.example.intfdefine.HardIntf;
import com.example.intfdefine.HardPwm;
import com.example.intfdefine.HardSerial;
import com.example.intfdefine.InterfaceFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class UsbCmdService extends Service {
    private final static String TAG = "USB_HOST";
    private final static int VENDOR_ID = 0x1f00;
    private final static int PRODUCT_ID = 0x2012;
    private HardGpio PC13, PA0;
    private HardPwm Pwm1, Pwm2, Pwm3, Pwm4, Pwm5, Pwm6;
    private HardSerial Serial2;
    private HardAdc adc1;
    private final HashMap<Integer, UsbEventIntf> mEventMap = new HashMap<Integer, UsbEventIntf>();
    private final IBinder mUsbBinder = new UsbCmdBinder();
    private boolean mReadThreadRun = true;

    public static final int BUTT_CHANGE_EVENT = 0;
    public static final int STR_READ_EVENT = 1;

    private class usbReadThread extends Thread {
        private byte mLastState = 1;
        private int pwm_value = 0;

        private void callEntry(int eventType, byte[] param) {
            UsbEventIntf entry = mEventMap.get(eventType);
            if (entry != null) {
                entry.callBack(param);
            }
        }

        @Override
        public void run() {
            int duty_cycle = 0;
            while (mReadThreadRun) {
                byte[] value = new byte[1];
                int adc_value = adc1.read();
                Log.e(TAG, "adc read: 0x" + Integer.toHexString(adc_value));

                Pwm1.write(duty_cycle++);
                duty_cycle = duty_cycle > 1000 ? 0 : duty_cycle;
                /*
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
                */

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

        adc1 = (HardAdc) intfFactory.createHardIntf(InterfaceFactory.IntfType.ADC);
        adc1.setPort(HardIntf.Group.A, 6);

        Serial2 = (HardSerial) intfFactory.createHardIntf(InterfaceFactory.IntfType.Serial);
        Serial2.setTx(HardIntf.Group.A, 2);
        Serial2.setRx(HardIntf.Group.A, 3);
        Serial2.setBuadRate(HardSerial.BuadRate.BUAD_115200);

        Pwm1 = (HardPwm) intfFactory.createHardIntf(InterfaceFactory.IntfType.PWM);
        Pwm1.setPort(HardIntf.Group.A, 8);
        Pwm1.setPwmFreq(5000);

        Pwm2 = (HardPwm) intfFactory.createHardIntf(InterfaceFactory.IntfType.PWM);
        Pwm2.setPort(HardIntf.Group.A, 9);
        Pwm2.setPwmFreq(1000);

        Pwm3 = (HardPwm) intfFactory.createHardIntf(InterfaceFactory.IntfType.PWM);
        Pwm3.setPort(HardIntf.Group.C, 6);
        Pwm3.setPwmFreq(20000);

        Pwm4 = (HardPwm) intfFactory.createHardIntf(InterfaceFactory.IntfType.PWM);
        Pwm4.setPort(HardIntf.Group.C, 7);
        Pwm4.setPwmFreq(20000);

        Pwm5 = (HardPwm) intfFactory.createHardIntf(InterfaceFactory.IntfType.PWM);
        Pwm5.setPort(HardIntf.Group.C, 8);
        Pwm5.setPwmFreq(20000);

        Pwm6 = (HardPwm) intfFactory.createHardIntf(InterfaceFactory.IntfType.PWM);
        Pwm6.setPort(HardIntf.Group.C, 9);
        Pwm6.setPwmFreq(20000);

        try {
            //PC13.config();
            //PA0.config();
            //Serial2.config();
            adc1.config();
            Pwm1.config();
            Pwm2.config();
            Pwm3.config();
            Pwm4.config();
            Pwm5.config();
            Pwm6.config();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: 移动到测试用例
        /*
        HardGpio PA13 = (HardGpio) intfFactory.createHardIntf(InterfaceFactory.IntfType.GPIO);
        PA13.setPort(HardIntf.Group.A, 13);
        try {
            PA13.config();
        } catch (IOException e) {
            Log.e(TAG, "Catch PA13 config exception");
        }
        */

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