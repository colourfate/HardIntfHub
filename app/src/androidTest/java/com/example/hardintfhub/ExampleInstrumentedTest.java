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
import static org.junit.Assert.assertEquals;

import com.example.intfdefine.HardGpio;
import com.example.intfdefine.HardI2C;
import com.example.intfdefine.HardIntf;
import com.example.intfdefine.HardSPI;
import com.example.intfdefine.InterfaceFactory;
import com.example.intfdefine.SpiMessage;

import java.io.IOException;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private static final String TAG = "USB_TEST";
    private final static int VENDOR_ID = 0x1f00;
    private final static int PRODUCT_ID = 0x2012;
    private Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private InterfaceFactory mIntfFactory;

    @Before
    public void setUp() {
        assertEquals("com.example.hardintfhub", appContext.getPackageName());

        UsbManager usbManager = (UsbManager)appContext.getSystemService(appContext.USB_SERVICE);
        mIntfFactory = new InterfaceFactory(usbManager);
        mIntfFactory.connect(VENDOR_ID, PRODUCT_ID);
        try {
           mIntfFactory.start();
        } catch (Exception e) {
           e.printStackTrace();
        };
    }

    /* SCL: PB6, SDA: PB7, connect i2c to MPU9250 */
    @Test
    public void i2cIOTest_1() throws InterruptedException {
        final byte WHO_AM_I_REG = 0x75;
        final byte DEVICE_ID = 0x71;
        final byte I2C_ADDR = (byte)(0x68 << 1);

        HardI2C i2c = (HardI2C) mIntfFactory.createHardIntf(InterfaceFactory.IntfType.I2C);
        i2c.setSCL(HardIntf.Group.B, 6);
        i2c.setSDA(HardIntf.Group.B, 7);
        i2c.setFreq(HardI2C.Frequency.F_400K);
        try {
            i2c.config();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final byte id = i2c.readReg(I2C_ADDR, WHO_AM_I_REG);
        assertEquals(id, DEVICE_ID);
    }

    /* SCL: PB6, SDA: PB7, connect i2c to BMP280 */
    @Test
    public void i2cIOTest_2() {
        HardI2C i2c = (HardI2C) mIntfFactory.createHardIntf(InterfaceFactory.IntfType.I2C);
        i2c.setSCL(HardIntf.Group.B, 6);
        i2c.setSDA(HardIntf.Group.B, 7);
        i2c.setFreq(HardI2C.Frequency.F_400K);
        try {
            i2c.config();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bmp280 bmp280 = new Bmp280(i2c);
        bmp280.init();
        for (int i = 0; i < 100; i++) {
            double temperature = bmp280.getTemperature();
            Log.i(TAG, "get current Temperature: " + temperature + "degrees");
            assertTrue(temperature > 10);
            assertTrue(temperature < 45);

            double pressure = bmp280.getPressure();
            Log.i(TAG, "get current Pressure: " + pressure + "Pa");
            assertTrue(pressure > 75000);
            assertTrue(pressure < 125000);
        }
    }

    /* MOSI: A7, MISO: A6, SCK: A5, CSB: A4, connect to BMP280 */
    @Test
    public void spiIOTest_1() {
        HardSPI spi = (HardSPI) mIntfFactory.createHardIntf(InterfaceFactory.IntfType.SPI);
        spi.setMOSI(HardIntf.Group.A, 7);
        spi.setMISO(HardIntf.Group.A, 6);
        spi.setSCK(HardIntf.Group.A, 5);

        try {
            spi.config();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] tx_buf = new byte[1];
        byte[] rx_buf = new byte[1];
        tx_buf[0] = Bmp280.BMP280_ID_REG | 0x80;
        spi.transfer(tx_buf, rx_buf);
        tx_buf[0] = 0;


        assertEquals(Bmp280.BMP280_ID_VALUE, rx2[0]);
    }

    /* MOSI: A7, MISO: A6, SCK: A5, CSB: A4, connect to BMP280 */
    @Test
    public void spiIOTest_2() {
        HardSPI spi = (HardSPI) mIntfFactory.createHardIntf(InterfaceFactory.IntfType.SPI);
        spi.setMOSI(HardIntf.Group.A, 7);
        spi.setMISO(HardIntf.Group.A, 6);
        spi.setSCK(HardIntf.Group.A, 5);
        spi.setCSB(HardIntf.Group.A, 4);

        try {
            spi.config();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bmp280 bmp280 = new Bmp280(spi);
        bmp280.init();
        for (int i = 0; i < 1000; i++) {
            double temperature = bmp280.getTemperature();
            Log.i(TAG, "get current Temperature: " + temperature + " degrees");
            assertTrue(temperature > 10);
            assertTrue(temperature < 45);

            double pressure = bmp280.getPressure();
            Log.i(TAG, "get current Pressure: " + pressure + " Pa");
            assertTrue(pressure > 75000);
            assertTrue(pressure < 125000);
        }
    }
}