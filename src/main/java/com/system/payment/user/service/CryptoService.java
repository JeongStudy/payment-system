package com.system.payment.user.service;

import com.system.payment.exception.CryptoException;
import com.system.payment.exception.ErrorCode;
import com.system.payment.user.domain.jaebin.AesKey;
import com.system.payment.user.domain.jaebin.RsaKeyPair;
import com.system.payment.user.model.reponse.AesKeyResponse;
import com.system.payment.user.model.reponse.RsaKeyResponse;
import com.system.payment.user.repository.AesKeyRepository;
import com.system.payment.user.repository.RsaKeyPairRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CryptoService {

	private final AesKeyRepository aesKeyRepository;
	private final RsaKeyPairRepository rsaKeyPairRepository;


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

	public RsaKeyResponse generateRsaKey() {
		KeyPairGenerator keyGen = null;
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(ErrorCode.RSA_KEY_GENERATION_FAIL);
		}
		keyGen.initialize(2048);
		KeyPair pair = keyGen.generateKeyPair();

		String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
		String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

		RsaKeyPair rsaKeyPair = RsaKeyPair.create(publicKey, privateKey);
		rsaKeyPairRepository.save(rsaKeyPair);

		return new RsaKeyResponse(rsaKeyPair.getPublicKey());

	}

}