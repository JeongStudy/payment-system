package com.system.payment.user.controller;

import com.system.payment.user.model.reponse.AesKeyResponse;
import com.system.payment.user.model.reponse.RsaKeyResponse;
import com.system.payment.user.model.request.EncryptAesKeyRequest;
import com.system.payment.user.model.request.EncryptPasswordRequest;
import com.system.payment.user.service.CryptoService;
import com.system.payment.util.Response;
import com.system.payment.util.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment/crypto")
@RequiredArgsConstructor
public class CryptoController {

	private final CryptoService cryptoService;

	@PostMapping("/aes")
	public ResponseEntity<Response<AesKeyResponse>> generateAesKey() {
		AesKeyResponse aesKey = cryptoService.generateAesKey();
		return Response.created(aesKey, SuccessCode.AES_KEY_CREATED);
	}

	@PostMapping("/rsa")
	public ResponseEntity<Response<RsaKeyResponse>> generateRsaKey(){
		RsaKeyResponse rsaKey = cryptoService.generateRsaKey();
		return Response.created(rsaKey, SuccessCode.RAS_KEY_CREATED);
	}

	@PostMapping("/encrypt/password")
	public ResponseEntity<Response<String>> encryptPasswordWithAesKey(
			@RequestBody EncryptPasswordRequest encryptPasswordRequest){
		String encPassword =  cryptoService.encryptPasswordWithAesKey(encryptPasswordRequest);
		return Response.ok(encPassword, SuccessCode.OK);
	}

	@PostMapping("/encrypt/aes")
	public ResponseEntity<Response<String>> encryptAesKeyWithRsaPublicKey(
			@RequestBody EncryptAesKeyRequest encryptAesKeyRequest
	){
		String encAesKey = cryptoService.encryptAesKeyWithRsaPublicKey(encryptAesKeyRequest);
		return Response.ok(encAesKey, SuccessCode.OK);
	}
}