package com.system.payment.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration              // ← 테스트 클래스패스에서 “유일한” 부트설정
@EnableAutoConfiguration
@EntityScan(basePackages = "com.system.payment")
@EnableJpaRepositories(basePackages = "com.system.payment")
@EnableJpaAuditing
public class TestBootConfig {
}