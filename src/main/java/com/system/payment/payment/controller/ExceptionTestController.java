package com.system.payment.payment.controller;

import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/payment/test")
@Slf4j
public class ExceptionTestController {

	@GetMapping("/business-error")
	public void businessError() {
		log.info("businessError start");
		throw new PaymentServerException(ErrorCode.USER_NOT_EXIST);
	}

	@PostMapping("/validation-error")
	public void validationError(@Valid @RequestBody TestDto dto) {
		// 바디 검증 실패시 자동으로 MethodArgumentNotValidException 발생
	}

	@GetMapping("/null-pointer")
	public void nullPointer() {
		log.info("nullPointer start");
		String str = null;
		str.length(); // NPE 발생
	}

	@GetMapping("/general-exception")
	public void generalException() {
		log.info("generalException start");
		throw new IllegalStateException("general-exception");
	}

	@Getter
	@Setter
	public static class TestDto {
		@NotBlank
		private String name;
	}
}
