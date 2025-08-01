package com.system.payment.user.service;

import com.system.payment.user.domain.jaebin.AesKey;
import com.system.payment.user.model.reponse.AesKeyResponse;
import com.system.payment.user.repository.AesKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CryptoService {

	private final AesKeyRepository aesKeyRepository;

	@Transactional
	public AesKeyResponse generateAesKey() {
		String aesKeyStr;
		do {
			aesKeyStr = UUID.randomUUID().toString();
		} while (aesKeyRepository.existsByAesKey(aesKeyStr));

		AesKey aesKey = AesKey.create(aesKeyStr);
		aesKeyRepository.save(aesKey);

		return new AesKeyResponse(aesKey.getAesKey());
	}

}