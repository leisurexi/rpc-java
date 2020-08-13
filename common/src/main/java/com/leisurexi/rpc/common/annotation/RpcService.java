package com.leisurexi.rpc.common.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC annotation for RPC service
 *
 * @author: leisurexi
 * @date: 2020-08-12 3:48 下午
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface RpcService {

    /** 接口类 */
    Class<?> value();

    /** 版本号 */
    String version() default "";

}
