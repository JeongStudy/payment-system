package com.system.payment.payment.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class HealthCheckController {
	private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);

	@GetMapping("/health-check")
	public ResponseEntity<String> healthCheck() {
		logger.info("health check called");
		return ResponseEntity.ok("ok");
	}
}
