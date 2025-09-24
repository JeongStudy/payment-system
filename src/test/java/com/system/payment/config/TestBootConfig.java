package com.system.payment.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.system.payment")
@EnableJpaRepositories(basePackages = "com.system.payment")
@EnableJpaAuditing
public class TestBootConfig {
}