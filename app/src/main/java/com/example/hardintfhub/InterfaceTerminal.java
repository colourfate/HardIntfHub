package com.example.hardintfhub;

import android.util.Log;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class InterfaceTerminal implements Runnable {
    private final static int USB_PACKET_MAX = 64;
    private final static int GPIO_PIN_CNT = 16;
    private static final String TAG = "USB_HOST";
    Queue<Byte> mOutQueue = new ArrayBlockingQueue<Byte>(8 * USB_PACKET_MAX);
    private Thread t;

    public enum Type {
        GPIO(0), ADC(1), SERIAL(2), I2C(3), SPI(4), CAN(5), MAX(6);
        private int value;
        Type(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
    }

    public enum Dir {
        IN(0), OUT(1), MAX(3);
        private int value;
        Dir(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
    }

    public enum Group {
        A(0), B(1), C(2), D(3), E(4), F(5), G(6), H(7), MUL_FUNC(8), MAX(9);
        private int value;
        Group(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
    }

    InterfaceTerminal() {}

    private byte getCmdByte(Type type, Dir dir) {
        return (byte)((type.getValue() << 1) | dir.getValue());
    }

    private byte getGpioByte(Group group, int pin) {
        return (byte)((group.getValue() << 4) | pin);
    }

    public void gpioWrite(Group group, int pin, boolean value) {
        if (pin > GPIO_PIN_CNT) {
            Log.e(TAG, "Not support pin: " + pin);
            return;
        }

        mOutQueue.add(getCmdByte(Type.GPIO, Dir.OUT));
        mOutQueue.add(getGpioByte(group, pin));
        mOutQueue.add((byte)1);
        mOutQueue.add((byte)(value ? 1 : 0));
    }

    @Override
    public void run() {
        while (true) {
            byte[] sendBuffer = new byte[USB_PACKET_MAX];
            int i = 0;

            try {
                for (i = 0; i < USB_PACKET_MAX; i++) {
                    sendBuffer[i] = mOutQueue.remove();
                }
            } catch (Exception e) {}

            if (i == 0) {
                continue;
            }

            Log.e(TAG, "send buffer: " + Arrays.toString(sendBuffer));
        }
    }

    public void start () {
        if (t == null) {
            t = new Thread (this, "UsbSendThread");
            t.start ();
        }
    }
}
