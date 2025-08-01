package com.system.payment.user.controller;

import com.system.payment.user.model.reponse.AesKeyResponse;
import com.system.payment.user.model.reponse.RsaKeyResponse;
import com.system.payment.user.service.CryptoService;
import com.system.payment.util.Response;
import com.system.payment.util.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment/crypto")
@RequiredArgsConstructor
public class CryptoController {

	private final CryptoService cryptoService;

	@PostMapping("/aes")
	public ResponseEntity<Response<AesKeyResponse>> generateAesKey() {
		AesKeyResponse aesKey = cryptoService.generateAesKey();
		Response<AesKeyResponse> response = Response.<AesKeyResponse>builder()
				.status(SuccessCode.AES_KEY_CREATED.getStatus())
				.message(SuccessCode.AES_KEY_CREATED.getMessage())
				.data(aesKey)
				.build();

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/rsa")
	public ResponseEntity<Response<RsaKeyResponse>> generateRsaKey(){
		RsaKeyResponse rsaKey = cryptoService.generateRsaKey();
		Response<RsaKeyResponse> response = Response.<RsaKeyResponse>builder()
				.status(SuccessCode.RAS_KEY_CREATED.getStatus())
				.message(SuccessCode.RAS_KEY_CREATED.getMessage())
				.data(rsaKey)
				.build();
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}