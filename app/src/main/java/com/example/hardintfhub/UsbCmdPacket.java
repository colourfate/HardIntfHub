package com.example.hardintfhub;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;

class UsbCmdPacket {
    public final static int GPIO_PIN_CNT = 16;
    public final static int USB_PACKET_MAX = 64;
    public final static int USB_PACKET_MIN = 4;
    private int mIndex;
    public final byte[] mPacket;
    private final static String TAG = "USB_HOST";

    public enum Type {
        GPIO(0), PWM(1), ADC(2), SERIAL(3), I2C(4), SPI(5), CAN(6);
        private final int value;
        Type(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
    }

    public enum Dir {
        IN(0), OUT(1);
        private final int value;
        Dir(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
    }

    public enum Group {
        A(0), B(1), C(2), D(3), E(4), F(5), G(6), H(7), MUL_FUNC(8);
        private final int value;
        Group(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
    }

    public void setType(Type type) {
        mPacket[0] |= (type.getValue() << 1);
    }

    public void setDir(Dir dir) {
        mPacket[0] |= dir.getValue();
    }

    public void setGroup(Group group) {
        mPacket[1] |= (group.getValue() << 4);
    }

    public void setPin(int pin) {
        if (pin >= GPIO_PIN_CNT) {
            pin = 15;
        }
        mPacket[1] |= pin;
    }

    public void setData(final byte[] data) {
        byte dataLen;

        dataLen = (byte)data.length;
        if (data.length > USB_PACKET_MAX - 4) {
            Log.w(TAG, "data length(" + data.length + ") should < " + (USB_PACKET_MAX - 4));
            dataLen = USB_PACKET_MAX - 4;
        }

        mPacket[2] = dataLen;
        if (dataLen >= 0) System.arraycopy(data, 0, mPacket, 3, dataLen);
    }

    public int getData(byte[] data) {
        int dataLen = mPacket[2];
        if (data.length < dataLen) {
            Log.w(TAG, "data length(" + data.length + ") should >= " + dataLen);
            dataLen = data.length;
        }

        System.arraycopy(mPacket, 3, data, 0, dataLen);
        return dataLen;
    }

    public boolean comparePack(UsbCmdPacket usbCmdPacket) {
        return mPacket[0] == usbCmdPacket.mPacket[0] && mPacket[1] == usbCmdPacket.mPacket[1];
    }

    UsbCmdPacket (int packetLen) {
        if (packetLen < USB_PACKET_MIN) {
            packetLen = USB_PACKET_MIN;
        } else if (packetLen > USB_PACKET_MAX) {
            packetLen = USB_PACKET_MAX;
        }

        mPacket = new byte[packetLen];
        mIndex = 0;
    }

    public void put(byte value) {
        mPacket[mIndex++] = value;
    }
}
