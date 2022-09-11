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
import com.example.intfdefine.InterfaceFactory;

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

    static int s8Tou8(byte x) {
        return Byte.toUnsignedInt(x);
    }

    static int s8Tou16 (byte msb, byte lsb) {
        return Byte.toUnsignedInt(msb) << 8 | Byte.toUnsignedInt(lsb);
    }

    static int s8Tos16(byte msb, byte lsb) {
        return (short)s8Tou16(msb, lsb);
    }

    /* SCL: PB6, SDA: PB7, connect i2c to BMP280 */
    private class Bmp280 {
        final byte BMP280_I2C_ADDR = (byte)(0x76 << 1);
        final byte BMP280_ID_VALUE = 0x58;
        final byte BMP280_CTRL_MEAS_REG = (byte)0xF4;
        final byte BMP280_CONFIG_REG = (byte)0xF5;
        final byte BMP280_ID_REG = (byte)0xD0;
        final byte BMP280_DIG_T1_LSB_REG = (byte)0x88;
        final byte BMP280_DIG_T2_LSB_REG = (byte)0x8A;
        final byte BMP280_DIG_T3_LSB_REG = (byte)0x8C;
        final byte BMP280_DIG_P1_LSB_REG = (byte)0x8E;
        final byte BMP280_DIG_P2_LSB_REG = (byte)0x90;
        final byte BMP280_DIG_P3_LSB_REG = (byte)0x92;
        final byte BMP280_DIG_P4_LSB_REG = (byte)0x94;
        final byte BMP280_DIG_P5_LSB_REG = (byte)0x96;
        final byte BMP280_DIG_P6_LSB_REG = (byte)0x98;
        final byte BMP280_DIG_P7_LSB_REG = (byte)0x9A;
        final byte BMP280_DIG_P8_LSB_REG = (byte)0x9C;
        final byte BMP280_DIG_P9_LSB_REG = (byte)0x9E;
        final byte BMP280_TEMP_MSB_REG = (byte)0xFA;
        final byte BMP280_PRESS_MSB_REG = (byte)0xF7;

        HardI2C mI2C;
        double mTFine;
        int[] mDigT = new int[3];
        int[] mDigP = new int[9];

        Bmp280(HardI2C i2c) {
            mI2C = i2c;
        }

        void init() {
            byte devID = mI2C.readReg(BMP280_I2C_ADDR, BMP280_ID_REG);
            assertEquals(devID, BMP280_ID_VALUE);

            mI2C.writeReg(BMP280_I2C_ADDR, BMP280_CTRL_MEAS_REG, (byte)0xFF);
            mI2C.writeReg(BMP280_I2C_ADDR, BMP280_CONFIG_REG, (byte)0x14);

            byte[] data = new byte[2];
            int readLen;
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_T1_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigT[0] = s8Tou16(data[1], data[0]);
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_T2_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigT[1] = s8Tos16(data[1], data[0]);
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_T3_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigT[2] = s8Tos16(data[1], data[0]);

            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_P1_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigP[0] = s8Tou16(data[1], data[0]);
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_P2_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigP[1] = s8Tos16(data[1], data[0]);
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_P3_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigP[2] = s8Tos16(data[1], data[0]);
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_P4_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigP[3] = s8Tos16(data[1], data[0]);
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_P5_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigP[4] = s8Tos16(data[1], data[0]);
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_P6_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigP[5] = s8Tos16(data[1], data[0]);
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_P7_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigP[6] = s8Tos16(data[1], data[0]);
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_P8_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigP[7] = s8Tos16(data[1], data[0]);
            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_DIG_P9_LSB_REG, data);
            assertEquals(readLen, 2);
            mDigP[8] = s8Tou16(data[1], data[0]);
        }

        double getTemperature() {
            int readLen;
            byte[] data = new byte[3];

            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_TEMP_MSB_REG, data);
            assertEquals(readLen, 3);

            int adcT = (s8Tou8(data[0]) << 12) | (s8Tou8(data[1]) << 4) | (s8Tou8(data[2]) >> 4);
            double var1 = (adcT / 16384.0 - mDigT[0] / 1024.0) * mDigT[1];
            double var2 = ((adcT / 131072.0 - mDigT[0] / 8192.0) * (adcT / 131072.0 - mDigT[0] / 8192.0)) * mDigT[2];
            mTFine = var1 + var2;

            return mTFine / 5120.0;
        }

        double getPressure() {
            int readLen;
            byte[] data = new byte[3];

            readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_PRESS_MSB_REG, data);
            assertEquals(readLen, 3);

            int adcP = (s8Tou8(data[0]) << 12) | (s8Tou8(data[1]) << 4) | (s8Tou8(data[2]) >> 4);
            double var1 = (mTFine / 2.0) - 64000.0;
            double var2 = var1 * var1 * mDigP[5] / 32768.0;
            var2 = var2 + var1 * mDigP[4] * 2.0;
            var2 = (var2 / 4.0) + (mDigP[3] * 65536.0);
            var1 = (mDigP[2] * var1 * var1 / 524288.0 + mDigP[1] * var1) / 524288.0;
            var1 = (1.0 + var1 / 32768.0) * mDigP[0];
            double pressure = 1048576.0 - adcP;
            pressure = (pressure - (var2 / 4096.0)) * 6250.0 / var1;
            var1 = mDigP[8] * pressure * pressure / 2147483648.0;
            var2 = pressure * mDigP[7] / 32768.0;
            pressure = pressure + (var1 + var2 + mDigP[6]) / 16.0;

            return pressure;
        }
    }

    @Test
    public void i2cIOTest_2() throws InterruptedException {
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
}