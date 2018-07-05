package com.liang.exception;

/**
 *  秒杀基础的异常
 * Created by liangshuai
 */
public class SeckillException extends RuntimeException {

    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
