package com.leisurexi.rpc.common.codec;

/**
 * 心跳消息相关信息配置
 *
 * @author: leisurexi
 * @date: 2020-08-12 3:52 下午
 */
public final class HeartBeat {

    /** 发送心跳包的时间间隔 */
    public static final int HEART_BEAT_INTERVAL = 30;

    /** 心跳超时时间 */
    public static final int HEART_BEAT_TIMEOUT = 3 * HEART_BEAT_INTERVAL;

    /** 心跳包消息id */
    public static final String HEART_BEAT_ID = "BEAT_PING_PONG";

    public static RpcRequest HEART_BEAT_PING;

    static {
        HEART_BEAT_PING = new RpcRequest();
        HEART_BEAT_PING.setRequestId(HEART_BEAT_ID);
    }

}
