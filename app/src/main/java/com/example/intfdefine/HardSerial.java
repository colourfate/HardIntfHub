package com.example.intfdefine;

import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HardSerial extends HardIntf {
    private BuadRate mBuadRate = BuadRate.BUAD_115200;
    private WordLen mWordLen = WordLen.LEN_8;
    private StopBit mStopBit = StopBit.BIT_1;
    private final Parity mParity = Parity.NONE; // Not support set
    private final HwCtl mHwCtl = HwCtl.NONE;    // Not support set
    private final static int TX = 0;
    private final static int RX = 1;

    public enum BuadRate {
        BUAD_1200(0), BUAD_2400(1), BUAD_4800(2), BUAD_9600(3), BUAD_19200(4),
        BUAD_38400(5), BUAD_57600(6), BUAD_115200(7), BUAD_230400(8), BUAD_460800(9),
        BUAD_921600(10), BUAD_1M(11), BUAD_2M(12), BUAD_4M(13);
        private final int value;
        BuadRate(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
        public int getPacket() { return value << 3; }
    }

    public enum WordLen {
        LEN_8(0), LEN_9(1);
        private final int value;
        WordLen(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
        public int getPacket() { return value << 7; }
    }

    public enum StopBit {
        BIT_1(0), BIT_2(1);
        private final int value;
        StopBit(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
        public int getPacket() { return value; }
    }

    public enum Parity {
        NONE(0), EVEN(1), ODD(2);
        private final int value;
        Parity(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
        public int getPacket() { return value << 1; }
    }

    public enum HwCtl {
        NONE(0), RTS(1), CTS(2), RTS_CTS(3);
        private final int value;
        HwCtl(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
        public int getPacket() { return value << 3; }
    }

    public void setBuadRate(BuadRate buadRate) {
        this.mBuadRate = buadRate;
    }

    public void setWordLen(WordLen wordLen) {
        this.mWordLen = wordLen;
    }

    public void setStopBit(StopBit stopBit) {
        this.mStopBit = stopBit;
    }

    public HardSerial(UsbSerialDevice usbSerial, ConcurrentHashMap<Integer, HardIntfEvent> eventMap) {
        super(usbSerial, eventMap, 2);
        super.setType(Type.SERIAL);
    }

    public void setTx(Group group, int pin) { super.setPort(TX, group, pin); }

    public void setRx(Group group, int pin) { super.setPort(RX, group, pin); }

    public void config() throws IOException {
        byte[] attr = new byte[2];
        attr[0] = (byte)0;        /* reserve */
        attr[0] |= (byte)(mBuadRate.getPacket());
        attr[0] |= (byte)(mWordLen.getPacket());
        attr[1] = (byte)mStopBit.getPacket();
        attr[1] |= (byte)(mParity.getPacket());
        attr[1] |= (byte)(mHwCtl.getPacket());
        super.config(attr);
    }

    public void write(byte[] content) {
        super.write(TX, content);
    }

    public int read(byte[] content) {
        return super.read(RX, content, new HardIntfEvent() {
            @Override
            public int userHandle(byte[] receivePacket) {
                int readLen = receivePacket[2];
                System.arraycopy(receivePacket, 3, content, 0, readLen);
                return readLen;
            }
        });
    }
}
