package com.healthagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.healthagent.mapper")
public class HealthAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthAgentApplication.class, args);
        System.out.println("智能健康助手启动成功");
	}

}
