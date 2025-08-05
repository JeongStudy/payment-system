package com.system.payment.payment.controller;

import com.system.payment.payment.model.response.ExampleResponse;
import com.system.payment.payment.service.ExampleService;
import com.system.payment.util.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payment")
public class ExampleController {
	private static final Logger logger = LoggerFactory.getLogger(ExampleController.class);

	private final ExampleService exampleService;

	public ExampleController(ExampleService exampleService) {
		this.exampleService = exampleService;
	}

	@GetMapping("/example")
	public ResponseEntity<Response<List<ExampleResponse>>> healthCheck() {
		List<ExampleResponse> list = exampleService.findAll();

		Response<List<ExampleResponse>> response = Response.<List<ExampleResponse>>builder()
				.status(200)
				.message("Success")
				.data(list)
				.build();

		return ResponseEntity.ok(response);
	}
}
