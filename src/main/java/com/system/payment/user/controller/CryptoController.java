package com.system.payment.user.controller;

import com.system.payment.user.model.reponse.AesKeyResponse;
import com.system.payment.user.service.CryptoService;
import com.system.payment.util.Response;
import lombok.RequiredArgsConstructor;
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
				.status(200)
				.message("Success")
				.data(aesKey)
				.build();

		return ResponseEntity.ok(response);
	}
}