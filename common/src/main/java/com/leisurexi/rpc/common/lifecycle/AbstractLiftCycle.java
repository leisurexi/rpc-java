package com.leisurexi.rpc.common.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: leisurexi
 * @date: 2020-08-13 11:15 上午
 */
public class AbstractLiftCycle implements LifeCycle {

    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    @Override
    public void startup() {
        if (isStarted.compareAndSet(false, true)) {
            return;
        }
        throw new IllegalStateException("this component has started");
    }

    @Override
    public void shutdown() {
        if (isStarted.compareAndSet(true, false)) {
            return;
        }
        throw new IllegalStateException("this component has closed");
    }

    @Override
    public boolean isStarted() {
        return isStarted.get();
    }

}
