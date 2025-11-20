package com.system.payment.util;


import com.system.payment.common.exception.CryptoException;
import com.system.payment.common.exception.ErrorCode;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class AesKeyCryptoUtils {

	public static String encryptPasswordWithAesKey(String password, String aesKey) {
		try {
			byte[] keyBytes = Arrays.copyOf(aesKey.getBytes(StandardCharsets.UTF_8), 16);
			SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

			byte[] iv = new byte[16];
			new SecureRandom().nextBytes(iv);
			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
			byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

			ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
			byteBuffer.put(iv);
			byteBuffer.put(encrypted);
			return Base64.getEncoder().encodeToString(byteBuffer.array());
		} catch (Exception e) {
			throw new CryptoException(ErrorCode.FAILED_TO_ENCRYPT_PASSWORD_WITH_AES_KEY);
		}
	}

	public static String decryptPasswordWithAesKey(String encryptedPassword, String aesKey) {
		try {
			byte[] allBytes = Base64.getDecoder().decode(encryptedPassword);
			ByteBuffer byteBuffer = ByteBuffer.wrap(allBytes);

			byte[] iv = new byte[16];
			byteBuffer.get(iv);
			byte[] encrypted = new byte[byteBuffer.remaining()];
			byteBuffer.get(encrypted);

			byte[] keyBytes = Arrays.copyOf(aesKey.getBytes(StandardCharsets.UTF_8), 16);
			SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
			byte[] decrypted = cipher.doFinal(encrypted);

			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new CryptoException(ErrorCode.FAILED_TO_DECRYPT_PASSWORD_WITH_AES_KEY);
		}
	}

}
