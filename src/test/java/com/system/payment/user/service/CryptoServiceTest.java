package com.system.payment.user.service;

import com.system.payment.user.domain.AesKey;
import com.system.payment.user.domain.RsaKeyPair;
import com.system.payment.user.model.reponse.AesKeyResponse;
import com.system.payment.user.model.reponse.RsaKeyResponse;
import com.system.payment.user.repository.AesKeyRepository;
import com.system.payment.user.repository.RsaKeyPairRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CryptoServiceTest {

	@Mock
	private AesKeyRepository aesKeyRepository;

	@Mock
	private RsaKeyPairRepository rsaKeyPairRepository;

	@InjectMocks
	private CryptoService cryptoService;

	private static final Logger logger = LoggerFactory.getLogger(CryptoServiceTest.class);

	@Test
	@DisplayName("AES 키 발급 단위 테스트 - 성공")
	void generateAesKey_success() {
		// given
		String uuid = UUID.randomUUID().toString();
		given(aesKeyRepository.existsByAesKey(anyString())).willReturn(false);

		// when
		AesKeyResponse response = cryptoService.generateAesKey();

		// then
		assertThat(response).isNotNull();
		assertThat(response.getAesKey()).isNotBlank();
		verify(aesKeyRepository).save(org.mockito.ArgumentMatchers.any(AesKey.class));

		logger.info("");
	}


	@Test
	@DisplayName("RSA 키 발급 단위 테스트 - 성공")
	void generateRsaKey_success() throws Exception {
		// when
		RsaKeyResponse response = cryptoService.generateRsaKey();

		// then
		assertThat(response).isNotNull();
		assertThat(response.getPublicKey()).isNotBlank();
		verify(rsaKeyPairRepository).save(org.mockito.ArgumentMatchers.any(RsaKeyPair.class));

		logger.info("RSA 공개키: {}", response.getPublicKey());
	}
}
