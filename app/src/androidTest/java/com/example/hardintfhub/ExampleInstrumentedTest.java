package com.example.hardintfhub;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.intfdefine.HardGpio;
import com.example.intfdefine.HardIntf;
import com.example.intfdefine.InterfaceFactory;

import java.util.concurrent.TimeUnit;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    /*
   private static final String TAG = "USB_TEST";
   private final static int VENDOR_ID = 0x483;
   private final static int PRODUCT_ID = 0x5740;
   private Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
   private InterfaceFactory mIntfFactory;
   private boolean mThreadRun = true;

   private void blackWait(int n) {
       for (int i = 0; i < n; i++) {
           for (int j = 0; j < 1000000; j++);
       }
   }

   @Before
   public void setUp() {
       UsbManager usbManager = (UsbManager)appContext.getSystemService(appContext.USB_SERVICE);
       mIntfFactory = new InterfaceFactory(usbManager);
       mIntfFactory.connect(VENDOR_ID, PRODUCT_ID);
       try {
           mIntfFactory.start();
       } catch (Exception e) {
           e.printStackTrace();
       };
   }

   @Test
    public void writeGpioTest() throws InterruptedException {
        assertEquals("com.example.hardintfhub", appContext.getPackageName());

        HardGpio PC13 = (HardGpio) mIntfFactory.createHardIntf(InterfaceFactory.IntfType.GPIO);
        PC13.setPort(HardIntf.Group.C, 13);
        PC13.setType(HardIntf.Type.GPIO);
        PC13.setDir(HardIntf.Dir.OUT);
        PC13.config();

        boolean value = true;
        Log.i(TAG, "start");
        for (int i = 0; i < 1000; i++) {
            Log.i(TAG, "writeGpioTest: ");
            PC13.write(value);
            value = !value;
            blackWait(10000);
        }
        Log.i(TAG, "end");
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

     */
}