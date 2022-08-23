package com.example.intfdefine;

abstract class HardIntfEvent {
    private volatile boolean mIsHandled = false;
    private int mUserRet = 0;

    abstract int userHandle(byte[] receivePacket);
    public void handle(byte[] receivePacket) {
        mUserRet = userHandle(receivePacket);
        mIsHandled = true;
    }

    public int getUserRet() {
        // FIXME: 添加超时处理
        while (!mIsHandled);
        return mUserRet;
    }
}
