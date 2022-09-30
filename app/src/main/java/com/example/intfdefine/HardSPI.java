package com.example.intfdefine;

import com.felhr.usbserial.UsbSerialDevice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InvalidPropertiesFormatException;
import java.util.concurrent.ConcurrentHashMap;

// TODO: 增加设置CSB功能
public class HardSPI extends HardIntf {
    private static final int MOSI = 0;
    private static final int MISO = 1;
    private static final int SCK = 2;
    private static final int SPI_MAX_FREQ = 18000;
    private static final int SPI_MIN_FREQ = 1;
    private static final int SPI_MAX_BITS = 32;
    private static final int SPI_MIN_BITS = 4;
    private int mFreq = 1000;   /* 1MHz */
    private int mBits = 8;
    private int mPority = 0;
    private int mPhase = 0;
    private Group mCsbGroup = Group.MAX;    /* not support */
    private int mCsbPin = 16;               /* not support */

    protected HardSPI(UsbSerialDevice usbSerial, ConcurrentHashMap<Integer, HardIntfEvent> eventMap) {
        super(usbSerial, eventMap, 3);
        super.setType(Type.SPI);
    }

    public void setFreq(int freq) {
        if (freq < SPI_MIN_FREQ) {
            freq = SPI_MIN_FREQ;
        } else if (freq > SPI_MAX_FREQ) {
            freq = SPI_MAX_FREQ;
        }
        mFreq = freq;
    }
    public void setBits(int bits) {
        if (bits < SPI_MIN_BITS) {
            bits = SPI_MIN_BITS;
        } else if (bits > SPI_MAX_BITS) {
            bits = SPI_MAX_BITS;
        }
        mBits = bits;
    }
    public void setMode(boolean pority, boolean phase) {
        mPority = pority ? 1 : 0;
        mPhase = phase ? 1 : 0;
    }
    public void setMOSI(Group group, int pin) { super.setPort(MOSI, group, pin); }
    public void setMISO(Group group, int pin) { super.setPort(MISO, group, pin); }
    public void setSCK(Group group, int pin) { super.setPort(SCK, group, pin); }

    public void config() throws IOException {
        byte[] attr = new byte[4];

        attr[0] = (byte)mFreq;
        attr[1] = (byte)(mFreq << 8);
        attr[2] = (byte)(mBits | mPority << 6 | mPhase << 7);
        attr[3] = (byte)(mCsbGroup.getPacket() | mCsbPin);
        super.config(attr);
    }

    public void transfer(SpiMessage spiMsg) {
        super.read(MISO, spiMsg.getArray(), new HardIntfEvent() {
            @Override
            public int userHandle(byte[] receivePacket) {
                spiMsg.setArray(receivePacket);
                return 0;
            }
        });
    }

    public void transfer(byte[] tx_buf, byte[] rx_buf) {
        int tx_len = tx_buf == null ? 0 : tx_buf.length;
        int rx_len = rx_buf == null ? 0 : rx_buf.length;

        byte[] msg = new byte[tx_len + rx_len + 2];
        msg[0] = (byte) tx_len;
        msg[1] = (byte) rx_len;
        if (tx_len != 0) {
            System.arraycopy(tx_buf, 0, msg, 2, tx_len);
        }
        super.read(MISO, msg, new HardIntfEvent() {
            @Override
            protected int userHandle(byte[] receivePacket) {
                if (rx_len != 0) {
                    System.arraycopy(receivePacket, PACKAGE_DATA + 2 + tx_len, rx_buf, 0, rx_len);
                }
                return 0;
            }
        });
    }
}
