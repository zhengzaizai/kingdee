package com.kingdee.qa.exception;

/**
 * Skill 执行异常：技能加载失败、调用失败等
 */
public class SkillException extends QASystemException {

    public SkillException(String message) {
        super(message);
    }

    public SkillException(String message, Throwable cause) {
        super(message, cause);
    }
}
