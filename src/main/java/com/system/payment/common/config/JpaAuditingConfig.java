package com.system.payment.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
//@Profile("!test & !integration")
public class JpaAuditingConfig {

}