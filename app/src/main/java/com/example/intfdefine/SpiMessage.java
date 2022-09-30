package com.example.intfdefine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class SpiMessage {
    private final ArrayList<Byte> mMsg = new ArrayList<Byte>();
    private final HashMap<Integer, byte[]> mUserBufMap = new HashMap<Integer, byte[]>();

    public void addBuf(byte[] tx_buf, byte[] rx_buf) {
        int tx_len = tx_buf == null ? 0 : tx_buf.length;
        int rx_len = rx_buf == null ? 0 : rx_buf.length;

        mUserBufMap.put(mMsg.size(), rx_buf);
        mMsg.add((byte)tx_len);
        mMsg.add((byte)rx_len);

        if (tx_buf != null) {
            for (byte data : tx_buf) {
                mMsg.add(data);
            }
        }
        if (rx_buf != null) {
            for (byte data : rx_buf) {
                mMsg.add((byte) 0);
            }
        }
    }

    protected byte[] getArray() {
        byte[] arr = new byte[mMsg.size()];

        for (int i = 0; i < mMsg.size(); i++) {
            arr[i] = mMsg.get(i);
        }

        return arr;
    }

    protected void setArray(byte[] arr) {
        for (Integer pos : mUserBufMap.keySet()) {
            byte tx_len = arr[pos];
            byte rx_len = arr[pos + 1];
            byte[] userBuf = mUserBufMap.get(pos);
            if (userBuf != null && rx_len != 0) {
                System.arraycopy(arr, pos + 2 + tx_len, userBuf, 0, rx_len);
            }
        }
    }
}
