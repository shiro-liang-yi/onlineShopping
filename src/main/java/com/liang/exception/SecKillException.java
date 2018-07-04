package com.liang.exception;

/**
 * 异常基类
 */
public class SecKillException extends RuntimeException {

    public SecKillException(String message){
        super(message);
    }

    public SecKillException(String message, Throwable cause){
        super(message,cause);
    }
}