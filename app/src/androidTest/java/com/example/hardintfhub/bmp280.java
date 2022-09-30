package com.example.hardintfhub;

import static com.example.intfdefine.ByteTool.s8Tos16;
import static com.example.intfdefine.ByteTool.s8Tou16;
import static com.example.intfdefine.ByteTool.s8Tou8;
import static org.junit.Assert.assertEquals;

import android.util.Log;

import com.example.intfdefine.HardGpio;
import com.example.intfdefine.HardI2C;
import com.example.intfdefine.HardSPI;
import com.example.intfdefine.SpiMessage;

import java.lang.reflect.Array;
import java.util.Arrays;

class Bmp280 {
    public static final byte BMP280_I2C_ADDR = (byte)(0x76 << 1);
    public static final byte BMP280_ID_VALUE = 0x58;
    public static final byte BMP280_CTRL_MEAS_REG = (byte)0xF4;
    public static final byte BMP280_CONFIG_REG = (byte)0xF5;
    public static final byte BMP280_ID_REG = (byte)0xD0;
    public static final byte BMP280_DIG_T1_LSB_REG = (byte)0x88;
    public static final byte BMP280_DIG_T2_LSB_REG = (byte)0x8A;
    public static final byte BMP280_DIG_T3_LSB_REG = (byte)0x8C;
    public static final byte BMP280_DIG_P1_LSB_REG = (byte)0x8E;
    public static final byte BMP280_DIG_P2_LSB_REG = (byte)0x90;
    public static final byte BMP280_DIG_P3_LSB_REG = (byte)0x92;
    public static final byte BMP280_DIG_P4_LSB_REG = (byte)0x94;
    public static final byte BMP280_DIG_P5_LSB_REG = (byte)0x96;
    public static final byte BMP280_DIG_P6_LSB_REG = (byte)0x98;
    public static final byte BMP280_DIG_P7_LSB_REG = (byte)0x9A;
    public static final byte BMP280_DIG_P8_LSB_REG = (byte)0x9C;
    public static final byte BMP280_DIG_P9_LSB_REG = (byte)0x9E;
    public static final byte BMP280_TEMP_MSB_REG = (byte)0xFA;
    public static final byte BMP280_PRESS_MSB_REG = (byte)0xF7;

    private HardI2C mI2C = null;
    private HardSPI mSPI = null;
    private HardGpio mCSB = null;
    private double mTFine;
    private final int[] mDigT = new int[3];
    private final int[] mDigP = new int[9];

    Bmp280(HardI2C i2c) {
        mI2C = i2c;
    }
    Bmp280(HardSPI spi, HardGpio csb) {
        mSPI = spi;
        mCSB = csb;
        mCSB.write(true);
    }

    private void i2cInit() {
        byte devID = mI2C.readReg(BMP280_I2C_ADDR, BMP280_ID_REG);
        assertEquals(devID, BMP280_ID_VALUE);

        mI2C.writeReg(BMP280_I2C_ADDR, BMP280_CTRL_MEAS_REG, (byte) 0xFF);
        mI2C.writeReg(BMP280_I2C_ADDR, BMP280_CONFIG_REG, (byte) 0x14);

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

    private void spiReadRegs(byte regAddr, byte[] data) {
        byte[] tx_buf = new byte[1];
        mCSB.write(false);
        tx_buf[0] = (byte)(regAddr | 0x80);
        mSPI.transfer(tx_buf, null);

        tx_buf[0] = 0;
        mSPI.transfer(tx_buf, data);
        mCSB.write(true);
    }

    private byte spiReadReg(byte regAddr) {
        byte[] rx_buf = new byte[1];

        spiReadRegs(regAddr, rx_buf);
        return rx_buf[0];
    }

    private void spiWriteReg(byte regAddr, byte value) {
        byte[] tx_buf = new byte[2];
        mCSB.write(false);
        tx_buf[0] = (byte)(regAddr & 0x7F);
        tx_buf[1] = value;
        mSPI.transfer(tx_buf, null);
        mCSB.write(true);
    }

    private void spiInit() {
        byte devID = spiReadReg(BMP280_ID_REG);
        assertEquals(BMP280_ID_VALUE, devID);

        spiWriteReg(BMP280_CTRL_MEAS_REG, (byte) 0xFF);
        spiWriteReg(BMP280_CONFIG_REG, (byte) 0x14);

        byte[] data = new byte[2];
        spiReadRegs(BMP280_DIG_T1_LSB_REG, data);
        mDigT[0] = s8Tou16(data[1], data[0]);
        spiReadRegs(BMP280_DIG_T2_LSB_REG, data);
        mDigT[1] = s8Tos16(data[1], data[0]);
        spiReadRegs(BMP280_DIG_T3_LSB_REG, data);
        mDigT[2] = s8Tos16(data[1], data[0]);

        spiReadRegs(BMP280_DIG_P1_LSB_REG, data);
        mDigP[0] = s8Tou16(data[1], data[0]);
        spiReadRegs(BMP280_DIG_P2_LSB_REG, data);
        mDigP[1] = s8Tos16(data[1], data[0]);
        spiReadRegs(BMP280_DIG_P3_LSB_REG, data);
        mDigP[2] = s8Tos16(data[1], data[0]);
        spiReadRegs(BMP280_DIG_P4_LSB_REG, data);
        mDigP[3] = s8Tos16(data[1], data[0]);
        spiReadRegs(BMP280_DIG_P5_LSB_REG, data);
        mDigP[4] = s8Tos16(data[1], data[0]);
        spiReadRegs(BMP280_DIG_P6_LSB_REG, data);
        mDigP[5] = s8Tos16(data[1], data[0]);
        spiReadRegs(BMP280_DIG_P7_LSB_REG, data);
        mDigP[6] = s8Tos16(data[1], data[0]);
        spiReadRegs(BMP280_DIG_P8_LSB_REG, data);
        mDigP[7] = s8Tos16(data[1], data[0]);
        spiReadRegs(BMP280_DIG_P9_LSB_REG, data);
        mDigP[8] = s8Tou16(data[1], data[0]);

        Log.i("bmp280", "mDigP: " + Arrays.toString(mDigP));
        Log.i("bmp280", "mDigT: " + Arrays.toString(mDigT));
    }

    public void init() {
        if (mI2C != null) {
            i2cInit();
        } else if (mSPI != null) {
            spiInit();
        }
    }

    public double getTemperature() {
        byte[] data = new byte[3];

        if (mI2C != null) {
            int readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_TEMP_MSB_REG, data);
            assertEquals(readLen, 3);
        } else if (mSPI != null) {
            spiReadRegs(BMP280_TEMP_MSB_REG, data);
        } else {
            return 0;
        }

        Log.i("bmp280", "getTemperature: " + Arrays.toString(data));
        int adcT = (s8Tou8(data[0]) << 12) | (s8Tou8(data[1]) << 4) | (s8Tou8(data[2]) >> 4);
        double var1 = (adcT / 16384.0 - mDigT[0] / 1024.0) * mDigT[1];
        double var2 = ((adcT / 131072.0 - mDigT[0] / 8192.0) * (adcT / 131072.0 - mDigT[0] / 8192.0)) * mDigT[2];
        mTFine = var1 + var2;

        return mTFine / 5120.0;
    }

    public double getPressure() {
        byte[] data = new byte[3];

        if (mI2C != null) {
            int readLen = mI2C.readRegs(BMP280_I2C_ADDR, BMP280_PRESS_MSB_REG, data);
            assertEquals(readLen, 3);
        } else if (mSPI != null) {
            spiReadRegs(BMP280_PRESS_MSB_REG, data);
        }

        Log.i("bmp280", "getPressure: " + Arrays.toString(data));
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
