package com.kingdee.qa.exception;

/**
 * 系统异常基类
 * 所有自定义异常都继承此类
 */
public class QASystemException extends RuntimeException {

    public QASystemException(String message) {
        super(message);
    }

    public QASystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
