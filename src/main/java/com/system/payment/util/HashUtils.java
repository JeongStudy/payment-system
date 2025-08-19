package com.system.payment.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HashUtils {

    private static final String SHA_256 = "SHA-256";

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 해시 생성 실패: " + e.getMessage(), e);
        }
    }

	public static String sha512(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(d.length * 2);
			for (byte b : d) sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-512 not available", e);
		}
	}
}