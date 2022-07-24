package com.example.intfdefine;

import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

abstract class HardIntfEvent {
    private volatile boolean mIsHandled = false;
    private int mUserRet = 0;

    abstract int userHandle(byte[] receivePakcet);
    public void handle(byte[] receivePacket) {
        mUserRet = userHandle(receivePacket);
        mIsHandled = true;
    }

    public int getUserRet() {
        while (!mIsHandled);
        return mUserRet;
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
    private final String TAG = "USB_INTF";
    private final ConcurrentHashMap<Integer, HardIntfEvent> mEventMap;
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
        public static int parsePacket(byte packet) { return packet >> 1 & 0x3; }
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

    protected HardIntf(UsbSerialDevice usbSerial, ConcurrentHashMap<Integer, HardIntfEvent> eventMap,
             int portNum) {
        this.mUsbSerial = usbSerial;
        this.mEventMap = eventMap;
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
        return (byte)(group.getValue() << 4 | pin);
    }

    public static int getPacketId(byte[] packet) {
        return packet[1] << 8 | packet[0];
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

    protected int read(byte[] content, Group group, int pin, HardIntfEvent event) {
        byte[] packet;
        byte dataLen = (byte)content.length;

        if (event == null) {
            Log.w(TAG, "event is null");
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

        int key = getPacketId(packet);
        mEventMap.put(key, event);
        mUsbSerial.write(packet);
        int ret = event.getUserRet();
        mEventMap.remove(key);

        return ret;
    }
}