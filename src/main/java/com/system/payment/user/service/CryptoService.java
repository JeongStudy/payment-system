package com.system.payment.user.service;

import com.system.payment.common.exception.CryptoException;
import com.system.payment.common.dto.response.ErrorCode;
import com.system.payment.user.domain.AesKey;
import com.system.payment.user.domain.RsaKeyPair;
import com.system.payment.user.model.reponse.AesKeyResponse;
import com.system.payment.user.model.reponse.RsaKeyResponse;
import com.system.payment.user.model.request.EncryptAesKeyRequest;
import com.system.payment.user.model.request.EncryptPasswordRequest;
import com.system.payment.user.repository.AesKeyRepository;
import com.system.payment.user.repository.RsaKeyPairRepository;
import com.system.payment.common.util.AesKeyCryptoUtils;
import com.system.payment.common.util.RsaKeyCryptoUtils;
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

		return AesKeyResponse.from(aesKey);
	}

	@Transactional
	public RsaKeyResponse generateRsaKey() {
		KeyPairGenerator keyGen = null;
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(ErrorCode.RSA_KEY_PAIR_GENERATION_FAIL);
		}

		keyGen.initialize(2048);
		KeyPair pair = keyGen.generateKeyPair();

		String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
		String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

		RsaKeyPair rsaKeyPair = RsaKeyPair.create(publicKey, privateKey);
		rsaKeyPairRepository.save(rsaKeyPair);

		return RsaKeyResponse.from(rsaKeyPair);

	}

	@Transactional
    public AesKey resolveValidAesKey(String rsaPublicKey, String encAesKey) {
        RsaKeyPair rsaKeyPair = rsaKeyPairRepository.getByPublicKeyOrThrow(rsaPublicKey);
        rsaKeyPair.validateNotExpired();

        String aesKeyPlain = RsaKeyCryptoUtils
                .decryptEncryptedAesKeyWithRsaPrivateKey(encAesKey, rsaKeyPair.getPrivateKey());

        AesKey aesKey = aesKeyRepository.getByAesKeyOrThrow(aesKeyPlain);
		aesKey.validateNotExpired();

        return aesKey;
    }

    public String decryptPasswordWithAes(String encPassword, String aesKeyPlain) {
        return AesKeyCryptoUtils.decryptPasswordWithAesKey(encPassword, aesKeyPlain);
    }

	public String encryptPasswordWithAesKey(EncryptPasswordRequest encryptPasswordRequest){
		return AesKeyCryptoUtils.encryptPasswordWithAesKey(encryptPasswordRequest.getPassword(), encryptPasswordRequest.getAesKey());
	}

	public String encryptAesKeyWithRsaPublicKey(EncryptAesKeyRequest encryptAesKeyRequest){
		return RsaKeyCryptoUtils.encryptAesKeyWithRsaPublicKey(encryptAesKeyRequest.getAesKey(), encryptAesKeyRequest.getRsaPublicKey());
	}
}