package com.liang.exception;

/**
 * 秒杀结束出现这个异常
 */
public class SeckillCloseException extends SecKillException {

    public SeckillCloseException(String message){
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause){
        super(message, cause);
    }
}