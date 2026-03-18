package com.kingdee.qa.exception;

/**
 * 配置异常：配置文件缺失、配置项无效等
 */
public class ConfigException extends QASystemException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
