package com.example.hardintfhub;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private static final String TAG = "USB_HOST";
    private final static int VENDOR_ID = 0x483;
    private final static int PRODUCT_ID = 0x5750;

    @Test
    public void writeGpioTest() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.hardintfhub", appContext.getPackageName());

        UsbManager usbManager = (UsbManager)appContext.getSystemService(appContext.USB_SERVICE);
        InterfaceTerminal intfTerm = new InterfaceTerminal(usbManager);

        try {
            intfTerm.connect(VENDOR_ID, PRODUCT_ID);
        } catch (Exception e) {
            fail("Usb connect failed");
        };

        int cnt = 1000;
        boolean value = true;
        while (cnt-- > 0) {
            Log.d(TAG, "start: " + cnt);
            intfTerm.gpioWrite(UsbCmdPacket.Group.C, 13, value);
            value = !value;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void readGpioTest() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.hardintfhub", appContext.getPackageName());

        UsbManager usbManager = (UsbManager)appContext.getSystemService(appContext.USB_SERVICE);
        InterfaceTerminal intfTerm = new InterfaceTerminal(usbManager);
        intfTerm.connect(VENDOR_ID, PRODUCT_ID);
        try {
            intfTerm.start();
        } catch (Exception e) {
            fail("Usb connect failed");
        };

        int cnt = 500;
        while (cnt-- > 0) {
            Log.d(TAG, "start: " + cnt);
            boolean isPress = !intfTerm.gpioRead(UsbCmdPacket.Group.A, 0);
            Log.d(TAG, "end");
            if (isPress) {
                //Log.d(TAG, "Press");
            } else {
                //Log.d(TAG, "No Press");
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //Log.d(TAG, "Next");
        }
    }
}