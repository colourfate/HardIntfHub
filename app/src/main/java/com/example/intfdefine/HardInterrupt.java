package com.example.intfdefine;

import com.felhr.usbserial.UsbSerialDevice;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class HardInterrupt extends HardIntf {
    private boolean mEnable = true;
    private byte mInterval = 0;
    private Mode mMode = Mode.RISE;
    private InterruptEvent mIntEvent;

    protected HardInterrupt(UsbSerialDevice usbSerial, ConcurrentHashMap<Integer, HardIntfEvent> eventMap) {
        super(usbSerial, eventMap, 1);
        super.setType(Type.INT);
    }

    public enum Mode {
        RISE(0), FALL(1);
        private final int value;
        Mode(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
        public int getPacket() { return value << 1; }
    }

    public void setPort(Group group, int pin) {
        super.setPort(0, group, pin);
    }
    public void setIntEnable(boolean enable) { mEnable = enable; }
    public void setInterval(byte interval) { mInterval = interval; };
    public void setMode(Mode mode) { mMode = mode; }

    public void config() throws IOException {
        byte[] attr = new byte[2];
        int enBit = mEnable ? 1 : 0;

        attr[0] = (byte)(mMode.getPacket() | enBit);
        attr[1] = mInterval;
        super.config(attr);
    }

    public void registerEvent(InterruptEvent event) {
        mIntEvent = event;
        super.registerEvent(0, new HardIntfEvent() {
            @Override
            protected int userHandle(byte[] receivePacket) {
                mIntEvent.callBack();
                return 0;
            }
        });
    }
}
