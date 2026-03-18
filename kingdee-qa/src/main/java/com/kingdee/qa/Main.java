package com.kingdee.qa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 金蝶 WebAPI 智能问答系统 - Spring Boot 启动入口
 *
 * 启动后访问 http://localhost:8080 即可使用 Web 界面。
 * 健康检查： http://localhost:8080/actuator/health
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
