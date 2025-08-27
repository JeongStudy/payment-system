package com.system.payment.runner;

import com.system.payment.user.repository.AesKeyRepository;
import com.system.payment.user.service.AuthService;
import com.system.payment.user.service.CryptoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
//@Order(Ordered.LOWEST_PRECEDENCE)
public class SqlInitRunner implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;

	@Value("${sql.init-sign-up-secret-sql:}")
	private String initSignUpSql;

	@Value("${sql.init-card-register-secret-sql:}")
	private String initCardRegisterSql;

	private static final Logger logger = LoggerFactory.getLogger(SqlInitRunner.class);

	@Override
    @Transactional
    public void run(ApplicationArguments args) {
        exec("sql.init-sign-up-secret-sql", initSignUpSql);
        exec("sql.init-card-register-secret-sql", initCardRegisterSql);
    }

    private void exec(String name, String sql) {
        if (sql == null || sql.isBlank()) {
            logger.info("[{}] 비어있어 실행 생략", name);
            return;
        }
        logger.info("[{}] 실행 시작", name);
        jdbcTemplate.execute(sql);
        logger.info("[{}] 실행 완료", name);
    }
}
