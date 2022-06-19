package com.example.intfdefine;

import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;

import java.util.concurrent.ConcurrentLinkedQueue;

abstract class IntfEventListener {
    private int mUserRet = 0;
    private boolean mIsHandled = false;
    private final byte[] mIdentifier = new byte[2];

    IntfEventListener() {}

    public void setmIdentifier(byte id0, byte id1) {
        mIdentifier[0] = id0;
        mIdentifier[1] = id1;
    }

    public int getUserRet() { return mUserRet; }
    public boolean isHandled() { return mIsHandled; }

    abstract int userHandle(byte[] receivePakcet);
    public void handleIntfEvent(byte[] receivePacket) {
        if (mIdentifier[0] != receivePacket[0] || mIdentifier[1] != receivePacket[1]) {
            return;
        }
        mUserRet = userHandle(receivePacket);
        mIsHandled = true;
    }
}

class Port {
    HardIntf.Group group;
    int pin;

    public Port(HardIntf.Group group, int pin) {
        this.group = group;
        this.pin = pin;
    }
};

public class HardIntf {
    public final static int USB_PACKET_MAX = 64;
    public final static int USB_PACKET_MIN = 4;
    public final static int GPIO_PIN_CNT = 16;
    private final UsbSerialDevice mUsbSerial;
    private final String TAG = "USB_HOST";
    private final ConcurrentLinkedQueue<IntfEventListener> mListenerList;
    private Type mType;
    private Port[] mPortTab;

    public void setType(Type type) {
        this.mType = type;
    }

    protected void setPort(int index, Port port) {
        mPortTab[index] = port;
    }

    protected Port getPort(int index) {
        return mPortTab[index];
    }

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

    public enum Mode {
        CTRL(0), CFG(1), INFO(2);
        private final int value;
        Mode(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
        static public int parsePacket(byte packet) { return packet >> 1 & 0x3; }
    }

    public enum Dir {
        NONE(0), IN(0), OUT(1);
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

    protected HardIntf(UsbSerialDevice usbSerial, ConcurrentLinkedQueue<IntfEventListener> listenerList,
             int portNum) {
        this.mUsbSerial = usbSerial;
        this.mListenerList = listenerList;
        if (portNum < 0 || portNum > GPIO_PIN_CNT) {
            Log.w(TAG, "HardIntf: Not support portNum: " + portNum);
            portNum = GPIO_PIN_CNT;
        }
        mPortTab = new Port[portNum];
    }

    private byte getIdentifier_0(Mode mode, Dir dir) {
        return (byte)((mType.getValue() << 3) | (mode.getValue() << 1) | dir.getValue());
    }

    private byte getIdentifier_1(Group group, int pin) {
        return (byte)((group.getValue() << 4) | pin);
    }

    protected void config(byte[] config, Dir dir) {
        byte[] packet;
        byte dataLen = (byte)config.length;

        if (dataLen > USB_PACKET_MAX - 3) {
            Log.w(TAG, "data length(" + config.length + ") should < " + (USB_PACKET_MAX - 3));
            dataLen = USB_PACKET_MAX - 3;
        }

        packet = new byte[USB_PACKET_MIN + config.length - 1];
        packet[0] = getIdentifier_0(Mode.CFG, dir);
        packet[2] = dataLen;
        if (dataLen >= 0) System.arraycopy(config, 0, packet, 3, dataLen);

        for (Port port : mPortTab) {
            packet[1] = getIdentifier_1(port.group, port.pin);
            mUsbSerial.write(packet);
        }
    }

    protected void write(byte[] content, Group group, int pin) {
        byte[] packet;
        byte dataLen = (byte)content.length;

        if (dataLen > USB_PACKET_MAX - 3) {
            Log.w(TAG, "data length(" + content.length + ") should < " + (USB_PACKET_MAX - 3));
            dataLen = USB_PACKET_MAX - 3;
        }

        packet = new byte[USB_PACKET_MIN + content.length - 1];
        packet[0] = getIdentifier_0(Mode.CTRL, Dir.OUT);
        packet[1] = getIdentifier_1(group, pin);
        packet[2] = dataLen;
        if (dataLen >= 0) System.arraycopy(content, 0, packet, 3, dataLen);

        mUsbSerial.write(packet);
    }

    protected int read(byte[] content, Group group, int pin, IntfEventListener listener) {
        byte[] packet;
        byte dataLen = (byte)content.length;

        if (listener == null) {
            Log.w(TAG, "read: Not attach listener");
            return 0;
        }

        if (dataLen > USB_PACKET_MAX - 3) {
            Log.w(TAG, "data length(" + content.length + ") should < " + (USB_PACKET_MAX - 3));
            dataLen = USB_PACKET_MAX - 3;
        }

        packet = new byte[USB_PACKET_MIN + content.length - 1];
        packet[0] = getIdentifier_0(Mode.CTRL, Dir.IN);
        packet[1] = getIdentifier_1(group, pin);
        packet[2] = dataLen;

        listener.setmIdentifier(packet[0], packet[1]);
        mListenerList.add(listener);
        mUsbSerial.write(packet);
        while (!listener.isHandled()) {};
        mListenerList.remove(listener);

        return listener.getUserRet();
    }
}