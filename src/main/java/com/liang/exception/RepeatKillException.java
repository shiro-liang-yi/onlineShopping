package com.liang.exception;

/**
 * 重复秒杀异常，不需要我们手动去try catch
 */
public class RepeatKillException extends SecKillException {

    public RepeatKillException(String message){
        super(message);
    }

    public RepeatKillException(String message,Throwable cause){
        super(message,cause);
    }
}