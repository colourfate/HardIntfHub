package com.example.hardintfhub;

import java.util.concurrent.atomic.AtomicReference;

public class SpinLock {
    private AtomicReference<Thread> cas = new AtomicReference<Thread>();

    public void lock() {
        Thread current = Thread.currentThread();
        while (!cas.compareAndSet(null, current)) {}
    }

    public void unlock() {
        Thread current = Thread.currentThread();
        cas.compareAndSet(current, null);
    }
}
