package com.system.payment.payment.controller;

import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerBadRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment/test")
@Slf4j
public class ExceptionTestController {

	@GetMapping("/business-error")
	public void businessError() {
		log.info("businessError start");
		throw new PaymentServerBadRequestException(ErrorCode.BAD_REQUEST_PARAM);
	}

	@PostMapping("/validation-error")
	public void validationError(@Valid @RequestBody TestDto dto) {

	}

	@GetMapping("/null-pointer")
	public void nullPointer() {
		log.info("nullPointer start");
		String str = null;
		str.length();
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
