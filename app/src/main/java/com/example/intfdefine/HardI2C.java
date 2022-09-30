package com.example.intfdefine;

import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class HardI2C extends HardIntf {
    private static final int SCL = 0;
    private static final int SDA = 1;
    private Frequency mFreq = Frequency.F_400K;

    private class I2cCtrl {
        public byte mAddr;
        public boolean mRepeated = false;
        public byte mData[];

        public byte[] getPacket() {
            byte packet[] = new byte[mData.length + 2];
            packet[0] = mAddr;
            packet[1] = (byte)(mRepeated ? 1 : 0);
            System.arraycopy(mData, 0, packet, 2, mData.length);
            return packet;
        }

        public void putPacket(byte[] packet) {
            mAddr = packet[0];
            mRepeated = (packet[1] == 1);
            System.arraycopy(packet, 2, mData, 0, packet.length - 2);
        }
    }

    protected HardI2C(UsbSerialDevice usbSerial, ConcurrentHashMap<Integer, HardIntfEvent> eventMap) {
        super(usbSerial, eventMap, 2);
        super.setType(Type.I2C);
    }

    public enum Frequency {
        F_100K(0), F_400K(1), F_1M(2), F_3D2M(3);
        private final int value;
        Frequency(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
        public int getPacket() { return value; }
    }

    public void setFreq(Frequency freq) { mFreq = freq; }
    public void setSCL(Group group, int pin) { super.setPort(SCL, group, pin); }
    public void setSDA(Group group, int pin) { super.setPort(SDA, group, pin); }

    public void config() throws IOException {
        byte[] attr = new byte[1];
        attr[0] = (byte)mFreq.getPacket();
        super.config(attr);
    }

    public void write(byte addr, byte data[]) {
        I2cCtrl ctrl = new I2cCtrl();
        ctrl.mAddr = addr;
        ctrl.mData = data;
        ctrl.mRepeated = false;

        super.write(SDA, ctrl.getPacket());
    }

    public int read(byte addr, byte data[]) {
        I2cCtrl ctrl = new I2cCtrl();
        ctrl.mAddr = addr;
        ctrl.mData = data;
        ctrl.mRepeated = false;

        return super.read(SDA, ctrl.getPacket(), new HardIntfEvent() {
            @Override
            public int userHandle(byte[] receivePacket) {
                int readLen = receivePacket[PACKAGE_DATA_LEN];
                ctrl.putPacket(Arrays.copyOfRange(receivePacket, PACKAGE_DATA, PACKAGE_DATA + readLen));
                return readLen - 2;
            }
        });
    }

    public void writeRegs(byte i2cAddr, byte regAddr, byte data[]) {
        byte[] newData = new byte[data.length + 1];
        newData[0] = regAddr;
        System.arraycopy(data, 0, newData, 1, data.length);
        write(i2cAddr, newData);
    }

    public void writeReg(byte i2cAddr, byte regAddr, byte value) {
        byte[] data = new byte[1];

        data[0] = value;
        writeRegs(i2cAddr, regAddr, data);
    }

    /*
    public void writeRegBits(byte i2cAddr, byte regAddr, int bit, int len, byte val) {
        byte[] data = new byte[1];
        readReg(i2cAddr, regAddr, data);
        Log.i(TAG, "read: 0x" + Integer.toHexString(data[0]));

        byte mask = (byte)(((1 << len) - 1) << bit);
        val = (byte)((val << bit) & mask);
        data[0] &= ~mask;
        data[0] |= val;

        Log.i(TAG, "write: 0x" + Integer.toHexString(data[0]));
        writeReg(i2cAddr, regAddr, data);

        readReg(i2cAddr, regAddr, data);
        Log.i(TAG, "read: 0x" + Integer.toHexString(data[0]));
    }
    */

    /* TODO: 增加复合操作 */
    public int readRegs(byte i2cAddr, byte regAddr, byte data[]) {
        byte[] addr = new byte[1];

        addr[0] = regAddr;
        write(i2cAddr, addr);
        return read(i2cAddr, data);
    }

    public byte readReg(byte i2cAddr, byte regAddr) {
        byte[] data = new byte[1];
        readRegs(i2cAddr, regAddr, data);
        return data[0];
    }
}
