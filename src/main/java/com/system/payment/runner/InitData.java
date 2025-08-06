package com.system.payment.runner;

import com.system.payment.user.repository.AesKeyRepository;
import com.system.payment.user.service.AuthService;
import com.system.payment.user.service.CryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitData implements ApplicationRunner {
    private final AuthService authService;
	private final CryptoService cryptoService;
	private final AesKeyRepository aesKeyRepository;

    @Override
    public void run(ApplicationArguments args) {

		//TODO insert
    }
}
