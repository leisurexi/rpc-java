package com.leisurexi.rpc.common.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: leisurexi
 * @date: 2020-08-13 11:07 上午
 */
public class ThreadPoolUtils {

    /**
     * 创建线程池
     *
     * @param threadName   线程名称
     * @param corePoolSize 核心线程数
     * @param maxPoolSize  最大线程数
     */
    public static ThreadPoolExecutor createThreadPool(final String threadName, int corePoolSize, int maxPoolSize) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000), r -> new Thread(r, "rpc-" + threadName + "-" + r.hashCode()), new ThreadPoolExecutor.AbortPolicy());
        return threadPoolExecutor;
    }

}
